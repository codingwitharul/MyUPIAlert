# MyUPIAlert System Architecture & Technical Specifications

> **Native Android Voice Soundbox Engine for UPI & Transactional Banking**  
> *100% Kotlin • Gradle 9+ & Kotlin DSL • Zero hardware rental • Universal payment app support • 100% On-Device Privacy*

---

## 1. Executive Architecture Summary

Commercial voice soundboxes (such as Paytm Soundbox, PhonePe SmartSpeaker, and Google Pay Soundpod) are dedicated cellular IoT devices charging recurring monthly rentals (₹125 – ₹200/mo) and locking merchants into a single acquiring network.

**MyUPIAlert** transforms any standard Android device (Android 7.0 / API 24 to Android 15 / API 35) into an unkillable, enterprise-grade, multi-tenant voice soundbox. The codebase is written **100% in Kotlin** and built with **Gradle 9+** and **Kotlin Symbol Processing (KSP)**. All parsing, text-to-speech synthesis, acoustic hardware amplification, and transaction logging happen **entirely offline** on the local device.

```
                      ┌──────────────────────────────────────────────┐
                      │             INCOMING PAYMENT ALERTS          │
                      └──────────────────────┬───────────────────────┘
                                             │
                    ┌────────────────────────┴────────────────────────┐
                    ▼                                                 ▼
     ┌─────────────────────────────┐                   ┌─────────────────────────────┐
     │ NotificationListenerService │                   │  SmsListener (Priority 1000)│
     │  - 28+ UPI Package Filters  │                   │  - Telephony SMS Broadcast  │
     │  - Intent upi://pay dynamic │                   │  - PDU Reassembly Engine    │
     └──────────────┬──────────────┘                   └──────────────┬──────────────┘
                    │                                                 │
                    └────────────────────────┬────────────────────────┘
                                             │ (Message Body / Package)
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │              SmsParser Engine                │
                      │  - Indian Comma-Aware Currency Regex         │
                      │  - Multi-syntax Sender Name Extraction       │
                      │  - Relative Index Credit vs Debit Classifier │
                      │  - Marketing / OTP Blacklist Filters         │
                      └──────┬───────────────────────────────┬───────┘
                             │                               │
        (Valid Transaction)  │                               │ (Credit Announceable)
                             ▼                               ▼
       ┌───────────────────────────────┐   ┌─────────────────────────────────────────┐
       │   Room Database (SQLite / KSP)│   │   ForegroundTtsService                  │
       │   - PaymentTransaction entity │   │   - 60s Sliding-Window Deduplication    │
       │   - AppDatabase (v1)          │   │   - Google Speech Engine Binding        │
       │   - TransactionDao            │   │   - 13 Indian Language Locales          │
       └──────────────┬────────────────┘   │   - Segmented Speech Rate Splitter (|)  │
                      │                    │   - AudioManager Volume Override        │
                      ▼                    │   - LoudnessEnhancer (+1500 mB Boost)   │
       ┌───────────────────────────────┐   └─────────────────────────────────────────┘
       │   MainActivity UI             │
       │   - RecyclerView Dashboard    │
       │   - Date & App Filters        │
       │   - HTML / CSV Exporter       │
       │   - Dynamic App Icon Cache    │
       └───────────────────────────────┘
```

---

## 2. Ingestion Pipelines

MyUPIAlert operates two simultaneous ingestion pipelines to guarantee zero missed payments:

### 2.1. Push Notification Ingestion (`NotificationListener.kt`)
- **Service Registration**: Subclasses Android's `NotificationListenerService` requiring `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
- **Target Filtering**:
  1. **Static List**: Evaluates package against `RECOGNIZED_UPI_PACKAGES` (28+ major UPI merchant and user apps, including PhonePe, Paytm, Google Pay, BHIM, Cred, Amazon Pay, FamPay, BharatPe, SBI, ICICI, HDFC, Axis, Kotak, Bank of Baroda, etc.).
  2. **Dynamic Querying**: Queries the Android `PackageManager` for apps responding to `Intent.ACTION_VIEW` with `upi://pay` URI scheme to automatically capture emerging regional UPI apps.
- **Payload Extraction**: Reconstructs payment text from `notification.extras.getCharSequence(Notification.EXTRA_TITLE)` and `Notification.EXTRA_TEXT`.

### 2.2. SMS Ingestion (`SmsListener.kt`)
- **Receiver Registration**: Registered with maximum priority (`android:priority="1000"`) listening for `android.provider.Telephony.SMS_RECEIVED`.
- **PDU Assembly**: Handles single and multi-part SMS PDUs using `Telephony.Sms.Intents.getMessagesFromIntent(intent)`.
- **Sender Address**: Preserves the originating bank shortcode/header (e.g., `VK-HDFCBK`, `AX-SBIUPI`).

