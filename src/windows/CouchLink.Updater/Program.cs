using System.Diagnostics;
using System.Security.Cryptography;

namespace CouchLink.Updater;

internal static class Program
{
    private static int Main(string[] args)
    {
        string? restart = null;
        string? target = null;
        string? backup = null;

        try
        {
            Dictionary<string, string> options = ParseArguments(args);
            int processId = int.Parse(GetRequired(options, "pid"));
            string source = Path.GetFullPath(GetRequired(options, "source"));
            target = Path.GetFullPath(GetRequired(options, "target"));
            restart = Path.GetFullPath(GetRequired(options, "restart"));
            string expectedSha256 = NormalizeSha256(GetRequired(options, "expected-sha256"));
            string? expectedTargetSha256 = GetOptional(options, "expected-target-sha256");
            if (!string.IsNullOrWhiteSpace(expectedTargetSha256))
            {
                expectedTargetSha256 = NormalizeSha256(expectedTargetSha256);
            }

            ValidatePaths(source, target, restart);
            WaitForExit(processId);

            if (!File.Exists(source))
            {
                throw new FileNotFoundException("Downloaded update was not found.", source);
            }

            if (!File.Exists(target))
            {
                throw new FileNotFoundException(
                    "The installed CouchLink Host was not found.",
                    target);
            }

            if (!string.IsNullOrWhiteSpace(expectedTargetSha256))
            {
                string actualTargetSha256 = ComputeSha256(target);
                if (!actualTargetSha256.Equals(
                        expectedTargetSha256,
                        StringComparison.OrdinalIgnoreCase))
                {
                    throw new InvalidDataException(
                        "The installed CouchLink Host changed before replacement.");
                }
            }

            string actualSha256 = ComputeSha256(source);
            if (!actualSha256.Equals(expectedSha256, StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidDataException("The downloaded Host failed updater-side SHA-256 verification.");
            }

            backup = target + ".previous";
            if (File.Exists(backup))
            {
                File.Delete(backup);
            }

            if (File.Exists(target))
            {
                File.Move(target, backup, overwrite: true);
            }

            try
            {
                File.Move(source, target, overwrite: true);
                StartHost(restart);
                WriteSuccessReceipt(
                    source,
                    target,
                    !string.IsNullOrWhiteSpace(expectedTargetSha256));

                // Keep the previous executable for manual rollback and for the next
                // updater run to replace. A future health-handshake can safely remove it.
                return 0;
            }
            catch
            {
                RestoreBackup(target, backup);
                TryRestartHost(restart);
                throw;
            }
        }
        catch (Exception ex)
        {
            if (target is not null && backup is not null)
            {
                try
                {
                    RestoreBackup(target, backup);
                }
                catch
                {
                    // Preserve the original updater failure in the log.
                }
            }

            if (restart is not null)
            {
                TryRestartHost(restart);
            }

            WriteErrorLog(ex);
            return 1;
        }
    }

    private static void ValidatePaths(
        string source,
        string target,
        string restart)
    {
        if (!target.Equals(restart, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("The restart path must match the Host target path.");
        }

        if (!Path.GetExtension(target).Equals(".exe", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("The updater target must be a Windows executable.");
        }

        if (!Path.GetExtension(source).Equals(".exe", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("The updater source must be a Windows executable.");
        }

        string trustedRoot = Path.GetFullPath(Path.Combine(
            Path.GetTempPath(),
            "CouchLink",
            "updates"));

        string relative = Path.GetRelativePath(trustedRoot, source);
        if (relative.Equals("..", StringComparison.Ordinal) ||
            relative.StartsWith($"..{Path.DirectorySeparatorChar}", StringComparison.Ordinal) ||
            Path.IsPathRooted(relative))
        {
            throw new InvalidDataException("The updater source is outside CouchLink's trusted staging directory.");
        }
    }

    private static string NormalizeSha256(string value)
    {
        string normalized = value.Trim().ToLowerInvariant();
        if (normalized.Length != 64 || normalized.Any(character => !Uri.IsHexDigit(character)))
        {
            throw new InvalidDataException("The expected SHA-256 value is invalid.");
        }

        return normalized;
    }

    private static string ComputeSha256(string path)
    {
        using FileStream stream = new(path, FileMode.Open, FileAccess.Read, FileShare.Read);
        return Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant();
    }

    private static void RestoreBackup(string target, string backup)
    {
        if (File.Exists(target))
        {
            File.Delete(target);
        }

        if (File.Exists(backup))
        {
            File.Move(backup, target, overwrite: true);
        }
    }

    private static void StartHost(string restart)
    {
        Process process = Process.Start(new ProcessStartInfo
        {
            FileName = restart,
            UseShellExecute = true,
            WorkingDirectory = Path.GetDirectoryName(restart) ?? Environment.CurrentDirectory,
        }) ?? throw new InvalidOperationException("The updated CouchLink Host could not be started.");

        process.Dispose();
    }

    private static void TryRestartHost(string restart)
    {
        try
        {
            if (File.Exists(restart))
            {
                StartHost(restart);
            }
        }
        catch
        {
            // The error log remains the final recovery path.
        }
    }

    private static void WriteSuccessReceipt(
        string source,
        string target,
        bool installedTargetSha256Verified)
    {
        try
        {
            string path = Path.Combine(
                Path.GetTempPath(),
                "CouchLink-Updater-success.log");

            string content =
                $"{DateTimeOffset.UtcNow:O}{Environment.NewLine}" +
                $"Source: {source}{Environment.NewLine}" +
                $"Target: {target}{Environment.NewLine}" +
                $"Downloaded payload SHA-256 verified: true{Environment.NewLine}" +
                $"Installed target SHA-256 verified: {installedTargetSha256Verified.ToString().ToLowerInvariant()}{Environment.NewLine}" +
                $"Replacement completed: true{Environment.NewLine}" +
                $"Restart requested: true{Environment.NewLine}";

            File.WriteAllText(path, content);
        }
        catch
        {
            // A diagnostic receipt must never turn a successful update into a failure.
        }
    }

    private static void WriteErrorLog(Exception ex)
    {
        string log = Path.Combine(Path.GetTempPath(), "CouchLink-Updater-error.log");
        File.WriteAllText(log, $"{DateTimeOffset.Now:u}{Environment.NewLine}{ex}");
    }

    private static void WaitForExit(int processId)
    {
        try
        {
            using Process process = Process.GetProcessById(processId);
            process.WaitForExit(30_000);
            if (!process.HasExited)
            {
                throw new TimeoutException("CouchLink Host did not exit within 30 seconds.");
            }
        }
        catch (ArgumentException)
        {
            // The Host already exited.
        }
    }

    private static Dictionary<string, string> ParseArguments(string[] args)
    {
        if (args.Length == 0 || args.Length % 2 != 0)
        {
            throw new ArgumentException("Updater arguments must be supplied as --name value pairs.");
        }

        Dictionary<string, string> result = new(StringComparer.OrdinalIgnoreCase);
        for (int index = 0; index < args.Length; index += 2)
        {
            string key = args[index];
            if (!key.StartsWith("--", StringComparison.Ordinal) || key.Length <= 2)
            {
                throw new ArgumentException($"Unexpected updater argument: {key}");
            }

            result[key[2..]] = args[index + 1];
        }

        return result;
    }

    private static string? GetOptional(
        IReadOnlyDictionary<string, string> values,
        string name) =>
        values.TryGetValue(name, out string? value) ? value : null;

    private static string GetRequired(IReadOnlyDictionary<string, string> values, string name) =>
        values.TryGetValue(name, out string? value) && !string.IsNullOrWhiteSpace(value)
            ? value
            : throw new ArgumentException($"Missing --{name} argument.");
}
