# Implementation Plan - Custom APK Update System (Müstəqil Yeniləmə)

The goal is to implement a manual in-app update mechanism that checks a custom server for the latest APK version, downloads it, and prompts the user to install it. This works without Google Play Store.

## User Review Required

> [!IMPORTANT]
> You will need to host a small JSON file (e.g., `version.json`) on your server.
> Example JSON content:
> ```json
> {
>   "versionCode": 2,
>   "versionName": "1.0.1",
>   "apkUrl": "https://yourserver.com/neoplay.apk",
>   "releaseNotes": "Səs problemləri düzəldildi."
> }
> ```

> [!WARNING]
> On Android 8.0 (Oreo) and above, the user must grant the "Install unknown apps" permission for Neo Play when the update starts.

## Proposed Changes

### [app]

#### [MODIFY] [AndroidManifest.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/AndroidManifest.xml)
- Add `android.permission.REQUEST_INSTALL_PACKAGES`.
- Add `android.permission.INTERNET` and `android.permission.WRITE_EXTERNAL_STORAGE` (if needed for older versions).
- Configure a `FileProvider` to securely share the downloaded APK with the system installer.

#### [NEW] [xml/file_paths.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/xml/file_paths.xml)
- Define paths for the `FileProvider`.

#### [NEW] [UpdateManager.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/utils/UpdateManager.java)
- Implement background check for the JSON file.
- Implement APK download using `DownloadManager`.
- Implement BroadcastReceiver to detect when download is complete and trigger installation.

#### [MODIFY] [MainActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/MainActivity.java)
- Trigger the update check during startup.

## Verification Plan

### Manual Verification
- Simulate a new version by hosting a mock JSON file.
- Verify the "Update Available" dialog appears.
- Verify the APK downloads and the installation screen pops up.
