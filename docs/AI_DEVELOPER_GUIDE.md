# AI Developer Guide & Engineering Playbook — MyUPIAlert

This guide is designed for AI coding agents and human contributors working on the **MyUPIAlert** codebase. It outlines code invariants, step-by-step recipes for common extensions, and strict safety guidelines.

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
