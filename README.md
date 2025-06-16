This is the first thing anyone (including your client or future self) will see. Make it clear and comprehensive.

# Web2App - APK WebView Builder

This project provides a system to dynamically generate Android APKs that display a specified website within a WebView. It consists of two main components:

1.  **Web2App - Android Client App:** A user-friendly Android application (built with Jetpack Compose) where users can input a URL and initiate the APK generation process by communicating with the server.
2.  **apk_gen_server - Flask Server:** A Python Flask backend that receives URL requests, copies and modifies an Android WebView template project, compiles the new APK, signs it, and makes it available for download.


---

## 🚀 Features

* **Dynamic APK Generation:** Input any URL and get a custom Android APK for it.
* **Automated Android Build Process:** The server handles all the complexities of Android SDK, Gradle, and signing.
* **User-Friendly Android Client:** Simple UI to request APKs and manage downloads.
* **Local Development Ready:** Comprehensive instructions for setting up the entire system on your local machine (Windows with WSL, Linux, or macOS).


---

## 🛠️ Prerequisites (Local Setup)

To run this project locally, you'll need the following installed on your development machine:

* **Java Development Kit (JDK):** Version 17 or higher.
* **Python 3.x:** (The server is built with Python 3).
* **Android Studio:** (For developing and testing the Android client app).
* **Android SDK Command-line Tools:** Required for the Flask server to build Android APKs.
* **Git:** For cloning the repository.


---

## 🚀 Getting Started

Follow these steps to set up and run the project on your local machine.

### Step 1: Clone the Repository

First, clone this repository to your local machine:

```bash
git clone [https://github.com/your_username/your_repo_name.git](https://github.com/your_username/your_repo_name.git)
cd your-apk-generator-project
```

### Step 2: Set Up the Flask Server

The Flask server needs a Python environment, the Android SDK, and a Keystore to function.

#### 2.1 Install Java Development Kit (JDK)

