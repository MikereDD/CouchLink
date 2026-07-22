using System.Runtime.InteropServices;
using Microsoft.Win32.SafeHandles;

namespace CouchLink.BootService;

/// <summary>
/// User-mode bridge to the CouchLink Virtual HID kernel driver.
///
/// Locates the driver's device interface, opens it, and translates CouchLink's
/// high-level input messages (relative moves, buttons, scroll, text, keys) into
/// the tiny fixed-size HID reports the driver accepts. Because those reports
/// travel through the real HID stack, they reach the Winlogon secure desktop —
/// which User32 SendInput from Session 0 cannot do.
///
/// If the driver is not installed/running, <see cref="IsAvailable"/> is false and
/// every input method is a no-op. The Boot Service then behaves exactly like the
/// status-only baseline, so nothing breaks when the driver is absent.
/// </summary>
internal sealed class VirtualHidBridge : IDisposable
{
    // Mirror of COUCHLINK_HID_SUBMIT / report IDs from CouchLinkHidProtocol.h.
    private const byte ProtocolVersion = 1;
    private const byte KindMouse = 1;
    private const byte KindKeyboard = 2;
    private const byte MousePayloadSize = 6;
    private const byte KeyboardPayloadSize = 8;

    // {6B2C4E10-3B9A-4C2D-9E7F-434C00000001}
    private static readonly Guid CouchLinkVhidInterface =
        new(0x6b2c4e10, 0x3b9a, 0x4c2d, 0x9e, 0x7f, 0x43, 0x4c, 0x00, 0x00, 0x00, 0x01);

    // CTL_CODE(0x8042, 0x800, METHOD_BUFFERED=0, FILE_WRITE_ACCESS=2)
    private const uint IoctlSubmitReport = (0x8042u << 16) | (0x2u << 14) | (0x800u << 2) | 0u;

    private readonly ILogger _logger;
    private readonly object _sync = new();
    private SafeFileHandle? _device;

    // Live mouse-button state so press/release/click map to correct HID masks.
    private byte _mouseButtons;

    public VirtualHidBridge(ILogger logger)
    {
        _logger = logger;
        TryOpen();
    }

    public bool IsAvailable => _device is { IsInvalid: false, IsClosed: false };

    /// <summary>Attempt to (re)open the driver. Safe to call repeatedly.</summary>
    public void TryOpen()
    {
        lock (_sync)
        {
            if (IsAvailable) return;
            _device?.Dispose();
            _device = null;

            string? path = FindDevicePath();
            if (path is null)
            {
                _logger.LogInformation("CouchLink Virtual HID driver not present; pre-login input stays disabled.");
                return;
            }

            SafeFileHandle handle = CreateFile(
                path,
                GENERIC_WRITE,
                FILE_SHARE_READ | FILE_SHARE_WRITE,
                IntPtr.Zero,
                OPEN_EXISTING,
                0,
                IntPtr.Zero);

            if (handle.IsInvalid)
            {
                _logger.LogWarning("Found Virtual HID device but could not open it (Win32 {Error}).", Marshal.GetLastWin32Error());
                handle.Dispose();
                return;
            }

            _device = handle;
            _logger.LogInformation("CouchLink Virtual HID driver connected. Pre-login pointer and keyboard are available.");
        }
    }

    // ---- High-level input surface (mirrors WindowsInputController) ----

    public void MoveRelative(int deltaX, int deltaY)
    {
        deltaX = Math.Clamp(deltaX, short.MinValue, short.MaxValue);
        deltaY = Math.Clamp(deltaY, short.MinValue, short.MaxValue);
        if (deltaX == 0 && deltaY == 0) return;
        SubmitMouse(_mouseButtons, (short)deltaX, (short)deltaY, 0);
    }

    public void Scroll(int delta)
    {
        delta = Math.Clamp(delta, sbyte.MinValue, sbyte.MaxValue);
        if (delta == 0) return;
        SubmitMouse(_mouseButtons, 0, 0, (sbyte)delta);
    }

