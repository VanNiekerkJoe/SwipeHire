# SwipeHire — Android Frontend (Part 2 Prototype)

Native Kotlin + Jetpack Compose frontend for **SwipeHire**, the reverse job-matching app from the
PROG7314 Part 1 design (companies swipe on student profiles, students swipe on job postings,
mutual interest opens a chat).

**Scope of this build:** UI layer only — no live networking. Screens run on realistic mock data
so the whole flow can be demoed end-to-end on a physical device while the API and feature work
land from the rest of the team. Swap the mock lists in `data/Models.kt` for real API calls once
the REST layer is ready.

## Screens

| Screen | File | Notes |
|---|---|---|
| Login | `ui/screens/LoginScreen.kt` | Google SSO + biometric sign-in entry points (stubbed) |
| Role select | `ui/screens/RoleSelectScreen.kt` | Choose Student / Company — same screen serves both |
| Discover | `ui/screens/DiscoverScreen.kt` | Draggable swipe-card stack (`ui/components/SwipeCardStack.kt`) |
| Matches | `ui/screens/MatchesScreen.kt` | List of mutual matches with unread indicator |
| Chat | `ui/screens/ChatDetailScreen.kt` | Per-match conversation, local send |
| Profile | `ui/screens/ProfileScreen.kt` | Adapts content to Student vs Company account type |
| **Settings** | `ui/screens/SettingsScreen.kt` | **20-mark deliverable — see below** |
| Jobs near you | `ui/screens/NearbyJobsScreen.kt` | Google Map + distance-sorted list, from Discover's "Near you" button (student view) |
| Job location | `ui/screens/JobLocationScreen.kt` | Single job on a map with a "Get directions" deep link into Google Maps |

## Settings menu (Application State requirement)

Every control on the Settings screen reads from and writes to Jetpack **DataStore Preferences**
(`data/SettingsDataStore.kt`) through `viewmodel/SettingsViewModel.kt`, so changes persist across
app restarts — this is what satisfies the "Application State: Changing and saving user
preferences via a dedicated settings menu" requirement:

- **Account type** — Student / Company toggle (shared UI, drives Discover + Profile content)
- **Notifications** — push master switch, match alerts, message alerts
- **Privacy** — profile visibility toggle
- **Appearance** — Light / Dark / Match system theme, applied live via `SwipeHireTheme`
- **Security** — biometric lock toggle (UI hook — wire to `BiometricPrompt` once auth lands)
- **Log out**

## Google Maps setup (required to run)

The Maps API key is kept out of version control via the Secrets Gradle Plugin:

1. Get a Maps SDK for Android API key from the [Google Cloud Console](https://console.cloud.google.com/google/maps-apis) (enable "Maps SDK for Android").
2. In the project root, create (or edit) `local.properties` — it's already gitignored — and add:
   ```
   MAPS_API_KEY=your_real_key_here
   ```
3. Sync Gradle. `local.defaults.properties` (committed) provides a placeholder so the project still
   builds without a key, but the map won't render tiles until each teammate sets their own key.

The app also requests `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` at runtime (handled in
`NearbyJobsScreen`) to power "Jobs near you."

## Stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- DataStore Preferences for persisted settings
- ViewModel + StateFlow for state management
- Custom gesture-driven swipe card stack (no third-party swipe library)
- Maps Compose + Play Services Location for job locations and "Jobs near you"

## Opening the project

1. Open the `SwipeHire/` folder in Android Studio (Koala or newer recommended).
2. Add your Maps API key per the section above.
3. Let Gradle sync — it will pull dependencies from Google's Maven and Maven Central.
4. Run on a physical device (min SDK 26) per the POE requirement.

## Next steps for the team

- Wire `LoginScreen` to real Google SSO and `SettingsScreen`'s biometric toggle to `BiometricPrompt`.
- Replace `MockData` with calls to the ASP.NET Core REST API once endpoints are live — this includes
  geocoding a job's typed address into `latitude`/`longitude` when an employer posts it.
- Hook Room for offline caching per the original Part 1 design.
