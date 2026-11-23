# Play Protect remediation

## What is verified

- The screenshot says **Harmful app blocked**. Google's documentation defines this as a Potentially Harmful Application (PHA) or mobile unwanted software verdict. It is not the ordinary unknown-app scan prompt and not the separate sensitive-permission sideload block.
- The old production APK requested Usage Access and `SYSTEM_ALERT_WINDOW`, registered a `specialUse` foreground service, polled foreground app events, and created `TYPE_APPLICATION_OVERLAY` windows over named food apps.
- The source contains no Internet permission, network implementation, Accessibility service, dynamic code loading, reflection, or third-party SDK.
- The old single-permission isolation builds were confounded: their Gradle source set bundled the full unminified production Kotlin code in every APK. Their results do not establish that an individual foreground-service permission is harmful by itself.
- The same minimal, permission-free diagnostic app reportedly warned with the original certificate and installed cleanly with an alternate certificate. This is strong evidence of a certificate or package-plus-certificate reputation signal, but Google does not expose the exact classifier key.
- Play Protect scans sideloaded apps after installation and can disable or automatically remove PHAs. That behavior can explain the reported disappearance.

## Ranked cause assessment

1. **False-positive PHA classification from the old cross-app feature combination - high confidence.** Foreground-app observation plus a persistent service plus overlay windows matches a well-known malware shape even though PauseBite's UI was dismissible.
2. **Original certificate or package/certificate reputation - medium-high confidence.** The controlled diagnostic result supports this, but it cannot distinguish certificate-only reputation from a package/certificate/backend timing effect.
3. **POC identity and raw sideload distribution increasing scrutiny - medium confidence.** The old `.overlaytest` package, default icon, no Play review history, and repeated experimental APKs are supporting signals, not documented causes.
4. **`specialUse` misuse - low confidence.** This is the technically accurate service type for continuous user-started monitoring that fits no standard type. It must be declared and reviewed in Play Console.

No external analyst can truthfully claim the private Play Protect model's exact rule or threshold. Only Google can confirm the classification through an appeal.

## Remediation implemented

- Removed `SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY`, and all overlay UI code.
- Replaced the cross-app window with a high-importance notification containing Dismiss and Snooze actions.
- Retained automatic, offline `UsageStatsManager` detection and the user-started `specialUse` foreground service.
- Added a real launcher icon, accurate disclosures, backup disabled, a production package ID, R8/resource shrinking, target API 36, and release permission checks.
- Stopped producing and publishing isolation/probe APKs in normal CI.
- Added an Android App Bundle for Google Play Internal Testing and kept the APK only for diagnostics.

## Confirm who removed the old app

Run these read-only commands soon after the app disappears:

```text
adb shell pm list packages -u -d | findstr /i pausebite
adb shell dumpsys package com.pausebite.overlaytest
adb logcat -b events -d | findstr /i "pausebite package_removed package_disabled verify"
adb logcat -d | findstr /i "VerifyApps PlayProtect PackageManager pausebite DELETE_PACKAGE"
```

Also check **Play Store > profile > Play Protect** and Samsung **Settings > Device care > App protection**. A package still listed as disabled points to disablement; absence plus a deletion event points to uninstall/removal.

## Final verification

1. Upload the AAB to Play Console Internal Testing with Play App Signing enabled. Do not treat a newly self-signed sideload as the final acceptance test.
2. Confirm the Play-generated install requests only Usage Access, notifications, and the two foreground-service permissions.
3. Test Start, Swiggy/Zomato detection, reminder, Dismiss, Snooze, screen-off pause, and Stop.
4. Leave Play Protect enabled and verify the app remains installed across at least two daily scans.
5. If the cleaned app is still classified, file one Play Protect appeal for the final package only. Include the exact APK/AAB SHA-256, package/version, signing certificate SHA-256, screenshot with details expanded, repository URL/commit, permission dump, and a short behavior video. Explain the legitimate use of each permission and that all processing is local.

Official appeal guidance: https://developers.google.com/android/play-protect/warning-dev-guidance
