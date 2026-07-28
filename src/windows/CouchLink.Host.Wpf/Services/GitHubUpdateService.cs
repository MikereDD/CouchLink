using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text.Json;
using CouchLink.Host.Core;

namespace CouchLink.Host.Wpf.Services;

public sealed class GitHubUpdateService
{
    public sealed record UpdateProgress(string Stage, int Percent);
    public sealed record UpdateInfo(
        string Version,
        string ReleaseNotes,
        string HostAssetName,
        Uri HostDownloadUri,
        long HostSize,
        string HostSha256,
        string UpdaterAssetName,
        Uri UpdaterDownloadUri,
        string UpdaterSha256);

    private static readonly Uri LatestStableReleaseUri = new(
        "https://api.github.com/repos/MikereDD/CouchLink/releases/latest");

    private static readonly Uri TestReleasesUri = new(
        "https://api.github.com/repos/MikereDD/CouchLink/releases?per_page=20");

    private const string DownloadPrefix =
        "https://github.com/MikereDD/CouchLink/releases/download/";

    private readonly HttpClient _httpClient;

    public GitHubUpdateService()
    {
        _httpClient = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(60),
        };

        _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(
            $"CouchLink-Windows/{HostConstants.HostVersion}");

        _httpClient.DefaultRequestHeaders.Accept.Add(
            new MediaTypeWithQualityHeaderValue(
                "application/vnd.github+json"));

