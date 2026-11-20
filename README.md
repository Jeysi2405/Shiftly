# Shiftly

> Track every earned peso, work shift, and daily expense—calm, private, and offline.

[![Version](https://img.shields.io/badge/Version-3.2.0-0A84FF?style=for-the-badge)](../../releases/latest)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-30D158?style=for-the-badge&logo=android&logoColor=white)](./release/Shiftly-v3.2.0.apk)
[![Download APK](https://img.shields.io/badge/Download-Shiftly%20v3.2.0%20APK-1C1C1E?style=for-the-badge)](./release/Shiftly-v3.2.0.apk)

---

## Download Shiftly v3.2.0 (APK)

- **Direct APK Download:** [Download Shiftly v3.2.0 APK](./release/Shiftly-v3.2.0.apk)
- **GitHub Releases:** [Release v3.2.0](../../releases/latest)

> **Quick Install:** Download `Shiftly-v3.2.0.apk` on your Android phone and open it to install. *(If prompted, allow installing unknown apps for your browser or file manager.)*
>
> **Updating Existing App:** Already have Shiftly installed? Simply download and open `Shiftly-v3.2.0.apk` and tap **Update**—all your shifts, expenses, and records will stay intact. Do not uninstall the app before updating.

---

## What's Inside Shiftly

1. **Shifts & Work Pay**
   Easy shift logging with saved presets, automatic pay computation (regular hours, night differential, overtime, and holiday rates), upcoming shift reminders, and monthly work goals.

2. **Daily Spending & Budgets**
   Simple expense tracking with receipt photos, editable expense history, flexible monthly and category budgets, and a calm daily safe-to-spend guide.

3. **Allowance & Take-Home Pay**
   Allowance and extra income tracking, paired with a PH Take-Home Pay Estimator covering SSS, PhilHealth, Pag-IBIG, and withholding tax estimates.

4. **Shared Bills, Loans & Savings**
   Fair group bill splitting, a Personal IOUs (Borrow & Lend) tracker, an **Installment Loans (Contract Tracker)** for financed items and payment schedules with one-tap payment logging, recurring bill reminders, and personal savings goals.

5. **Expanded Quick Add (`+`) & Clear Insights**
   Log shifts, expenses, allowances, split bills, recurring bills, installment loans, savings goals, or IOUs in one tap from the center `+` menu—alongside a calm home overview with a privacy balance mask, visual trends, and an activity calendar.

6. **Privacy & Peace of Mind**
   100% offline and local-first storage—no accounts, no cloud tracking. Protected with PIN & Biometric App Lock, plus full JSON backup/restore and CSV spreadsheet export.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3 with an iOS-inspired Dark & Light design system)
- **Local Storage:** Room Database & Preferences DataStore
- **Async & Architecture:** Kotlin Coroutines, StateFlow, MVVM + Repository pattern

---

## Build from Source

```bash
./gradlew :app:assembleDebug
```

Requires Android Studio (Ladybug / Otter or newer) and JDK 17+ (`minSdk 26`, `targetSdk 37`).

---

## Developer Contact

Questions, feature ideas, or bug reports? Reach out at **[jameschristian.huelar24@gmail.com](mailto:jameschristian.huelar24@gmail.com)**.
