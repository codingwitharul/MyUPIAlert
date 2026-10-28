# MyUPIAlert AI Coding Instructions & Rules

These guidelines apply to all AI agents, assistant models, and automated tools working on the **MyUPIAlert** Android codebase.

## Project Structure Quick Reference
- **Repository Path**: `MyUPIAlert/`
- **Application Package**: `com.codingwitharul.myupialert`
- **Android Target**: minSdk 24 (Android 7.0), targetSdk 34 (Android 14), compileSdk 34 / Java 17
- **Language & Build**: 100% Kotlin, Gradle 9+, Gradle Kotlin DSL (`build.gradle.kts`), Kotlin Symbol Processing (KSP)
- **Architecture**:
  - `ForegroundTtsService.kt`: Core foreground voice synthesis engine + hardware loudness boost.
  - `NotificationListener.kt`: Intercepts UPI app notifications (`NotificationListenerService`).
  - `SmsListener.kt`: Intercepts transactional banking SMS (`BroadcastReceiver`).
  - `SmsParser.kt`: Regex-based amount extraction, sender name identification, and credit classifier.
  - `AnnouncementPhrases.kt`: 13 Indian language announcement phrase builder.
  - `AppDatabase.kt` / `TransactionDao.kt`: Local SQLite persistence via Jetpack Room (KSP).
  - `MainActivity.kt`: Control dashboard, filters, settings drawer, permission diagnostics.

## Strict Rules & Constraints
1. **100% Kotlin Idioms**:
   - The entire codebase is written in Kotlin. Do not introduce raw Java files.
   - Use coroutines, data classes, extension functions, and modern Kotlin idioms.
2. **Zero External Data Transmission**:
   - Do NOT add network clients (Retrofit, OkHttp, Volley) that transmit user transaction data, notifications, or SMS bodies to remote servers.
   - Do NOT add analytics SDKs. The app must remain 100% offline and private.
3. **Segmented TTS Strings**:
   - All spoken announcement templates MUST use the pipe delimiter `|` around the amount: e.g. `Received |%1$s| rupees`.
   - `ForegroundTtsService.kt` depends on this format to slow down amount articulation while keeping boilerplate sentence structures fluent.
4. **Audio Volume Invariants**:
   - Any modifications to the `AudioManager` stream volume must be safely restored when speaking completes or fails via `UtteranceProgressListener`.
   - Do not leave `LoudnessEnhancer` permanently enabled after speech completes.
5. **Localization Coverage**:
   - If UI strings or TTS phrases are changed, ensure changes are mirrored across all 13 supported Indian languages: `en`, `hi`, `gu`, `mr`, `ta`, `te`, `kn`, `ml`, `pa`, `bn`, `or`, `as`, `ur`.
6. **Testing**:
   - When modifying `SmsParser.kt`, always add or update unit tests in `SmsParserTest.kt`.
   - Validate regex changes against both Indian numbering format (`1,50,000.00`) and standard format (`1000.00`).

## Documentation & Skills
- Detailed architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Extension recipes: [docs/AI_DEVELOPER_GUIDE.md](docs/AI_DEVELOPER_GUIDE.md)
- Workflow skills available in `.agents/skills/`:
  - `upi-parser-management`: Managing UPI apps, regex patterns, and parser test suites.
  - `tts-audio-pipeline`: Managing voice models, loudness enhancer, and speech rate.
  - `background-longevity`: Managing foreground services, battery optimizations, and auto-start.
  - `transaction-storage-export`: Managing Room schema, filters, and CSV export.
