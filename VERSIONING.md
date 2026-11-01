# Expenso versioning policy

Every bug fix or new feature ships as a **main version**:

1. Bump the patch version in `app/build.gradle.kts` (`appVersionName`, and `appVersionCode` +1).
2. Build the main version:
   ```
   gradle -p Expenso assembleMain
   ```
3. Ship the artifact at:
   ```
   Expenso/app/build/outputs/apk/main/Expenso-<versionName>-main.apk
   ```

The `assembleMain` task depends on `assembleDebug`, so the APK stays signed with
the local debug key (installable on any device without a release keystore) while
reporting the plain main version name, with no `-debug` suffix.

## Version history

| Version | versionCode | Notes |
|---------|-------------|-------|
| 1.0.0   | 1           | Initial complete build |
| 1.0.1   | 2           | Dark-mode rendering fix, first named main version |
| 1.0.2   | 3           | Smooth screen transitions and animated bottom bar |
| 1.0.3   | 4           | Renamed app/package to Expenso (com.expenso.app) and new liquid-glass UI |
| 1.0.4   | 5           | New Expenso brand logo as launcher icon (adaptive, round and legacy) |
| 1.0.5   | 6           | UI reverted to the pre-liquid-glass design; keeps Expenso name/package and new logo |
| 1.0.6   | 7           | New brand logo as launcher icon; profile keeps only name and adds 8 preset profile photos |
| 1.0.7   | 8           | Home spending chart restyled like the analytics trend chart; day and date labels now align under each bar |
| 1.0.8   | 9           | Home budget progress replaced with a cumulative spending line/area chart, budget target line and aligned date axis |