    public void Button(string button, string action)
    {
        byte mask = button.Trim().ToLowerInvariant() switch
        {
            "left" => 0x01,
            "right" => 0x02,
            "middle" => 0x04,
            _ => 0x00
        };
        if (mask == 0) return;

        lock (_sync)
        {
            switch (action.Trim().ToLowerInvariant())
            {
                case "down":
                    _mouseButtons |= mask;
                    SubmitMouse(_mouseButtons, 0, 0, 0);
                    break;
                case "up":
                    _mouseButtons &= (byte)~mask;
                    SubmitMouse(_mouseButtons, 0, 0, 0);
                    break;
                case "click":
                    SubmitMouse((byte)(_mouseButtons | mask), 0, 0, 0);
                    SubmitMouse(_mouseButtons, 0, 0, 0);
                    break;
            }
        }
    }

    public void SendText(string text)
    {
        if (string.IsNullOrEmpty(text)) return;
        foreach (char character in text)
        {
            if (!HidKeyMap.TryMap(character, out byte usage, out bool shift)) continue;
            byte modifiers = shift ? HidKeyMap.LeftShift : (byte)0;
            SubmitKeyboard(modifiers, usage);   // key down
            SubmitKeyboard(0, 0);               // key up (all released)
        }
    }

    public void PressKey(string key)
    {
        byte usage = key.Trim().ToLowerInvariant() switch
        {
            "enter" => 0x28,
            "escape" or "esc" => 0x29,
            "backspace" => 0x2A,
            "tab" => 0x2B,
            "space" => 0x2C,
            "delete" => 0x4C,
            "right" => 0x4F,
            "left" => 0x50,
            "down" => 0x51,
            "up" => 0x52,
            "home" => 0x4A,
            "end" => 0x4D,
            _ => 0x00
        };
        if (usage == 0) return;
        SubmitKeyboard(0, usage);
        SubmitKeyboard(0, 0);
    }

    public void PressShortcut(string shortcut)
    {
        (byte modifiers, byte usage) = shortcut.Trim().ToLowerInvariant() switch
        {
            "alt_tab" => (HidKeyMap.LeftAlt, (byte)0x2B),        // Alt+Tab
            "close_window" => (HidKeyMap.LeftAlt, (byte)0x3D),   // Alt+F4
            "task_manager" => ((byte)(HidKeyMap.LeftCtrl | HidKeyMap.LeftShift), (byte)0x29), // Ctrl+Shift+Esc
            "show_desktop" => (HidKeyMap.LeftGui, (byte)0x07),   // Win+D
            _ => ((byte)0, (byte)0)
        };
        if (usage == 0 && modifiers == 0) return;
        SubmitKeyboard(modifiers, usage);
        SubmitKeyboard(0, 0);
    }

    public void ReleaseAll()
    {
        lock (_sync)
        {
            _mouseButtons = 0;
            SubmitMouse(0, 0, 0, 0);
            SubmitKeyboard(0, 0);
        }
    }

    // ---- Report submission ----

    private void SubmitMouse(byte buttons, short dx, short dy, sbyte wheel)
    {
        Span<byte> payload = stackalloc byte[MousePayloadSize];
        payload[0] = buttons;
        payload[1] = (byte)(dx & 0xFF);
        payload[2] = (byte)((dx >> 8) & 0xFF);
        payload[3] = (byte)(dy & 0xFF);
        payload[4] = (byte)((dy >> 8) & 0xFF);
        payload[5] = (byte)wheel;
        Submit(KindMouse, MousePayloadSize, payload);
    }

    private void SubmitKeyboard(byte modifiers, byte usage)
    {
        Span<byte> payload = stackalloc byte[KeyboardPayloadSize];
        payload[0] = modifiers;   // modifier byte
        payload[1] = 0;           // reserved
        payload[2] = usage;       // first keycode (0 = none)
        // payload[3..7] left zero: single-key model is enough for typing + shortcuts
        Submit(KindKeyboard, KeyboardPayloadSize, payload);
    }

