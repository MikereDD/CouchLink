[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$Version = '1.1',

    [ValidateNotNullOrEmpty()]
    [string]$Runtime = 'win-x64',

    [string]$KeystorePath,

    [ValidateNotNullOrEmpty()]
    [string]$KeyAlias = 'couchlink-release',

    [switch]$SelfContained,

    [switch]$SkipAndroidSignatureVerification,

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

function Invoke-CouchLinkReleaseBuild {
    $repoRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
    $androidRoot = Join-Path $repoRoot 'src\android'
    $windowsRoot = Join-Path $repoRoot 'src\windows'
    $androidBuildScript = Join-Path $androidRoot 'tools\Build-SignedRelease.ps1'
    $androidVerifyScript = Join-Path $androidRoot 'tools\Verify-SignedRelease.ps1'
    $windowsProject = Join-Path $windowsRoot 'CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj'

    foreach ($requiredPath in @($androidBuildScript, $androidVerifyScript, $windowsProject)) {
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

    Write-Host ''
    Write-Host "CouchLink v$Version release build" -ForegroundColor Cyan
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
    Write-Host '[1/4] Building signed Android APK...' -ForegroundColor Cyan

    # Hashtable splatting is required here because these are named parameters.
    # Array splatting passes values positionally and caused the Android helper
    # to exit before reaching its signing prompts.
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
        Write-Host '[2/4] Verifying Android signature...' -ForegroundColor Cyan
        & $androidVerifyScript -ApkPath $finalApk
    }
    else {
        Write-Host '[2/4] Android signature verification skipped by request.' -ForegroundColor Yellow
    }

    Write-Host '[3/4] Publishing Windows host...' -ForegroundColor Cyan
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

    Write-Host '[4/4] Generating SHA-256 checksums...' -ForegroundColor Cyan
    $apkHash = Write-Sha256File -Path $finalApk
    $exeHash = Write-Sha256File -Path $finalExe

    $combinedChecksumPath = Join-Path $resolvedOutputDirectory "SHA256SUMS-v$Version.txt"
    @(
        "$($apkHash.Hash)  $($apkHash.FileName)"
        "$($exeHash.Hash)  $($exeHash.FileName)"
    ) | Set-Content -LiteralPath $combinedChecksumPath -Encoding ascii

    Write-Host ''
    Write-Host 'Release build complete.' -ForegroundColor Green
    Write-Host "Signed APK:       $finalApk"
    Write-Host "Windows EXE:      $finalExe"
    Write-Host "APK checksum:     $($apkHash.Checksum)"
    Write-Host "Windows checksum: $($exeHash.Checksum)"
    Write-Host "Combined sums:    $combinedChecksumPath"
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
