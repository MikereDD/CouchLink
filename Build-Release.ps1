[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$Version = '1.3.1-dev.6.1',

    [ValidateNotNullOrEmpty()]
    [string]$Runtime = 'win-x64',

    [string]$KeystorePath,

    [ValidateNotNullOrEmpty()]
    [string]$KeyAlias = 'couchlink-release',

    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedAndroidCertificateSha256 = 'a3d6a2b2a81c3ee0c96c911f31b3e46be663e5cf81ebfe1d5deccfc39d5b96bb',

    [switch]$SelfContained,

    [switch]$SkipAndroidSignatureVerification,

    [switch]$SkipSourceArchive,

    [switch]$RequireCleanTaggedSource,

    [string]$OutputDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$ArgumentList,

        [Parameter(Mandatory)]
        [string]$FailureMessage
    )

    & $FilePath @ArgumentList
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "$FailureMessage Exit code: $exitCode"
    }
}

function Write-Sha256File {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    $item = Get-Item -LiteralPath $Path
    $hash = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumPath = "$($item.FullName).sha256"
    "$hash  $($item.Name)" | Set-Content -LiteralPath $checksumPath -Encoding ascii

    [pscustomobject]@{
        Artifact = $item.FullName
        FileName = $item.Name
        Hash = $hash
        Checksum = $checksumPath
    }
}

