# Therapy Schedule

A native Android app for keeping a psychotherapist's client list and appointment schedule on one device. It uses the Android SDK and Java, with an app-private SQLite database; it requests no network permissions and disables Android backup.

## Build and run

Open the project in Android Studio, or build a debug APK from the repository root:

```sh
gradle assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. The project targets Android 15 (API 35) and supports Android 8.0 (API 26) and newer.

## Features

- Maintain a client list using names only.
- Browse dates and schedule sessions with a client, start time, and duration.
- Reject sessions that overlap an existing appointment.
- Cancel scheduled sessions without deleting the appointment record.
- Send a five-minute reminder notification with the patient’s name and a notification sound before each appointment.

Client and appointment data stays in the app's private on-device database and is excluded from Android backups. The database is not separately encrypted or protected by an in-app lock, so this MVP should not be used with real patient records until the practice's privacy and compliance requirements are addressed. Removing the app removes its local data.
