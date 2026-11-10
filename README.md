# Stream Helper (Android)

Native Android rewrite of [streamhelper-mobile](https://github.com/srrmstk/streamhelper-mobile) (React Native).
Helps streamers read and moderate Twitch chat and watch DonationAlerts tips from a phone.

See [PLAN.md](PLAN.md) for the migration plan, decisions, and deferred work.

## Setup

1. Android Studio (or JDK 17 + Android SDK).
2. `cp local.properties.example local.properties` and fill in the values (same keys as the RN `.env`).
3. `./gradlew installDebug` — or Run in Android Studio.

Note: This app is not associated with Twitch, 7tv and Donation Alerts.
