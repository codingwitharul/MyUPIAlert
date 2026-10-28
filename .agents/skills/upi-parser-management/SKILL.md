---
name: upi-parser-management
description: >-
  Use this skill when adding or updating UPI app package names, banking SMS regex patterns,
  sender name extraction heuristics, credit/debit classification rules, or writing parser unit tests.
---

# UPI Parser Management Skill

This skill guides the agent in safely modifying and validating the SMS and notification parsing engine in MyUPIAlert (Kotlin edition).

## Target Files
- `app/src/main/java/com/example/upipaymentalert/smsparser/SmsParser.kt`: Core extraction regexes and classification heuristics.
- `app/src/main/java/com/example/upipaymentalert/NotificationListener.kt`: `RECOGNIZED_UPI_PACKAGES` array and intent filtering.
- `app/src/main/AndroidManifest.xml`: `<queries>` package visibility entries for Android 11+.
- `app/src/main/java/com/example/upipaymentalert/PaymentAdapter.kt`: App icon and friendly label resolution.
- `app/src/test/java/com/example/upipaymentalert/smsparser/SmsParserTest.kt`: Unit tests.

---

## Workflow: Adding Support for a New UPI App or Bank

1. **Register the Package in AndroidManifest.xml**:
   Ensure Android 11+ package visibility query includes the new package under `<queries>`:
   ```xml
   <package android:name="com.example.bankapp" />
   ```

2. **Add to Recognized App Array in NotificationListener.kt**:
   Append the package string to `RECOGNIZED_UPI_PACKAGES`:
   ```kotlin
   val RECOGNIZED_UPI_PACKAGES = arrayOf(
       // ... existing packages ...
       "com.example.bankapp"
   )
   ```

3. **Map App Name and Icon in PaymentAdapter.kt**:
   In `PaymentAdapter.onBindViewHolder`:
   ```kotlin
   senderLabel.contains("com.example.bankapp") -> {
       senderLabel = "Example Bank"
       packageNameToExtractIcon = "com.example.bankapp"
   }
   ```

---

## Workflow: Updating Regex Patterns in SmsParser.kt

### 1. Currency & Amount Extraction
The Indian numbering format (`1,00,000` / `5,000.50`) uses:
```kotlin
val indianAmountGroup = "(?:\\d{1,3}(?:,\\d{2,3})+|\\d+)(?:\\.\\d{1,2})?"
```
When adjusting `extractAmount`:
- Ensure matches strip commas: `.replace(",", "")`.
- Ensure boundary assertions avoid interpreting phone numbers, OTPs, or dates as amounts.

### 2. Sender Name Extraction
In `getSenderName(body: String?)`:
- Check existing patterns (`paid by`, `from`, `has paid`, VPA regex).
- Clean extracted strings with `cleanName()` and capitalize with `capitalizeName()`.
- Validate against the rejection list in `isValidName(name: String?)` to ensure words like `bank`, `account`, `transferred`, `credited` are rejected.

### 3. Credit vs Debit Classification
- `isCreditTransaction()` checks if `creditIdx < debitIdx`.
- `isDebitTransaction()` checks if `debitIdx < creditIdx`.
- If new keywords are needed (e.g. regional language verbs), add them to `indexOfAny(...)`.
- Non-transactional messages (OTPs, balance reminders) must be rejected via `hasBlacklistedKeywords()`.

---

## Verification & Testing

Every change to `SmsParser` MUST be validated by running unit tests:
```bash
./gradlew test
```
Ensure new test cases are added to `app/src/test/java/com/example/upipaymentalert/smsparser/SmsParserTest.kt` covering:
- Standard format: `Rs 500 received from John`
- Indian comma notation: `₹1,50,000.00 credited`
- Edge case: Non-credit message containing the word "credit card bill due" (must return false).
