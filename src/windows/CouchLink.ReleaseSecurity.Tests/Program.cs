using System.Security.Cryptography;
using CouchLink.ReleaseSecurity;

namespace CouchLink.ReleaseSecurity.Tests;

internal static class Program
{
    private static int Main(string[] args)
    {
        string? temporaryDirectory = null;

        try
        {
            Dictionary<string, string> options = ParseArguments(args);
            string payload = Path.GetFullPath(GetRequired(options, "payload"));
            string signature = Path.GetFullPath(GetRequired(options, "signature"));
            string label = GetOptional(options, "label") ?? Path.GetFileName(payload);

            ReleaseSignatureVerifier.EnsureConfigured();
            string fingerprint =
                ReleaseSignatureVerifier.GetPublicKeyFingerprintSha256();

            if (!fingerprint.Equals(
                    PinnedReleaseKey.PublicKeySha256Fingerprint,
                    StringComparison.OrdinalIgnoreCase))
            {
                throw new CryptographicException(
                    "The computed release-key fingerprint does not match the pinned fingerprint.");
            }

            ReleaseSignatureVerifier.VerifyFile(payload, signature);
            Console.WriteLine($"PASS: {label} valid signature accepted.");

            temporaryDirectory = Path.Combine(
                Path.GetTempPath(),
                $"CouchLink-signature-tests-{Guid.NewGuid():N}");
            Directory.CreateDirectory(temporaryDirectory);

            string tamperedPayload = Path.Combine(
                temporaryDirectory,
                Path.GetFileName(payload));
            File.Copy(payload, tamperedPayload, overwrite: true);
            FlipOneByte(tamperedPayload);

            ExpectFailure<CryptographicException>(
                () => ReleaseSignatureVerifier.VerifyFile(
                    tamperedPayload,
                    signature),
                $"{label} modified payload rejected");

            string tamperedSignature = Path.Combine(
                temporaryDirectory,
                Path.GetFileName(signature));
            byte[] signatureBytes = Convert.FromBase64String(
                File.ReadAllText(signature).Trim());
            signatureBytes[^1] ^= 0x01;
            File.WriteAllText(
                tamperedSignature,
                Convert.ToBase64String(signatureBytes));

            ExpectFailure<CryptographicException>(
                () => ReleaseSignatureVerifier.VerifyFile(
                    payload,
                    tamperedSignature),
                $"{label} modified signature rejected");

            string malformedSignature = Path.Combine(
                temporaryDirectory,
                "malformed.sig");
            File.WriteAllText(malformedSignature, "not-valid-base64!");

            ExpectFailure<InvalidDataException>(
                () => ReleaseSignatureVerifier.VerifyFile(
                    payload,
                    malformedSignature),
                $"{label} malformed signature rejected");

            Console.WriteLine(
                $"PASS: {label} release-authenticity test suite completed.");
            return 0;
        }
        catch (Exception exception)
        {
            Console.Error.WriteLine(exception);
            return 1;
        }
        finally
        {
            if (temporaryDirectory is not null)
            {
                try
                {
                    Directory.Delete(temporaryDirectory, recursive: true);
                }
                catch
                {
                    // Test cleanup must not conceal the actual result.
                }
            }
        }
    }

    private static void FlipOneByte(string path)
    {
        using FileStream stream = new(
            path,
            FileMode.Open,
            FileAccess.ReadWrite,
            FileShare.None);

        if (stream.Length == 0)
        {
            throw new InvalidDataException(
                "The payload is empty and cannot be tampered for testing.");
        }

        long position = stream.Length / 2;
        stream.Position = position;
        int original = stream.ReadByte();
        if (original < 0)
        {
            throw new EndOfStreamException(
                "Could not read the payload byte selected for tampering.");
        }

        stream.Position = position;
        stream.WriteByte((byte)(original ^ 0x01));
        stream.Flush(flushToDisk: true);
    }

    private static void ExpectFailure<TException>(
        Action action,
        string description)
        where TException : Exception
    {
        try
        {
            action();
        }
        catch (TException)
        {
            Console.WriteLine($"PASS: {description}.");
            return;
        }

        throw new InvalidOperationException(
            $"FAIL: {description}; verification unexpectedly succeeded.");
    }

    private static Dictionary<string, string> ParseArguments(string[] args)
    {
        if (args.Length == 0 || args.Length % 2 != 0)
        {
            throw new ArgumentException(
                "Arguments must be supplied as --name value pairs.");
        }

        Dictionary<string, string> result =
            new(StringComparer.OrdinalIgnoreCase);

        for (int index = 0; index < args.Length; index += 2)
        {
            string key = args[index];
            if (!key.StartsWith("--", StringComparison.Ordinal) ||
                key.Length <= 2)
            {
                throw new ArgumentException(
                    $"Unexpected argument: {key}");
            }

            result[key[2..]] = args[index + 1];
        }

        return result;
    }

    private static string GetRequired(
        IReadOnlyDictionary<string, string> values,
        string name) =>
        values.TryGetValue(name, out string? value) &&
        !string.IsNullOrWhiteSpace(value)
            ? value
            : throw new ArgumentException($"Missing --{name} argument.");

    private static string? GetOptional(
        IReadOnlyDictionary<string, string> values,
        string name) =>
        values.TryGetValue(name, out string? value) ? value : null;
}
