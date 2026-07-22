//
// CouchLink Virtual HID source driver (KMDF + Microsoft Virtual HID Framework).
//
// Exposes ONE virtual HID device with two collections — a relative-motion
// mouse and a boot-compatible keyboard — and injects input reports that reach
// the interactive/secure desktop through the real HID stack. Reports arrive
// only via IOCTL_COUCHLINK_VHID_SUBMIT_REPORT, carrying a fixed-size, version-
// checked COUCHLINK_HID_SUBMIT. Nothing else is accepted.
//
// SAFETY: this is a kernel component. Build and validate it ONLY inside a
// disposable, test-signing VM before it goes anywhere near a real host. See
// BUILD-SIGN-TEST.md.
//

#include <initguid.h>
#include "Driver.h"

#ifdef ALLOC_PRAGMA
#pragma alloc_text(INIT, DriverEntry)
#pragma alloc_text(PAGE, CouchLinkEvtDeviceAdd)
#pragma alloc_text(PAGE, CouchLinkEvtDeviceCleanup)
#pragma alloc_text(PAGE, CouchLinkVhfInitialize)
#endif

//
// HID report descriptor: mouse (Report ID 1) + keyboard (Report ID 2).
//   Mouse    input report body = 6 bytes: buttons, dx(16), dy(16), wheel(8)
//   Keyboard input report body = 8 bytes: modifiers, reserved, 6 keycodes
//
static UCHAR g_CouchLinkReportDescriptor[] = {
    // ---- Mouse (Report ID 1) ----
    0x05, 0x01,       // Usage Page (Generic Desktop)
    0x09, 0x02,       // Usage (Mouse)
    0xA1, 0x01,       // Collection (Application)
    0x85, CouchLinkHidReportMouse, //   Report ID (1)
    0x09, 0x01,       //   Usage (Pointer)
    0xA1, 0x00,       //   Collection (Physical)
    0x05, 0x09,       //     Usage Page (Button)
    0x19, 0x01,       //     Usage Minimum (Button 1)
    0x29, 0x03,       //     Usage Maximum (Button 3)
    0x15, 0x00,       //     Logical Minimum (0)
    0x25, 0x01,       //     Logical Maximum (1)
    0x95, 0x03,       //     Report Count (3)
    0x75, 0x01,       //     Report Size (1)
    0x81, 0x02,       //     Input (Data,Var,Abs)   ; 3 buttons
    0x95, 0x01,       //     Report Count (1)
    0x75, 0x05,       //     Report Size (5)
    0x81, 0x03,       //     Input (Const,Var,Abs)  ; 5-bit padding
    0x05, 0x01,       //     Usage Page (Generic Desktop)
    0x09, 0x30,       //     Usage (X)
    0x09, 0x31,       //     Usage (Y)
    0x16, 0x01, 0x80, //     Logical Minimum (-32767)
    0x26, 0xFF, 0x7F, //     Logical Maximum (32767)
    0x75, 0x10,       //     Report Size (16)
    0x95, 0x02,       //     Report Count (2)
    0x81, 0x06,       //     Input (Data,Var,Rel)   ; X, Y relative
    0x09, 0x38,       //     Usage (Wheel)
    0x15, 0x81,       //     Logical Minimum (-127)
    0x25, 0x7F,       //     Logical Maximum (127)
    0x75, 0x08,       //     Report Size (8)
    0x95, 0x01,       //     Report Count (1)
    0x81, 0x06,       //     Input (Data,Var,Rel)   ; wheel relative
    0xC0,             //   End Collection
    0xC0,             // End Collection

    // ---- Keyboard (Report ID 2), boot-compatible ----
    0x05, 0x01,       // Usage Page (Generic Desktop)
    0x09, 0x06,       // Usage (Keyboard)
    0xA1, 0x01,       // Collection (Application)
    0x85, CouchLinkHidReportKeyboard, //   Report ID (2)
    0x05, 0x07,       //   Usage Page (Keyboard/Keypad)
    0x19, 0xE0,       //   Usage Minimum (Left Control)
    0x29, 0xE7,       //   Usage Maximum (Right GUI)
    0x15, 0x00,       //   Logical Minimum (0)
    0x25, 0x01,       //   Logical Maximum (1)
    0x75, 0x01,       //   Report Size (1)
    0x95, 0x08,       //   Report Count (8)
    0x81, 0x02,       //   Input (Data,Var,Abs)    ; modifier byte
    0x95, 0x01,       //   Report Count (1)
    0x75, 0x08,       //   Report Size (8)
    0x81, 0x03,       //   Input (Const,Var,Abs)   ; reserved byte
    0x95, 0x06,       //   Report Count (6)
    0x75, 0x08,       //   Report Size (8)
    0x15, 0x00,       //   Logical Minimum (0)
    0x26, 0xFF, 0x00, //   Logical Maximum (255)
    0x05, 0x07,       //   Usage Page (Keyboard/Keypad)
    0x19, 0x00,       //   Usage Minimum (0)
    0x29, 0xFF,       //   Usage Maximum (255)
    0x81, 0x00,       //   Input (Data,Array)      ; 6 keycodes
    0xC0              // End Collection
};

