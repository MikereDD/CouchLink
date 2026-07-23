# CouchLink v0.1-dev.15.1 Build Fix

Fixed invalid XAML in the Windows host `MainWindow.xaml`.

The unescaped ampersand in `Devices & pairing` caused WPF markup compilation error MC3000. It is now encoded as `Devices &amp; pairing`.

No Android source or Windows host behavior was otherwise changed.
