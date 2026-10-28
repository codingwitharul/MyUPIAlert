---
name: tts-audio-pipeline
description: >-
  Use this skill when configuring TTS engines, modifying audio volume and LoudnessEnhancer boost,
  adjusting segmented speech speed, or adding new Indian language TTS voices.
---

# TTS Audio Pipeline Skill

This skill guides the agent in working with Text-To-Speech engines, low-level Android audio effects, and segmented multi-language vocal announcements in MyUPIAlert (Kotlin edition).

## Target Files
- `app/src/main/java/com/example/upipaymentalert/ForegroundTtsService.kt`: Foreground service handling speech synthesis, deduplication, volume overrides, and audio effects.
- `app/src/main/java/com/example/upipaymentalert/TtsEngineHelper.kt`: Detection, pre-warming, and binding for Google Speech Services.
- `app/src/main/java/com/example/upipaymentalert/smsparser/AnnouncementPhrases.kt`: Programmatic phrase templates for 13 Indian languages.
- `app/src/main/res/values-*/strings_tts.xml`: Segmented XML localized speech strings.

---

## Key Concepts & Mechanisms

### 1. Hardware Loudness Boost (+1500 mB)
- Ordinary media volume peaks at 100%. In noisy store counters, merchants need louder audio.
- When `volumePercent > 100`:
  ```kotlin
  val boostAmount = volumePercent - 100
  val targetGainMb = (boostAmount * 15).coerceIn(0, 1500)
  loudnessEnhancer?.setTargetGain(targetGainMb)
  loudnessEnhancer?.enabled = true
  ```
- The `LoudnessEnhancer` is linked to `audioSessionId` generated from `AudioManager.generateAudioSessionId()`.
- **CRITICAL**: When utterance completes or errors, `restoreVolume()` must disable the enhancer and reset stream volume.

### 2. Segmented Speech Speed Tokenization (`|`)
- TTS sentences are formatted with pipe delimiters around amounts:
  - English: `Received |%1$s| rupees from %2$s`
  - Hindi: `%1$s से |%2$s| रुपये प्राप्त हुए`
- In `ForegroundTtsService.speak()`:
  - Even index (`0`, `2`, ...): Sentence framing words -> read at fixed baseline `0.9f` speed.
  - Odd index (`1`, `3`, ...): Amount numbers -> read at user's `customSpeed - 0.1f`.
  - Both segments are queued seamlessly with `TextToSpeech.QUEUE_ADD`.

### 3. Deduplication Sliding Window
- In-memory `ConcurrentHashMap<String, Long> recentAnnouncements`.
- `DEDUPLICATION_WINDOW_MS = 60000L` (60 seconds).
- Drops identical text if already announced within 60 seconds (prevents echo when both push notification and bank SMS arrive).

---

## Workflow: Adding a New Indian Language or Dialect

1. **Add Language to AnnouncementPhrases.kt**:
   - Add new constant to `AnnouncementPhrases.Lang` enum.
   - Update `AnnouncementPhrases.buildPhrase()` with localized sentence structure.
   - Include pipe delimiters around amounts.

2. **Add Voice Binding in ForegroundTtsService.kt**:
   - In `speak()`, map language name to its `Locale`:
     ```kotlin
     lower.contains("bodo") -> Locale("brx", "IN")
     ```
   - Verify `localTts.setLanguage(locale)` and voice iteration.

3. **Add strings_tts.xml in Corresponding Resource Directory**:
   Create `app/src/main/res/values-[langCode]/strings_tts.xml` with:
   - `tts_rupees_only_with_sender`
   - `tts_rupees_and_paise_with_sender`
   - `tts_rupees_only_no_sender`
   - `tts_rupees_and_paise_no_sender`
   - Ensure amount placeholders are wrapped in `|%1$s|`.

4. **Verify Google TTS Pre-warming in TtsEngineHelper.kt**:
   - Add the new `Locale` to `indianLocales` in `TtsEngineHelper.prewarmAndPrepareVoiceData()`.

---

## Verification
- Test audio synthesis with `MainActivity` test sound buttons.
- Confirm volume returns to original user setting after TTS speech finishes.
