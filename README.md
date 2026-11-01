<p align="center">
  <img src="docs/logo.png" width="140" alt="Expenso logo" />
</p>

<h1 align="center">Expenso</h1>

<p align="center">
  <b>A local-first personal expense tracker for Android.</b><br>
  Track expenses, budgets, bills and loans. Everything stays on your device &mdash; no account, no server, no cloud.
</p>

## Features

- **Home** — monthly summary, cumulative budget trend chart, weekly spending, upcoming bills
- **Expenses** — add, edit and delete income/expense transactions with categories and notes
- **Analytics** — category breakdown, monthly trend and budget progress
- **Bills & Loans (EMI)** — recurring bills, calendar view, loan schedule tracking
- **Settings** — profile name + avatar, currency, theme, category management, app lock
- Light and dark themes, Material 3, fully offline

## Tech stack

| Layer | Used |
|-------|------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (`ViewModel` + `StateFlow`) |
| Local storage | Room (SQLite) + DataStore Preferences |
| Navigation | Navigation Compose |
| Build | Gradle KTS (Kotlin DSL) |

## Project layout

```
app/src/main/java/com/expenso/app/
├── data/           Room entities, DAOs, repositories, finance math
└── ui/
    ├── home/       Home screen + view model
    ├── expenses/   Transaction list and editor
    ├── analytics/  Charts and category breakdown
    ├── emi/        Bills, calendar, loans
    ├── settings/   Profile, budgets, categories
    ├── components/ Shared Compose widgets and charts
    ├── theme/      Colours, typography, Material theme
    └── navigation/ App navigation graph
```
## Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/192a042b-a936-47ba-8d5d-d560a21d143d" width="118" alt="Home screen" />
  <img src="https://github.com/user-attachments/assets/58e9b24f-334e-475f-999f-51e42c78ec1f" width="118" alt="Expenses screen" />
  <img src="https://github.com/user-attachments/assets/389b6453-561a-4425-8f65-1eb093cc584f" width="118" alt="Analytics screen" />
  <img src="https://github.com/user-attachments/assets/63a32b28-3ae2-4c8c-a82b-27ce6ad9f0eb" width="118" alt="Bills and EMI screen" />
  <img src="https://github.com/user-attachments/assets/528000ec-8054-4745-8033-ee6dbf08b1f2" width="118" alt="Settings screen" />
  <img src="https://github.com/user-attachments/assets/7b31dae3-6435-494d-9140-e520c3be30cd" width="118" alt="App lock screen" />
</p>

<p align="center"><sub>Home · Expenses · Analytics · Bills &amp; EMI · Settings · App lock</sub></p>

## Build & run

Requirements: JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).

1. Point Gradle at your SDK by creating `local.properties` in the project root:
   ```
   sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```
2. Build a normal debug APK:
   ```
   ./gradlew assembleDebug
   ```
3. Build the named "main version" APK (project-specific task):
   ```
   ./gradlew assembleMain
   ```
   Output: `app/build/outputs/apk/main/Expenso-<version>-main.apk`

On Windows use `gradlew.bat` instead of `./gradlew`.

> APKs are signed with the local Android **debug** key, so they install easily for personal use but cannot be published to Google Play as-is.

## Versioning

Every fix or feature bumps the patch version (`appVersionName` and `appVersionCode` in `app/build.gradle.kts`) and ships a new `Expenso-<version>-main.apk`. See [VERSIONING.md](VERSIONING.md) for the full history.

Current version: **1.0.8** (versionCode 9).

## License

Personal project — no license file yet. Add one before accepting outside contributions.
