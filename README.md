# CloudSync - CalDAV & CardDAV Android Client

A modern, high-performance Android application built with **Kotlin** and **Jetpack Compose** for seamless synchronization with **ownCloud** (and Nextcloud/WebDAV) CalDAV calendars and CardDAV contacts.

---

## 📱 Features

- **CalDAV Synchronization**: Automatic discovery and event sync with ownCloud Calendar.
- **CardDAV Synchronization**: VCard contact synchronization with ownCloud Address Books.
- **Android KeyStore Vault**: Credentials and passwords encrypted with hardware-backed AES-256 GCM.
- **Background Sync Strategy**: Scheduled background sync with battery optimization, Wi-Fi only mode, and charging constraint settings.
- **Detailed Sync Diagnostics**: Live HTTP/DAV handshake trace viewer and copyable error diagnostics.
- **Material 3 Design**: Professional Polish UI with dark/light dynamic theme support and smooth Compose animations.

---

## 🚀 Building the Smartphone-Ready APK with GitHub Actions

This repository includes an automated **GitHub Actions CI/CD pipeline** (`.github/workflows/build_apk.yml`) that compiles the codebase and generates an installable `.apk` file for Android smartphones.

### How to trigger the APK build on GitHub:

1. **Push to GitHub**: Push any commit or pull request to the `main` or `master` branch.
2. **Manual Trigger**:
   - Go to your repository on GitHub.
   - Click on the **Actions** tab.
   - Select **Build Smartphone APK** from the left sidebar.
   - Click **Run workflow**.

### How to download and install the APK on your Smartphone:

1. Go to the **Actions** tab in your GitHub repository.
2. Click on the latest workflow run under **Build Smartphone APK**.
3. Scroll down to the **Artifacts** section at the bottom of the summary page.
4. Download **`CloudSync-Android-APK`** (contains `CloudSync-debug.apk`).
5. Transfer `CloudSync-debug.apk` to your Android phone or open the link directly on your phone.
6. Tap the downloaded `.apk` file to install it (*enable "Install from unknown sources" if prompted by Android*).

---

## 🛠 Local Development & Building

### Requirements
- **JDK 17** or higher
- **Android SDK** (API Level 34 / 36)
- **Gradle**

### Build Commands
To compile and generate the debug APK locally:
```bash
gradle assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Security & Privacy

All authentication data is held strictly on-device using Android's hardware-backed **KeyStore System**. Passwords and app tokens are never exposed in plaintext or transmitted to third-party tracking services.
