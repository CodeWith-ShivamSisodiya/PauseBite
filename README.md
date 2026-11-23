# PauseBite

PauseBite is an opt-in, offline Android app that posts a dismissible mindful-eating reminder when the user opens Swiggy or Zomato.

## Privacy and permissions

- **Usage Access (`PACKAGE_USAGE_STATS`)** is used locally to detect foreground transitions for Swiggy and Zomato. PauseBite does not store a history of other apps.
- **Foreground Service / Special Use** keeps user-started monitoring active while the screen is on. The ongoing notification always includes a Stop action.
- **Notifications** show the monitoring status and optional reminder. Reminders can be dismissed or snoozed for 30 minutes.
- PauseBite has no Internet, overlay, Accessibility, SMS, contacts, location, or package-enumeration permission.

Monitoring starts only after the user grants Usage Access and notifications, then taps **START**. It stops when the user taps **STOP** and pauses while the screen is off. No app-usage data is uploaded or shared.

## Why the overlay implementation was removed

The earlier proof of concept combined `UsageStatsManager` foreground-app polling, hard-coded target package names, a foreground service, and `TYPE_APPLICATION_OVERLAY`. That feature combination resembles the static and runtime signals of Android overlay malware even though PauseBite's reminder was benign and user-controlled. Google Play Protect classified the sideloaded APK as a Potentially Harmful Application.

The production implementation preserves automatic detection but uses a high-priority Android notification instead. It does not request `SYSTEM_ALERT_WINDOW` and contains no overlay-window code. See [docs/PLAY_PROTECT_REMEDIATION.md](docs/PLAY_PROTECT_REMEDIATION.md) for the findings, verification, and appeal procedure.

## Release path

The production identity is `com.pausebite.app`, version code 8, and target API 36. The previous `com.pausebite.overlaytest` package was a POC identity and is intentionally not update-compatible.

1. Configure `PAUSEBITE_KEYSTORE_BASE64`, `PAUSEBITE_KEYSTORE_PASSWORD`, `PAUSEBITE_KEY_ALIAS`, and `PAUSEBITE_KEY_PASSWORD` as GitHub Actions secrets. Keep this upload key permanent and backed up.
2. Run **Build verified PauseBite release**.
3. Download the `PauseBite-Play-Internal-Testing` artifact and upload `app-release.aab` to a new Google Play Console app with Play App Signing enabled.
4. Complete the foreground-service declaration in **Policy > App content**, selecting `specialUse`. Explain that monitoring is explicitly started/stopped by the user, occurs only on-device, pauses with the screen off, and produces a visible ongoing notification.
5. Add a short review video showing the disclosure, Usage Access grant, notification permission, Start, a reminder, snooze/dismiss, and Stop.
6. Use the public [privacy policy](PRIVACY_POLICY.md) URL in Play Console. Complete Data safety accurately: this version does not transmit user data off-device, so its on-device app-activity processing is not "collected" under Google's Data safety definition.

The APK artifact exists only for controlled verification and a Play Protect appeal. Internal Testing through Google Play is the primary installation path.
