using System.Runtime.InteropServices;

namespace CouchLink.Host.Core;

internal sealed class AudioOutputController
{
    public AudioOutputListResult List()
    {
        var devices = new List<AudioOutputDevice>();
        IMMDeviceEnumerator? enumerator = null;
        IMMDeviceCollection? collection = null;
        IMMDevice? defaultDevice = null;
        try
        {
            enumerator = (IMMDeviceEnumerator)new MMDeviceEnumeratorComObject();
            Marshal.ThrowExceptionForHR(enumerator.EnumAudioEndpoints(EDataFlow.Render, DeviceState.Active, out collection));
            string? defaultId = null;
            if (enumerator.GetDefaultAudioEndpoint(EDataFlow.Render, ERole.Multimedia, out defaultDevice) == 0)
                defaultDevice.GetId(out defaultId);

            collection.GetCount(out uint count);
            for (uint i = 0; i < count; i++)
            {
                collection.Item(i, out IMMDevice device);
                try
                {
                    device.GetId(out string id);
                    string name = ReadFriendlyName(device) ?? id;
                    devices.Add(new AudioOutputDevice(id, name, string.Equals(id, defaultId, StringComparison.OrdinalIgnoreCase)));
                }
                finally { Marshal.ReleaseComObject(device); }
            }

            return new AudioOutputListResult(true, devices.OrderBy(d => d.Name, StringComparer.OrdinalIgnoreCase).ToArray(), null);
        }
        catch (Exception ex)
        {
            return new AudioOutputListResult(false, Array.Empty<AudioOutputDevice>(), ex.Message);
        }
        finally
        {
            if (defaultDevice is not null) Marshal.ReleaseComObject(defaultDevice);
            if (collection is not null) Marshal.ReleaseComObject(collection);
            if (enumerator is not null) Marshal.ReleaseComObject(enumerator);
        }
    }

    public AudioOutputSetResult SetDefault(string endpointId)
    {
        if (string.IsNullOrWhiteSpace(endpointId))
            return new AudioOutputSetResult(false, endpointId, null, "Audio endpoint ID is required.");

        var available = List();
        AudioOutputDevice? target = available.Devices.FirstOrDefault(d =>
            string.Equals(d.Id, endpointId, StringComparison.OrdinalIgnoreCase));
        if (target is null)
            return new AudioOutputSetResult(false, endpointId, null, "The selected audio output is unavailable.");

        IPolicyConfig? policy = null;
        try
        {
            policy = (IPolicyConfig)new PolicyConfigClient();
            foreach (ERole role in new[] { ERole.Console, ERole.Multimedia, ERole.Communications })
                Marshal.ThrowExceptionForHR(policy.SetDefaultEndpoint(endpointId, role));
            return new AudioOutputSetResult(true, endpointId, target.Name, $"Audio switched to {target.Name}.");
        }
        catch (Exception ex)
        {
            return new AudioOutputSetResult(false, endpointId, target.Name, $"Unable to switch audio: {ex.Message}");
        }
        finally
        {
            if (policy is not null) Marshal.ReleaseComObject(policy);
        }
    }

    private static string? ReadFriendlyName(IMMDevice device)
    {
        device.OpenPropertyStore(0, out IPropertyStore store);
        try
        {
            var key = PropertyKey.DeviceFriendlyName;
            store.GetValue(ref key, out PropVariant value);
            try { return value.GetString(); }
            finally { value.Clear(); }
        }
        finally { Marshal.ReleaseComObject(store); }
    }

    [Flags] private enum DeviceState : uint { Active = 0x1 }
    private enum EDataFlow { Render, Capture, All }
    private enum ERole { Console, Multimedia, Communications }

    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    private class MMDeviceEnumeratorComObject { }

