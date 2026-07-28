using CouchLink.Host.Core;

namespace CouchLink.Versioning.Tests;

internal static class Program
{
    private sealed record VersionCase(string Left, string Right, int ExpectedSign);

    private static int Main()
    {
        VersionCase[] cases =
        [
            new("1.3.1-dev.6.1", "1.3.1-dev.6", 1),
            new("1.3.1-dev.6", "1.3.1-dev.6.1", -1),
            new("1.3.1-dev.6", "1.3.1-dev.5", 1),
            new("1.3.1-dev.5", "1.3.1-dev.6", -1),
            new("1.3.1-dev.5", "1.3.1-dev.4", 1),
            new("1.3.1-dev.4", "1.3.1-dev.5", -1),
            new("1.3.1-dev.4", "1.3.1-dev.4", 0),
            new("1.3.1", "1.3.1-dev.4", 1),
            new("1.3.1-rc.1", "1.3.1-beta.9", 1),
            new("1.3.1-beta.1", "1.3.1-alpha.9", 1),
            new("1.3.1-alpha.2", "1.3.1-dev.9", 1),
            new("1.3.1-dev.9", "1.3.1-alpha.2", -1),
            new("1.3.2-dev.1", "1.3.1", 1),
            new("v1.3.1-dev.5", "1.3.1-dev.4", 1),
            new("1.3.1-dev.5-debug", "1.3.1-dev.5", 0),
            new("1.3.1-dev.5-release", "1.3.1-dev.5", 0),
            new("1.3.1-preview.2", "1.3.1-rc.9", 1),
            new("1.3.1", "1.3.1.0", 0),
        ];

        int failures = 0;
        foreach (VersionCase test in cases)
        {
            int actual = Math.Sign(CouchLinkVersion.Compare(test.Left, test.Right));
            if (actual == test.ExpectedSign)
            {
                Console.WriteLine($"PASS  {test.Left} ? {test.Right} => {actual}");
                continue;
            }

            failures++;
            Console.Error.WriteLine(
                $"FAIL  {test.Left} ? {test.Right}: expected {test.ExpectedSign}, got {actual}");
        }

        Console.WriteLine();
        Console.WriteLine(failures == 0
            ? $"All {cases.Length} CouchLink version-comparison tests passed."
            : $"{failures} of {cases.Length} CouchLink version-comparison tests failed.");

        return failures == 0 ? 0 : 1;
    }
}
