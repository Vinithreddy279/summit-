

# 🏔 Summit

### Premium Outdoor Fitness & Hiking Tracker

Track hikes, runs, and walks with real-time GPS, offline maps, weather integration, and detailed activity analytics.

           🏔 SUMMIT

Outdoor Fitness & Hiking Tracker

GPS • Offline Maps • Hiking • Analytics

────────────────────────────────────────────

## 📖 About

Summit is a premium Android outdoor fitness application inspired by Strava and AllTrails.

It enables users to track hiking, running, and walking activities with real-time GPS recording, offline OpenStreetMap support, weather integration, GPX import/export, and detailed performance analytics.

The app is built using modern Android development practices including Jetpack Compose, MVVM architecture, and Room Database.


 🌟 Highlights

- 🛰️ Real-time GPS tracking with intelligent filtering
- 🗺️ Offline OpenStreetMap integration
- 🥾 Hiking, Running, and Walking activity modes
- 🌦️ Built-in weather dashboard with offline cache
- 📊 Activity history with performance analytics
- 📂 GPX import and export support
- 💾 Room Database for offline storage
- 🎨 Modern Material 3 user interface


##
 ## 🛠 Tech Stack

### Android

- Kotlin
- Jetpack Compose
- Coroutines
- Material 3

## 🏗️ Architecture

```text
UI (Jetpack Compose)
        │
        ▼
    ViewModel
        │
        ▼
    Repository
        │
 ┌───────────────┐
 │ Room Database │
 │ GPS Service   │
 │ Weather API   │
 │ OpenStreetMap │
 └───────────────┘
```

### Database

- Room

### Maps

- OpenStreetMap (osmdroid)

### APIs

- Weather API
- Fused Location Provider

## 🚀 Roadmap

- [x] GPS Tracking
- [x] Hiking
- [x] Running
- [x] Walking
- [x] Offline Maps
- [x] GPX Import
- [x] GPX Export
- [x] Weather

This contains everything you need to run your app locally.
### Planned

View your app in AI Studio: https://ai.studio/apps/4ad05929-2644-42af-9d62-e0bb9c3d7abc
- [ ] Cloud Sync
- [ ] Wear OS Support
- [ ] AI Coach
- [ ] Social Sharing

## Run Locally
## 👨‍💻 Developer
**Vinith Reddy**

🌐 Portfolio  
https://vinithreddy.vercel.app

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
💻 GitHub  
https://github.com/Vinithreddy279
