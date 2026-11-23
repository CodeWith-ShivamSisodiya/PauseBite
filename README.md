# PauseBite v0.7 — mindful food-app reminders

PauseBite is an opt-in, offline Android proof of concept. It detects foreground Swiggy/Zomato activity and shows a dismissible reminder. It does not intercept orders, payment information, messages, or passwords.

## Permission disclosure

- **Usage Access (PACKAGE_USAGE_STATS):** Android provides app-activity events. PauseBite checks foreground transitions locally to detect Swiggy and Zomato. Other app events may be returned by Android's query, but PauseBite does not store a history of those apps.
- **Display Over Other Apps (SYSTEM_ALERT_WINDOW):** draws a clearly branded, dismissible reminder on top of the food app.
- **Foreground Service / Special Use:** runs the user-started monitoring process while the screen is on, with an ongoing notification and Stop action.
- **Notifications (Android 13+):** allows the monitoring notification to be shown in the notification drawer. No Internet, Accessibility, SMS, Contacts, or Location permissions are requested.

Monitoring begins only after the user grants the required access and taps START. STOP cancels monitoring and removes the overlay. Monitoring pauses while the screen is off. Local preferences hold cooldowns and aggregate diagnostics. No network upload code exists in this app.

## Installation and updates

The GitHub Actions workflow requires four signing secrets: `PAUSEBITE_KEYSTORE_BASE64`, `PAUSEBITE_KEYSTORE_PASSWORD`, `PAUSEBITE_KEY_ALIAS`, `PAUSEBITE_KEY_PASSWORD`. Never commit or expose these secrets. In-place updates require the same signing certificate, application ID `com.pausebite.overlaytest`, and a higher versionCode. The workflow verifies these properties. Version 0.7 uses versionCode 7.

## Play Protect investigation

Google Play Protect flagged both the full app and a **permission-free, independently installable baseline**. This shows that the sensitive permissions are not necessary for the warning to appear, but does **not** establish the root cause or prove the app safe. The diagnostic baseline is under `diagnostic/` and is not the production app. Keep Play Protect enabled. Submit the signed APK and warning details to Google's official Play Protect developer appeal process: https://support.google.com/googleplay/android-developer/contact/protectappeals

### Suggested appeal description

> PauseBite is a small, offline Android application that provides an optional mindful reminder when the user opens Swiggy or Zomato. The user explicitly grants Usage Access and Display Over Other Apps, and starts or stops monitoring. It has no INTERNET, SMS, contacts, location, or Accessibility permission. We have also reproduced the same "Harmful app blocked" warning on a separate minimal APK that requests no permissions. Please review the classification and identify any actionable policy or security concern.

The developer should provide the exact flagged APK, SHA-256 hash, package name, version, signing certificate fingerprint, and screenshots. Do not publish private signing keys or passwords.
