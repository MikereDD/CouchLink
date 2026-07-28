using System.Security.Cryptography;

namespace CouchLink.ReleaseSecurity;

public static class ReleaseSignatureVerifier
{
    public static void EnsureConfigured()
    {
        if (string.IsNullOrWhiteSpace(PinnedReleaseKey.PublicKeyPem) ||
            PinnedReleaseKey.PublicKeyPem.Equals(
                "UNCONFIGURED",
                StringComparison.Ordinal))
        {
            throw new InvalidOperationException(
                "The CouchLink Windows release-signing public key is not configured.");
        }

        string expectedFingerprint =
            NormalizeSha256(PinnedReleaseKey.PublicKeySha256Fingerprint);
        string actualFingerprint = GetPublicKeyFingerprintSha256();

        if (!actualFingerprint.Equals(
                expectedFingerprint,
                StringComparison.OrdinalIgnoreCase))
        {
            throw new CryptographicException(
                "The pinned CouchLink release public-key fingerprint does not match the configured key.");
        }
    }

    public static string GetPublicKeyFingerprintSha256()
    {
        if (string.IsNullOrWhiteSpace(PinnedReleaseKey.PublicKeyPem) ||
            PinnedReleaseKey.PublicKeyPem.Equals(
                "UNCONFIGURED",
                StringComparison.Ordinal))
        {
            throw new InvalidOperationException(
                "The CouchLink Windows release-signing public key is not configured.");
        }

        using ECDsa verifier = ECDsa.Create();
        verifier.ImportFromPem(PinnedReleaseKey.PublicKeyPem);
        byte[] publicKey = verifier.ExportSubjectPublicKeyInfo();
        return Convert.ToHexString(SHA256.HashData(publicKey)).ToLowerInvariant();
    }

    public static void VerifyFile(
        string payloadPath,
        string signaturePath)
    {
        EnsureConfigured();

        if (!File.Exists(payloadPath))
        {
            throw new FileNotFoundException(
                "The signed CouchLink payload was not found.",
                payloadPath);
        }

        if (!File.Exists(signaturePath))
        {
            throw new FileNotFoundException(
                "The CouchLink detached signature was not found.",
                signaturePath);
        }

        byte[] signature;
        try
        {
            string encoded = File.ReadAllText(signaturePath).Trim();
            signature = Convert.FromBase64String(encoded);
        }
        catch (FormatException exception)
        {
            throw new InvalidDataException(
                "The CouchLink detached signature is not valid Base64.",
                exception);
        }

        using ECDsa verifier = ECDsa.Create();
        verifier.ImportFromPem(PinnedReleaseKey.PublicKeyPem);

        using FileStream payload = new(
            payloadPath,
            FileMode.Open,
            FileAccess.Read,
            FileShare.Read);

        if (!verifier.VerifyData(
                payload,
                signature,
                HashAlgorithmName.SHA256))
        {
            throw new CryptographicException(
                $"Detached signature verification failed for {Path.GetFileName(payloadPath)}.");
        }
    }

    private static string NormalizeSha256(string value)
    {
        string normalized = value.Trim().ToLowerInvariant();
        if (normalized.Length != 64 ||
            normalized.Any(character => !Uri.IsHexDigit(character)))
        {
            throw new InvalidDataException(
                "The pinned public-key SHA-256 fingerprint is invalid.");
        }

        return normalized;
    }
}
