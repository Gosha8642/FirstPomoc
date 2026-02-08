# AED Slovakia - Product Requirements Document

## Overview
Android application for finding AED (Automated External Defibrillators) in Slovakia with first aid training functionality.

## Architecture
- **Platform**: Native Android (Java)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Map**: OSMDroid with OpenStreetMap tiles
- **Routing**: OSRM (Open Source Routing Machine)

## Core Features
1. **Home Screen**
   - Large SOS button with pulse animation
   - Training button for first aid instructions
   - Language switcher (EN/SK/UA)
   - Theme toggle (Light/Dark)

2. **Map Screen**
   - OpenStreetMap with Slovakia boundaries
   - AED markers loaded from GeoJSON
   - Route building to selected AED
   - My Location FAB
   - AED info panel with details

3. **Training Screen**
   - 5-step CPR training with ViewPager2
   - Landscape orientation
   - Step-by-step illustrations

## What's Been Implemented (2024-02-08)

### UI/UX Redesign
- [x] New color palette: Emergency Red (#FF3B30), Safety Green (#34C759), Accent Blue (#007AFF)
- [x] Light/Dark theme support with toggle button
- [x] Pulse animation for SOS button
- [x] Entrance animations for buttons
- [x] Slide animations for navigation
- [x] Page indicator for training ViewPager
- [x] Modern card-based training layout
- [x] Bottom panel for AED info with animation
- [x] Language selection with visual feedback

### Files Modified
- `values/colors.xml` - New color palette
- `values/themes.xml` - DayNight theme without ActionBar
- `values-night/themes.xml` - Dark theme
- `fragment_home.xml` - Redesigned with theme toggle, pulse ring
- `fragment_dashboard.xml` - FAB styling, animated panel
- `fragment_training.xml` - Page indicator added
- `item_training_page.xml` - Card-based layout
- `activity_main.xml` - Removed padding, styled bottom nav
- `mobile_navigation.xml` - Added transition animations
- `HomeFragment.java` - Animations, theme toggle logic
- `DashboardFragment.java` - Panel animations, styling
- `TrainingFragment.java` - Page indicator logic
- `ThemeHelper.java` - Theme persistence
- `LocaleHelper.java` - Locale persistence

### New Drawables
- `bg_sos_button.xml` - SOS ripple effect
- `bg_button_training.xml` - Training button style
- `bg_bottom_sheet.xml` - Panel background
- `bg_card.xml` - Card styling
- `bg_lang_button.xml` - Language button
- `bg_lang_button_selector.xml` - Selection state
- `bg_theme_toggle.xml` - Theme button
- `bg_aed_badge.xml` - AED label
- `bg_step_badge.xml` - Step number badge
- `bg_handle_bar.xml` - Bottom sheet handle
- `ic_theme_dark.xml` - Moon icon
- `ic_theme_light.xml` - Sun icon
- `ic_training.xml` - Training icon
- `ic_navigate.xml` - Navigation arrow
- `ic_swipe.xml` - Swipe hint

### New Animations
- `pulse.xml` - SOS pulse effect
- `slide_in_right.xml` - Fragment enter
- `slide_out_left.xml` - Fragment exit
- `slide_in_left.xml` - Fragment pop enter
- `slide_out_right.xml` - Fragment pop exit
- `fade_scale_in.xml` - Scale fade animation

## Backlog / Future Features

### P0 (Critical)
- [ ] SOS button actual functionality (call 112)
- [ ] AED data update mechanism

### P1 (Important)
- [ ] Offline map support
- [ ] Push notifications for nearby AED
- [ ] Distance calculation to AED

### P2 (Nice to have)
- [ ] User accounts
- [ ] AED reporting/updating
- [ ] Favorites/bookmarks
- [ ] Training completion tracking

## User Personas
1. **Tourist** - Needs quick access to AED in unfamiliar area
2. **Local Resident** - Wants to know nearest AED location
3. **First Responder** - Needs fastest route to AED
4. **Training Participant** - Learning CPR basics

## Design Philosophy
"Calm Urgency" - Minimalist interface that reduces cognitive load during emergencies while being engaging for training purposes.
