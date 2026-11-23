# PauseBite — Automatic detection POC (v0.3)

Experimental, opt-in Android proof of concept. The user grants Usage Access and overlay permission, explicitly starts monitoring, and then opens Swiggy or Zomato normally. A foreground service checks local app-usage events and attempts to display a dismissible motivational overlay. No network permission, no remote data transfer, no Accessibility Service.

## Testing
1. Install the latest GitHub Actions debug APK.
2. Grant Usage Access and Display Over Other Apps; allow notifications if prompted.
3. Tap START automatic monitoring. Confirm the persistent monitoring notification.
4. Leave PauseBite and launch Swiggy or Zomato normally.
5. If the overlay appears, dismiss it; return to PauseBite to read detection details.
6. Tap STOP monitoring when done.

## Limitations and security
- Android may delay or restrict usage events, foreground services and overlays.
- The monitoring service is user-started, not automatically restarted on boot.
- Google Play Protect previously flagged the debug APK. This project has **not** been independently security-cleared; do not bypass a security block. A successful debug build does not imply Play Store policy approval.
- Foreground service specialUse eligibility must be reviewed before any Play Store release.
- This is not a production app or medical advice.
