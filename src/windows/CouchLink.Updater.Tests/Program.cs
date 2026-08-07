using UpdaterProgram = CouchLink.Updater.Program;

namespace CouchLink.Updater.Tests;

// Focused coverage for the two pre-RC updater fixes:
//   #1 RestoreBackup must be harmless when no backup exists (no double-restore delete).
//   #2 A detached signature is mandatory; unsigned invocation fails closed.
internal static class UpdaterSafetyTests
{
    private static int Main()
    {
        int failures = 0;
        failures += RunRestoreBackupTests();
        failures += RunSignatureRequiredTests();

        Console.WriteLine();
        Console.WriteLine(failures == 0
            ? "All CouchLink updater safety tests passed."
            : $"{failures} CouchLink updater safety test(s) failed.");

        return failures == 0 ? 0 : 1;
    }

    // Fix #1 — the rollback recovery path must not delete an already-restored Host.
    private static int RunRestoreBackupTests()
    {
        int failures = 0;
        string root = Path.Combine(
            Path.GetTempPath(),
            "CouchLink-Updater-Tests-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(root);

        try
        {
            string target = Path.Combine(root, "host.exe");
            string backup = target + ".previous";

            // Case A: backup present -> target is replaced by the backup contents,
            // and the backup is consumed.
            File.WriteAllText(target, "new-broken");
            File.WriteAllText(backup, "old-good");
            UpdaterProgram.RestoreBackup(target, backup);
            failures += Check(
                "restore replaces target from backup",
                File.Exists(target) && File.ReadAllText(target) == "old-good");
            failures += Check(
                "restore consumes backup",
                !File.Exists(backup));

            // Case B (the fix): a second restore with no backup present must NOT
            // delete the target that was just restored.
            UpdaterProgram.RestoreBackup(target, backup);
            failures += Check(
                "second restore with no backup preserves restored target",
                File.Exists(target) && File.ReadAllText(target) == "old-good");

            // Case C: neither file present -> no throw, nothing created.
            string lone = Path.Combine(root, "lonely.exe");
            UpdaterProgram.RestoreBackup(lone, lone + ".previous");
            failures += Check(
                "restore with neither file present is a no-op",
                !File.Exists(lone));
        }
        finally
        {
            Directory.Delete(root, recursive: true);
        }

        return failures;
    }

    // Fix #2 — the updater requires a detached signature and fails closed without one.
    private static int RunSignatureRequiredTests()
    {
        int failures = 0;

        var withoutSignature = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["pid"] = "1234",
            ["source"] = "s.exe",
            ["target"] = "t.exe",
            ["restart"] = "t.exe",
            ["expected-sha256"] = new string('a', 64),
        };
        failures += Check(
            "missing --signature is rejected (fail closed)",
            Throws<ArgumentException>(() => UpdaterProgram.GetRequired(withoutSignature, "signature")));

        var blankSignature = new Dictionary<string, string>(withoutSignature, StringComparer.OrdinalIgnoreCase)
        {
            ["signature"] = "   ",
        };
        failures += Check(
            "blank --signature is rejected (fail closed)",
            Throws<ArgumentException>(() => UpdaterProgram.GetRequired(blankSignature, "signature")));

        var withSignature = new Dictionary<string, string>(withoutSignature, StringComparer.OrdinalIgnoreCase)
        {
            ["signature"] = "s.exe.sig",
        };
        failures += Check(
            "present --signature is accepted",
            UpdaterProgram.GetRequired(withSignature, "signature") == "s.exe.sig");

        return failures;
    }

    private static bool Throws<TException>(Action action)
        where TException : Exception
    {
        try
        {
            action();
            return false;
        }
        catch (TException)
        {
            return true;
        }
        catch
        {
            return false;
        }
    }

    private static int Check(string name, bool condition)
    {
        Console.WriteLine((condition ? "PASS  " : "FAIL  ") + name);
        return condition ? 0 : 1;
    }
}
