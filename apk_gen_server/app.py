import os
import uuid
import json
import time
import shutil # For copying directories
import subprocess # For running shell commands (like gradlew)
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS # For handling Cross-Origin Resource Sharing
from urllib.parse import urlparse

app = Flask(__name__)
CORS(app) # Enable CORS for all routes (important for local testing with Android app)

# --- Configuration ---
# Directory to store generated APKs
GENERATED_APKS_DIR = 'generated_apks'
# Path to your pre-configured Android WebView template project
# This should be the root of your 'WebViewTemplateApp' project you created in Android Studio
TEMPLATE_DIR = 'WebViewTemplate01'
# Temporary directory where projects will be copied and built
BUILD_TEMP_DIR = 'build_temp'

# Ensure necessary directories exist
for directory in [GENERATED_APKS_DIR, BUILD_TEMP_DIR]:
    if not os.path.exists(GENERATED_APKS_DIR):
        os.makedirs(GENERATED_APKS_DIR)
    
# --- Real APK Generation (Conceptual Outline) ---
def generate_webview_apk_real(url):
    """
    CONCEPTUAL: This outlines the real APK generation process.
    Requires Android SDK, Gradle, and a keystore configured on the server.
    """
    build_id = uuid.uuid4().hex
    temp_project_path = os.path.join(BUILD_TEMP_DIR, f'project_{build_id}')
    apk_filename = None
    
    try:
        # 1. Copy the template project to a temporary build directory
        print(f"Copying template from {TEMPLATE_DIR} to {temp_project_path}")
        shutil.copytree(TEMPLATE_DIR, temp_project_path)

        # 2. Modify MainActivity.kt to inject the URL
        # Assumes the MainActivity.kt is at this specific path within the template
        main_activity_rel_path = os.path.join('app', 'src', 'main', 'java', 'com', 'template', 'webviewtemplate01', 'MainActivity.kt')
        main_activity_abs_path = os.path.join(temp_project_path, main_activity_rel_path)

        if not os.path.exists(main_activity_abs_path):
            raise FileNotFoundError(f"MainActivity.kt not found at: {main_activity_abs_path}")

        print(f"Modifying {main_activity_abs_path} to embed URL: {url}")
        with open(main_activity_abs_path, 'r') as f:
            content = f.read()
        
        # Replace the placeholder string in MainActivity.kt
        modified_content = content.replace("___YOUR_DYNAMIC_WEBVIEW_URL_PLACEHOLDER___", url)

        with open(main_activity_abs_path, 'w') as f:
            f.write(modified_content)
        print("MainActivity.kt modified successfully.")

        # 3. Build the APK using Gradle
        # IMPORTANT: These commands require Gradle and Android SDK to be set up
        # and in your server's PATH, or you need to provide full paths.
        # This can be CPU and RAM intensive!
        try:
            # Use a list for the command and arguments for safety
            # Ensure gradlew is executable: chmod +x {temp_project_path}/gradlew
            # This will run the `assembleRelease` task to build the unsigned APK
            build_command = ['./gradlew', 'clean', 'assembleRelease'] # clean first
            #if you're using windows
            build_result = subprocess.run(
                build_command,
                cwd=temp_project_path, # Execute from the project root
                capture_output=True,   # Capture stdout and stderr
                text=True,             # Decode output as text
                check=True             # Raise CalledProcessError if return code is non-zero
            )
            print(f"Gradle build output:\n{build_result.stdout}")
            print("Gradle build completed successfully.")
        except subprocess.CalledProcessError as e:
            print(f"Gradle build failed:\n{e.stdout}\n{e.stderr}")
            raise Exception(f"Gradle build failed with exit code {e.returncode}")
        except FileNotFoundError:
            raise Exception("Gradlew not found or not executable. Check permissions and PATH.")

        # 4. Locate the unsigned APK
        # Assuming typical Gradle output path for release APK
        unsigned_apk_path = os.path.join(temp_project_path, 'app', 'build', 'outputs', 'apk', 'release', 'app-release-unsigned.apk')
        if not os.path.exists(unsigned_apk_path):
            raise FileNotFoundError(f"Unsigned APK not found at: {unsigned_apk_path}")
        
        # 5. Sign the APK (CRITICAL FOR INSTALLATION)
        # You MUST replace these with your actual keystore details and path
        # And ensure 'apksigner' from Android SDK build-tools is in your PATH.
        KEYSTORE_PATH = os.getenv('KEYSTORE_PATH', '/path/to/your/my-release-key.jks')
        KEYSTORE_ALIAS = os.getenv('KEYSTORE_ALIAS', 'myalias')
        KEYSTORE_PASSWORD = os.getenv('KEYSTORE_PASSWORD', 'your_keystore_password')
        KEY_PASSWORD = os.getenv('KEY_PASSWORD', 'your_key_password')

        signed_apk_name_base = urlparse(url).netloc.replace('.', '_').replace('-', '_').lower()
        if not signed_apk_name_base:
            signed_apk_name_base = "webview_app"
        
        apk_filename = f"{signed_apk_name_base}_{uuid.uuid4().hex[:8]}.apk"
        final_apk_path = os.path.join(GENERATED_APKS_DIR, apk_filename)

        # Use the 'apksigner' from Android SDK build-tools
        # Ensure build-tools are in PATH (e.g., /opt/android-sdk/build-tools/34.0.0/)
        sign_command = [
             'apksigner', 'sign',
             '--ks', KEYSTORE_PATH,
             '--ks-key-alias', KEYSTORE_ALIAS,
             '--ks-pass', f"pass:{KEYSTORE_PASSWORD}",
             '--key-pass', f"pass:{KEY_PASSWORD}",
             '--out', final_apk_path,
             unsigned_apk_path
        ]
        print(f"Starting APK signing: {' '.join(sign_cmd)}")
        try:
            sign_result = subprocess.run(sign_cmd, capture_output=True, text=True)
            if sign_result.returncode != 0:
                print(f"APK signing failed:\n{sign_result.stderr}")
                raise Exception(f"APK signing failed: {sign_result.stderr}")
            print(f"APK signing output:\n{sign_result.stdout}")
            print("APK signed successfully.")
        except subprocess.CalledProcessError as e:
            print(f"APK signing failed:\n{e.stdout}\n{e.stderr}")
            raise Exception(f"APK signing failed with exit code {e.returncode}")
        except FileNotFoundError:
            raise Exception("apksigner not found. Check Android SDK build-tools installation and PATH.")

        return apk_filename
        
    except Exception as e:
        print(f"Error during APK generation: {e}")
        return None
    finally:
        # 6. Clean up the temporary build directory
        if os.path.exists(temp_project_path):
            print(f"Cleaning up temporary directory: {temp_project_path}")
            shutil.rmtree(temp_project_path)

        