//
// HID device attributes reported to VHF. VID/PID are private-use values;
// change them if you ever ship signed, but keep them stable per install.
//
#define COUCHLINK_VENDOR_ID   0x1D6B   // (private use; not a real allocation)
#define COUCHLINK_PRODUCT_ID  0xC001
#define COUCHLINK_VERSION_ID  0x0001

NTSTATUS
DriverEntry(
    _In_ PDRIVER_OBJECT  DriverObject,
    _In_ PUNICODE_STRING RegistryPath
    )
{
    WDF_DRIVER_CONFIG config;
    NTSTATUS status;

    WDF_DRIVER_CONFIG_INIT(&config, CouchLinkEvtDeviceAdd);

    status = WdfDriverCreate(
        DriverObject,
        RegistryPath,
        WDF_NO_OBJECT_ATTRIBUTES,
        &config,
        WDF_NO_HANDLE);

    return status;
}

NTSTATUS
CouchLinkEvtDeviceAdd(
    _In_    WDFDRIVER       Driver,
    _Inout_ PWDFDEVICE_INIT DeviceInit
    )
{
    NTSTATUS status;
    WDF_OBJECT_ATTRIBUTES deviceAttributes;
    WDF_OBJECT_ATTRIBUTES queueAttributes;
    WDF_IO_QUEUE_CONFIG queueConfig;
    WDFDEVICE device;
    WDFQUEUE queue;
    PDEVICE_CONTEXT context;

    UNREFERENCED_PARAMETER(Driver);
    PAGED_CODE();

    WDF_OBJECT_ATTRIBUTES_INIT_CONTEXT_TYPE(&deviceAttributes, DEVICE_CONTEXT);
    deviceAttributes.EvtCleanupCallback = CouchLinkEvtDeviceCleanup;

    //
    // This device accepts commands that become mouse/keyboard input, including
    // on the secure desktop. Restrict handles to LocalSystem and administrators.
    // FILE_AUTOGENERATED_DEVICE_NAME is required before assigning an SDDL to an
    // otherwise unnamed PnP device object.
    //
    WdfDeviceInitSetCharacteristics(DeviceInit, FILE_AUTOGENERATED_DEVICE_NAME, FALSE);
    status = WdfDeviceInitAssignSDDLString(DeviceInit, &SDDL_DEVOBJ_SYS_ALL_ADM_ALL);
    if (!NT_SUCCESS(status)) {
        return status;
    }

    status = WdfDeviceCreate(&DeviceInit, &deviceAttributes, &device);
    if (!NT_SUCCESS(status)) {
        return status;
    }

    context = DeviceGetContext(device);
    context->VhfHandle = NULL;
    context->VhfStarted = FALSE;

    //
    // Publish the device interface so the user-mode bridge can find us.
    //
    status = WdfDeviceCreateDeviceInterface(
        device,
        &GUID_DEVINTERFACE_COUCHLINK_VHID,
        NULL);
    if (!NT_SUCCESS(status)) {
        return status;
    }

    //
    // Default sequential queue for the submit IOCTL. Sequential keeps report
    // submission strictly ordered, which is what a keyboard/mouse stream needs.
    //
    WDF_IO_QUEUE_CONFIG_INIT_DEFAULT_QUEUE(&queueConfig, WdfIoQueueDispatchSequential);
    queueConfig.EvtIoDeviceControl = CouchLinkEvtIoDeviceControl;

    WDF_OBJECT_ATTRIBUTES_INIT(&queueAttributes);

    status = WdfIoQueueCreate(device, &queueConfig, &queueAttributes, &queue);
    if (!NT_SUCCESS(status)) {
        return status;
    }

    //
    // Spin up the virtual HID device.
    //
    status = CouchLinkVhfInitialize(device);
    return status;
}

NTSTATUS
CouchLinkVhfInitialize(
    _In_ WDFDEVICE Device
    )
{
    NTSTATUS status;
    VHF_CONFIG config;
    PDEVICE_CONTEXT context = DeviceGetContext(Device);

    PAGED_CODE();

    //
    // We only submit device->host input reports; we do not implement
    // Get/Set feature or output callbacks, so leave those NULL. VHF is
    // configured directly from our report descriptor.
    //
    VHF_CONFIG_INIT(
        &config,
        WdfDeviceWdmGetDeviceObject(Device),
        sizeof(g_CouchLinkReportDescriptor),
        (PUCHAR)g_CouchLinkReportDescriptor);

    config.VendorID  = COUCHLINK_VENDOR_ID;
    config.ProductID = COUCHLINK_PRODUCT_ID;
    config.VersionNumber = COUCHLINK_VERSION_ID;
    config.VhfClientContext = context;

    status = VhfCreate(&config, &context->VhfHandle);
    if (!NT_SUCCESS(status)) {
        context->VhfHandle = NULL;
        return status;
    }

    status = VhfStart(context->VhfHandle);
    if (!NT_SUCCESS(status)) {
        VhfDelete(context->VhfHandle, TRUE);
        context->VhfHandle = NULL;
        return status;
    }

    context->VhfStarted = TRUE;
    return STATUS_SUCCESS;
}

