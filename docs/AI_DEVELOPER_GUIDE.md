# AI Developer Guide & Engineering Playbook — MyUPIAlert

## 🌟 Why MyUPIAlert Was Created (The Origin Story)
In India, digital payments (UPI) are ubiquitous, and local merchants, shopkeepers, and street vendors rely heavily on them. To verify payments without constantly checking their phones, many subscribe to commercial hardware "soundboxes" (like Paytm Soundbox or PhonePe SmartSpeaker). 

However, these commercial soundboxes present significant issues for small business owners:
- **Cost**: They charge recurring monthly rentals of ₹125 to ₹200.
- **Lock-in**: They lock merchants into a single payment ecosystem.
- **Hardware Limitations**: They require dedicated cellular SIM cards, physical charging, and extra counter space.

**MyUPIAlert was created to democratize this technology.** By utilizing a spare or existing Android phone, any merchant can get the exact same (or better) functionality for **absolutely free**. It intercepts local bank SMS and UPI push notifications securely on the device, translating them into ultra-loud voice announcements in 13 Indian languages, with a completely 100% offline privacy architecture.

## 🎯 Target Audience & Use Cases
- **Small Business Owners & Street Vendors**: Who need instant audio confirmation of payments to prevent fraud in busy environments.
- **Delivery Agents & Drivers**: Who need hands-free payment verification while on the road.
- **Freelancers & Service Professionals**: Seeking a unified dashboard to track payments across Google Pay, PhonePe, Paytm, and direct bank transfers without multiple apps.
- **Visually Impaired Users**: Who require clear, customizable audio feedback for digital transactions.

## ✨ Detailed Features Breakdown
- ⚡ **Dual Engine Listeners**: Actively monitors both `NotificationListenerService` (for UPI App Push notifications) and `BroadcastReceiver` (for SMS) to guarantee a 99.9% catch rate.
- 🛡️ **Cross-Channel Deduplication**: A specialized 60-second window in `ForegroundTtsService` ignores duplicate alerts (e.g., when a user receives a Google Pay notification and an HDFC Bank SMS for the exact same transaction).
- 🔊 **Hardware Loudness Boost (+1500 mB)**: Exploits Android's low-level `LoudnessEnhancer` to push the device speaker beyond its stock limit, ensuring announcements cut through street noise.
- ⏱️ **Segmented Speech Speed**: The amount string is separated via pipe delimiters (`|`). The service reads the structural phrase (e.g., "Received") at a normal cadence but slows down and emphasizes the actual amount (e.g., "500 rupees") for crystal-clear comprehension.
- 📊 **Financial Analytics Dashboard**: Provides an offline Jetpack Compose dashboard with heatmaps, hourly sales distribution, and CSV export capabilities.
- 🔐 **Privacy-First Architecture**: Relies on native Android Text-to-Speech (TTS), SQLite (Room), and local Regex parsers. There is zero cloud telemetry, ensuring financial privacy.

---

## 1. Core Invariants & Architectural Rules