# --- API Endpoint to Generate APK ---
@app.route('/generate-apk', methods=['POST'])
def generate_apk():
    data = request.get_json()
    if not data or 'url' not in data:
        return jsonify({"success": False, "message": "Missing 'url' in request body"}), 400
        
    url = data['url']
    
    # Basic URL validation (you'd need more robust validation)
    if not url.startswith('http://') and not url.startswith('https://'):
        return jsonify({"success": False, "message": "URL must start with http:// or https://"}), 400

    print(f"Received request to generate APK for: {url}")

    apk_filename = generate_webview_apk_real(url)

    if apk_filename:
        # Construct the download URL
        # IMPORTANT: In a real deployed environment, replace 'http://127.0.0.1:5000' with your server's public IP/domain
        download_url = f"{request.url_root}download-apk/{apk_filename}"
        return jsonify({
            "success": True,
            "message": "APK generation initiated (simulated).",
            "apk_filename": apk_filename,
            "download_url": download_url
        }), 200
    else:
        return jsonify({
            "success": False,
            "message": "APK generation failed (simulated)."
        }), 500

        
# --- API Endpoint to Download APK ---
@app.route('/download-apk/<filename>', methods=['GET'])
def download_apk(filename):
    """
    Serves the generated APK file for download.
    """
    print(f"Serving request for file: {filename}")
    try:
        return send_from_directory(
            GENERATED_APKS_DIR,
            filename,
            as_attachment=True, # Forces download instead of displaying in browser
            mimetype='application/vnd.android.package-archive' # Standard APK MIME type
        )
    except FileNotFoundError:
        return jsonify({"success": False, "message": "APK not found"}), 404
    
if __name__ == "__main__":
    # When running locally for testing, use your machine's IP if testing from emulator/device
    # For Android emulator: host_name = '10.0.2.2' (emulator's special alias for host loopback) (host='0.0.0.0' allows connection from '10.0.0.2')
    # For physical device on same LAN: host_name = '0.0.0.0' or 'your_specific_local_ip_address' (e.g., '192.168.1.100')
    print(f"Starting Flask server. Ensure {GENERATED_APKS_DIR} and {TEMPLATE_DIR} exist in the same directory.")
    print("If you're using a physical device, remember to use your computer's local IP address in the Android app.")
    app.run(debug=True, host='0.0.0.0', port=5000)