    private void Submit(byte kind, byte payloadLength, ReadOnlySpan<byte> payload)
    {
        // Preserve report order and prevent one thread from disposing the handle
        // while another thread is inside DeviceIoControl. Monitor locks are
        // re-entrant, so Button/ReleaseAll may safely call this while holding _sync.
        lock (_sync)
        {
            SafeFileHandle? device = _device;
            if (device is null || device.IsInvalid || device.IsClosed) return;

            // COUCHLINK_HID_SUBMIT: Version, Kind, PayloadLength, Reserved, Payload[8] = 12 bytes.
            byte[] submit = new byte[4 + 8];
            submit[0] = ProtocolVersion;
            submit[1] = kind;
            submit[2] = payloadLength;
            submit[3] = 0;
            payload.Slice(0, payloadLength).CopyTo(submit.AsSpan(4, payloadLength));

            bool ok = DeviceIoControl(device, IoctlSubmitReport, submit, (uint)submit.Length,
                IntPtr.Zero, 0, out _, IntPtr.Zero);

            if (!ok)
            {
                int error = Marshal.GetLastWin32Error();
                _logger.LogDebug("Virtual HID submit failed (Win32 {Error}); reopening device.", error);
                _device.Dispose();
                _device = null;
            }
        }
    }

    // ---- Device discovery via SetupAPI ----

