---
name: background-longevity
description: >-
  Use this skill when modifying foreground service lifecycles, handling OEM aggressive background killers,
  auto-start intents, notification listener bindings, or battery optimization bypasses.
---

# Background Longevity & Service Persistence Skill

This skill guides the agent in ensuring MyUPIAlert runs continuously 24/7 as an unkillable voice soundbox without being terminated by aggressive OEM task killers (MIUI, ColorOS, FuntouchOS, One UI).

## Target Files
- `app/src/main/java/com/example/upipaymentalert/ForegroundTtsService.kt`: Ongoing foreground service lifecycle and notification channel.
- `app/src/main/java/com/example/upipaymentalert/AutoStartHelper.kt`: Manufacturer-specific intent resolver for auto-start settings.
- `app/src/main/java/com/example/upipaymentalert/NotificationListener.kt`: NotificationListenerService lifecycle and reconnection.
- `app/src/main/java/com/example/upipaymentalert/MainActivity.kt`: Permission checks and power management intents.
- `app/src/main/AndroidManifest.xml`: Permissions, service declarations, and foregroundServiceType.

---

## Architectural Pillars of Longevity

### 1. Foreground Service Configuration
- On Android 8.0+ (API 26+), background services are killed within minutes unless started as Foreground Services.
- In `AndroidManifest.xml`:
  ```xml
  <service android:name=".ForegroundTtsService" 
      android:exported="false"
      android:foregroundServiceType="specialUse">
      <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" 
                android:value="TTS Announcement" />
  </service>
  ```
- In `onCreate()` of `ForegroundTtsService.kt`:
  ```kotlin
  val n = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("UPI Payment Alert Active")
      .setContentText("Listening for incoming payments via SMS and Notifications.")
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setOngoing(true)
      .build()
  startForeground(12345, n)
  ```
- Return `START_STICKY` in `onStartCommand()` to request Android OS to recreate the service if killed under memory pressure.

### 2. Android 12+ (API 31+) Foreground Service Launch Exception
- Starting a foreground service from background in Android 12+ can throw `ForegroundServiceStartNotAllowedException`.
- Always wrap service starts from receivers/listeners:
  ```kotlin
  try {
      if (Build.VERSION.SDK_INT >= 31) {
          try {
              context.startForegroundService(svc)
          } catch (e: Exception) {
              try { context.startService(svc) } catch (ignored: Exception) {}
          }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(svc)
      } else {
          context.startService(svc)
      }
  } catch (ignored: Exception) {}
  ```

### 3. Battery Optimization Exemption
- Prompt the user to whitelist the app from Android Doze mode:
  ```kotlin
  val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
  if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
      val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
      startActivity(intent)
  }
  ```

### 4. OEM Auto-Start Intents (`AutoStartHelper.kt`)
Aggressive OEM battery managers stop background broadcast receivers unless explicitly whitelisted:
- **Xiaomi / POCO / Redmi**: `com.miui.securitycenter / AutoStartManagementActivity`
- **Oppo / Realme / OnePlus**: `com.coloros.safecenter / StartupAppListActivity`
- **Vivo / iQOO**: `com.vivo.permissionmanager / BgStartUpManagerActivity`
- **Huawei / Honor**: `com.huawei.systemmanager / ProtectActivity`
- **Asus**: `com.asus.mobilemanager / AutoStartActivity`
- **Samsung**: Launch application details settings (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) directing user to Battery -> Unrestricted.

---

## Verification Checklist
1. Verify `NotificationListener` reconnects after device restart or app update.
2. Confirm persistent notification is visible in Android notification tray while service is active.
3. Test killing app from recents menu; verify `ForegroundTtsService` continues announcing incoming SMS/notifications.
