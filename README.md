<div align="center">

<a href="https://github.com/codingwitharul/MyUPIAlert">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f0c29,50:302b63,100:24243e&height=220&section=header&text=MyUPIAlert&fontSize=64&fontColor=ffffff&fontAlignY=38&desc=Open%20Source%20Voice%20Soundbox%20%26%20Dashboard%20for%20Android&descAlignY=58&descSize=20&animation=fadeIn" width="100%"/>
</a>

<br/>

[![Typing SVG](https://readme-typing-svg.herokuapp.com?font=JetBrains+Mono&weight=700&size=22&duration=3000&pause=800&color=f97316&center=true&vCenter=true&multiline=false&width=750&lines=Dual+SMS+%26+Notification+Listener+%E2%9A%A1;Hardware+Loudness+Boost+200%25+%F0%9F%94%8A;13+Indian+Languages+Supported+%F0%9F%87%AE%F0%9F%87%B3;Cross-Channel+Twin+Deduplication+%F0%9F%9B%A1%EF%B8%8F;100%25+Offline+Privacy+%F0%9F%94%92;Modern+Jetpack+Compose+Glass+UI+%E2%9C%A8)](https://github.com/codingwitharul/MyUPIAlert)

<br/>

<a href="https://github.com/codingwitharul/MyUPIAlert/releases/latest">
  <img src="https://img.shields.io/github/v/release/codingwitharul/MyUPIAlert?style=for-the-badge&logo=github&logoColor=white&label=Release&color=FFD700" alt="Latest Release"/>
</a>
<a href="https://github.com/codingwitharul/MyUPIAlert/stargazers">
  <img src="https://img.shields.io/github/stars/codingwitharul/MyUPIAlert?style=for-the-badge&logo=starship&logoColor=white&color=f7c948" alt="Stars"/>
</a>
&nbsp;
<a href="LICENSE">
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge&logo=opensourceinitiative&logoColor=white" alt="License"/>
</a>

<br/><br/>

> **A free, native Android voice soundbox and payment dashboard for your shop or business.**  
> Hear payment announcements instantly, aloud, and completely offline. No monthly hardware rentals, no subscriptions, zero data sharing.

<br/>

<a href="https://github.com/codingwitharul/MyUPIAlert/releases">
  <img src="https://img.shields.io/badge/Download_GitHub_Release_APK-0f0c29?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
</a>

</div>

---

## ⚡ What Is MyUPIAlert?

Commercial soundboxes (Paytm Soundbox, PhonePe SmartSpeaker, Google Pay Soundpod) charge **monthly rentals of ₹125 – ₹200**, require dedicated SIM cards, and lock merchants into a single payment app.

**MyUPIAlert turns any standard Android phone or tablet into an unkillable, high-powered voice soundbox and financial dashboard**:
- **Zero hardware costs**: Use an existing shop phone, tablet, or secondary Android device.
- **Universal payment app support**: Works with **all** UPI apps and transactional bank SMS simultaneously.
- **Cross-Channel Twin Deduplication**: Dual-engine intelligence ensures that incoming App Notifications and companion Bank SMS for the same payment are announced exactly once, preventing double counting and false alarms.
- **No recurring monthly rentals**: Free forever.
- **100% Offline Privacy**: All text-to-speech synthesis, regex extraction, and SQLite database storage happen purely on your device. Zero external cloud servers, zero financial telemetry.

---

## 🗂️ Core Features

- ⚡ **Dual SMS & Notification Listeners** — Captures alerts from all financial push notifications and transactional bank SMS simultaneously.
- 🛡️ **Intelligent Cross-Channel Deduplication** — Smart multi-channel deduplicator absorbs twin notifications and bank confirmations for the same transaction without duplicate announcements or double-counted sales totals.
- 🔊 **Hardware Loudness Boost (Up to 200%)** — Integrates Android's low-level `LoudnessEnhancer` (+1500 mB acoustic boost) so your device cuts through busy market chatter and noisy storefronts.
- 🇮🇳 **13 Indian Languages Supported** — Deterministic vocal announcements and full in-app UI language switching in **English, Hindi, Tamil, Telugu, Kannada, Malayalam, Gujarati, Punjabi, Bengali, Marathi, Odia, Assamese, and Urdu**.
- 📊 **Dedicated Analytics Dashboard** — Visual rush-hour heatmap chart, KPI cards (Total Volume, Average Ticket Size, Peak Sale), UPI app market share, and time-of-day sales distribution.
- 🎛️ **Service Operations Hub** — Manual override Start/Pause control and automated store operating schedules (e.g. 9:00 AM – 9:00 PM) to silence after-hours announcements while preserving transaction logging.
- ⏱️ **Segmented Speech Speed** — Custom slider sets the pronunciation speed rate of the currency amount independently of structural phrases for crystal-clear comprehension.
- 🎙️ **Google Speech Services Pre-warming** — Automatically pre-warms Google TTS on startup and binds directly to native Indian voice models.
- 🏦 **Comprehensive UPI App Coverage** — Works out-of-the-box with PhonePe, Google Pay, Paytm, BHIM, Cred, Amazon Pay, Navi, FamPay, SBI, HDFC, ICICI, Axis, Kotak, and 28+ banking apps.
- 📤 **Carrier SMS Forwarder** — Optional SMS forwarding with configurable target recipient, transaction type filter (Credit/Debit/Both), and single-alert dispatch.
- 🎨 **Modern Glass Dashboard** — Built with Jetpack Compose, featuring day and night color schemes, tactile quick action tiles, collapsible transaction cards, and instant statement export.

---

## 🇮🇳 Full 13 Indian Languages Matrix

| Language | Code | Native Script | Spoken Announcement |
| :--- | :---: | :---: | :--- |
| **English** | `en` | English | *Received 500 rupees from Rahul* |
| **Hindi** | `hi` | हिन्दी | *Rahul से 500 रुपये प्राप्त हुए* |
| **Gujarati** | `gu` | ગુજરાતી | *Rahul તરફથી 500 રૂપિયા મળ્યા* |
| **Marathi** | `mr` | मराठी | *Rahul कडून 500 रुपये मिळाले* |
| **Tamil** | `ta` | தமிழ் | *Rahul இடமிருந்து 500 ரூபாய் பெறப்பட்டது* |
| **Telugu** | `te` | తెలుగు | *Rahul నుండి 500 రూపాయలు అందాయి* |
| **Kannada** | `kn` | ಕನ್ನಡ | *Rahul ಅವರಿಂದ 500 ರೂಪಾಯಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ* |
| **Malayalam** | `ml` | മലയാളം | *Rahul-ൽ നിന്ന് 500 രൂപ ലഭിച്ചു* |
| **Punjabi** | `pa` | ਪੰਜਾਬੀ | *Rahul ਵੱਲੋਂ 500 ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ* |
| **Bengali** | `bn` | বাংলা | *Rahul এর কাছ থেকে 500 টাকা পাওয়া গেছে* |
| **Odia** | `or` | ଓଡ଼ିଆ | *Rahul ଙ୍କଠାରୁ 500 ଟଙ୍କା ପ୍ରାପ୍ତ ହେଲା* |
| **Assamese** | `as` | অসমীয়া | *Rahul ৰ পৰা 500 টকা পোৱা গ'ল* |
| **Urdu** | `ur` | اردو | *Rahul سے 500 روپے موصول ہوئے* |

---

## 🔊 Hardware Loudness Boost (+1500 mB)

Standard Android media volume is often too quiet for open markets, busy retail counters, and street stalls. 

MyUPIAlert hooks into the hardware `android.media.audiofx.LoudnessEnhancer` audio session, supplying up to **+1500 mB extra acoustic gain** when the volume slider is boosted up to 200%.

---

## 🏗️ How to Build from Source

### Prerequisites:
- Android Studio Iguana (2023.2.1)+ or IntelliJ IDEA
- JDK 17 (Eclipse Adoptium or Oracle JDK)
- Android SDK Platform 34 / 35
- Gradle 9+ (Wrapper included)

```bash
# Clone the repository
git clone https://github.com/codingwitharul/MyUPIAlert.git
cd MyUPIAlert

# Run unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🚀 Getting Started & Permissions

1. Download the latest APK from the **[Releases Section](https://github.com/codingwitharul/MyUPIAlert/releases)**.
2. Install the APK on your Android device (Android 7.0 to Android 15).
3. Open the app and grant the essential permissions:
   - **Notification Access**: To read incoming payment pushes from UPI apps.
   - **SMS Permission**: To intercept and announce banking transactional SMS.
   - **Battery Optimization Bypass**: To prevent aggressive OEM task killers from freezing background announcements.
   - **Auto-Start**: Enable Auto-Start in device settings to restart the speaker on boot.
4. Select your preferred Language, adjust the Volume and Speed sliders, and tap **Test Sound**.

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for complete terms.

```text
Copyright (c) 2026 MyUPIAlert Contributors
```
