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

    private static readonly Uri LatestReleaseUri = new(
        "https://api.github.com/repos/MikereDD/CouchLink/releases/latest");

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
        CancellationToken cancellationToken = default)
    {
        using Stream response = await _httpClient.GetStreamAsync(
            LatestReleaseUri,
            cancellationToken);

        using JsonDocument document = await JsonDocument.ParseAsync(
            response,
            cancellationToken: cancellationToken);

        JsonElement root = document.RootElement;

        if (root.GetProperty("draft").GetBoolean() ||
            root.GetProperty("prerelease").GetBoolean())
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
        CancellationToken cancellationToken = default)
    {
        string updateRoot = Path.Combine(
            Path.GetTempPath(),
            "CouchLink",
            "updates",
            update.Version);

        if (Directory.Exists(updateRoot))
        {
            Directory.Delete(updateRoot, recursive: true);
        }

        Directory.CreateDirectory(updateRoot);

        string hostPath = Path.Combine(
            updateRoot,
            update.HostAssetName);

        string updaterPath = Path.Combine(
            updateRoot,
            update.UpdaterAssetName);

        await DownloadVerifiedAsync(
            update.HostDownloadUri,
            hostPath,
            update.HostSha256,
            cancellationToken);

        await DownloadVerifiedAsync(
            update.UpdaterDownloadUri,
            updaterPath,
            update.UpdaterSha256,
            cancellationToken);

        string target = Environment.ProcessPath
            ?? throw new InvalidOperationException(
                "The current CouchLink executable path is unavailable.");

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

        _ = Process.Start(startInfo)
            ?? throw new InvalidOperationException(
                "CouchLink Updater could not be started.");
    }

    private async Task DownloadVerifiedAsync(
        Uri uri,
        string destination,
        string expectedSha256,
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

            await input.CopyToAsync(
                output,
                cancellationToken);

            await output.FlushAsync(
                cancellationToken);
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
        string right)
    {
        static int[] Parts(string value) =>
            value.Split('-', 2)[0]
                .Split('.')
                .Select(
                    part => int.TryParse(
                        part,
                        out int number)
                        ? number
                        : 0)
                .ToArray();

        int[] a = Parts(left);
        int[] b = Parts(right);

        for (int index = 0;
             index < Math.Max(a.Length, b.Length);
             index++)
        {
            int result = a
                .ElementAtOrDefault(index)
                .CompareTo(
                    b.ElementAtOrDefault(index));

            if (result != 0)
            {
                return result;
            }
        }

        return 0;
    }
}
