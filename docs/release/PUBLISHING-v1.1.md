# Publishing CouchLink 1.1

This procedure publishes the exact stable source and binaries as one traceable release.

## 1. Build the final artifacts

From the stable repository root on Windows:

```powershell
.\Build-Release.ps1
```

Enter the keystore path and both passwords when prompted. Do not put passwords on the command line.

The default output is `release\CouchLink-v1.1\`, or a timestamped sibling when that directory already contains files.

## 2. Verify the release directory

The final directory should contain at least:

```text
CouchLink-v1.1.apk
CouchLink-v1.1.apk.sha256
CouchLink-Host-v1.1-win-x64.exe
CouchLink-Host-v1.1-win-x64.exe.sha256
CouchLink-v1.1-source.zip
CouchLink-v1.1-source.zip.sha256
SHA256SUMS-v1.1.txt
RELEASE-INFO-v1.1.txt
RELEASE-NOTES-v1.1.md
INSTALLATION-v1.1.md
README.md
CHANGELOG.md
CONTRIBUTING.md
SECURITY.md
SOURCE-MANIFEST-v1.1.sha256
LICENSE
NOTICE
PRIVACY.md
TRADEMARKS.md
```

Verify the combined checksum list from PowerShell:

```powershell
$release = '.\release\CouchLink-v1.1'
Get-Content "$release\SHA256SUMS-v1.1.txt"
Get-FileHash "$release\CouchLink-v1.1.apk" -Algorithm SHA256
Get-FileHash "$release\CouchLink-Host-v1.1-win-x64.exe" -Algorithm SHA256
Get-FileHash "$release\CouchLink-v1.1-source.zip" -Algorithm SHA256
```

## 3. Commit and tag

Commit the final stable source, then tag that exact commit:

```powershell
git status
git add --all
git commit -m "release: CouchLink v1.1 stable"
git tag -a v1.1 -m "CouchLink v1.1 stable"
git push origin main
git push origin v1.1
```

Push the same commit and tag to any public mirror. Do not rebuild after tagging unless the tag is deliberately replaced before publication.

## 4. Publish the release

Use `RELEASE-NOTES-v1.1.md` as the release description and attach the APK, EXE, source ZIP, their individual checksum files, and `SHA256SUMS-v1.1.txt`.

Keep the original v1.1 release directory unchanged after publication so the exact distributed binaries can always be verified and restored.
