<div align="center">

<a href="https://github.com/codingwitharul/MyUPIAlert">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f0c29,50:302b63,100:24243e&height=180&section=header&text=Contributing%20to%20MyUPIAlert&fontSize=32&fontColor=ffffff&fontAlignY=38&desc=Help%20make%20payments%20audible%20and%20private&descAlignY=58&descSize=16&animation=fadeIn" width="100%"/>
</a>

<br/>

[![Typing SVG](https://readme-typing-svg.herokuapp.com?font=JetBrains+Mono&weight=700&size=18&duration=2500&pause=800&color=f97316&center=true&vCenter=true&width=650&lines=Fork+it.+Build+it.+PR+it.+Get+credited.+%F0%9F%9A%80;Open+Source+Offline+Voice+Soundbox;No+corporate+red-tape.+Just+clean+Kotlin+code.)](https://github.com/codingwitharul/MyUPIAlert)

<br/>

<a href="https://github.com/codingwitharul/MyUPIAlert/issues">
  <img src="https://img.shields.io/github/issues/codingwitharul/MyUPIAlert?style=for-the-badge&logo=github&logoColor=white&color=e74c3c&label=Open%20Issues" alt="Issues"/>
</a>
<a href="https://github.com/codingwitharul/MyUPIAlert/pulls">
  <img src="https://img.shields.io/github/issues-pr/codingwitharul/MyUPIAlert?style=for-the-badge&logo=git&logoColor=white&color=3498db&label=Open%20PRs" alt="Pull Requests"/>
</a>
<a href="https://github.com/codingwitharul/MyUPIAlert/graphs/contributors">
  <img src="https://img.shields.io/github/contributors/codingwitharul/MyUPIAlert?style=for-the-badge&logo=handshake&logoColor=white&color=22c55e&label=Contributors" alt="Contributors"/>
</a>

</div>

---

## ⚡ Welcome Contributors!

**MyUPIAlert** is an open-source, hardware-accelerated soundbox and analytics utility for Android. It allows merchants, shopkeepers, and everyday users to receive instant, audible voice announcements for incoming UPI payments without expensive monthly rental hardware.

We welcome code contributions, bug reports, language improvements, and bank parser refinements from developers worldwide.

---

## 📋 Code Quality & Contribution Standards

To keep the codebase reliable, secure, and production-grade, all contributors must observe these core rules:
- **100% Kotlin Idioms**: The codebase is modern Kotlin with Jetpack Compose, Room, and Koin.
- **Privacy By Design**: The app is strictly 100% offline. Never add tracking, analytics, or external API pings.
- **Battery Optimization**: Never run polling loops or unthrottled wake locks. Background work must follow strict Android foreground service guidelines.
- **Maintain Comments & Tests**: Always add or update unit tests when modifying SMS parsing or deduplication rules.

---

## 🚀 Pull Request Workflow

1. **Fork** the repository on GitHub.
2. **Create a topic branch** from `main`:
   ```bash
   git checkout -b feature/your-awesome-feature
   ```
3. **Commit your changes** with clear, descriptive commit messages:
   ```bash
   git commit -m "feat(parser): add support for XYZ Bank payment SMS pattern"
   ```
4. **Test thoroughly**:
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```
5. **Push to your fork**:
   ```bash
   git push origin feature/your-awesome-feature
   ```
6. **Open a Pull Request** targeting the `main` branch on `codingwitharul/MyUPIAlert`.
