# Cooly

**Find cool, stay safe.** Cooly helps you find the nearest cool, safe place before extreme heat or bad air turns dangerous.

One live map shows the "feels like" temperature and air quality where you are, plus the closest refuge — libraries, malls, cafes — sorted by walking distance. Water, shade and public toilets are on the same map, and danger alerts warn you even when the app is closed.

Cooly is a single **Kotlin Multiplatform + Compose Multiplatform** codebase — the same UI runs on both **Android and iOS**.

## Features

- Real-time feels-like temperature + US EPA air-quality index for your location
- Nearest cool refuge, ranked by walking distance
- Water fountains, shade and public toilets nearby
- Danger alerts for extreme heat and bad air, even when the app is closed
- Home-screen widget for a one-glance safety check
- Watch cities you care about with a 7-day heat outlook
- Cooly Plus (in-app purchase) via RevenueCat — one purchase across both stores

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin (Multiplatform) |
| UI | Compose Multiplatform (Android + iOS from one codebase) |
| Networking | Ktor client |
| Payments | RevenueCat (`purchases-kmp`) |
| Weather / air | Open-Meteo |
| Places | OpenStreetMap (Overpass API) |
| Maps | Google Maps |
| Background alerts | WorkManager (Android) |

## Project layout

```
shared/       Kotlin Multiplatform module — domain, data, and the full Compose UI
  commonMain/   shared logic + UI (runs on both platforms)
  androidMain/  Android platform bindings (location, directions, widgets)
  iosMain/      iOS platform bindings (CLLocationManager, etc.)
androidApp/   Android entry point, home-screen widget, background alert worker
iosApp/       iOS entry point (Xcode project) hosting the shared Compose UI
```

## Building

### Android

Add a `local.properties` at the repo root:

```
sdk.dir=/path/to/Android/sdk
MAPS_API_KEY=your_google_maps_key
```

Then:

```
./gradlew :androidApp:assembleDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme, or build on CI (see `codemagic.yaml`).

## License

All rights reserved. This source is published for review; it is not licensed for reuse.
