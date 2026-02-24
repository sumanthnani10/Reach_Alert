# Reach Alert

**Set a destination. Get there. We’ll tell you when you’ve arrived.**  

Reach Alert is an Android app that uses your location (including in the background) to figure out when you’ve reached a place you chose — and then alerts you with a notification and full-screen “You’ve arrived” screen, even if the app was in the background or the phone was locked.

---

## What it does

- **Pick a place** — Search or tap on the map (Google Maps + Places). Set your target and an optional radius.
- **Track in the background** — A foreground location service keeps an eye on where you are so we don’t miss the moment.
- **Alert when you arrive** — When you enter the target area, you get a notification and a full-screen “You’ve arrived at [place]” screen (with sound/vibrate and a Stop button). Works when the app is closed or the device is locked.
- **Optional sign-in** — Firebase Auth (and Firestore) are wired for user accounts; the app can run without signing in.
- **Consent first** — Before using background location, we show a Flutter-based consent screen (from the `reach_alert` module) that explains why we need it and lets the user Accept or Decline.

So: one job — **“Tell me when I get there.”** No fluff.

---

## What problem it solves

- **“Did I pass it?”** — You’re driving or walking to a spot and don’t want to keep checking the map. Set the destination once; we ping you when you’re there.
- **Delivery / errands** — Head to a pin, get notified at arrival so you can focus on the road.
- **Background reliability** — We use a foreground service and background location so the “arrived” logic keeps running even when the app isn’t in the foreground.

---

## Tech stack

- **Android** — Java, minSdk 24, targetSdk 33, Gradle 8.2
- **Maps & location** — Google Maps, Places API, Fused Location Provider, background location
- **Flutter** — Embedded via the `reach_alert` module for the background-location consent UI
- **Firebase** — Auth, Firestore (for optional user data)
- **Ads** — Google Mobile Ads, Unity Ads
- **Other** — Picasso, full-screen intent for “You’ve arrived” on lock screen, foreground service (location type)

---

## Project structure (high level)

```
Reach-Alert/
├── app/                          # Main Android application
│   ├── src/main/
│   │   ├── java/.../reachalert/
│   │   │   ├── SplashActivity         # Launcher, routing
│   │   │   ├── MapsActivityPrimary    # Main map UI
│   │   │   ├── MapsActivitySecondary  # Map flow step 2
│   │   │   ├── MapsActivityFinal      # Map flow step 3
│   │   │   ├── FlutterEmbeddingActivity  # Flutter consent screen
│   │   │   ├── LocationUpdatesService # Background location + “arrived?” logic
│   │   │   ├── RemainderReceiver      # “You’ve arrived” notification
│   │   │   ├── FullScreenIntent       # Full-screen “You’ve arrived” + Stop
│   │   │   ├── LoginActivity
│   │   │   ├── TargetDetails, Utils, etc.
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── reach_alert/                  # Flutter module (consent UI)
│   └── README.md                 # See here for module details
├── build.gradle
├── settings.gradle               # Includes reach_alert Flutter module
└── README.md                     # You are here
```

---

## How to run

### Prerequisites

- **Android Studio** (or compatible IDE) with Android SDK 24+
- **JDK** (Java 8 compatible; project uses `sourceCompatibility = "1.8"`)
- **Flutter SDK** (for the `reach_alert` module)
- **API keys** — See **Setting up API keys** below.

### Steps

1. **Clone and open**
   - Clone the repo and open the **Reach-Alert** root in Android Studio (the folder that contains `app/` and `reach_alert/`).

2. **Flutter**
   - From the repo root:
     ```bash
     cd reach_alert && flutter pub get && cd ..
     ```

3. **Gradle**
   - Sync the project (e.g. **File → Sync Project with Gradle Files**).
   - Ensure `settings.gradle` points at `reach_alert/.android/include_flutter.groovy` (it does if you haven’t moved the module).

4. **Config (API keys)**
   - Copy `local.properties.example` to `local.properties` and fill in your keys (Maps, OAuth, AdMob). See **Setting up API keys** below.
   - Copy `app/google-services.json.example` to `app/google-services.json` and replace placeholders with your Firebase project values (or download `google-services.json` from Firebase Console).

5. **Run**
   - Select a device/emulator and run the **app** configuration.  
   - First launch goes through splash → map flow; when background location is needed, the Flutter consent screen is shown.

### Building a release APK/AAB

- Use the **release** build type (minify/shrink enabled in `app/build.gradle`).
- Sign with your keystore (e.g. `reachalert.jks`); configure signing in the app’s build.gradle or in the IDE.

---

## Setting up API keys

All API keys are kept out of the repo. You provide them locally:

1. **`local.properties`** (at project root)
   - Copy `local.properties.example` to `local.properties`.
   - Fill in:
     - `GOOGLE_MAPS_KEY_DEBUG` / `GOOGLE_MAPS_KEY_RELEASE` — from [Google Cloud Console](https://console.cloud.google.com/apis/credentials) (Maps SDK for Android).
     - `GOOGLE_OAUTH_CLIENT_ID` — Web client ID from Firebase Console → Project settings → Your apps.
     - `ADMOB_APP_ID` / `ADMOB_AD_UNIT_ID` — from [AdMob](https://admob.google.com/).
   - `local.properties` is gitignored and never committed.

2. **`app/google-services.json`**
   - Copy `app/google-services.json.example` to `app/google-services.json`, then replace placeholders with your Firebase project id, API key, and client config — or download the real file from [Firebase Console](https://console.firebase.google.com/) → Project settings → Your apps.
   - `app/google-services.json` is gitignored. If you had a working build before, re-download this file from Firebase Console (it is no longer in the repo).

---

## Permissions

The app requests (and explains via the consent screen):

- **Foreground + background location** — To compute distance to the target and trigger “You’ve arrived” when you enter the radius.
- **Full-screen intent** — So the “You’ve arrived” screen can show over the lock screen.
- **Foreground service (location)** — So tracking continues in the background.
- **Vibrate, Internet, etc.** — As needed for notifications and maps.

---

## Flutter module (`reach_alert`)

The in-app **background location consent** screen is implemented in Flutter and embedded via `FlutterEmbeddingActivity`. For what that module does, how it’s wired, and how to run it standalone, see **[reach_alert/README.md](reach_alert/README.md)**.

---

## Geo intent

The app can be opened from **geo:** links (e.g. from another map app). SplashActivity declares an intent filter for `geo` so a shared location can open Reach Alert.

---

**TL;DR** — Reach Alert is an Android “arrival alert” app: set a destination on the map, and we notify you when you get there, even in the background. This repo is the native app plus the Flutter consent module. Open it in Android Studio, add your API keys, sync Gradle and Flutter, and run.
