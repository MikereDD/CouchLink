# CouchLink v1.3.1-dev.5

Audit-fix test build focused on the Windows updater.

- Correct prerelease-aware Windows version ordering.
- Add automated version-comparison tests.
- Re-verify the downloaded Host inside the external updater.
- Restrict updater paths and randomize staging directories.
- Improve rollback and restart-on-failure behavior.
- Add cancellation and safer updater UI state handling.
- Add release-version consistency checks.

Windows Authenticode or detached-signature authenticity remains a required item before stable promotion.
