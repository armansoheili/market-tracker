# Market Tracker

Live market-price Android app — gold, US dollar, euro and Tether (USDT) in Toman/Rial, with charts, price alerts, a home-screen widget and an English UI.

- Live prices: baha24 (USD, EUR, USDT, 18k gold) + Nobitex cross-check for USDT
- Price unit toggle: Toman / Rial (e.g. `1670000 Rial`)
- Dark home-screen widget styled like a market terminal
- Price alerts with background refresh (WorkManager)

Built with Kotlin + Jetpack Compose (Material 3) + Glance.

## Build

```sh
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (compileSdk 34, minSdk 26).
