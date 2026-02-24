# reach_alert

**The Flutter brain of Reach Alert** — the bit that shows up when we need to ask nicely for background location. No sketchy fine print, no walls of legalese. Just a clear “here’s why we need it” and a proper Accept / Decline.

---

## What’s Reach Alert?

Reach Alert is an **arrival alert** app. You pick a destination, we keep an eye on where you are (even when the app is in the background), and we **ping you when you’ve reached it**. No more “did I pass it?” or staring at the map. Set it and forget it.

This repo is **not** the whole app. It’s the **Flutter module** that powers the in-app **background location consent screen** — the screen that explains why we use location and lets the user accept or decline. The rest (maps, places, alerts, Firebase, etc.) lives in the native **Reach-Alert** Android host.

---

## What this module does

- **Consent screen** — Explains in plain language that we use location to:
  - Calculate distance to your destination
  - Alert you when you arrive
  - Do this even when the app is closed or in the background
- **Privacy-first** — States clearly that location data isn’t shared with third parties and is only used for the app.
- **Accept / Decline** — Two buttons; choice is sent back to the host app via a **method channel** (`com.confegure.reach_alert/methods` → `locationPermissionResponse`).

So: **one job, done well** — transparent, user-friendly permission flow.

---

## Problem it solves

Users (and stores) care a lot about **why** an app wants background location. A generic system dialog doesn’t explain that we’re only using it for “you’ve arrived” alerts. This module gives Reach Alert a **dedicated, on-brand consent step** before the native app requests or uses background location — so we stay compliant and users know exactly what they’re agreeing to.

---

## How to run

### As part of the main app (normal use)

This module is **embedded in the Reach-Alert Android app**. Open and run the **Reach-Alert** project (the parent directory). The consent screen is shown when the host app launches the Flutter embedding and needs location permission.

### Standalone (for Flutter-only dev)

From this directory:

```bash
cd reach_alert
flutter pub get
flutter run
```

That runs the default Flutter Runner; you’ll see the consent screen. For full flow (maps, destinations, real alerts), use the host app.

### Add-to-app (integrating into another host)

See Flutter’s [add-to-app docs](https://flutter.dev/docs/development/add-to-app). This module is set up as an AndroidX Flutter module:

- **Android package:** `com.confegure.reach_alert`
- **iOS bundle ID:** `com.confegure.reachAlert`

---

## Tech stack

- **Flutter** (SDK `>=3.3.4 <4.0.0`)
- **Method channel** to talk to the host (`locationPermissionResponse` with `accepted: true/false`)
- **Poppins** font, Material theme, no debug banner

---

## Project structure

```
reach_alert/
├── lib/
│   └── main.dart          # App entry + ConsentPage UI & method channel
├── pubspec.yaml
└── README.md              # You are here
```

---

**TL;DR** — Reach Alert tells you when you’ve reached your destination. This repo is its Flutter consent screen for background location: clear copy, Accept/Decline, and a clean handoff to the native app. Run it inside Reach-Alert for the full experience, or `flutter run` here to poke the UI.
