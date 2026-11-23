# Shiftly

> Track every earned peso, work shift, and daily expense—calm, private, and offline.

[![Version](https://img.shields.io/badge/Version-3.9.0-0A84FF?style=for-the-badge)](../../releases/latest)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-30D158?style=for-the-badge&logo=android&logoColor=white)](./release/Shiftly%203.9.0.apk)
[![Download APK](https://img.shields.io/badge/Download-Shiftly%203.9.0%20APK-1C1C1E?style=for-the-badge)](./release/Shiftly%203.9.0.apk)

---

## Download Shiftly 3.9.0 (APK)

- **Direct APK Download:** [Download Shiftly 3.9.0 APK](./release/Shiftly%203.9.0.apk)
- **GitHub Releases:** [Release v3.9.0](../../releases/latest)

> **Quick Install:** Download `Shiftly 3.9.0.apk` on your Android phone and open it to install. *(If prompted, allow installing unknown apps for your browser or file manager.)*

---

### Upgrading to v3.9.0

> - **From v3.3.0 or newer:** Install `Shiftly 3.9.0.apk` directly and tap **Update**—your data upgrades automatically (`Room DB v9`).
> - **From v3.2.0 or earlier:** Back up via **Profile → Backup → Export JSON**, uninstall the older build, install `Shiftly 3.9.0.apk`, and restore via **Restore Backup**.

---

## What's Inside Shiftly 3.9.0

1. **Shifts & Work Pay**
   Tap **Log Shift** or any calendar date with saved time presets, automatic pay math (`Regular Day`, `Rest Day`, and Holiday acronyms like `SNW Day` & `RH + Rest`, plus overtime and night differential), upcoming shift reminders, monthly goals, and swipe-to-edit/delete cards.

2. **Daily Spending & Monthly Limits**
   Track daily expenses with receipt photos, `< MMM yyyy >` + `All` month and category filters, swipe-to-edit/delete with quick Undo, flexible monthly limits, category caps, and a calm daily safe-to-spend guide.

3. **Allowance & Take-Home Pay**
   Track allowances, stipends, and extra income against semi-monthly, weekly, or custom payday cutoffs, paired with a PH Take-Home Pay Estimator covering SSS, PhilHealth, Pag-IBIG, and withholding tax.

4. **Shared Bills, Loans & Savings**
   Split group bills cleanly, track Personal IOUs offline, manage **Installment Loans** with `Monthly %` or `Total %` interest rates and a custom **Record Payment** editor (Amount, Date & Time Paid), auto-post Recurring Bills to Expenses, and save toward Goals with daily pace and confetti celebrations.

5. **Refined UI, Floating Nav, Quick Add (`+`) & Guided Tour**
   Built with a 4-tab expanding-pill floating bottom bar, a detached upper-right `+` button that morphs into a floating Quick Add modal (`Log Shift`, `Add Expense`, `Allowance`, `Split Bill`, `Recurring`, `Loan`, `Goal`, `IOU`), native Compose animated Lucide icons with Tools spotlight transitions, 1:1 loading skeleton shimmers, a dark `#090A0D` Welcome + intro sequence, and updated interactive tutorials & `?` screen guides.

6. **Privacy & Peace of Mind**
   100% offline and local-first storage—no accounts, no cloud tracking. Features a clean outline-free Shiftly app mark, borderless PIN keypad & Biometric App Lock with smooth logo zoom unlock, plus full JSON backup/restore and CSV spreadsheet export.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3 with an iOS-inspired Dark & Light design system)
- **Local Storage:** Room Database & Preferences DataStore
- **Async & Architecture:** Kotlin Coroutines, StateFlow, MVVM + Repository pattern

---

## Build from Source

```bash
./gradlew :app:assembleRelease
```

Requires Android Studio (Ladybug / Otter or newer) and JDK 17+ (`minSdk 26`, `targetSdk 37`).

---

## Developer Contact

Questions, feature ideas, or bug reports? Reach out at **[jameschristian.huelar24@gmail.com](mailto:jameschristian.huelar24@gmail.com)**.
