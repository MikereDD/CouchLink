#pragma once

#include <ntddk.h>
#include <wdf.h>
#include <wdmsec.h>
#include <hidport.h>
#include <vhf.h>

#include "CouchLinkHidProtocol.h"

//
// Per-device context. One VHF virtual HID device per WDFDEVICE.
//
typedef struct _DEVICE_CONTEXT {
    VHFHANDLE VhfHandle;      // handle to the VHF virtual HID device
    BOOLEAN   VhfStarted;     // TRUE once VhfStart succeeded
} DEVICE_CONTEXT, *PDEVICE_CONTEXT;

WDF_DECLARE_CONTEXT_TYPE_WITH_NAME(DEVICE_CONTEXT, DeviceGetContext)

DRIVER_INITIALIZE DriverEntry;

EVT_WDF_DRIVER_DEVICE_ADD        CouchLinkEvtDeviceAdd;
EVT_WDF_DEVICE_CONTEXT_CLEANUP   CouchLinkEvtDeviceCleanup;
EVT_WDF_IO_QUEUE_IO_DEVICE_CONTROL CouchLinkEvtIoDeviceControl;

NTSTATUS
CouchLinkVhfInitialize(
    _In_ WDFDEVICE Device
    );

NTSTATUS
CouchLinkSubmitReport(
    _In_ PDEVICE_CONTEXT Context,
    _In_ const COUCHLINK_HID_SUBMIT* Submit
    );
