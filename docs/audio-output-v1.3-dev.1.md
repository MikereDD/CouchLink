# CouchLink v1.3-dev.1 — Audio Output Switching

## First test build

- Enumerates active Windows Core Audio render endpoints.
- Reports the current default playback endpoint to Android.
- Switches the Console, Multimedia, and Communications defaults by stable endpoint ID.
- Adds an Audio Output panel to the Android Home screen.
- Refreshes the endpoint list when the trusted host session opens and after a successful switch.
- Uses native Windows Core Audio COM interfaces; AudioDeviceCmdlets is not a runtime dependency.

## Test targets on Netzach

- Headphones (3- Arctis 7 Game)
- LG ULTRAGEAR (NVIDIA High Definition Audio)
- Speakers (Sound Blaster X4)

## Validation required

This environment could not run .NET or Gradle builds. Compile both projects locally before testing.
