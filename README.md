# PauseBite Reliability POC v0.4

Android test app that detects Swiggy/Zomato foreground events after user enables Usage Access, overlay permission and starts a foreground monitoring service. The app does not request Internet access, use Accessibility Service, or transmit app usage information.

## Reliability changes
- Overlapping usage-event queries and deduplication to tolerate late event publication.
- Detect target app transitions; ignore in-app screen changes.
- Distinguish detections, popups, snooze/cooldown suppression, and overlay failures.
- 5-second cooldown for testing; 30-minute cooldown for normal usage.
- On-device diagnostic counters and manual refresh.

## Updates and signing
Android updates require **the same application ID, a higher versionCode, and the same signing certificate**. Previous GitHub debug APKs may use different automatically generated signing keys, so an in-place update from v0.3 is **not guaranteed**. A one-time uninstall may be needed before installing a build signed with a persistent key.

For reliable future in-place updates, configure GitHub Actions repository secrets:
- `PAUSEBITE_KEYSTORE_BASE64`: base64 encoding of your private Android release keystore.
- `PAUSEBITE_KEYSTORE_PASSWORD`
- `PAUSEBITE_KEY_ALIAS`
- `PAUSEBITE_KEY_PASSWORD`

The workflow produces a persistently signed release APK **only when these secrets are configured**. Never commit a signing key or passwords to this public repository. Keep a secure backup of the key; losing it prevents updates signed with that certificate.

A debug APK remains available for basic testing but is not a dependable update channel.

## Security and distribution
Google Play Protect has flagged earlier builds. This warning has **not** been resolved or explained by this code change. Do not disable Play Protect or assume an APK is safe just because GitHub Actions builds it. For Play Store release, review overlay/usage permissions, foreground-service eligibility, and app interference policies. No automatic self-update installation is implemented; Android requires user-approved installation or a store-managed update.
