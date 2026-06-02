# Google Play — Compliance Package (DateCountdown)

Internal reference for filling in the **App Content** section of the Google Play Console:
Privacy policy URL, Data Safety form, IARC content-rating questionnaire, and a verification
checklist of the technical claims. This document is the source of truth for the manual form
entry — answer the Console exactly as written here.

Covers EPIC 8 issue #68 (privacy policy + content rating + compliance). Out of scope: app
signing/keystore (#…/8.2), store listing & screenshots (8.6), app icon (8.7), AAB upload.

---

## 0. Privacy policy URL — single source of truth

```
https://kirich1409.github.io/DateCountdown/privacy-policy.html
```

- Hosted from GitHub Pages, source = branch `main`, folder `/docs`
  (`docs/privacy-policy.html`, `docs/.nojekyll` disables Jekyll so the raw HTML is served as-is).
- This exact URL goes into **two** places and must match byte-for-byte:
  1. Play Console → App content → **Privacy policy**.
  2. The in-app "Privacy Policy" menu item (declared once as a string resource/constant in
     `:app`; the feature module reads that single constant, never a duplicated literal).
- Before wiring the URL into the app, confirm it returns **HTTP 200** (GitHub Pages has a few
  minutes of deploy lag; a fresh URL may 404 first): `curl -I <URL>`.

---

## 1. Data Safety form

**Headline answer: the app does NOT collect or share any user data.**

Justification (all verifiable, see §3):
- No `INTERNET` permission → the app makes no network requests of its own.
- All fonts/icons are bundled in the APK → nothing is downloaded at runtime.
- All event data lives in a local Room database + DataStore preferences on the device.
- `android:allowBackup="false"` → data is not copied to Google's backup cloud.
- Notifications are local (`AlarmManager` + `BroadcastReceiver`), not push/server-driven.

### Console answers (Data safety → Data collection and security)

| Console question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data encrypted in transit? | Not applicable — no data is transmitted. (If the form forces a choice, select "No data is transmitted.") |
| Do you provide a way for users to request that their data be deleted? | **Yes** — users delete an event in-app or uninstall the app (removes the entire local DB + preferences). State this in the deletion-method free-text if prompted. |
| Data types collected (Location, Personal info, Financial, Messages, Photos, Contacts, App activity, Device IDs, etc.) | **None selected.** Event title/date/color/icon stay on-device and are never collected or transmitted, so no data type is declared as collected. |

> Note: "data collected" in the Data Safety sense means data sent off the device. On-device-only
> storage is **not** "collection". Event title, date/time, color, and icon are stored locally only,
> therefore nothing is declared.

---

## 2. IARC content-rating questionnaire

Expected outcome: **"Everyone" / lowest age rating** in all regions (ESRB Everyone, PEGI 3, etc.).

Standard questionnaire answers (a utility countdown app — no objectionable content):

| Category | Answer |
|---|---|
| Violence (cartoon/realistic/blood/gore) | No |
| Sexual content / nudity | No |
| Profanity / crude humor | No |
| Controlled substances (alcohol, tobacco, drugs) | No |
| Gambling (simulated or real) | No |
| Fear / horror content | No |
| Discrimination / hate content | No |
| User-generated content shared between users | No — events are private, on-device only; no sharing, no social features |
| User communication / chat | No |
| Shares user location | No |
| Allows purchase of digital goods (IAP) | No |
| Contains ads | No |
| Shares personal information with third parties | No |

Category for store listing: **Tools / Productivity** (not a game).

---

## 3. Verification checklist (technical claims)

All verified against the repository at the time of writing (issue #68). No code changes were
required for compliance itself; the only product change is the in-app privacy-policy link.

| Claim | Status | Evidence |
|---|---|---|
| `targetSdk = 36` (meets Play 2026 requirement) | ✓ | `build-logic/convention/src/main/kotlin/datecountdown.android.application.gradle.kts:15` |
| `compileSdk = 36`, `minSdk = 29` | ✓ | `build-logic/convention/src/main/kotlin/com/datecountdown/buildlogic/KotlinAndroid.kt:19-20` |
| No foreground service (`foregroundServiceType` N/A) | ✓ | `app/src/main/AndroidManifest.xml` declares only two `<receiver>` elements (`AlarmReceiver`, `BootReceiver`); no `<service>`, no `FOREGROUND_SERVICE*` permission. Reminders use `AlarmManager.setExactAndAllowWhileIdle` + `BroadcastReceiver`. |
| `INTERNET` permission NOT declared | ✓ | `app/src/main/AndroidManifest.xml` — only the 4 permissions below are present. |
| `allowBackup="false"` | ✓ | `app/src/main/AndroidManifest.xml:18` |
| Fonts/icons bundled (no runtime download) | ✓ | `:core:design` `res/font` (Roboto Flex + Material Symbols Rounded); rendered via local codepoints. |

### Declared permissions — audit (exactly 4)

The manifest declares **4** permissions. Privacy policy, Data Safety, and this checklist all name
the same 4 with the same purposes — they must stay consistent, because a reviewer compares the
policy against the manifest.

| Permission | Purpose | Manifest line |
|---|---|---|
| `POST_NOTIFICATIONS` | Local event reminders (Android 13+ runtime permission) | `AndroidManifest.xml:14` |
| `USE_EXACT_ALARM` | Exact-time reminders; exempt path for alarm/reminder apps | `AndroidManifest.xml:7` |
| `SCHEDULE_EXACT_ALARM` | Exact-alarm fallback for API 31–32 | `AndroidManifest.xml:8` |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule reminders after device reboot | `AndroidManifest.xml:10` |

> The earlier spec (8.3) named 2 permissions and the research checklist named 3; the **manifest is
> authoritative — there are 4**. This document reflects the manifest.

---

## 4. Consistency guarantee

Privacy policy ↔ Data Safety ↔ AndroidManifest must agree:

- **Same 4 permissions, same purposes** in all three. ✓ (§3 table mirrors `docs/privacy-policy.html` §5.)
- **"No network" wording is precise and non-contradictory**: the app declares no `INTERNET`
  permission and makes no network requests; the only outbound action is handing the privacy-policy
  link to the device's external browser via `ACTION_VIEW` — which does not require the app to hold
  `INTERNET`. Stated identically in the policy (§3) and here.
- **Single privacy-policy URL** (§0) used by both the Console field and the in-app link.

---

## 5. Manual Console steps (performed by the maintainer)

1. Enable GitHub Pages: Settings → Pages → Source = `Deploy from a branch`, branch `main`, folder `/docs`.
   (Or `gh api -X POST repos/kirich1409/DateCountdown/pages -f 'source[branch]=main' -f 'source[path]=/docs'`.)
2. Wait for deploy, confirm `curl -I <URL>` → HTTP 200.
3. Play Console → App content → Privacy policy → paste the URL from §0.
4. App content → Data safety → fill per §1.
5. App content → Content rating (IARC questionnaire) → answer per §2.
