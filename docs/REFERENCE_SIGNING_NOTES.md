# Reference study: DividendVault signing workflow (captured 2026-11-23)

Source examined read-only: CodeWith-ShivamSisodiya/DividendVault.
Relevant files: .github/workflows/build-apk.yml, app/build.gradle.kts, docs/UPDATE_SIGNING.md, docs/PHONE_QA.md, .gitignore, settings.gradle.kts, gradle.properties, app/src/main/AndroidManifest.xml.

## Observed design
- GitHub Actions ubuntu-24.04; JDK 17 Temurin; Gradle 8.9; Android SDK 35/build-tools 35.0.0.
- Four secrets: DV_KEYSTORE_BASE64, DV_KEYSTORE_PASSWORD, DV_KEY_ALIAS, DV_KEY_PASSWORD.
- Workflow fails closed if any signing secret is absent. Decodes PKCS12 into RUNNER_TEMP, validates alias with keytool.
- Gradle takes key path, store password, alias and key password from environment. Signing config only exists when all variables are present.
- Builds release APK; audits APK permissions with aapt; verifies APK signature with apksigner; compares certificate SHA-256 to keytool-exported certificate SHA-256.
- Publishes signed release APK and signing-certificate artifact.
- .gitignore excludes *.jks, *.keystore, *.p12, .signing/, build outputs.
- Update instructions: preserve application ID and signing certificate; increase versionCode; never uninstall app with irreplaceable local data; debug keys from ephemeral runners cannot be recovered.
- DividendVault itself uses package com.dividendvault.local and a separate PKCS12 key. Its key must NEVER be reused for PauseBite.
- Its manifest does not request PauseBite's usage-access, overlay, or special-use foreground service permissions; it is not proof that PauseBite will pass Play Protect.

## PauseBite adaptation
- Package com.pausebite.overlaytest retained, versionCode increased to 5.
- Dedicated PAUSEBITE_* secrets; fail-closed release pipeline; APK signing/permission/metadata verification; one installable release artifact.
- Do not commit a keystore or passwords. Make and back up a dedicated PKCS12 key securely, then add GitHub Actions secrets.
- Existing v0.4 installed from ephemeral debug signing may not update to first signed release; one-time uninstall might be unavoidable. Export any valuable data first.
- No in-app updater implemented; a user installs the new signed APK over the existing signed APK.
- Play Protect harmful-app warning is a separate unresolved security review. Do not advise bypassing it.

## Setup (Windows PowerShell)
1. Generate once on a trusted computer: `keytool -genkeypair -v -storetype PKCS12 -keystore C:\Secure\pausebite-update.p12 -alias pausebite -keyalg RSA -keysize 4096 -validity 10000`
2. Back up the .p12 privately in two safe locations.
3. Base64 for GitHub secret: `[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\Secure\pausebite-update.p12')) | Set-Clipboard`
4. In PauseBite GitHub repository > Settings > Secrets and variables > Actions, create PAUSEBITE_KEYSTORE_BASE64, PAUSEBITE_KEYSTORE_PASSWORD, PAUSEBITE_KEY_ALIAS, PAUSEBITE_KEY_PASSWORD.
5. Run the workflow once, verify its certificate artifact, and preserve the signing key for future builds.
