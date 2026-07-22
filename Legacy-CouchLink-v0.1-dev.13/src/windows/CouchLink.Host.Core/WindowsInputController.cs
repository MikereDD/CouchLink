using System.ComponentModel;
using System.Runtime.InteropServices;

namespace CouchLink.Host.Core;

internal sealed class WindowsInputController
{
    private readonly object _sync = new();
    private bool _leftDown;
    private bool _rightDown;

    public void MoveRelative(int deltaX, int deltaY)
    {
        deltaX = Math.Clamp(deltaX, -HostConstants.MaximumRelativeMouseDelta, HostConstants.MaximumRelativeMouseDelta);
        deltaY = Math.Clamp(deltaY, -HostConstants.MaximumRelativeMouseDelta, HostConstants.MaximumRelativeMouseDelta);
        if (deltaX == 0 && deltaY == 0) return;
        SendMouse((uint)MouseEventFlags.Move, deltaX, deltaY, 0);
    }

    public void Scroll(int delta)
    {
        delta = Math.Clamp(delta, -HostConstants.MaximumScrollDelta, HostConstants.MaximumScrollDelta);
        if (delta == 0) return;
        SendMouse((uint)MouseEventFlags.Wheel, 0, 0, unchecked((uint)delta));
    }

    public void Button(string button, string action)
    {
        string normalizedButton = button.Trim().ToLowerInvariant();
        string normalizedAction = action.Trim().ToLowerInvariant();
        lock (_sync)
        {
            if (normalizedButton == "left")
                ApplyButton(ref _leftDown, normalizedAction, MouseEventFlags.LeftDown, MouseEventFlags.LeftUp);
            else if (normalizedButton == "right")
                ApplyButton(ref _rightDown, normalizedAction, MouseEventFlags.RightDown, MouseEventFlags.RightUp);
        }
    }

    public void SendText(string text)
    {
        if (string.IsNullOrEmpty(text)) return;
        foreach (char character in text.Take(HostConstants.MaximumKeyboardTextLength))
        {
            SendKeyboardUnicode(character, keyUp: false);
            SendKeyboardUnicode(character, keyUp: true);
        }
    }

    public void PressKey(string key)
    {
        ushort virtualKey = key.Trim().ToLowerInvariant() switch
        {
            "enter" => 0x0D,
            "escape" or "esc" => 0x1B,
            "backspace" => 0x08,
            "tab" => 0x09,
            "delete" => 0x2E,
            "left" => 0x25,
            "up" => 0x26,
            "right" => 0x27,
            "down" => 0x28,
            "home" => 0x24,
            "end" => 0x23,
            _ => (ushort)0,
        };
        if (virtualKey == 0) return;
        SendKeyboardVirtualKey(virtualKey, keyUp: false);
        SendKeyboardVirtualKey(virtualKey, keyUp: true);
    }


    public void PressShortcut(string shortcut)
    {
        ushort[] keys = shortcut.Trim().ToLowerInvariant() switch
        {
            "alt_tab" => [0x12, 0x09],
            "show_desktop" => [0x5B, 0x44],
            "task_manager" => [0x11, 0x10, 0x1B],
            "close_window" => [0x12, 0x73],
            "game_bar" => [0x5B, 0x47],
            _ => [],
        };
        if (keys.Length == 0) return;
        foreach (ushort key in keys) SendKeyboardVirtualKey(key, keyUp: false);
        for (int index = keys.Length - 1; index >= 0; index--) SendKeyboardVirtualKey(keys[index], keyUp: true);
    }

    public void ReleaseAll()
    {
        lock (_sync)
        {
            if (_leftDown)
            {
                SendMouse((uint)MouseEventFlags.LeftUp, 0, 0, 0);
                _leftDown = false;
            }
            if (_rightDown)
            {
                SendMouse((uint)MouseEventFlags.RightUp, 0, 0, 0);
                _rightDown = false;
            }
        }
    }

    private void ApplyButton(ref bool isDown, string action, MouseEventFlags downFlag, MouseEventFlags upFlag)
    {
        switch (action)
        {
            case "down" when !isDown:
                SendMouse((uint)downFlag, 0, 0, 0);
                isDown = true;
                break;
            case "up" when isDown:
                SendMouse((uint)upFlag, 0, 0, 0);
                isDown = false;
                break;
            case "click":
                SendMouse((uint)downFlag, 0, 0, 0);
                SendMouse((uint)upFlag, 0, 0, 0);
                isDown = false;
                break;
        }
    }

    private static void SendMouse(uint flags, int dx, int dy, uint mouseData)
    {
        INPUT[] inputs =
        [
            new INPUT
            {
                type = InputType.Mouse,
                union = new InputUnion
                {
                    mouse = new MOUSEINPUT { dx = dx, dy = dy, mouseData = mouseData, dwFlags = flags },
                },
            },
        ];
        Send(inputs);
    }

    private static void SendKeyboardUnicode(char character, bool keyUp)
    {
        INPUT[] inputs =
        [
            new INPUT
            {
                type = InputType.Keyboard,
                union = new InputUnion
                {
                    keyboard = new KEYBDINPUT
                    {
                        wScan = character,
                        dwFlags = (uint)(KeyboardEventFlags.Unicode | (keyUp ? KeyboardEventFlags.KeyUp : 0)),
                    },
                },
            },
        ];
        Send(inputs);
    }

    private static void SendKeyboardVirtualKey(ushort virtualKey, bool keyUp)
    {
        INPUT[] inputs =
        [
            new INPUT
            {
                type = InputType.Keyboard,
                union = new InputUnion
                {
                    keyboard = new KEYBDINPUT
                    {
                        wVk = virtualKey,
                        dwFlags = keyUp ? (uint)KeyboardEventFlags.KeyUp : 0,
                    },
                },
            },
        ];
        Send(inputs);
    }

    private static void Send(INPUT[] inputs)
    {
        if (SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>()) == 0)
            throw new Win32Exception(Marshal.GetLastWin32Error(), "Windows rejected the CouchLink input event.");
    }

    private enum InputType : uint { Mouse = 0, Keyboard = 1 }

    [Flags]
    private enum MouseEventFlags : uint
    {
        Move = 0x0001, LeftDown = 0x0002, LeftUp = 0x0004,
        RightDown = 0x0008, RightUp = 0x0010, Wheel = 0x0800,
    }

    [Flags]
    private enum KeyboardEventFlags : uint { KeyUp = 0x0002, Unicode = 0x0004 }

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT { public InputType type; public InputUnion union; }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public MOUSEINPUT mouse;
        [FieldOffset(0)] public KEYBDINPUT keyboard;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MOUSEINPUT
    {
        public int dx; public int dy; public uint mouseData; public uint dwFlags; public uint time; public nint dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk; public ushort wScan; public uint dwFlags; public uint time; public nint dwExtraInfo;
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint numberOfInputs, INPUT[] inputs, int sizeOfInputStructure);
}