    private static string? FindDevicePath()
    {
        IntPtr set = SetupDiGetClassDevs(
            in CouchLinkVhidInterface, null, IntPtr.Zero,
            DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
        if (set == INVALID_HANDLE_VALUE) return null;

        try
        {
            var data = new SP_DEVICE_INTERFACE_DATA { cbSize = (uint)Marshal.SizeOf<SP_DEVICE_INTERFACE_DATA>() };
            if (!SetupDiEnumDeviceInterfaces(set, IntPtr.Zero, in CouchLinkVhidInterface, 0, ref data))
                return null;

            SetupDiGetDeviceInterfaceDetail(set, ref data, IntPtr.Zero, 0, out uint required, IntPtr.Zero);
            if (required == 0) return null;

            IntPtr detail = Marshal.AllocHGlobal((int)required);
            try
            {
                // cbSize of SP_DEVICE_INTERFACE_DETAIL_DATA_W is 8 on x64, 6 on x86.
                Marshal.WriteInt32(detail, IntPtr.Size == 8 ? 8 : 6);
                if (!SetupDiGetDeviceInterfaceDetail(set, ref data, detail, required, out _, IntPtr.Zero))
                    return null;
                return Marshal.PtrToStringUni(detail + 4);
            }
            finally
            {
                Marshal.FreeHGlobal(detail);
            }
        }
        finally
        {
            SetupDiDestroyDeviceInfoList(set);
        }
    }

    public void Dispose()
    {
        lock (_sync)
        {
            _device?.Dispose();
            _device = null;
        }
    }

    // ---- P/Invoke ----

    private const uint GENERIC_WRITE = 0x40000000;
    private const uint FILE_SHARE_READ = 0x1;
    private const uint FILE_SHARE_WRITE = 0x2;
    private const uint OPEN_EXISTING = 3;
    private const uint DIGCF_PRESENT = 0x2;
    private const uint DIGCF_DEVICEINTERFACE = 0x10;
    private static readonly IntPtr INVALID_HANDLE_VALUE = new(-1);

    [StructLayout(LayoutKind.Sequential)]
    private struct SP_DEVICE_INTERFACE_DATA
    {
        public uint cbSize;
        public Guid InterfaceClassGuid;
        public uint Flags;
        public IntPtr Reserved;
    }

    [DllImport("setupapi.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr SetupDiGetClassDevs(in Guid classGuid, string? enumerator, IntPtr hwndParent, uint flags);

    [DllImport("setupapi.dll", SetLastError = true)]
    private static extern bool SetupDiEnumDeviceInterfaces(IntPtr deviceInfoSet, IntPtr deviceInfoData,
        in Guid interfaceClassGuid, uint memberIndex, ref SP_DEVICE_INTERFACE_DATA deviceInterfaceData);

    [DllImport("setupapi.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool SetupDiGetDeviceInterfaceDetail(IntPtr deviceInfoSet, ref SP_DEVICE_INTERFACE_DATA deviceInterfaceData,
        IntPtr deviceInterfaceDetailData, uint detailSize, out uint requiredSize, IntPtr deviceInfoData);

    [DllImport("setupapi.dll", SetLastError = true)]
    private static extern bool SetupDiDestroyDeviceInfoList(IntPtr deviceInfoSet);

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern SafeFileHandle CreateFile(string fileName, uint desiredAccess, uint shareMode,
        IntPtr securityAttributes, uint creationDisposition, uint flagsAndAttributes, IntPtr templateFile);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool DeviceIoControl(SafeFileHandle device, uint ioControlCode,
        [In] byte[] inBuffer, uint inBufferSize, IntPtr outBuffer, uint outBufferSize,
        out uint bytesReturned, IntPtr overlapped);
}

/// <summary>ASCII → HID Usage (Keyboard/Keypad page 0x07) with shift flag.</summary>
internal static class HidKeyMap
{
    public const byte LeftCtrl = 0x01;
    public const byte LeftShift = 0x02;
    public const byte LeftAlt = 0x04;
    public const byte LeftGui = 0x08;

    public static bool TryMap(char c, out byte usage, out bool shift)
    {
        shift = false;
        usage = 0;

        if (c is >= 'a' and <= 'z') { usage = (byte)(0x04 + (c - 'a')); return true; }
        if (c is >= 'A' and <= 'Z') { usage = (byte)(0x04 + (c - 'A')); shift = true; return true; }
        if (c is >= '1' and <= '9') { usage = (byte)(0x1E + (c - '1')); return true; }
        if (c == '0') { usage = 0x27; return true; }

        switch (c)
        {
            case ' ': usage = 0x2C; return true;
            case '\n': case '\r': usage = 0x28; return true;
            case '\t': usage = 0x2B; return true;
            case '-': usage = 0x2D; return true;
            case '=': usage = 0x2E; return true;
            case '[': usage = 0x2F; return true;
            case ']': usage = 0x30; return true;
            case '\\': usage = 0x31; return true;
            case ';': usage = 0x33; return true;
            case '\'': usage = 0x34; return true;
            case '`': usage = 0x35; return true;
            case ',': usage = 0x36; return true;
            case '.': usage = 0x37; return true;
            case '/': usage = 0x38; return true;

            // shifted symbols
            case '!': usage = 0x1E; shift = true; return true;
            case '@': usage = 0x1F; shift = true; return true;
            case '#': usage = 0x20; shift = true; return true;
            case '$': usage = 0x21; shift = true; return true;
            case '%': usage = 0x22; shift = true; return true;
            case '^': usage = 0x23; shift = true; return true;
            case '&': usage = 0x24; shift = true; return true;
            case '*': usage = 0x25; shift = true; return true;
            case '(': usage = 0x26; shift = true; return true;
            case ')': usage = 0x27; shift = true; return true;
            case '_': usage = 0x2D; shift = true; return true;
            case '+': usage = 0x2E; shift = true; return true;
            case '{': usage = 0x2F; shift = true; return true;
            case '}': usage = 0x30; shift = true; return true;
            case '|': usage = 0x31; shift = true; return true;
            case ':': usage = 0x33; shift = true; return true;
            case '"': usage = 0x34; shift = true; return true;
            case '~': usage = 0x35; shift = true; return true;
            case '<': usage = 0x36; shift = true; return true;
            case '>': usage = 0x37; shift = true; return true;
            case '?': usage = 0x38; shift = true; return true;
            default: return false;
        }
    }
}