1. **100% Kotlin Idioms & Gradle 9+**:
   - All code is written in Kotlin (`.kt`).
   - Build scripts use Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`) on Gradle 9+.
   - Use KSP (`com.google.devtools.ksp`) for Room compiler; avoid `kapt` or `annotationProcessor`.

2. **100% Offline Privacy Guarantee (NON-NEGOTIABLE)**:
   - Never introduce third-party analytics (Firebase Analytics, Mixpanel, Crashlytics, etc.).
   - Never send user financial data, notifications, or SMS bodies to any external cloud API or remote server.
   - All parsing, NLP classification, speech generation, and database storage must execute locally on the device.

3. **Dual-Pipeline Deduplication**:
   - Both `NotificationListener` and `SmsListener` may process the exact same payment.
   - Deduplication is handled by `ForegroundTtsService.recentAnnouncements` (60-second window).
   - Both events are logged in the database to maintain full audit history, but only one announcement is voiced.

4. **Segmented Speed Notation (`|`)**:
   - All TTS template strings (in `res/values-*/strings_tts.xml` and `AnnouncementPhrases.kt`) must place the amount variable between pipes: `|%1$s|` or `|%2$s|`.
   - `ForegroundTtsService.kt` relies on this delimiter to read amounts at custom clarity and sentence structure at 0.9x speed.

5. **Hardware Audio Integrity**:
   - Any modifications to volume or `LoudnessEnhancer` in `ForegroundTtsService.kt` must be accompanied by restoration logic in `UtteranceProgressListener.onDone()` and `onError()`.
   - Never leave system music volume in a permanently altered state.

6. **Multi-Language Parity (13 Indian Languages)**:
   - When a string is added to `strings.xml`, corresponding entries must be provided for all 13 languages:
     `en`, `hi`, `ta`, `te`, `kn`, `ml`, `gu`, `pa`, `bn`, `mr`, `or`, `as`, `ur`.

---

## 2. Developer Recipes

### Recipe A: Adding Support for a New UPI App or Bank
1. **Declare Query Intent/Package in `AndroidManifest.xml`**:
   Add the app package inside `<queries>`:
   ```xml
   <package android:name="com.example.newbank.upi" />
   ```
2. **Add to Recognized Array in `NotificationListener.kt`**:
   Add to `RECOGNIZED_UPI_PACKAGES`:
   ```kotlin
   "com.example.newbank.upi",
   ```
3. **Add Friendly Display Name & Icon Mapping in `PaymentAdapter.kt`**:
   Add a `when` branch in `onBindViewHolder()`:
   ```kotlin
   senderLabel.contains("com.example.newbank.upi") -> {
       senderLabel = "New Bank UPI"
       packageNameToExtractIcon = "com.example.newbank.upi"
   }
   ```
4. **Test with Sample Notification Payload**:
   Add a unit test in `SmsParserTest.kt` verifying the message body classifies as credit and extracts amount/sender properly.

---

### Recipe B: Updating Regex or Adding New Bank SMS Formats
1. Locate `app/src/main/java/com/example/upipaymentalert/smsparser/SmsParser.kt`.
2. To improve **amount extraction**:
   - Update `extractAmount(body: String?)`.
   - Ensure the Indian comma-aware pattern `(?:\d{1,3}(?:,\d{2,3})+|\d+)(?:\.\d{1,2})?` is maintained.
3. To improve **sender name extraction**:
   - Update `getSenderName(body: String?)`.
   - Ensure `isValidName(name: String?)` rejects unwanted tokens (such as `bank`, `account`, `credited`).
4. To improve **credit/debit detection**:
   - Update `isCreditTransaction(body: String?)` and `isDebitTransaction(body: String?)`.
5. **Always add a test case** in `app/src/test/java/com/codingwitharul/myupialert/smsparser/SmsParserTest.kt`.

---

### Recipe C: Modifying Database Schema (Room with KSP)
1. Edit entity `com.codingwitharul.myupialert.model.PaymentTransaction`.
2. If adding fields, update `AppDatabase.kt`:
   - Increment `version` in `@Database(entities = [PaymentTransaction::class], version = 2, ...)`.
   - Write a migration or verify fallback behavior:
     ```kotlin
     val MIGRATION_1_2 = object : Migration(1, 2) {
         override fun migrate(db: SupportSQLiteDatabase) {
             db.execSQL("ALTER TABLE transactions ADD COLUMN referenceId TEXT")
         }
     }
     ```
3. Update `TransactionDao.kt` if new queries or filters are required.

---

### Recipe D: Adding or Updating TTS Spoken Phrases
1. In `app/src/main/res/values-[lang]/strings_tts.xml`:
   - Keep the `|%1$s|` pipe markers around currency numbers.
2. In `app/src/main/java/com/example/upipaymentalert/smsparser/AnnouncementPhrases.kt`:
   - Update the `buildPhrase(...)` function for the target language to match the string resource.

---

## 3. Build & Test Commands

Run from `MyUPIAlert` subfolder:
```bash
# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```
