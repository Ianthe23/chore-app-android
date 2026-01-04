# Google Maps API Key Setup

The LocationPickerActivity requires a Google Maps API key to function properly. Follow these steps to set it up:

## Step 1: Get a Google Maps API Key

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Maps SDK for Android** API:
   - Go to "APIs & Services" > "Library"
   - Search for "Maps SDK for Android"
   - Click on it and press "Enable"
4. Create credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "API Key"
   - Copy the generated API key

## Step 2: Restrict the API Key (Recommended)

1. Click on the API key you just created
2. Under "Application restrictions", select "Android apps"
3. Click "Add an item"
4. Enter your package name: `com.choreapp.android`
5. Get your SHA-1 fingerprint:
   ```bash
   # For debug keystore (development)
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
6. Copy the SHA-1 fingerprint and paste it in the console
7. Click "Done" and "Save"

## Step 3: Add the API Key to Your App

Create or edit the file: `app/src/main/res/values/api_keys.xml`

This file is already listed in `.gitignore` so your API key will remain secure and won't be committed to version control.

Add your API key to the file:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Google Maps API Key - DO NOT COMMIT THIS FILE -->
    <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">YOUR_API_KEY</string>
</resources>
```

Replace `AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q` with your actual API key.

**Note:** The file `api_keys.xml` is gitignored, so it won't be tracked by version control. This keeps your API key secure.

## Step 4: Test the App

1. Build and run the app
2. Go to create or edit a chore
3. Click "Add Location"
4. The map should now load properly
5. Move the map to select a location
6. Click "Confirm Location"

## Troubleshooting

### Map shows "This app won't run unless you update Google Play services"
- Update Google Play Services on your device/emulator

### Map shows blank/gray tiles
- Your API key might not be properly configured
- Check that Maps SDK for Android is enabled in Google Cloud Console
- Verify the API key is correctly added to strings.xml
- Make sure your SHA-1 fingerprint is added to the API key restrictions

### App crashes when opening location picker
- Check logcat for detailed error messages
- Verify the API key is not restricted to a different package name or SHA-1

## Important Notes

- Never commit your actual API key to public repositories
- Consider using a `.gitignore` entry for sensitive files
- For production, use API key restrictions to prevent unauthorized usage
- Monitor your API usage in Google Cloud Console to avoid unexpected charges
