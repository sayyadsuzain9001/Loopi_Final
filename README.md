<div align="center">

# 🔁 Loopi

### Learn. Review. Repeat.

An offline-first Android study app that helps university students practise with flashcards, find their weak topics, and check whether their confidence matches their results.

![Android](https://img.shields.io/badge/Android-API%2029%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-SQLite-003B57?logo=sqlite&logoColor=white)
![Offline](https://img.shields.io/badge/Works-100%25%20offline-14B89A)
![Tests](https://img.shields.io/badge/unit%20tests-21%20passing-success)

<img src="report_screens/04_home.png" width="230" alt="Home dashboard"/>
<img src="report_screens/13_quiz_feedback.png" width="230" alt="Quiz feedback"/>
<img src="report_screens/15_progress.png" width="230" alt="Progress"/>

</div>

---

## 📖 About

Loopi was built by Group 6 for **IBM4203 Mobile Application Development Using Android** (August 2026). Our Assignment 1 research, a 35-person survey plus an audit of ten education apps, showed that students were tired of ads, subscriptions and apps that need a constant internet connection. They wanted more practice on the topics they actually struggle with.

Loopi answers that with three ideas:

- 🎯 **Confidence Check:** before an answer is revealed you say how sure you are, and Loopi compares that with how you actually did.
- 🔥 **Weak spots:** topics under 70% accuracy and cards you got wrong are surfaced for quick practice.
- 📦 **Study Packs:** topics and flashcards can be exported as a file and shared with classmates, with no account or server.

Everything is stored on the phone. There are no ads, no subscriptions and no internet requirement.

---

## ✨ Features

| | Feature | What it does |
|---|---|---|
| 👋 | **Onboarding & login** | Splash, welcome, local sign-up/sign-in with validation, forgot/reset password, interest setup. Stays signed in. Passwords are stored as salted PBKDF2 hashes. |
| 🏠 | **Home dashboard** | Gradient header, animated daily-goal ring, streak, topics due today, and quick actions (Study, Add topic, Packs, Groups). |
| 📚 | **Topics** | Search, filter and sort. Add and edit topics with field validation, or create a whole topic from a PDF. |
| 🗂️ | **Topic detail** | Stat tiles, flip-card study mode (Not sure / Maybe / I know it), and flashcards and resources (notes, links, files) with full create/edit/delete. |
| 📄 | **PDF to quiz** | Pick a PDF of notes. Loopi reads it **on the device**, finds definitions and key terms, and writes multiple-choice questions you can review and edit before saving. |
| 🧠 | **Quiz with confidence** | One screen: choose an answer, rate your confidence, then get instant feedback, the correct answer and an "Explain simply" note. Answers, confidence and response time are saved. |
| 📊 | **Progress** | Accuracy chart (week / month / all time), correct vs incorrect, a weak-spot card with a Practice button, a confidence calibration score, and per-topic performance. |
| 📦 | **Study Packs** | Group topics into packs, export or share them as a file (flashcards included), and import packs from classmates. |
| 👤 | **Profile & settings** | Profile photo, topic/card/accuracy stats, edit profile, review preferences, log out. |
| 👥 | **Community** | Local study groups with About / Members / Posts, join or leave, and post tips. |
| 🆘 | **Help & Support** | FAQs, app guides, contact by email, and a Report a Problem form with an optional screenshot. |

---

## 📱 Screenshots

| Welcome | Sign in | Home | Topics |
|:---:|:---:|:---:|:---:|
| <img src="report_screens/01_welcome.png" width="180"/> | <img src="report_screens/03_sign_in.png" width="180"/> | <img src="report_screens/04_home.png" width="180"/> | <img src="report_screens/05_topics.png" width="180"/> |

| Topic detail | Flip cards | Quiz | Feedback |
|:---:|:---:|:---:|:---:|
| <img src="report_screens/08_topic_detail.png" width="180"/> | <img src="report_screens/10_flip_card.png" width="180"/> | <img src="report_screens/12_quiz_question.png" width="180"/> | <img src="report_screens/13_quiz_feedback.png" width="180"/> |

| Progress | Weak spots | PDF to quiz | Review questions |
|:---:|:---:|:---:|:---:|
| <img src="report_screens/15_progress.png" width="180"/> | <img src="report_screens/16_weak_spots.png" width="180"/> | <img src="report_screens/17_pdf_quiz_pick.png" width="180"/> | <img src="report_screens/18_pdf_quiz_review.png" width="180"/> |

| Study packs | Profile | Community | Help |
|:---:|:---:|:---:|:---:|
| <img src="report_screens/19_study_packs.png" width="180"/> | <img src="report_screens/22_profile.png" width="180"/> | <img src="report_screens/24_community.png" width="180"/> | <img src="report_screens/26_help_support.png" width="180"/> |

More screens are in [`report_screens/`](report_screens).

---

## 🛠️ Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3, Material Icons Extended |
| Navigation | Navigation Compose (routes with arguments) |
| State | Compose state holders (`mutableStateOf`, snapshot lists) fed by Room `Flow`s |
| Database | Room 2.8 (SQLite), schema version 9 with migrations (10 tables), KSP code generation |
| Accounts, session & settings | Room tables `users` and `app_settings`; passwords and recovery codes stored only as PBKDF2-HMAC-SHA256 hashes with a random salt |
| PDF reading | [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) (offline text extraction) |
| Question generation | Custom on-device generator (`McqGenerator.kt`), no network or AI service |
| Sharing | Storage Access Framework (open/create document), Android share sheet |
| Testing | JUnit 4 (21 unit tests) |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin 9, compile/target SDK 37, min SDK 29 |

---

## 🗂️ Project structure

```
Loopi_Final/
├── app/
│   ├── build.gradle.kts               # App module config and dependencies
│   ├── schemas/                       # Exported Room schemas (v7, v8, v9)
│   └── src/
│       ├── main/java/com/example/myapplication/
│       │   ├── MainActivity.kt        # App entry point and navigation graph
│       │   ├── LoopiComponents.kt     # Shared UI: bottom nav, gradient hero, icon tiles, progress bar
│       │   ├── flow1screens.kt        # Flow 1: splash, welcome, sign up/in, password reset, setup
│       │   ├── flow2screens.kt        # Flow 2: home dashboard, topics, add/edit topic, notifications
│       │   ├── Flow3Screens.kt        # Flow 3: topic detail, flashcards, resources
│       │   ├── Flow3StudyScreen.kt    # Flow 3: flip-card study mode
│       │   ├── PdfQuizScreen.kt       # PDF to quiz, and new topic from PDF
│       │   ├── McqGenerator.kt        # Offline multiple-choice question generator
│       │   ├── Flow4Screens.kt        # Flow 4: quiz, confidence, results, weak-card practice
│       │   ├── Flow5Screens.kt        # Flow 5: progress, calibration, profile
│       │   ├── Flow6Screens.kt        # Flow 6: study packs, settings
│       │   ├── Flow78Screens.kt       # Flows 7 & 8: community, help & support
│       │   ├── LoopiDatabase.kt       # Room database and migrations
│       │   ├── *Entity.kt / *Dao.kt   # Topics, flashcards, resources, review results, study packs
│       │   ├── DefaultStudyContent.kt # Starter topics and flashcards
│       │   └── ui/theme/              # Loopi colours, typography, shapes
│       └── test/java/com/example/myapplication/   # Unit tests
├── gradle/                            # Version catalog and wrapper
├── report_screens/                    # App screenshots and the Assignment 2 report
├── run_loopi.bat                      # Build, install and launch on a running emulator
├── RUN_IN_VSCODE.md                   # Step-by-step setup for VS Code / terminal
└── PRODUCTION_READINESS.md            # Verification notes and release checklist
```

---

## 🚀 Getting started

### Requirements

- Android Studio with **Android SDK Platform 37** and **Build-Tools 36**
- JDK 25 (Gradle can download the toolchain on the first build)
- An Android emulator, or a phone with USB debugging (Android 10 / API 29 or newer)

### Run in Android Studio

1. Clone the repo and open the folder in Android Studio.
2. Let Gradle sync and install any SDK packages it asks for.
3. Start an emulator from **Device Manager** (or plug in a phone).
4. Select the `app` configuration and press ▶️ **Run**.

### Run from a terminal (Windows)

With an emulator or device already running:

```powershell
.\run_loopi.bat
```

Or step by step:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_HOME = $sdk
.\gradlew.bat :app:assembleDebug
& "$sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
& "$sdk\platform-tools\adb.exe" shell am start -n com.example.myapplication/.MainActivity
```

See [RUN_IN_VSCODE.md](RUN_IN_VSCODE.md) for a full walkthrough, including emulator setup.

### 🧪 Run the tests

```powershell
.\gradlew.bat testDebugUnitTest
```

The tests cover password hashing, the starter content, progress and accuracy maths, confidence calibration, the study pack file format, and the PDF question generator.

---

## 📄 How PDF to quiz works

1. **Read:** PdfBox-Android extracts the text layer of the PDF (first 40 pages) on the device.
2. **Find definitions:** lines like *"Normalization is the process of…"* or *"Transaction: a group of operations…"* become *"Which term matches this description?"* questions.
3. **Find key terms:** repeated terms and capitalised phrases are blanked out of other sentences to make fill-in-the-blank questions.
4. **Pick wrong options:** other terms from the same PDF with a similar length, so every option looks plausible.
5. **Review:** you edit or untick questions before they are saved as flashcards. The PDF is kept in the topic's Resources.

It works best on lecture notes and slides that define terms. Scanned PDFs (photos of pages) have no text layer, so Loopi tells you it can't read them.

---

## 💾 Data & privacy

- 📴 **Fully offline.** No backend, no analytics and no network calls.
- 🔐 The account is local to the device. Passwords and recovery codes are stored as salted PBKDF2 hashes.
- 🗄️ Accounts, the login session, settings, topics, cards, resources, answers, packs, community groups/posts and problem reports all live in one Room database on the phone (`loopi_database`). Uninstalling or clearing app data removes them.
- 🤝 Data only leaves the phone when you export or share a study pack yourself.

---

## 👥 Team (Group 6)

| Name | Student ID | Main area |
|---|---|---|
| Akhmetova Azhar | I26043015 | Flow 2: Home, Topics, Add Topic |
| Tussupova Dariya | I26043025 | Flow 3: Topic detail, Flashcards, Resources |
| Zhassyuzak Nursaule | I26043027 | Flow 4: Quiz, Review, Confidence |
| Suzain Mudabbir Sayyad | I26043300 | Flow 5: Progress, Weak spots, Profile & Settings |
| Sanjeet Sachin Zele | I26043274 | Flow 6: Study Packs, Offline support |

Lecturer: Lai Mei Yoon · IBM4203 Mobile Application Development Using Android · August 2026

---

## 🗺️ Roadmap

- 🔔 Scheduled spaced-repetition reminders with WorkManager
- 📡 Nearby / Bluetooth study pack sharing
- ☁️ Optional sync and an online community (the current Community is local only)
- 🌙 Dark mode

<div align="center">

Made with 💙 by Group 6

</div>