---

## 3. Parsing & Heuristics Engine (`SmsParser.kt`)

Banking transactional alerts in India lack a single standard schema. The `SmsParser` uses a robust four-stage pipeline:

### 3.1. Amount Extraction Regex
Indian numbering groups currency as lakhs and crores (`1,00,000.00` rather than `100,000.00`).
The parser uses a specialized comma-aware pattern:
```regex
(?:\d{1,3}(?:,\d{2,3})+|\d+)(?:\.\d{1,2})?
```
Evaluation order:
1. **Leading Decimal Pattern (`m0`)**: `(?:[₹]|rs\.?|inr|mrp)[\s]*(\.\d{1,2})` (e.g. `₹.50`).
2. **Prefix Currency Symbol (`m1`)**: `(?:[₹]|rs\.?|inr|mrp)[\s]*(amount)` (e.g. `Rs 500.00`, `₹1,50,000`).
3. **Suffix Currency Symbol (`m2`)**: `(amount)\s*(?:rs\.?|inr|[₹])` (e.g. `500.00 Rs`).
4. **Bare Numeric Fallback (`m3`)**: Digits bounded by `1 <= val <= 9,999,999` with negative lookbehind against `@` (preventing UPI ID numbers).

### 3.2. Credit vs. Debit Relative Index Classifier
SMS messages frequently mention both words (e.g. *"Rs 500 credited to account ending 1234. Available balance debited for previous charge..."*).
- **Blacklist Filter**: Discards non-transactional messages containing `due`, `recharge`, `expiring`, `plan`, `loan`, `otp`, `code`, `validity`, `data`, `pack`.
- **Banking Indicators**: Requires banking tokens (`a/c`, `acct`, `account`, `bank`, `upi`, `vpa`, `inr`, `rs`, `₹`).
- **Index Comparison**: Calculates `indexOfAny(...)` for credit tokens vs debit tokens. The earliest occurring verb determines whether the transaction is an incoming credit or outgoing debit.

### 3.3. Sender Name Resolution
Evaluates 8 distinct regex patterns to capture the sender:
1. `(?:paid|sent|transferred)\s+by\s+([A-Z][a-zA-Z\s]{1,28}?)`
2. `from\s+([A-Z][a-zA-Z\s]{1,28}?)`
3. `([A-Z][a-zA-Z\s]{1,28}?)\s+has\s+(?:paid|sent|transferred|credited)`
4. `(?:from\s+VPA|from\s+UPI\s+ID|VPA)[:\s]+([a-zA-Z0-9._-]+)@...`
5. VPA bank handle mapping (`@ybl`, `@paytm`, `@okicici`, `@oksbi`, etc.)
6. IMPS / NEFT transfer headers (`trf frm`, `imps frm`, `neft frm`)
7. `received\s+from\s+([A-Z][a-zA-Z\s]{1,28}?)`
8. Generic fallback with token rejection against common non-name words (e.g. *bank, upi, customer, service, ltd*).

---

## 4. Acoustic & Voice Synthesis Pipeline (`ForegroundTtsService.kt`)

```
                 Incoming Alert Request (EXTRA_TEXT)
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │  Deduplication Check      │
                    │  60s sliding window       │
                    └─────────────┬─────────────┘
                                  │ (Not duplicate)
                                  ▼
                    ┌───────────────────────────┐
                    │  Locale Resolution        │
                    │  13 Indian Locales        │
                    └─────────────┬─────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │  Text Segmenter (|)       │
                    │  - Even index: 0.9x speed │
                    │  - Odd index: Custom speed│
                    └─────────────┬─────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │  AudioManager Override    │
                    │  Stream Music takeover    │
                    └─────────────┬─────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │  LoudnessEnhancer (+1500) │
                    │  AudioSessionId boost     │
                    └─────────────┬─────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │  UtteranceProgressListener│
                    │  Restores original volume │
                    └───────────────────────────┘
```

### 4.1. Sliding-Window Deduplication
When a merchant receives a payment, both the bank SMS and the payment app push notification arrive within 1–15 seconds.
- `recentAnnouncements`: `ConcurrentHashMap<String, Long>`
- `DEDUPLICATION_WINDOW_MS = 60000` (60 seconds)
- If an exact text announcement was spoken within the last 60 seconds, it is silently dropped, eliminating echoing.

