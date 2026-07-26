using System.Diagnostics;

namespace CouchLink.Updater;

internal static class Program
{
    private static int Main(string[] args)
    {
        try
        {
            Dictionary<string, string> options = ParseArguments(args);
            int processId = int.Parse(GetRequired(options, "pid"));
            string source = Path.GetFullPath(GetRequired(options, "source"));
            string target = Path.GetFullPath(GetRequired(options, "target"));
            string restart = Path.GetFullPath(GetRequired(options, "restart"));

            WaitForExit(processId);
            if (!File.Exists(source)) throw new FileNotFoundException("Downloaded update was not found.", source);

            string backup = target + ".previous";
            if (File.Exists(backup)) File.Delete(backup);
            if (File.Exists(target)) File.Move(target, backup, true);

            try
            {
                File.Move(source, target, true);
                Process.Start(new ProcessStartInfo
                {
                    FileName = restart,
                    UseShellExecute = true,
                    WorkingDirectory = Path.GetDirectoryName(restart) ?? Environment.CurrentDirectory,
                });
                Thread.Sleep(2500);
                if (File.Exists(backup)) File.Delete(backup);
            }
            catch
            {
                if (File.Exists(target)) File.Delete(target);
                if (File.Exists(backup)) File.Move(backup, target, true);
                throw;
            }

            return 0;
        }
        catch (Exception ex)
        {
            string log = Path.Combine(Path.GetTempPath(), "CouchLink-Updater-error.log");
            File.WriteAllText(log, $"{DateTimeOffset.Now:u}\n{ex}");
            return 1;
        }
    }

    private static void WaitForExit(int processId)
    {
        try
        {
            using Process process = Process.GetProcessById(processId);
            process.WaitForExit(30_000);
            if (!process.HasExited) throw new TimeoutException("CouchLink Host did not exit within 30 seconds.");
        }
        catch (ArgumentException)
        {
            // The host already exited.
        }
    }

    private static Dictionary<string, string> ParseArguments(string[] args)
    {
        Dictionary<string, string> result = new(StringComparer.OrdinalIgnoreCase);
        for (int index = 0; index + 1 < args.Length; index += 2)
            result[args[index].TrimStart('-')] = args[index + 1];
        return result;
    }

    private static string GetRequired(IReadOnlyDictionary<string, string> values, string name) =>
        values.TryGetValue(name, out string? value) && !string.IsNullOrWhiteSpace(value)
            ? value
            : throw new ArgumentException($"Missing --{name} argument.");
}