function Test-SourceFileExcluded {
    param(
        [Parameter(Mandatory)]
        [string]$RelativePath
    )

    $normalized = $RelativePath.Replace('\', '/')
    $segments = $normalized.Split('/', [System.StringSplitOptions]::RemoveEmptyEntries)
    $excludedDirectories = @('.git', '.gradle', '.idea', '.vs', 'bin', 'obj', 'build', 'publish', 'signing')

    if ($segments.Length -gt 1) {
        foreach ($index in 0..($segments.Length - 2)) {
            $segment = $segments[$index]
            if ($excludedDirectories -contains $segment) {
                return $true
            }
            if ($segment -eq 'release' -and ($index -eq 0 -or $segments[$index - 1] -ne 'docs')) {
                return $true
            }
        }
    }

    $name = [System.IO.Path]::GetFileName($normalized)
    if ($name -in @('local.properties', 'keystore.properties', 'Build-Release-error.log', 'Thumbs.db', '.DS_Store')) {
        return $true
    }

    foreach ($pattern in @('*.apk', '*.aab', '*.jks', '*.keystore', '*.pfx', '*.p12', '*.user', '*.suo', '*.zip')) {
        if ($name -like $pattern) {
            return $true
        }
    }

    return $false
}

function New-CouchLinkSourceArchive {
    param(
        [Parameter(Mandatory)]
        [string]$RepoRoot,

        [Parameter(Mandatory)]
        [string]$DestinationDirectory,

        [Parameter(Mandatory)]
        [string]$ReleaseVersion
    )

    $stagingParent = Join-Path ([System.IO.Path]::GetTempPath()) "CouchLink-source-$([guid]::NewGuid().ToString('N'))"
    $stagingRoot = Join-Path $stagingParent "CouchLink-v$ReleaseVersion"
    $archivePath = Join-Path $DestinationDirectory "CouchLink-v$ReleaseVersion-source.zip"

    try {
        New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null

        Get-ChildItem -LiteralPath $RepoRoot -File -Recurse -Force | ForEach-Object {
            $relativePath = [System.IO.Path]::GetRelativePath($RepoRoot, $_.FullName)
            if (-not (Test-SourceFileExcluded -RelativePath $relativePath)) {
                $destination = Join-Path $stagingRoot $relativePath
                $destinationParent = Split-Path -Parent $destination
                New-Item -ItemType Directory -Force -Path $destinationParent | Out-Null
                Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
            }
        }

        if (Test-Path -LiteralPath $archivePath) {
            Remove-Item -LiteralPath $archivePath -Force
        }

        Compress-Archive -LiteralPath $stagingRoot -DestinationPath $archivePath -CompressionLevel Optimal
        return $archivePath
    }
    finally {
        Remove-Item -LiteralPath $stagingParent -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Copy-ReleaseDocument {
    param(
        [Parameter(Mandatory)]
        [string]$Source,

        [Parameter(Mandatory)]
        [string]$Destination
    )

    if (-not (Test-Path -LiteralPath $Source -PathType Leaf)) {
        throw "Required release document was not found: $Source"
    }

    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}


function Assert-CouchLinkVersionConsistency {
    param(
        [Parameter(Mandatory)]
        [string]$RepoRoot,

        [Parameter(Mandatory)]
        [string]$ReleaseVersion,

        [switch]$RequireCleanTag
    )

    $hostConstants = Get-Content -LiteralPath (Join-Path $RepoRoot 'src\windows\CouchLink.Host.Core\HostConstants.cs') -Raw
    $hostProject = Get-Content -LiteralPath (Join-Path $RepoRoot 'src\windows\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj') -Raw
    $androidGradle = Get-Content -LiteralPath (Join-Path $RepoRoot 'src\android\app\build.gradle.kts') -Raw

    foreach ($check in @(
        @{ Name = 'HostConstants.HostVersion'; Pattern = [regex]::Escape('HostVersion = "' + $ReleaseVersion + '"') },
        @{ Name = 'Windows project Version'; Pattern = [regex]::Escape('<Version>' + $ReleaseVersion + '</Version>') },
        @{ Name = 'Windows InformationalVersion'; Pattern = [regex]::Escape('<InformationalVersion>' + $ReleaseVersion + '</InformationalVersion>') },
        @{ Name = 'Android versionName'; Pattern = [regex]::Escape('versionName = "' + $ReleaseVersion + '"') }
    )) {
        $source = switch ($check.Name) {
            'HostConstants.HostVersion' { $hostConstants }
            'Android versionName' { $androidGradle }
            default { $hostProject }
        }
        if ($source -notmatch $check.Pattern) {
            throw "$($check.Name) does not match release version $ReleaseVersion."
        }
    }

    if (-not $RequireCleanTag) {
        return
    }

    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git -or -not (Test-Path -LiteralPath (Join-Path $RepoRoot '.git'))) {
        throw 'A Git working tree is required when -RequireCleanTaggedSource is used.'
    }

    $status = & $git.Source -C $RepoRoot status --porcelain
    if ($LASTEXITCODE -ne 0 -or $status) {
        throw 'The Git working tree must be clean for a tagged release build.'
    }

    $tag = "v$ReleaseVersion"
    $head = (& $git.Source -C $RepoRoot rev-parse HEAD).Trim()
    $tagCommit = (& $git.Source -C $RepoRoot rev-list -n 1 $tag 2>$null).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($tagCommit)) {
        throw "Required release tag $tag was not found."
    }
    if ($head -ne $tagCommit) {
        throw "HEAD $head does not match release tag $tag ($tagCommit)."
    }
}

function Invoke-CouchLinkReleaseBuild {
    $repoRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
    $androidRoot = Join-Path $repoRoot 'src\android'
    $windowsRoot = Join-Path $repoRoot 'src\windows'
    $androidBuildScript = Join-Path $androidRoot 'tools\Build-SignedRelease.ps1'
    $androidVerifyScript = Join-Path $androidRoot 'tools\Verify-SignedRelease.ps1'
    $sourceManifestScript = Join-Path $repoRoot 'tools\Update-SourceManifest.ps1'
    $sourceManifestVerifyScript = Join-Path $repoRoot 'tools\Test-SourceManifest.ps1'
    $windowsProject = Join-Path $windowsRoot 'CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj'
    $updaterProject = Join-Path $windowsRoot 'CouchLink.Updater\CouchLink.Updater.csproj'
    $versionTestsProject = Join-Path $windowsRoot 'CouchLink.Versioning.Tests\CouchLink.Versioning.Tests.csproj'

    foreach ($requiredPath in @(
        $androidBuildScript,
        $androidVerifyScript,
        $sourceManifestScript,
        $sourceManifestVerifyScript,
        $windowsProject,
        $updaterProject,
        $versionTestsProject
    )) {
        if (-not (Test-Path -LiteralPath $requiredPath)) {
            throw "Required project file was not found: $requiredPath"
        }
    }

    $dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
    if (-not $dotnet) {
        throw 'dotnet was not found. Install the .NET 8 SDK or add dotnet to PATH.'
    }

    if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
        $resolvedOutputDirectory = Join-Path $repoRoot "release\CouchLink-v$Version"
    }
    else {
        $resolvedOutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
    }

    if (Test-Path -LiteralPath $resolvedOutputDirectory) {
        $existingFiles = Get-ChildItem -LiteralPath $resolvedOutputDirectory -Force -ErrorAction SilentlyContinue
        if ($existingFiles) {
            $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
            $resolvedOutputDirectory = "$resolvedOutputDirectory-build-$timestamp"
            Write-Host "Existing release output preserved. Using: $resolvedOutputDirectory" -ForegroundColor Yellow
        }
    }

    New-Item -ItemType Directory -Force -Path $resolvedOutputDirectory | Out-Null

    Assert-CouchLinkVersionConsistency -RepoRoot $repoRoot -ReleaseVersion $Version -RequireCleanTag:$RequireCleanTaggedSource

    Write-Host ''
    Write-Host "CouchLink v$Version test release build" -ForegroundColor Cyan
    Write-Host "Output: $resolvedOutputDirectory"
    Write-Host ''

    $resolvedKeystorePath = $KeystorePath
    if ([string]::IsNullOrWhiteSpace($resolvedKeystorePath)) {
        $defaultKeystore = Join-Path $androidRoot 'signing\couchlink-release.jks'
        Write-Host 'Android signing configuration' -ForegroundColor Cyan
        $enteredPath = Read-Host "Keystore (.jks) path [$defaultKeystore]"
        $resolvedKeystorePath = if ([string]::IsNullOrWhiteSpace($enteredPath)) {
            $defaultKeystore
        }
        else {
            $enteredPath.Trim().Trim('"')
        }
    }

    $resolvedKeystorePath = [System.IO.Path]::GetFullPath($resolvedKeystorePath)
    if (-not (Test-Path -LiteralPath $resolvedKeystorePath -PathType Leaf)) {
        throw "Keystore not found: $resolvedKeystorePath"
    }

    Write-Host ''
    Write-Host '[0/5] Running Windows version-comparison tests...' -ForegroundColor Cyan
    Invoke-NativeCommand -FilePath $dotnet.Source -ArgumentList @('run', '--project', $versionTestsProject, '-c', 'Release') -FailureMessage 'Windows version-comparison tests failed.'

    Write-Host '[1/5] Building signed Android APK...' -ForegroundColor Cyan
    $androidParameters = @{
        Version = $Version
        Alias = $KeyAlias
        OutputDirectory = $resolvedOutputDirectory
        KeystorePath = $resolvedKeystorePath
    }
    & $androidBuildScript @androidParameters

    $finalApk = Join-Path $resolvedOutputDirectory "CouchLink-v$Version.apk"
    if (-not (Test-Path -LiteralPath $finalApk)) {
        throw "Expected signed APK was not produced: $finalApk"
    }

    if (-not $SkipAndroidSignatureVerification) {
        Write-Host '[2/5] Verifying Android signature and certificate...' -ForegroundColor Cyan
        & $androidVerifyScript `
            -ApkPath $finalApk `
            -ExpectedCertificateSha256 $ExpectedAndroidCertificateSha256
    }
    else {
        Write-Host '[2/5] Android signature verification skipped by request.' -ForegroundColor Yellow
    }

    Write-Host '[3/5] Publishing Windows host...' -ForegroundColor Cyan
    $windowsPublishDirectory = Join-Path $windowsRoot "publish\CouchLink-Host-v$Version-$Runtime"
    if (Test-Path -LiteralPath $windowsPublishDirectory) {
        Remove-Item -LiteralPath $windowsPublishDirectory -Recurse -Force
    }

    $selfContainedValue = $SelfContained.IsPresent.ToString().ToLowerInvariant()
    $publishArguments = @(
        'publish',
        $windowsProject,
        '-c', 'Release',
        '-r', $Runtime,
        '--self-contained', $selfContainedValue,
        '-p:PublishSingleFile=true',
        '-p:DebugType=None',
        '-p:DebugSymbols=false',
        '-o', $windowsPublishDirectory
    )
    Invoke-NativeCommand -FilePath $dotnet.Source -ArgumentList $publishArguments -FailureMessage 'Windows publish failed.'

    $publishedExe = Join-Path $windowsPublishDirectory 'CouchLink.Host.exe'
    if (-not (Test-Path -LiteralPath $publishedExe)) {
        throw "Expected Windows executable was not produced: $publishedExe"
    }

    $finalExe = Join-Path $resolvedOutputDirectory "CouchLink-Host-v$Version-$Runtime.exe"
    Copy-Item -LiteralPath $publishedExe -Destination $finalExe -Force

    $updaterPublishDirectory = Join-Path $windowsRoot "publish\CouchLink-Updater-v$Version-$Runtime"
    if (Test-Path -LiteralPath $updaterPublishDirectory) {
        Remove-Item -LiteralPath $updaterPublishDirectory -Recurse -Force
    }
    $updaterArguments = @(
        'publish',
        $updaterProject,
        '-c', 'Release',
        '-r', $Runtime,
        '--self-contained', $selfContainedValue,
        '-p:PublishSingleFile=true',
        '-p:DebugType=None',
        '-p:DebugSymbols=false',
        '-o', $updaterPublishDirectory
    )
    Invoke-NativeCommand -FilePath $dotnet.Source -ArgumentList $updaterArguments -FailureMessage 'Windows updater publish failed.'
    $publishedUpdater = Join-Path $updaterPublishDirectory 'CouchLink.Updater.exe'
    if (-not (Test-Path -LiteralPath $publishedUpdater)) {
        throw "Expected Windows updater executable was not produced: $publishedUpdater"
    }
    $finalUpdater = Join-Path $resolvedOutputDirectory "CouchLink-Updater-v$Version-$Runtime.exe"
    Copy-Item -LiteralPath $publishedUpdater -Destination $finalUpdater -Force

    Write-Host '[4/5] Packaging source and release documents...' -ForegroundColor Cyan
    & $sourceManifestScript -Version $Version -RootPath $repoRoot
    & $sourceManifestVerifyScript -Version $Version -RootPath $repoRoot

    $sourceArchive = $null
    if (-not $SkipSourceArchive) {
        $sourceArchive = New-CouchLinkSourceArchive `
            -RepoRoot $repoRoot `
            -DestinationDirectory $resolvedOutputDirectory `
            -ReleaseVersion $Version
    }
    else {
        Write-Host 'Source archive skipped by request.' -ForegroundColor Yellow
    }

    foreach ($releaseDocument in @(
        "RELEASE-NOTES-v$Version.md",
        "RELEASE-CHECKLIST-v$Version.md",
        "RELEASE-VALIDATION-v$Version.md"
    )) {
        Copy-ReleaseDocument `
            -Source (Join-Path $repoRoot "docs\release\$releaseDocument") `
            -Destination (Join-Path $resolvedOutputDirectory $releaseDocument)
    }
    Copy-ReleaseDocument `
        -Source (Join-Path $repoRoot 'docs\release\INSTALLATION.md') `
        -Destination (Join-Path $resolvedOutputDirectory "INSTALLATION-v$Version.md")

    foreach ($document in @(
        'README.md',
        'CHANGELOG.md',
        'CONTRIBUTING.md',
        'SECURITY.md',
        'LICENSE',
        'NOTICE',
        'PRIVACY.md',
        'TRADEMARKS.md',
        "SOURCE-MANIFEST-v$Version.sha256"
    )) {
        Copy-ReleaseDocument `
            -Source (Join-Path $repoRoot $document) `
            -Destination (Join-Path $resolvedOutputDirectory $document)
    }

    Write-Host '[5/5] Generating SHA-256 checksums...' -ForegroundColor Cyan
    $releaseHashes = [System.Collections.Generic.List[object]]::new()
    $releaseHashes.Add((Write-Sha256File -Path $finalApk))
    $releaseHashes.Add((Write-Sha256File -Path $finalExe))
    $releaseHashes.Add((Write-Sha256File -Path $finalUpdater))
    if ($sourceArchive) {
        $releaseHashes.Add((Write-Sha256File -Path $sourceArchive))
    }

    $combinedChecksumPath = Join-Path $resolvedOutputDirectory "SHA256SUMS-v$Version.txt"
    $releaseHashes |
        ForEach-Object { "$($_.Hash)  $($_.FileName)" } |
        Set-Content -LiteralPath $combinedChecksumPath -Encoding ascii

    $releaseInfoPath = Join-Path $resolvedOutputDirectory "RELEASE-INFO-v$Version.txt"
    @(
        "CouchLink v$Version"
        "Built: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
        "Runtime: $Runtime"
        "Windows self-contained: $($SelfContained.IsPresent)"
        "Android signing certificate SHA-256: $($ExpectedAndroidCertificateSha256.ToLowerInvariant())"
        "Checksums: $([System.IO.Path]::GetFileName($combinedChecksumPath))"
    ) | Set-Content -LiteralPath $releaseInfoPath -Encoding utf8

    Write-Host ''
    Write-Host 'Test release build complete.' -ForegroundColor Green
    Write-Host "Signed APK:       $finalApk"
    Write-Host "Windows EXE:      $finalExe"
    Write-Host "Windows updater:  $finalUpdater"
    if ($sourceArchive) {
        Write-Host "Source archive:   $sourceArchive"
    }
    Write-Host "Combined sums:    $combinedChecksumPath"
    Write-Host "Release metadata: $releaseInfoPath"
    if (-not $SelfContained) {
        Write-Host 'Windows package type: framework-dependent single-file app (.NET 8 Desktop Runtime required).' -ForegroundColor Yellow
    }
    else {
        Write-Host 'Windows package type: self-contained single-file app.'
    }
}

try {
    Invoke-CouchLinkReleaseBuild
}
catch {
    $repoRootForLog = [System.IO.Path]::GetFullPath($PSScriptRoot)
    $logDirectory = Join-Path $repoRootForLog 'release'
    New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
    $errorLog = Join-Path $logDirectory 'Build-Release-error.log'

    $details = @(
        "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
        "Message: $($_.Exception.Message)"
        ''
        'Error record:'
        ($_ | Out-String)
        'Script stack trace:'
        $_.ScriptStackTrace
    )
    $details | Set-Content -LiteralPath $errorLog -Encoding utf8

    Write-Host ''
    Write-Host 'COUCHLINK RELEASE BUILD FAILED' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Full error log: $errorLog" -ForegroundColor Yellow
    throw
}
