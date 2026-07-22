#pragma once

//
// CouchLink Virtual HID — shared contract.
//
// This single header is the ONLY interface between the user-mode CouchLink
// Boot Service bridge and the kernel-mode Virtual HID source driver. It is
// deliberately tiny and fixed-size. The driver must never accept anything
// other than the structures defined here: no pointers, no file paths, no
// variable-length payloads, no executable data.
//
// It is C89-clean so it can be included from both the WDK driver (C) and,
// conceptually, mirrored by the managed P/Invoke layer.
//

#ifdef _KERNEL_MODE
#include <ntdef.h>
#else
#include <windows.h>
#include <winioctl.h>
#include <stdint.h>
#endif

#define COUCHLINK_HID_PROTOCOL_VERSION 1u

//
// HID numbered report IDs. These match the HID report descriptor compiled
// into the driver (see Driver.c / g_CouchLinkReportDescriptor).
//
typedef enum COUCHLINK_HID_REPORT_KIND {
    CouchLinkHidReportMouse    = 1,
    CouchLinkHidReportKeyboard = 2
} COUCHLINK_HID_REPORT_KIND;

//
// Wire size of each HID input report payload (NOT counting the report-ID byte).
//   Mouse:    buttons(1) + dx(2) + dy(2) + wheel(1)            = 6
//   Keyboard: modifiers(1) + reserved(1) + keycodes(6)         = 8
//
#define COUCHLINK_MOUSE_PAYLOAD_SIZE     6u
#define COUCHLINK_KEYBOARD_PAYLOAD_SIZE  8u
#define COUCHLINK_HID_MAX_PAYLOAD        8u

//
// Mouse button bitmask (matches HID button usages 1..3).
//
#define COUCHLINK_MOUSE_BUTTON_LEFT   0x01u
#define COUCHLINK_MOUSE_BUTTON_RIGHT  0x02u
#define COUCHLINK_MOUSE_BUTTON_MIDDLE 0x04u

//
// Keyboard modifier bitmask (standard HID boot keyboard).
//
#define COUCHLINK_KBD_LCTRL   0x01u
#define COUCHLINK_KBD_LSHIFT  0x02u
#define COUCHLINK_KBD_LALT    0x04u
#define COUCHLINK_KBD_LGUI    0x08u
#define COUCHLINK_KBD_RCTRL   0x10u
#define COUCHLINK_KBD_RSHIFT  0x20u
#define COUCHLINK_KBD_RALT    0x40u
#define COUCHLINK_KBD_RGUI    0x80u

//
// The one and only structure accepted by the driver's IOCTL. Fixed size.
// The driver validates Version, Kind, and PayloadLength before it does
// anything. Everything past PayloadLength is ignored and must be zero.
//
#pragma pack(push, 1)
typedef struct _COUCHLINK_HID_SUBMIT {
    unsigned char Version;                          // == COUCHLINK_HID_PROTOCOL_VERSION
    unsigned char Kind;                             // COUCHLINK_HID_REPORT_KIND
    unsigned char PayloadLength;                    // 6 for mouse, 8 for keyboard
    unsigned char Reserved;                         // must be 0
    unsigned char Payload[COUCHLINK_HID_MAX_PAYLOAD];
} COUCHLINK_HID_SUBMIT, *PCOUCHLINK_HID_SUBMIT;
#pragma pack(pop)

//
// Convenience views over COUCHLINK_HID_SUBMIT.Payload. These describe the
// exact byte layout the driver forwards to the HID stack (after prepending
// the report-ID byte).
//
#pragma pack(push, 1)
typedef struct _COUCHLINK_MOUSE_PAYLOAD {
    unsigned char Buttons;   // COUCHLINK_MOUSE_BUTTON_* bitmask
    short         DeltaX;    // relative, signed, little-endian
    short         DeltaY;    // relative, signed, little-endian
    signed char   Wheel;     // relative wheel detents
} COUCHLINK_MOUSE_PAYLOAD;

typedef struct _COUCHLINK_KEYBOARD_PAYLOAD {
    unsigned char Modifiers; // COUCHLINK_KBD_* bitmask
    unsigned char Reserved;  // must be 0
    unsigned char Keys[6];   // HID usage IDs (Keyboard/Keypad page), 0 = none
} COUCHLINK_KEYBOARD_PAYLOAD;
#pragma pack(pop)

//
// Device interface GUID published by the driver. The user-mode bridge uses
// this with SetupDiGetClassDevs to locate the device path, then CreateFile.
//
// {6B2C4E10-3B9A-4C2D-9E7F-CL0000000001}
//
DEFINE_GUID(GUID_DEVINTERFACE_COUCHLINK_VHID,
    0x6b2c4e10, 0x3b9a, 0x4c2d, 0x9e, 0x7f, 0x43, 0x4c, 0x00, 0x00, 0x00, 0x01);

//
// The single control code. METHOD_BUFFERED, write access. Input buffer must
// be exactly sizeof(COUCHLINK_HID_SUBMIT). No output.
//
#define COUCHLINK_VHID_DEVICE_TYPE 0x8042u

#define IOCTL_COUCHLINK_VHID_SUBMIT_REPORT \
    CTL_CODE(COUCHLINK_VHID_DEVICE_TYPE, 0x800, METHOD_BUFFERED, FILE_WRITE_ACCESS)
