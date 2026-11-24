# PauseBite — Original Vision, Investigation, and Project Closure

## Project status

**PauseBite is archived as an experimental proof of concept.**

Development on the original automatic cross-app intervention has been stopped. The repository is being preserved as a record of the product idea, the Android implementation experiments, and the Google Play Protect investigation. It is not currently intended for production distribution.

## 1. Original idea

Food-delivery applications make ordering extremely quick. That convenience can also encourage people to order impulsively—because of a craving, boredom, stress, habit, or tiredness—before considering whether they are actually hungry.

PauseBite was designed to introduce a small, friendly pause at that exact moment of temptation.

The intended experience was:

1. The user installs PauseBite and chooses the food-delivery apps for which they want reminders.
2. The user explicitly enables monitoring and the required Android permissions.
3. When a selected food app enters the foreground, PauseBite detects it locally on the device.
4. A branded PauseBite card appears above the food app.
5. The card presents a short motivational or thought-provoking message, a healthier alternative, or a brief delay exercise.
6. The user can reconsider, dismiss the card, or continue to the original food app.

The goal was never to block food-delivery apps or remove the user's control. It was to interrupt automatic behaviour for a few seconds and turn an impulsive action into a conscious decision.

## 2. Problem the app attempted to solve

Most health applications engage users before or after a decision: meal planners help before eating, while calorie and habit trackers record what happened afterwards. PauseBite attempted to intervene **during the decision itself**, immediately after a food-delivery app was opened.

The proposed value was:

- reduce habitual or impulsive food ordering;
- help users distinguish hunger from boredom, stress, tiredness, and cravings;
- encourage smaller or healthier choices without shaming the user;
- offer quick alternatives before an order is placed;
- preserve the user's freedom to continue ordering;
- process app-detection information locally instead of uploading an app-usage history.

## 3. Technical approach used in the proof of concept

To provide the automatic experience, the Android proof of concept combined:

- **Usage Access (`PACKAGE_USAGE_STATS`)** to detect when a user-selected food app entered the foreground;
- a **user-started foreground service** to keep monitoring active and visible;
- **Display over other apps (`SYSTEM_ALERT_WINDOW`)** to place the PauseBite card above the selected app;
- a `TYPE_APPLICATION_OVERLAY` window for the intervention UI;
- a persistent notification containing monitoring controls;
- local processing with no Accessibility Service, SMS, contacts, location, or Internet permission.

Each capability had a legitimate purpose and was disclosed to the user. However, the combination resembles techniques also used by unwanted or harmful Android applications: observing foreground apps, running continuously, and drawing over other applications.

## 4. Issue encountered

Sideloaded release APKs repeatedly triggered a Google Play Protect warning:

> **Harmful app blocked — This app can put your device or data at risk.**

This was not merely Android's normal warning for an app installed outside Google Play. Play Protect classified or treated the APK as potentially harmful and prevented installation on the test device.

The repository and build history were investigated for obvious harmful behaviour. No Internet permission, network upload implementation, Accessibility Service, dynamic code loading, reflection-based payload, or third-party tracking SDK was found.

The most likely explanation is a false-positive or reputation-based classification caused by some combination of:

1. foreground-app observation;
2. persistent background monitoring;
3. overlay permission and overlay-window code;
4. repeated experimental sideloaded APKs;
5. the original proof-of-concept package/signing identity.

Google does not publicly expose the exact Play Protect classifier, rule, or threshold. Therefore, the repository cannot prove that one individual permission was solely responsible.

## 5. Investigation and alternatives tested

During the investigation, several variants and product alternatives were considered or implemented:

### Minimal permission and code probes

Diagnostic builds attempted to isolate permissions and implementation components. Some early isolation results were inconclusive because production code was still bundled into variants, even when a specific permission was removed.

### Notification instead of overlay

The cross-app overlay was replaced with a notification. This was safer and less intrusive, but it changed the core experience: a notification does not interrupt the decision at the moment the food app opens and can easily be ignored.

### Opening PauseBite as a full-screen activity

Automatically launching the PauseBite app when another app opens was examined. Modern Android restricts background activity launches, so this is not a dependable or policy-friendly replacement for an overlay.

### Toast or temporary banner

A short toast-style message was explored. Standard Android toasts are time-limited, constrained by the system, cannot cover a large portion of the screen, and do not provide the interactive experience required by the original concept.

### Requesting permissions after installation

The app was changed so that sensitive access would not be enabled by default and would be requested only after installation with user disclosure. This improves consent and product transparency, but permissions declared in the APK and suspicious implementation patterns can still be evaluated by Play Protect before the user grants them.

### User-selected target apps

Hard-coded food-app names were removed from the intended setup flow in favour of asking users which applications they wanted to target. This improved user control but did not eliminate the underlying cross-app monitoring and overlay architecture.

### Safer overlay design

The full intervention was reduced to a small, branded, dismissible, non-modal card. Areas outside the card remained visible and touchable. Despite the safer UI and reduced scope, the sideloaded APK was still reported as blocked during testing.

More detailed technical findings are recorded in [`docs/PLAY_PROTECT_REMEDIATION.md`](docs/PLAY_PROTECT_REMEDIATION.md).

## 6. Why the original product is being wrapped

The original experience depends on three capabilities working together: detecting another app, monitoring continuously, and displaying content above it. Removing any one of them significantly changes the product:

- without Usage Access, PauseBite cannot reliably know when the selected app opens;
- without continuous monitoring, detection is not immediate or dependable;
- without an overlay, PauseBite cannot present the intervention in the original app context;
- replacing the overlay with a notification or manual shortcut no longer delivers the intended automatic pause.

It may be possible to pursue the concept through Google Play review, a formal Play Protect appeal, persistent signing, and carefully justified declarations. However, that path carries continuing distribution, policy, reputation, maintenance, and user-trust risk. It also requires disproportionate effort before the core product value has been validated.

For those reasons, development of the original implementation is ending here rather than continuing to weaken the product until it no longer represents the original idea.

## 7. What was learned

- A legitimate purpose does not automatically make a sensitive permission combination safe for distribution.
- Android permissions should be evaluated as a behavioural combination, not only one by one.
- Asking for permission after installation does not hide manifest declarations or implementation patterns from security scanning.
- Notifications, toasts, overlays, and activities are not interchangeable; each creates a materially different user experience.
- Stable signing, a production package identity, Play-based distribution, accurate disclosures, and narrowly scoped permissions should be established early.
- Platform feasibility and distribution risk should be validated before investing heavily in product content and UI.

## 8. Possible future direction

The mission can be revisited later without cross-app monitoring or overlays. A future product could use a user-initiated Quick Settings tile, widget, shared cart screenshot, craving timer, fridge-based meal suggestions, or savings tracker. Such a product would preserve the broader purpose—helping users pause before an impulsive order—while using standard, user-initiated Android interactions.

That would be a new product direction rather than a continuation of the original automatic overlay implementation.

## Final conclusion

PauseBite began with a clear and meaningful idea: create a brief moment of reflection between craving and purchase. The prototype demonstrated the intended interaction, but implementing it reliably required a combination of sensitive Android capabilities that repeatedly encountered Play Protect distribution barriers.

The project is therefore closed as a valuable proof of concept. Its product thinking, designs, diagnostic modules, and investigation notes are intentionally retained for learning and for any future privacy-first redesign.