VOID
CouchLinkEvtDeviceCleanup(
    _In_ WDFOBJECT Object
    )
{
    PDEVICE_CONTEXT context = DeviceGetContext((WDFDEVICE)Object);

    PAGED_CODE();

    if (context->VhfHandle != NULL) {
        VhfDelete(context->VhfHandle, TRUE);
        context->VhfHandle = NULL;
        context->VhfStarted = FALSE;
    }
}

//
// Build a HID input report and hand it to VHF. Buffer layout is
// [reportId][payload...] as required for numbered reports. This driver uses
// VHF's default buffering policy (no EvtVhfReadyForNextReadReport callback),
// so Microsoft permits the transfer buffer to be reused after the call returns.
//
NTSTATUS
CouchLinkSubmitReport(
    _In_ PDEVICE_CONTEXT Context,
    _In_ const COUCHLINK_HID_SUBMIT* Submit
    )
{
    HID_XFER_PACKET packet;
    UCHAR reportBuffer[1 + COUCHLINK_HID_MAX_PAYLOAD];
    UCHAR expected;

    if (Context == NULL || !Context->VhfStarted || Context->VhfHandle == NULL) {
        return STATUS_DEVICE_NOT_READY;
    }

    if (Submit->Version != COUCHLINK_HID_PROTOCOL_VERSION || Submit->Reserved != 0) {
        return STATUS_REVISION_MISMATCH;
    }

    switch (Submit->Kind) {
    case CouchLinkHidReportMouse:
        expected = (UCHAR)COUCHLINK_MOUSE_PAYLOAD_SIZE;
        break;
    case CouchLinkHidReportKeyboard:
        expected = (UCHAR)COUCHLINK_KEYBOARD_PAYLOAD_SIZE;
        break;
    default:
        return STATUS_INVALID_DEVICE_REQUEST;
    }

    if (Submit->PayloadLength != expected) {
        return STATUS_INVALID_BUFFER_SIZE;
    }

    reportBuffer[0] = (UCHAR)Submit->Kind;                 // report ID
    RtlCopyMemory(&reportBuffer[1], Submit->Payload, expected);

    RtlZeroMemory(&packet, sizeof(packet));
    packet.reportId        = (UCHAR)Submit->Kind;
    packet.reportBuffer    = reportBuffer;
    packet.reportBufferLen = (ULONG)(1 + expected);

    return VhfReadReportSubmit(Context->VhfHandle, &packet);
}

VOID
CouchLinkEvtIoDeviceControl(
    _In_ WDFQUEUE   Queue,
    _In_ WDFREQUEST Request,
    _In_ size_t     OutputBufferLength,
    _In_ size_t     InputBufferLength,
    _In_ ULONG      IoControlCode
    )
{
    NTSTATUS status;
    PDEVICE_CONTEXT context;
    PVOID inBuffer;
    size_t inSize;

    UNREFERENCED_PARAMETER(OutputBufferLength);

    context = DeviceGetContext(WdfIoQueueGetDevice(Queue));

    if (IoControlCode != IOCTL_COUCHLINK_VHID_SUBMIT_REPORT) {
        WdfRequestComplete(Request, STATUS_INVALID_DEVICE_REQUEST);
        return;
    }

    if (InputBufferLength != sizeof(COUCHLINK_HID_SUBMIT)) {
        WdfRequestComplete(Request, STATUS_INVALID_BUFFER_SIZE);
        return;
    }

    status = WdfRequestRetrieveInputBuffer(
        Request,
        sizeof(COUCHLINK_HID_SUBMIT),
        &inBuffer,
        &inSize);
    if (!NT_SUCCESS(status) || inSize != sizeof(COUCHLINK_HID_SUBMIT)) {
        WdfRequestComplete(Request, STATUS_INVALID_BUFFER_SIZE);
        return;
    }

    //
    // Copy into a local so we validate a stable snapshot, never the caller's
    // buffer in place.
    //
    {
        COUCHLINK_HID_SUBMIT submit = *(const COUCHLINK_HID_SUBMIT*)inBuffer;
        status = CouchLinkSubmitReport(context, &submit);
    }

    WdfRequestComplete(Request, status);
}