    [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("A95664D2-9614-4F35-A746-DE8DB63617E6")]
    private interface IMMDeviceEnumerator
    {
        [PreserveSig] int EnumAudioEndpoints(EDataFlow dataFlow, DeviceState stateMask, out IMMDeviceCollection devices);
        [PreserveSig] int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice endpoint);
        [PreserveSig] int GetDevice([MarshalAs(UnmanagedType.LPWStr)] string id, out IMMDevice device);
        [PreserveSig] int RegisterEndpointNotificationCallback(IntPtr client);
        [PreserveSig] int UnregisterEndpointNotificationCallback(IntPtr client);
    }

    [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("0BD7A1BE-7A1A-44DB-8397-C0A7A8B22E79")]
    private interface IMMDeviceCollection
    {
        [PreserveSig] int GetCount(out uint count);
        [PreserveSig] int Item(uint index, out IMMDevice device);
    }

    [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("D666063F-1587-4E43-81F1-B948E807363F")]
    private interface IMMDevice
    {
        [PreserveSig] int Activate(ref Guid iid, uint clsCtx, IntPtr activationParams, out IntPtr interfacePointer);
        [PreserveSig] int OpenPropertyStore(uint accessMode, out IPropertyStore properties);
        [PreserveSig] int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);
        [PreserveSig] int GetState(out DeviceState state);
    }

    [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99")]
    private interface IPropertyStore
    {
        [PreserveSig] int GetCount(out uint count);
        [PreserveSig] int GetAt(uint index, out PropertyKey key);
        [PreserveSig] int GetValue(ref PropertyKey key, out PropVariant value);
        [PreserveSig] int SetValue(ref PropertyKey key, ref PropVariant value);
        [PreserveSig] int Commit();
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct PropertyKey
    {
        public Guid FormatId;
        public uint PropertyId;
        public static PropertyKey DeviceFriendlyName => new()
        {
            FormatId = new Guid("A45C254E-DF1C-4EFD-8020-67D146A850E0"),
            PropertyId = 14,
        };
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct PropVariant
    {
        [FieldOffset(0)] private ushort valueType;
        [FieldOffset(8)] private IntPtr pointerValue;
        public string? GetString() => valueType == 31 && pointerValue != IntPtr.Zero ? Marshal.PtrToStringUni(pointerValue) : null;
        public void Clear() => PropVariantClear(ref this);
    }

    [DllImport("ole32.dll")]
    private static extern int PropVariantClear(ref PropVariant value);

    [ComImport, Guid("870AF99C-171D-4F9E-AF0D-E63DF40C2BC9")]
    private class PolicyConfigClient { }

    [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("F8679F50-850A-41CF-9C72-430F290290C8")]
    private interface IPolicyConfig
    {
        [PreserveSig] int GetMixFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceId, out IntPtr format);
        [PreserveSig] int GetDeviceFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int defaultFormat, out IntPtr format);
        [PreserveSig] int ResetDeviceFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceId);
        [PreserveSig] int SetDeviceFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceId, IntPtr endpointFormat, IntPtr mixFormat);
        [PreserveSig] int GetProcessingPeriod([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int defaultPeriod, out long defaultValue, out long minimumValue);
        [PreserveSig] int SetProcessingPeriod([MarshalAs(UnmanagedType.LPWStr)] string deviceId, ref long period);
        [PreserveSig] int GetShareMode([MarshalAs(UnmanagedType.LPWStr)] string deviceId, IntPtr mode);
        [PreserveSig] int SetShareMode([MarshalAs(UnmanagedType.LPWStr)] string deviceId, IntPtr mode);
        [PreserveSig] int GetPropertyValue([MarshalAs(UnmanagedType.LPWStr)] string deviceId, ref PropertyKey key, out PropVariant value);
        [PreserveSig] int SetPropertyValue([MarshalAs(UnmanagedType.LPWStr)] string deviceId, ref PropertyKey key, ref PropVariant value);
        [PreserveSig] int SetDefaultEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, ERole role);
        [PreserveSig] int SetEndpointVisibility([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int visible);
    }
}

internal sealed record AudioOutputDevice(string Id, string Name, bool IsDefault);
internal sealed record AudioOutputListResult(bool Success, AudioOutputDevice[] Devices, string? Error);
internal sealed record AudioOutputSetResult(bool Success, string EndpointId, string? Name, string Message);
