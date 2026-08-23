# ⚡ PulsePoll - Real-Time Social Polling & Event Consensus for Android

[![Live Showcase](https://img.shields.io/badge/GitHub%20Pages-Live%20Showcase-indigo.svg?style=flat-square&logo=github)](https://your-username.github.io/pulsepoll-android/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-indigo.svg?style=flat-square&logo=materialdesign)](https://m3.material.io)
[![Room Database](https://img.shields.io/badge/Storage-Room%20SQLite-brightgreen.svg?style=flat-square&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%2B-green.svg?style=flat-square&logo=android)](https://developer.android.com)

**PulsePoll** is a modern, reactive Android application built with **Jetpack Compose** and **Material 3 (M3)** for organizing group events, deciding team plans, gathering instant feedback, and reaching group consensus with real-time analytics, RSVP headcounts, and multi-user participation.

> 🌐 **Interactive GitHub Pages Showcase**: View the live web showcase and interactive phone preview at [`docs/index.html`](docs/index.html) or host via GitHub Pages.

---

## 🌟 Key Highlights & Feature Showcase

### 1. 🎯 Instant 6-Character Join Codes
* Every created poll receives a unique 6-character short code (e.g. `SOC5V5`, `DRK777`, `LUNCH1`).
* Participants can tap **"Join with Code"** in the top action bar to jump directly into any poll across devices.

### 2. 👥 Multi-User Consensus & Social SSO
* Built-in **Google**, **Facebook**, and **Apple ID** authentication profiles.
* Switch attendee profiles on the fly (*Alex Rivera (Google)*, *Sarah Jenkins (Facebook)*, *Marcus Chen (Apple)*, etc.) to simulate multi-person voting, test quorum thresholds, and watch live leaderboards react dynamically.

### 3. 📊 Real-Time Analytics & Quorum Tracking
* Dynamic winner and leading option indicators with live percentage bars (`LinearProgressIndicator`).
* Quorum progress meters (e.g., minimum 8 attendees required for booking a soccer pitch).
* Deep analytical breakdowns: vote counts, percentages, voter lists, and breakdown by attendee.

### 4. 🎟️ RSVP Headcount & Guest Attendance (+1s)
* Integrated attendance tracker for group outings and dinners.
* Track headcount categories: **Going (+guests)**, **Maybe**, and **Decline**.
* Real-time headcount tallies automatically calculate total attendance numbers for reservations and bookings.

### 5. 🚀 1-Tap Ready Templates
* Instant poll setup with pre-populated options, category icons, and rules:
  * ⚽ **Soccer Pitch Booking** (Thursday 7PM, Friday 6PM, Saturday 9AM)
  * 🍻 **Friday Drinks & Happy Hour** (Rooftop Bar, Craft Brewery, Speakeasy)
  * 🍕 **Team Lunch Decision** (Taco Fiesta, Italian Bistro, Ramen House)
  * 🎬 **Weekend Movie Night** (Action Sci-Fi, Horror Thriller, Comedy)
  * 🏕️ **Outdoor Camping Trip** (Lakeview Trails, Mountain Camp, Beach Pines)
  * ⭐ **Sprint Retro & Feedback** (Ranked-choice improvements)

### 6. 📤 Multi-Format Export & Sharing
* 1-tap formatted text summary generation ready for **WhatsApp**, **Telegram**, **Slack**, and **SMS**.
* Download complete **CSV reports** containing poll metadata, option statistics, voter logs, and RSVP rosters.

### 7. ⏰ Deadlines & Intelligent Notifications
* Set voting deadlines with live countdown timers (`Ending Soon`, `Hours Left`).
* Automatic notifications when quorums are reached, deadlines approach, or leading options shift.

---

## 📱 UI Showcase & Architecture

### Material 3 Design System
* **Dynamic Surface Palette**: Crisp `Slate 50` background (`#F8FAFC`), pure white cards (`#FFFFFF`), and refined tonal elevations (`surfaceContainerLowest` through `surfaceContainerHighest`).
* **Accent Colors**: Electric Indigo (`#4F46E5`), Sky Cyan (`#0284C7`), and Coral Orange (`#EA580C`).
* **Accessible Touch Targets**: Strict compliance with 48dp interactive component standards.
* **Collapsible Insights**: Quick-view voting metrics that expand or collapse on demand to keep the homepage focused and uncluttered.

```
┌────────────────────────────────────────────────────────┐
│  ⚡ PulsePoll                      🔍  🔔 [👤 Alex ▼]  ⋮│
├────────────────────────────────────────────────────────┤
│  [⚡ 3 Active • 4 Polls ▼]        [🚀 Templates] [🔑 Join]│
├────────────────────────────────────────────────────────┤
│  [All (4)] [⚡ Active (3)] [⚽ Soccer] [🍻 Drinks] [🍕] │
├────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐  │
│  │ ⚽  5v5 Thursday Soccer Pitch          ⏰ 4h left │  │
│  │     By Alex Rivera • SOC5V5 • Weekend Squad      │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │ 🗳️ 8 Votes • 👥 6 Voters    ✓ 10 Attending │  │  │
│  │  ├────────────────────────────────────────────┤  │  │
│  │  │ LEADING CHOICE                        63%  │  │  │
│  │  │ Thursday 7:00 PM (Downtown Arena)          │  │  │
│  │  │ [████████████████████████░░░░░░░░░░░░░░░░] │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │  [  🗳️ Vote Now  ]   [ 📊 Results ]   [ 📤 Share ]│  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 🏗️ Architecture & Tech Stack

PulsePoll adheres to modern Android Clean Architecture and MVVM design principles:

* **Language**: [Kotlin](https://kotlinlang.org/) (Coroutines, StateFlow, Serialization)
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
* **State Management**: Android Architecture Components `ViewModel` + `MutableStateFlow`
* **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) with reactive DAO queries
* **Navigation**: Compose Navigation with type-safe routing
* **Testing**: Robolectric unit and UI test suite (`:app:testDebugUnitTest`)

### Package Structure
```
com.example/
├── data/
│   ├── local/
│   │   ├── dao/             # PollDao (reactive database queries)
│   │   ├── database/        # AppDatabase (Room SQLite instance)
│   │   └── entity/          # PollEntity, OptionEntity, VoteEntity, RsvpEntity, NotificationEntity
│   ├── model/               # Domain models, AuthProvider, PollTemplate, HeadcountSummary
│   └── repository/          # PollRepository (single source of truth)
├── ui/
│   ├── components/          # PollCard, TemplatesBottomSheet, JoinCodeDialog, SharePollDialog, AuthDialog
│   ├── screens/             # PollListScreen, VotingScreen, CreatePollScreen, PollAnalyticsScreen
│   ├── theme/               # Color, Theme, Type, Shape (Material 3 Tokens)
│   └── viewmodel/           # PollViewModel (Reactive UI state management)
└── util/                    # DataExportHelper, Formatters
```

---

## 🚀 Getting Started & Building

### Prerequisites
* Android Studio Ladybug / Koala or newer
* JDK 17 or higher
* Android SDK (API Level 26+)

### Build & Run via Command Line
```bash
# Clone the repository
git clone https://github.com/your-username/pulsepoll-android.git
cd pulsepoll-android

# Run unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

The generated APK will be available in `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 License
This project is licensed under the Apache License 2.0.