If you don't have JDK 17 (or newer) installed, install it.

  * **On Ubuntu/Debian (WSL or Linux):**
    ```bash
    sudo apt update
    sudo apt install openjdk-17-jdk -y
    ```
  * **On macOS (using Homebrew):**
    ```bash
    brew install openjdk@17
    sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
    ```
  * **On Windows:** Download and install from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [Adoptium OpenJDK](https://adoptium.net/temurin/releases/).

**Verify Installation:**

```bash
java -version
javac -version
```

#### 2.2 Configure Android SDK Command-Line Tools

The Flask server uses the command-line tools to build and sign APKs.

1.  **Create SDK Directory:**
    Choose a directory for your SDK, e.g., `~/Android/sdk` (macOS/Linux) or `C:\Android\sdk` (Windows).

    ```bash
    mkdir -p ~/Android/sdk # Or your chosen path
    cd ~/Android/sdk
    ```

2.  **Download Command-Line Tools:**
    Go to [Android Developers Command-line Tools](https://www.google.com/search?q=https://developer.android.com/studio%23command-line-tools), find "Command line tools only" for your OS, and copy the download link.

    ```bash
    wget <copied_download_url_for_linux_x64.zip> # For Linux/WSL/macOS
    # Or download manually on Windows
    ```

3.  **Unzip and Organize:**
    Unzip the downloaded file. It typically creates a `cmdline-tools` directory. Move its contents so `sdkmanager` is located at `your_sdk_path/cmdline-tools/latest/bin/sdkmanager`.

    ```bash
    unzip commandlinetools-linux-*.zip
    mv cmdline-tools latest # Assuming it extracts to cmdline-tools
    ```

    The final structure should be `your_sdk_path/cmdline-tools/latest/bin/sdkmanager`.

4.  **Set Environment Variables:**
    Add these to your shell's profile file (`~/.bashrc`, `~/.zshrc` on Linux/macOS/WSL; Windows Environment Variables). **Restart your terminal or `source` the profile file after adding.**

      * **Linux/WSL/macOS:**
        ```bash
        export ANDROID_HOME=~/Android/sdk # Use your chosen path
        export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
        export PATH=$PATH:$ANDROID_HOME/platform-tools
        export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0 # Will install later
        ```
      * **Windows (Command Prompt):**
        ```cmd
        set ANDROID_HOME=C:\Android\sdk # Use your chosen path
        set PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\build-tools\34.0.0
        ```

5.  **Install SDK Packages:**
    Run `sdkmanager` from your terminal to install necessary components.

    ```bash
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
    ```

    (You can change `android-34` and `34.0.0` to desired API levels.)

6.  **Accept SDK Licenses (Crucial\!):**

    ```bash
    sdkmanager --licenses
    ```

    Type `y` and press Enter for each license prompt.

#### 2.3 Generate a Keystore for APK Signing

You need a unique keystore to sign the generated APKs. **Do NOT upload this file to GitHub\!**

1.  **Choose a Secure Location:**
    Create a directory outside your project, e.g., `~/keystores/` (Linux/macOS/WSL) or `C:\Users\YourUser\keystores\` (Windows).
    ```bash
    mkdir -p ~/keystores
    cd ~/keystores
    ```
    
2.  **Generate Keystore:**
    Use `keytool` (comes with JDK) to generate your keystore.
    ```bash
    keytool -genkeypair -v -storepass your_keystore_password -keypass your_key_password -keystore my-release-key.jks -alias myalias -keyalg RSA -keysize 2048 -validity 10000
    ```
      * Replace `your_keystore_password` and `your_key_password` with strong passwords.
      * Fill in the requested information (name, organization, etc.).
    
3.  **Set Permissions:**
    ```bash
    chmod 400 my-release-key.jks # Linux/macOS/WSL
    ```

#### 2.4 Prepare Flask Server Code

1.  **Navigate to the server directory:**
    ```bash
    cd server/
    ```

2.  **Create and activate a Python virtual environment:**
    ```bash
    python3 -m venv venv
    source venv/bin/activate # On Windows: .\venv\Scripts\activate
    ```

3.  **Install Python dependencies:**
    ```bash
    pip install -r requirements.txt
    ```

4.  **Make Gradle Wrapper Executable:**
    ```bash
    chmod +x webview_template/gradlew # For Linux/WSL/macOS
    ```

5.  **Set Server Environment Variables (Crucial for Keystore Access):**
    These variables tell your Flask app where to find the keystore and its passwords. **Set these in the same terminal session where you will run `app.py`.**

      * **Linux/WSL/macOS:**
        ```bash
        export KEYSTORE_PATH=~/keystores/my-release-key.jks # Use your actual path
        export KEYSTORE_PASSWORD="your_keystore_password"
        export KEYSTORE_ALIAS="myalias"
        export KEY_PASSWORD="your_key_password"
        ```
      * **Windows (Command Prompt):**
        ```cmd
        set KEYSTORE_PATH=C:\Users\YourUser\keystores\my-release-key.jks # Use your actual path
        set KEYSTORE_PASSWORD="your_keystore_password"
        set KEYSTORE_ALIAS="myalias"
        set KEY_PASSWORD="your_key_password"
        ```

#### 2.5 Run the Flask Server

With all environment variables set and the virtual environment active:

```bash
python app.py
```

The server should start and display something like `* Running on http://0.0.0.0:5000`. Keep this terminal open.


### Step 3: Set Up and Run the Android Client App

This app will connect to your local Flask server to request APKs.

1.  **Open the `android-app/` project in Android Studio.**
2.  **Configure Server URL:**
      * Navigate to `android-app/app/src/main/java/com/example/webviewbuilder/MainActivity.kt` (or your main activity file).
      * Find the `BASE_URL` (or similar variable) that points to your server.
      * **Change it to your local server address:**
          * **For Android Emulator:** `http://10.0.2.2:5000` (This IP points to your host machine from the emulator).
          * **For Physical Android Device:** `http://YOUR_LOCAL_PC_IP_ADDRESS:5000` (Find your PC's IP using `ipconfig` on Windows or `ip a` on Linux/macOS).
              * **Important:** Ensure your computer's firewall allows inbound connections on port 5000.
3.  **Run the Android App:**
      * Connect an Android device or start an emulator in Android Studio.
      * Run the `android-app` on it.


### Step 4: Test the Entire System

1.  In the running **Android client app**, enter a URL (e.g., `https://www.google.com`, `https://example.com`) into the input field.
2.  Tap the "Generate Web App" button.
3.  **Monitor your Flask server's terminal:** You should see logs indicating the request, template copying, Gradle build progress, and signing. This process can take a minute or two depending on your machine's performance.
4.  **Monitor Android Studio's Logcat:** Look for logs from your Android app confirming successful network requests and the download manager.
5.  **Verify APK Generation:**
      * Once the Flask server finishes, it will print the download URL.
      * The Android app should automatically start downloading the APK.
      * After download, the Android app should prompt you to install the APK (ensure you've granted "Install unknown apps" permission if prompted).
6.  **Install and Launch:** Install the newly generated APK. Launch it and verify that it opens the specified website correctly within its WebView.


-----

## ⚠️ Important Security Notes & Best Practices

  * **NEVER Share Your Keystore:** The `my-release-key.jks` file is like your app's identity. Keep it absolutely private and secure. Do not commit it to Git\!
  * **Environment Variables for Secrets:** Always use environment variables for sensitive information (like keystore passwords) instead of hardcoding them in code.
  * **Input Validation:** The Flask server performs basic URL validation, but ensure any user input is thoroughly validated to prevent malicious code injection.
  * **Local Testing is Key:** Before even thinking about cloud deployment, ensure everything works perfectly on your local machine.


-----

## ❓ Troubleshooting

  * **"Connection refused" error in Android app:**
      * Is your Flask server running?
      * Did you use the correct IP address (`10.0.2.2` for emulator, your PC's LAN IP for physical device)?
      * Is your PC's firewall blocking port 5000?
  * **"APK Generation Failed" in Flask logs:**
      * Check your `ANDROID_HOME` and `PATH` environment variables.
      * Did you accept all SDK licenses (`sdkmanager --licenses`)?
      * Are your Keystore paths and passwords correct in your environment variables?
      * Check for specific error messages from Gradle or `apksigner` in the Flask server's output. Gradle build errors are usually verbose.
  * **"Permission denied" for `gradlew`:**
      * Run `chmod +x server/webview_template/gradlew` from your project root.
  * **Generated APK doesn't install or launch:**
      * Ensure the "Install unknown apps" permission is granted on your Android device/emulator for your client app.
      * For Android 7.0 (API 24) and above, ensure your Android client app uses `FileProvider` correctly for APK installation.
      * Check the `webview_template/app/build.gradle` file for correct build settings.


-----

Feel free to reach out if you encounter any issues during setup or testing\!
