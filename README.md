# Color-Sensor
Name

    Jeff Kim
    David Mei
    Dante Berouty
    Jason Marquez

All setups should be included in the project currently. If not, the current Java version is Java 21

In Tools > SDK Manager, make sure the following are installed

**SDK Platforms** 
Android 14.0 ("UpsideDownCake") API Level 34
Android 15.0 ("UpsideDownCake") API Level 35

**SDK Tools**
Android SDK Build-Tools 36-rc1
Android SDK Command-line Tools (latest)
Android Emulator
Android Emulator hypervisor driver (installer)
Android SDK Platform-Tools

Current some files may be a bit outdated, but it is working. If issues are ran into, versions can be updated for build.gradle (app and root) and settings.gradle


# Project Setup Guide – Kotlin Jetpack Compose (API 35, Java 21)

## Prerequisites
Before starting, make sure the following are installed on the machine:

1. **Android Studio (Giraffe or newer recommended)**
   - [Download here](https://developer.android.com/studio)

2. **Java 21 JDK**
   - Download from [Oracle](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) or [Adoptium](https://adoptium.net/)

3. **Git** (if cloning from a remote repo)
   - [Download here](https://git-scm.com/downloads)

4. **(Optional) GitHub Desktop** – for easier repository cloning and management
   - [Download here](https://desktop.github.com)

---

## 🔧 Environment Setup

### 1. Set JAVA_HOME (Java 21):

On **Windows**:
```cmd
setx JAVA_HOME "C:\Path\To\Java\jdk-21"
```

On **Mac/Linux** (add to `.bashrc` / `.zshrc`):
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Install Android SDK components (via Android Studio):
- Open Android Studio.
- Go to **Tools > SDK Manager**.
- Ensure the following are installed:
   - **SDK Platforms** 
        - Android 14.0 ("UpsideDownCake") API Level 34
        - Android 15.0 ("UpsideDownCake") API Level 35
    - **SDK Tools**
        - Android SDK Build-Tools 36-rc1
        - Android SDK Command-line Tools (latest)
        - Android Emulator
        - Android Emulator hypervisor driver (installer)
        - Android SDK Platform-Tools
- Search for build tools on the left and go to Gradle tab.
- Ensure Distribution is set to "Wrapper" and Gradle JDK is set to JBR 21 (should be automatically set by Android Studio)

---

## Clone the Project

Using **Git**:
```bash
git clone https://github.com/your-org/your-project.git
cd your-project
```

Or use **GitHub Desktop**:
1. Open GitHub Desktop.
2. Click **"File > Clone Repository"**.
3. Paste the repository URL and choose the local path.

---

## Open the Project

1. Open Android Studio.
2. Click **“Open”** and select the project folder.
3. Android Studio will:
   - Prompt to download missing SDKs or build tools — accept all prompts.
   - Sync Gradle and download dependencies automatically.

---

## Set Up Emulator (API 35 – Medium Phone)

1. Go to **Tools > Device Manager**.
2. Click **“Create Device”**.
3. Choose **Phone > Pixel 9 Pro (or Medium Phone equivalent)**.
4. Select **API Level 35 (Android 14)** image (download if not installed).
5. Click **Next > Finish** to create the emulator.
6. Click the play icon ▶ to launch the emulator.

---

## Run the App

1. Select the emulator from the device list.
2. Click **Run ▶** or press **Shift + F10** to build and launch the app.

---

## Notes

- No manual Kotlin or Gradle installation is needed — Android Studio handles it.
- Compose dependencies are resolved through `build.gradle.kts` or `build.gradle`.
