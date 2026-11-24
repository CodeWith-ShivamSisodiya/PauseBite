# PauseBite Privacy Policy

Effective date: 24 November 2026

PauseBite is developed by CodeWith-ShivamSisodiya. Questions or privacy requests can be submitted through the public contact mechanism at https://github.com/CodeWith-ShivamSisodiya/PauseBite/issues.

## Data accessed and purpose

When the user explicitly selects supported food apps, enables Android Usage Access, and starts monitoring, PauseBite reads foreground-app transition events supplied by Android. It uses those events only to detect when a selected food app opens and to show a mindful-eating Pause Card. Android may return events for other apps, but PauseBite immediately ignores them and does not save their history.

The Pause Card uses Android's Display over other apps access. It is a small, non-modal window clearly branded as PauseBite. It does not dim or cover the whole screen, and areas outside it remain visible and touchable. It can be closed immediately, does not imitate the underlying app, and does not read, capture, or modify that app's screen. The user can continue, dismiss Pause Cards for the rest of the day, stop monitoring directly from the card, or revoke overlay access at any time.

PauseBite stores only local preferences needed for the feature: whether monitoring is active, dismiss and cooldown times, the last supported food app detected, aggregate detection/Pause Card counts, and basic diagnostic timing. It also reads screen and battery state locally to pause or reduce monitoring.

## Collection and sharing

PauseBite has no Internet permission, analytics SDK, advertising SDK, account system, or server component. App-activity events and local preferences are not transmitted off the device, sold, or shared with the developer or third parties.

The foreground-service notification only indicates that monitoring is active and provides a Stop action. PauseBite does not read order contents, payment information, messages, credentials, contacts, location, screenshots, or notification content from other apps.

## Retention, control, and deletion

Local preferences remain on the device until the user clears PauseBite's storage or uninstalls the app. The user can stop monitoring at any time from the app or its ongoing notification, revoke Usage Access or Display over other apps access in Android Settings, disable notifications, clear app storage, or uninstall PauseBite. Clearing storage or uninstalling deletes the app's local data.

PauseBite does not create user accounts, so there is no server-side account or data to delete.

## Security and changes

PauseBite minimizes data access, keeps processing on-device, disables Android backup for its local data, and does not include third-party SDKs. Material changes to this policy will be published at this URL with a new effective date.
