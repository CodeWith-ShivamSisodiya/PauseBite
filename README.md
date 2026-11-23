# PauseBite Overlay-only POC

This build is a limited safety diagnostic, **not automatic food-app detection**.

- Requests only the Android Display Over Other Apps permission.
- Does not request Usage Access, notification, foreground-service or network permissions.
- Does not monitor or collect app activity.
- User explicitly taps **Show popup**, then can press Home and open another app to verify the popup remains visible.
- Popup is dismissible at any time.

The earlier monitor implementation is retained in the repository for future review, but the monitor service is **not declared in the manifest**, is not started, and is not used in this build.

A Google Play Protect warning is not proof of maliciousness or safety. Do not bypass a security block just to test the APK.