### 4.2. Segmented Speech Speed
Merchants need payment amounts articulated at custom clarity, while boilerplate phrases ("Received payment of") can remain standard.
- Announcement phrases are formatted with pipe delimiters: e.g., `Received |%1$s| rupees from %2$s`.
- `ForegroundTtsService.speak()` splits by `|`:
  - **Even indices** (sentence structure): spoken at fixed 0.9x speed.
  - **Odd indices** (monetary amount): spoken at user-selected `customSpeed - 0.1x`.

### 4.3. Acoustic Loudness Boost (+1500 mB)
Standard Android media volume is inadequate in noisy Indian markets.
- Obtains an audio session ID via `AudioManager.generateAudioSessionId()`.
- When volume slider is between 101% and 200%:
  - The excess percentage `boostAmount = volumePercent - 100` maps to target acoustic gain:
    $$\text{targetGainMb} = \min(1500, \text{boostAmount} \times 15)$$
  - Attaches `android.media.audiofx.LoudnessEnhancer(audioSessionId)` with `setTargetGain(targetGainMb)`.
- When playback ends or errors out, `UtteranceProgressListener.onDone()` restores the system volume to its original state and disables the enhancer.

---

## 5. Storage & Persistence (`AppDatabase.kt`)

The local persistence layer uses Android Jetpack Room processed with **KSP**:
- **Entity**: `PaymentTransaction` (`transactions` table)
  - `id`: String Primary Key (UUID + timestamp)
  - `amount`: String? (normalized decimal amount)
  - `type`: String? (`"credit"` or `"debit"`)
  - `sender`: String? (Package name or originating phone number)
  - `body`: String? (Full raw notification or SMS text)
  - `timestamp`: Long (Epoch millisecond timestamp)
- **DAO Queries (`TransactionDao.kt`)**:
  - `getAllTransactions(): List<PaymentTransaction>`
  - `getTransactionsByDate(start, end): List<PaymentTransaction>`
  - `getTransactionsByApp(appName): List<PaymentTransaction>`
  - `getTransactionsByAppAndDate(appName, start, end): List<PaymentTransaction>`
  - `deleteAll()`
- **Exporting**: `MainActivity.kt` formats queried transactions into an HTML / CSV statement and writes to public Downloads via MediaStore or FileProvider.

---

## 6. OEM Background Longevity & Auto-Start

To survive aggressive OEM task killers (Xiaomi MIUI, Oppo ColorOS, Vivo FuntouchOS, Samsung One UI, Huawei EMUI):
1. **Foreground Service**: `ForegroundTtsService` runs with ongoing persistent notification and `android:foregroundServiceType="specialUse"`.
2. **Battery Optimization Bypass**: Requests `android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for `PowerManager.isIgnoringBatteryOptimizations()`.
3. **Hardware Auto-Start Intent Resolver (`AutoStartHelper.kt`)**: Inspects `Build.MANUFACTURER` and launches device-specific background whitelist activities across Xiaomi, Oppo, Realme, OnePlus, Vivo, iQOO, Huawei, Asus, LeTV, and Samsung.

---

## 7. Supported Indian Languages (13 Matrix)

| Language | Code | Spoken Format Template |
| :--- | :---: | :--- |
| **English** | `en` | `Received \|%1$s\| rupees from %2$s` |
| **Hindi** | `hi` | `%1$s से \|%2$s\| रुपये प्राप्त हुए` |
| **Tamil** | `ta` | `%1$s இடமிருந்து \|%2$s\| ரூபாய் பெறப்பட்டது` |
| **Telugu** | `te` | `%1$s నుండి \|%2$s\| రూపాయలు అందాయి` |
| **Kannada** | `kn` | `%1$s ಅವರಿಂದ \|%2$s\| ರೂಪಾಯಿ ಜಮೆಯಾಗಿದೆ` |
| **Malayalam** | `ml` | `%1$s ൽ നിന്ന് \|%2$s\| രൂപ ലഭിച്ചു` |
| **Gujarati** | `gu` | `%1$s તરફથી \|%2$s\| રૂપિયા મળ્યા` |
| **Punjabi** | `pa` | `%1$s ਤੋਂ \|%2$s\| ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ` |
| **Bengali** | `bn` | `%1$s থেকে \|%2$s\| টাকা পাওয়া গেছে` |
| **Marathi** | `mr` | `%1$s कडून \|%2$s\| रुपये मिळाले` |
| **Odia** | `or` | `%1$s ଠାରୁ \|%2$s\| ଟଙ୍କା ମିଳିଲା` |
| **Assamese** | `as` | `%1$s ৰ পৰা \|%2$s\| টকা পোৱা গল` |
| **Urdu** | `ur` | `%1$s سے \|%2$s\| روپے موصول ہوئے` |
