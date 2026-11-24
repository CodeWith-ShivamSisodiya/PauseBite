# PauseBite

PauseBite is an opt-in, offline Android app that shows a clearly branded, dismissible mindful-eating Pause Card when the user opens a food app they explicitly selected.

## Privacy and permissions

- **Usage Access (`PACKAGE_USAGE_STATS`)** is used locally to detect foreground transitions only for supported food apps selected by the user. PauseBite ignores and does not store a history of other apps.
- **Foreground Service / Special Use** keeps user-started monitoring active while the screen is on. The ongoing notification always includes a Stop action.
- **Display over other apps (`SYSTEM_ALERT_WINDOW`)** shows a small non-modal Pause Card only after a selected food app opens. It does not dim or cover the whole screen, and areas outside the card remain visible and touchable. The permission is requested through Android Settings after a prominent disclosure and can be revoked at any time.
- **Notifications** show only the required monitoring status and its Stop action.
- PauseBite has no Internet, Accessibility, SMS, contacts, location, or package-enumeration permission.

Monitoring starts only after the user selects at least one supported food app, grants Usage Access, overlay access, and notifications, then taps **START**. It stops from the app, the ongoing status notification, or the Pause Card itself, and pauses while the screen is off. No app-usage data is uploaded or shared.

## Overlay preview scope

Version 1.1.1 uses a deliberately minimal `TYPE_APPLICATION_OVERLAY` experiment while retaining the production package identity, persistent release signing, offline operation, explicit disclosures, user-selected targets, user-controlled monitoring, and minimum permission set. The preview contains one small Pause Card with Continue, Dismiss-for-today, and Stop-monitoring actions; richer content comes later.

The original Play Protect warning was observed on the older POC after several variables changed together, so overlay permission alone was not isolated as the cause. This build is intended for controlled Play Internal Testing. See [docs/PLAY_PROTECT_REMEDIATION.md](docs/PLAY_PROTECT_REMEDIATION.md) for the historical findings and appeal procedure.

## Release path

The production identity is `com.pausebite.app`, version code 10, and target API 36. The previous `com.pausebite.overlaytest` package was a POC identity and is intentionally not update-compatible.

1. Configure `PAUSEBITE_KEYSTORE_BASE64`, `PAUSEBITE_KEYSTORE_PASSWORD`, `PAUSEBITE_KEY_ALIAS`, and `PAUSEBITE_KEY_PASSWORD` as GitHub Actions secrets. Keep this upload key permanent and backed up.
2. Run **Build verified PauseBite release**.
3. Download the `PauseBite-Play-Internal-Testing` artifact and upload `app-release.aab` to a new Google Play Console app with Play App Signing enabled.
4. Complete the foreground-service declaration in **Policy > App content**, selecting `specialUse`. Explain that monitoring is explicitly started/stopped by the user, occurs only on-device, pauses with the screen off, and produces a visible ongoing notification.
5. Add a short review video showing the disclosures, Usage Access grant, overlay access grant, notification permission, Start, the Pause Card, Continue/Dismiss-for-today, and Stop.
6. Use the public [privacy policy](PRIVACY_POLICY.md) URL in Play Console. Complete Data safety accurately: this version does not transmit user data off-device, so its on-device app-activity processing is not "collected" under Google's Data safety definition.

The APK artifact exists only for controlled verification and a Play Protect appeal. Internal Testing through Google Play is the primary installation path.
