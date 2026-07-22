# CouchLink v0.2-dev.11.1 build fix

Fixed eight `Unresolved reference: Accent` errors in `HomeScreen.kt` by importing the existing theme color:

```kotlin
import dev.typezero.couchlink.remote.ui.theme.Accent
```

No visual values or UI behavior were changed. The clean CouchLink-brand orange outlines and white launcher icons remain intact.
