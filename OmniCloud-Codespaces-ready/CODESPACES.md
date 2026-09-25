# OmniCloud in GitHub Codespaces

This repository is prepared to build OmniCloud without installing Android Studio on your PC.

## 1. Put the project on GitHub

Create a new GitHub repository, then upload the contents of this folder. The `.devcontainer` folder must be in the repository root.

## 2. Open the Codespace

On GitHub:

1. Open the repository.
2. Click **Code**.
3. Open the **Codespaces** tab.
4. Click **Create codespace on main** (or the current branch).

GitHub creates the development container from `.devcontainer/devcontainer.json`. Codespaces supports repository-specific dev container configurations. See the GitHub documentation: https://docs.github.com/en/codespaces/setting-up-your-project-for-codespaces/adding-a-dev-container-configuration

## 3. Build the APK

When the terminal is ready, run:

```bash
gradle :app:assembleDebug
```

The APK will be at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also run tests with:

```bash
gradle test
```

## 4. Download the APK

In the Codespaces file explorer, open:

`app/build/outputs/apk/debug/`

Right-click `app-debug.apk` and download it.

## Notes

The container uses the public `ghcr.io/cirruslabs/android-sdk:36` image. Its Android 36 image includes the Android 36 platform and Build Tools 36.0.0. The project uses Gradle 9.3.1, so `.devcontainer/setup.sh` installs that Gradle version into the Codespace if it is not already available.

This setup is intended for building and editing the app in the browser. A browser-based Android emulator is a separate service; the Codespace itself is the cloud development/build environment.
