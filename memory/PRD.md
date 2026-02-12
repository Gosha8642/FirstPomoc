# FirstPomoc - Product Requirements Document

## Overview
Android application for finding AED (Automated External Defibrillators) in Slovakia with first aid training functionality and SOS push notifications.
**App Name**: FirstPomoc

## Architecture
- **Platform**: Native Android (Java)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Map**: OSMDroid with OpenStreetMap tiles
- **Routing**: OSRM (Open Source Routing Machine)
<<<<<<< HEAD
- **Push Notifications**: OneSignal SDK (App ID: 0d2df905-4641-48e5-b9df-c684735e89f1)
=======
- **Push Notifications**: OneSignal
- **Backend**: FastAPI + MongoDB (for SOS notifications)
>>>>>>> 552105aaafdee6c893057b00592ed0e3ca2a863a

## Core Features
1. **Home Screen**
   - Large SOS button with pulse animation
   - Training button for first aid instructions
   - Language switcher (EN/SK/UA)
   - Theme toggle (Light/Dark)

2. **Map Screen**
   - OpenStreetMap with Slovakia boundaries
   - AED markers loaded from GeoJSON
   - Route building to selected AED (blue color)
   - My Location FAB
   - AED info panel with details
   - SOS alert system with 200m radius

3. **Training Screen**
   - 5-step CPR training with ViewPager2
   - Landscape orientation
   - Step-by-step illustrations

4. **Push Notifications (NEW)**
   - OneSignal integration for receiving notifications
   - Geofenced SOS alerts to nearby users (200m radius)
   - Action buttons: "Help Coming" / "False Alarm"

## What's Been Implemented

<<<<<<< HEAD
### Latest Changes (December 2025)
- [x] **New App Icon** - Custom SOS/AED map icon
- [x] **OneSignal Push Notifications** - Integrated for 200m radius SOS alerts
- [x] **SOS Banner Redesign** - Wider horizontal rectangle with rounded corners (280dp width)
- [x] Changed map route color from green to blue (#007AFF)
- [x] Fixed .gitignore with proper API key security setup
- [x] Created local.properties template for API keys

### OneSignal Integration Files
- `SOSApplication.java` - Application class with OneSignal init
- `SOSNotificationService.java` - Service for sending SOS alerts to nearby users
- `MainActivity.java` - Updated with notification permission handling
- `build.gradle.kts` - Added OneSignal and OkHttp dependencies
- `AndroidManifest.xml` - Added POST_NOTIFICATIONS and BACKGROUND_LOCATION permissions
=======
### OneSignal Push Notifications (December 2025)
- [x] OneSignal SDK integrated in Android app
- [x] SOSApplication class with OneSignal initialization
- [x] SOSAlertService for API communication
- [x] NotificationClickHandler for handling notification actions
- [x] FastAPI backend for sending notifications
- [x] MongoDB for storing user locations and alerts
- [x] Haversine distance calculation for nearby users
- [x] SOS banner redesigned (rounded corners, narrower width)

### Backend API Endpoints
- `POST /api/users/location` - Update user location
- `GET /api/users/nearby` - Find users within radius
- `POST /api/alerts/sos` - Trigger SOS notification
- `POST /api/alerts/cancel` - Cancel SOS alert
- `GET /api/alerts/history/{user_id}` - Get alert history
- `GET /api/stats` - System statistics
- `GET /api/health` - Health check

### UI Changes (December 2025)
- [x] Changed map route color from green to blue (#007AFF)
- [x] SOS banner: narrower (280dp), rounded corners (20dp radius)
- [x] Fixed .gitignore with proper API key security
>>>>>>> 552105aaafdee6c893057b00592ed0e3ca2a863a

### Files Added
- `SOSApplication.java` - OneSignal initialization
- `services/SOSAlertService.java` - SOS API service
- `services/NotificationClickHandler.java` - Notification handler
- `backend/server.py` - FastAPI backend
- `backend/requirements.txt` - Python dependencies
- `backend/.env` - Environment variables (gitignored)
- `drawable/bg_sos_banner.xml` - Rounded banner background

## Configuration

### OneSignal
- App ID: `0d2df905-4641-48e5-b9df-c684735e89f1`
- REST API Key: stored in backend/.env

### Firebase Setup Required
To enable push notifications:
1. Create Firebase project at https://console.firebase.google.com
2. Add Android app with package: `com.example.sosapplication`
3. Download `google-services.json` to `/app/app/`
4. Generate Service Account JSON
5. Upload to OneSignal Dashboard → Settings → Push Platforms → Android FCM

## Backlog / Future Features

### P0 (Critical)
- [ ] Firebase FCM setup (user action required)
- [ ] Test push notifications on real device
- [ ] SOS button call 112 functionality

### P1 (Important)
<<<<<<< HEAD
- [x] Push notifications via OneSignal - IMPLEMENTED (needs REST API key from user)
- [ ] FAB animation improvement (recurring issue - needs programmatic listener)
=======
- [ ] FAB animation improvement (recurring issue)
>>>>>>> 552105aaafdee6c893057b00592ed0e3ca2a863a
- [ ] Offline map support
- [ ] Distance calculation to AED

### P2 (Nice to have)
- [ ] User accounts with authentication
- [ ] AED reporting/updating
- [ ] Favorites/bookmarks
- [ ] Training completion tracking

## Setup Required
**OneSignal REST API Key**: User needs to add their REST API key to `SOSNotificationService.java` or implement backend proxy for security.
- Get key from: OneSignal Dashboard -> Settings -> Keys & IDs -> REST API Key

## Known Issues
- **FAB Animation**: The location button animation when AED panel appears has been inconsistent
- **Firebase**: Requires manual setup by user in Firebase Console

## Testing Notes
⚠️ This is a **native Android application**. Cannot be tested on Emergent platform.
- Download via "Save to GitHub"
- Build and test in **Android Studio**
- Use physical device or emulator

## User Personas
1. **Tourist** - Needs quick access to AED in unfamiliar area
2. **Local Resident** - Wants to know nearest AED location
3. **First Responder** - Needs fastest route to AED
4. **Training Participant** - Learning CPR basics

## Design Philosophy
"Calm Urgency" - Minimalist interface that reduces cognitive load during emergencies while being engaging for training purposes.
