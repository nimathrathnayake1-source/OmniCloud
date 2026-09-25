# OmniCloud — working Android build

This project has been upgraded from the original demo/prototype so that it does not report fake cloud uploads as successful.

## What works in this build

- Android Jetpack Compose UI and Room local vault
- Device file picker for arbitrary files
- Client-side AES-256-GCM encryption
- Stronger PBKDF2-HMAC-SHA256 work factor (120,000 iterations)
- Encrypted provider passwords using an Android Keystore AES key
- Real WebDAV/Nextcloud/self-hosted upload via HTTP PUT
- WebDAV directory creation (MKCOL)
- Basic authentication / app-password support
- SHA-256 checksum header on uploads
- Real HTTP failure handling and sync error states
- Unsupported cloud providers are **not** falsely marked as synced
- Destructive Room migrations removed
- Android app backup disabled for the vault

## Current provider status

### Real upload integration

- WebDAV / NAS
- Nextcloud WebDAV endpoints
- Self-hosted WebDAV-compatible servers

Use **Add Your Own Cloud Server** and provide:

- endpoint URL
- remote directory
- username/account identifier
- password or app password

For Nextcloud, an app password is recommended.

### Not yet wired to provider APIs

Google Drive, Dropbox, OneDrive, Box, pCloud and native AWS S3 OAuth/API flows are intentionally not simulated. The UI may list these providers, but this build will refuse to claim they are connected/synced without a real provider integration.

## Run

Open the project in Android Studio and let Gradle sync. Then run the `app` debug configuration on an Android 7.0+ device/emulator.

The supplied environment does not include the Android SDK/Gradle executable, so the source package could not be compiled into an APK in this environment. Android Studio can perform the final Gradle build.

## Important architecture note

The existing UI's upload picker currently reads selected files into memory before handing them to the ViewModel. The next production-hardening step should replace that path with streaming `ContentResolver` reads, chunked encryption and resumable uploads for very large files.
