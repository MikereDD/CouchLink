using System.Globalization;

namespace CouchLink.Host.Core;

public static class CouchLinkVersion
{
    private sealed record ParsedVersion(
        IReadOnlyList<int> Core,
        int Rank,
        IReadOnlyList<int> SuffixNumbers,
        string SuffixLabel);

    public static int Compare(string left, string right)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(left);
        ArgumentException.ThrowIfNullOrWhiteSpace(right);

        ParsedVersion a = Parse(left);
        ParsedVersion b = Parse(right);

        int result = CompareNumberLists(a.Core, b.Core);
        if (result != 0)
        {
            return result;
        }

        result = a.Rank.CompareTo(b.Rank);
        if (result != 0)
        {
            return result;
        }

        result = string.Compare(
            a.SuffixLabel,
            b.SuffixLabel,
            StringComparison.OrdinalIgnoreCase);
        if (result != 0)
        {
            return result;
        }

        return CompareNumberLists(a.SuffixNumbers, b.SuffixNumbers);
    }

    private static ParsedVersion Parse(string raw)
    {
        string value = raw.Trim();
        if (value.StartsWith('v') || value.StartsWith('V'))
        {
            value = value[1..];
        }

        value = RemoveBuildSuffix(value, "-debug");
        value = RemoveBuildSuffix(value, "-release");

        string[] parts = value.Split('-', 2, StringSplitOptions.TrimEntries);
        IReadOnlyList<int> core = ParseNumberList(parts[0]);

        if (parts.Length == 1 || string.IsNullOrWhiteSpace(parts[1]))
        {
            return new ParsedVersion(core, 4, Array.Empty<int>(), string.Empty);
        }

        string[] suffixTokens = parts[1].Split(
            '.',
            StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);

        string label = suffixTokens.FirstOrDefault()?.ToLowerInvariant() ?? string.Empty;
        int rank = label switch
        {
            "dev" => 0,
            "alpha" => 1,
            "beta" => 2,
            "rc" => 3,
            _ => 4,
        };

        List<int> numbers = suffixTokens
            .Skip(1)
            .SelectMany(ParseMixedToken)
            .ToList();

        return new ParsedVersion(core, rank, numbers, label);
    }

    private static string RemoveBuildSuffix(string value, string suffix) =>
        value.EndsWith(suffix, StringComparison.OrdinalIgnoreCase)
            ? value[..^suffix.Length]
            : value;

    private static IReadOnlyList<int> ParseNumberList(string value) =>
        value.Split('.', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(ParseNumber)
            .ToArray();

    private static IEnumerable<int> ParseMixedToken(string token)
    {
        if (int.TryParse(token, NumberStyles.None, CultureInfo.InvariantCulture, out int exact))
        {
            yield return exact;
            yield break;
        }

        string digits = new(token.Where(char.IsDigit).ToArray());
        if (digits.Length > 0 &&
            int.TryParse(digits, NumberStyles.None, CultureInfo.InvariantCulture, out int extracted))
        {
            yield return extracted;
        }
    }

    private static int ParseNumber(string token) =>
        int.TryParse(token, NumberStyles.None, CultureInfo.InvariantCulture, out int number)
            ? number
            : 0;

    private static int CompareNumberLists(
        IReadOnlyList<int> left,
        IReadOnlyList<int> right)
    {
        int length = Math.Max(left.Count, right.Count);
        for (int index = 0; index < length; index++)
        {
            int a = index < left.Count ? left[index] : 0;
            int b = index < right.Count ? right[index] : 0;
            int result = a.CompareTo(b);
            if (result != 0)
            {
                return result;
            }
        }

        return 0;
    }
}