        _httpClient.DefaultRequestHeaders.Add(
            "X-GitHub-Api-Version",
            "2022-11-28");
    }

    public async Task<UpdateInfo?> CheckAsync(
        bool testChannel = false,
        CancellationToken cancellationToken = default)
    {
        Uri releaseUri = testChannel ? TestReleasesUri : LatestStableReleaseUri;

        using Stream response = await _httpClient.GetStreamAsync(
            releaseUri,
            cancellationToken);

        using JsonDocument document = await JsonDocument.ParseAsync(
            response,
            cancellationToken: cancellationToken);

        JsonElement root = testChannel
            ? FindLatestTestRelease(document.RootElement)
            : document.RootElement;

        if (root.GetProperty("draft").GetBoolean() ||
            (!testChannel && root.GetProperty("prerelease").GetBoolean()))
        {
            return null;
        }

        string version =
            root.GetProperty("tag_name").GetString()?.TrimStart('v')
            ?? throw new InvalidDataException(
                "The GitHub release did not contain a tag name.");

        if (CompareVersions(version, HostConstants.HostVersion) <= 0)
        {
            return null;
        }

        string hostName =
            $"CouchLink-Host-v{version}-win-x64.exe";

        string updaterName =
            $"CouchLink-Updater-v{version}-win-x64.exe";

        JsonElement assets = root.GetProperty("assets");
        JsonElement host = FindAsset(assets, hostName);
        JsonElement updater = FindAsset(assets, updaterName);

        return new UpdateInfo(
            version,
            root.TryGetProperty(
                "body",
                out JsonElement body)
                ? body.GetString() ?? string.Empty
                : string.Empty,
            hostName,
            ValidateDownloadUri(
                host.GetProperty(
                    "browser_download_url").GetString()),
            host.GetProperty("size").GetInt64(),
            ReadDigest(host),
            updaterName,
            ValidateDownloadUri(
                updater.GetProperty(
                    "browser_download_url").GetString()),
            ReadDigest(updater));
    }

    public async Task DownloadAndLaunchAsync(
        UpdateInfo update,
        IProgress<UpdateProgress>? progress = null,
        CancellationToken cancellationToken = default)
    {
        string updateRoot = Path.Combine(
            Path.GetTempPath(),
            "CouchLink",
            "updates",
            $"{update.Version}-{Guid.NewGuid():N}");

        Directory.CreateDirectory(updateRoot);

        string hostPath = Path.Combine(
            updateRoot,
            update.HostAssetName);

        string updaterPath = Path.Combine(
            updateRoot,
            update.UpdaterAssetName);

        progress?.Report(new UpdateProgress("Downloading Host", 0));
        await DownloadVerifiedAsync(
            update.HostDownloadUri,
            hostPath,
            update.HostSha256,
            0,
            75,
            progress,
            cancellationToken);

        progress?.Report(new UpdateProgress("Downloading updater", 75));
        await DownloadVerifiedAsync(
            update.UpdaterDownloadUri,
            updaterPath,
            update.UpdaterSha256,
            75,
            95,
            progress,
            cancellationToken);

        progress?.Report(new UpdateProgress("Preparing restart", 98));

        string target = Environment.ProcessPath
            ?? throw new InvalidOperationException(
                "The current CouchLink executable path is unavailable.");

        string installedHostSha256;
        await using (FileStream installedHostStream = new(
            target,
            FileMode.Open,
            FileAccess.Read,
            FileShare.Read | FileShare.Delete,
            bufferSize: 81920,
            useAsync: true))
        {
            installedHostSha256 = Convert.ToHexString(
                    await SHA256.HashDataAsync(
                        installedHostStream,
                        cancellationToken))
                .ToLowerInvariant();
        }

        var startInfo = new ProcessStartInfo
        {
            FileName = updaterPath,
            UseShellExecute = true,
            WorkingDirectory = updateRoot,
        };

        startInfo.ArgumentList.Add("--pid");
        startInfo.ArgumentList.Add(
            Environment.ProcessId.ToString());

        startInfo.ArgumentList.Add("--source");
        startInfo.ArgumentList.Add(hostPath);

        startInfo.ArgumentList.Add("--target");
        startInfo.ArgumentList.Add(target);

        startInfo.ArgumentList.Add("--restart");
        startInfo.ArgumentList.Add(target);

        startInfo.ArgumentList.Add("--expected-sha256");
        startInfo.ArgumentList.Add(update.HostSha256);

        startInfo.ArgumentList.Add("--expected-target-sha256");
        startInfo.ArgumentList.Add(installedHostSha256);

        _ = Process.Start(startInfo)
            ?? throw new InvalidOperationException(
                "CouchLink Updater could not be started.");
    }

    private async Task DownloadVerifiedAsync(
        Uri uri,
        string destination,
        string expectedSha256,
        int startPercent,
        int endPercent,
        IProgress<UpdateProgress>? progress,
        CancellationToken cancellationToken)
    {
        using HttpResponseMessage response =
            await _httpClient.GetAsync(
                uri,
                HttpCompletionOption.ResponseHeadersRead,
                cancellationToken);

        response.EnsureSuccessStatusCode();

        await using (Stream input =
            await response.Content.ReadAsStreamAsync(
                cancellationToken))
        {
            await using FileStream output = new(
                destination,
                FileMode.Create,
                FileAccess.Write,
                FileShare.None,
                bufferSize: 81920,
                useAsync: true);

            long total = response.Content.Headers.ContentLength ?? 0;
            byte[] buffer = new byte[81920];
            long copied = 0;
            while (true)
            {
                int read = await input.ReadAsync(buffer, cancellationToken);
                if (read == 0) break;
                await output.WriteAsync(buffer.AsMemory(0, read), cancellationToken);
                copied += read;
                if (total > 0)
                {
                    int percent = startPercent + (int)((endPercent - startPercent) * copied / total);
                    progress?.Report(new UpdateProgress("Downloading and verifying", Math.Clamp(percent, startPercent, endPercent)));
                }
            }

            await output.FlushAsync(cancellationToken);
        }

        string actual;

        await using (FileStream verificationStream = new(
            destination,
            FileMode.Open,
            FileAccess.Read,
            FileShare.Read,
            bufferSize: 81920,
            useAsync: true))
        {
            actual = Convert.ToHexString(
                    await SHA256.HashDataAsync(
                        verificationStream,
                        cancellationToken))
                .ToLowerInvariant();
        }

        progress?.Report(new UpdateProgress("Verifying SHA-256", endPercent));

        if (!actual.Equals(
            expectedSha256,
            StringComparison.OrdinalIgnoreCase))
        {
            File.Delete(destination);

            throw new InvalidDataException(
                $"SHA-256 verification failed for " +
                $"{Path.GetFileName(destination)}.");
        }
    }

    private static JsonElement FindLatestTestRelease(JsonElement releases)
    {
        foreach (JsonElement release in releases.EnumerateArray())
        {
            if (!release.GetProperty("draft").GetBoolean() &&
                release.GetProperty("prerelease").GetBoolean())
            {
                return release;
            }
        }

        throw new FileNotFoundException(
            "No published CouchLink test release is available yet.");
    }

    private static JsonElement FindAsset(
        JsonElement assets,
        string name)
    {
        foreach (JsonElement asset in assets.EnumerateArray())
        {
            if (asset.GetProperty("name")
                .GetString()
                ?.Equals(
                    name,
                    StringComparison.Ordinal) == true)
            {
                return asset;
            }
        }

        throw new FileNotFoundException(
            $"GitHub release asset {name} was not found.");
    }

    private static string ReadDigest(
        JsonElement asset)
    {
        string digest = asset.TryGetProperty(
            "digest",
            out JsonElement value)
            ? value.GetString() ?? string.Empty
            : string.Empty;

        if (!digest.StartsWith(
            "sha256:",
            StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException(
                $"{asset.GetProperty("name").GetString()} " +
                "has no GitHub SHA-256 digest.");
        }

        return digest[
            (digest.IndexOf(':') + 1)..]
            .ToLowerInvariant();
    }

    private static Uri ValidateDownloadUri(
        string? raw)
    {
        if (!Uri.TryCreate(
                raw,
                UriKind.Absolute,
                out Uri? uri) ||
            uri.Scheme != Uri.UriSchemeHttps ||
            !uri.AbsoluteUri.StartsWith(
                DownloadPrefix,
                StringComparison.Ordinal))
        {
            throw new InvalidDataException(
                "The release contains an unexpected download URL.");
        }

        return uri;
    }

    private static int CompareVersions(
        string left,
        string right) =>
        CouchLinkVersion.Compare(left, right);
}
