# StreamHelper Android — Migration Plan

Native Android rewrite of the React Native app at `../streamhelper-mobile`
(the RN repo is frozen; no new work goes there). iOS is out of scope for now.

## Scope (v1)

Exact parity with the current RN app — nothing more, nothing less:

- Twitch login (in-app WebView, implicit OAuth flow) + token validation on start + logout
- Twitch chat over EventSub WebSocket, with Twitch and 7TV emotes (animated WebP)
- Chat moderation from a bottom sheet: delete message, ban, unban, timeout
- DonationAlerts login + donations list
- StreamInfo and Raid tabs stay **empty placeholders**

Polls/predictions are mentioned in the README/scopes but not implemented in RN — not in v1.

## Process

Feature-by-feature rewrite. After each step: stop, user reviews and runs the app, then continue.

1. ✅ **Skeleton** — Gradle project, theme, strings, splash, navigation shell with placeholder screens, Hilt, BuildConfig config
2. **Twitch auth** — WebView login, token storage, token validation, logout, Auth ⇄ Main routing
3. **Chat (read-only)** — EventSub WebSocket, subscription creation, message list
4. **Emotes** — Twitch global/channel + 7TV sets, inline rendering in messages
5. **Moderation** — message bottom sheet: delete, ban, unban, timeout
6. **DonationAlerts** — DA login, donations list

## Decisions

| Area | Decision |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3, single Activity |
| Architecture | Google-recommended MVVM + unidirectional data flow: `ViewModel` exposing `StateFlow<UiState>`; repositories (+ services where useful) as plain Kotlin classes |
| Modules | Single `:app` module, package-by-feature (no multi-module) |
| DI | Hilt (`hiltViewModel()` in Compose) |
| Navigation | Navigation Compose (type-safe routes). Root graph: Auth (Entry → Login), Main (bottom tabs Chat / Alerts / StreamInfo / Raid), generic WebView route |
| HTTP / JSON | Retrofit + OkHttp + kotlinx.serialization. One Retrofit instance per backend (Twitch Helix, 7TV, DonationAlerts). Bearer token via OkHttp interceptor |
| WebSocket | OkHttp WebSocket wrapped in a repository exposing a `Flow`. Auto-reconnect with backoff; handles EventSub `session_reconnect` and keepalive timeout |
| Background behavior | Socket lives while the app is visible; reconnect + re-subscribe on return to foreground. No foreground service |
| OAuth | Keep in-app WebView + implicit flow (same as RN). Intercept navigation to `https://streamhelpermobile.com/login` in `shouldOverrideUrlLoading`, read `access_token` from the URL fragment. **No** deep link / `autoVerify` intent-filter |
| Token storage | Jetpack DataStore; values encrypted with AES-GCM using a key held in Android Keystore (platform APIs only). No migration from RN (app never released) |
| Images | Coil 3 with animated-image decoder. Animated WebP on API 28+; static first frame on Android 8.x (accepted) |
| Config | `local.properties` (git-ignored) → `BuildConfig` fields. Committed `local.properties.example` |
| Localization | `strings.xml` — `values/` (en), `values-ru/` (ru); follows system locale; per-app language support (`localeConfig`) |
| Theme | RN palette mapped to a Material 3 `ColorScheme` + `Typography`. **Light only** for now; dynamic color off |
| Logging | `android.util.Log` |
| App identity | `applicationId` / namespace `com.streamhelper` (RN app was never released). minSdk 26, target/compileSdk 37 (latest stable; required by current AndroidX) |
| Build | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), latest stable AGP / Kotlin / Compose BOM, JDK 17 |
| Repo | Standalone git repo at `../streamhelper-android`, fresh history |

## RN → Android mapping

| RN | Android |
|---|---|
| MobX stores (`*Store`) | `ViewModel` + `StateFlow<UiState>` |
| `*Service`, `*Repository` | Kotlin classes injected by Hilt |
| `LoadingModel` | `isLoading` field in `UiState` |
| axios / `AxiosClient` | Retrofit + OkHttp |
| `ts-serializable`, `Dto`, `ModelFactory` | `@Serializable` data classes |
| `WebSocketStore` | OkHttp WebSocket + `Flow` |
| `react-native-encrypted-storage` | DataStore + Android Keystore AES-GCM |
| `@react-native-async-storage/async-storage` | DataStore (plain) |
| `react-native-config` | `local.properties` → `BuildConfig` |
| i18next `en.json` / `ru.json` | `values/strings.xml`, `values-ru/strings.xml` |
| React Navigation stack + bottom tabs | Navigation Compose + M3 `NavigationBar` |
| `react-native-webview` | `android.webkit.WebView` via `AndroidView` |
| `react-native-fast-image`, `react-native-animated-webp` | Coil 3 |
| `@gorhom/bottom-sheet` | M3 `ModalBottomSheet` |
| `react-native-toast-message` | M3 `Snackbar` (bottom) |
| `react-native-vector-icons/MaterialIcons` | Material Icons |
| `react-native-bootsplash` | AndroidX `core-splashscreen` |
| `react-native-reanimated-skeleton` | Shimmer placeholder modifier |
| `react-native-linear-gradient` | `Brush.linearGradient` |
| `styled-components` theme (`colors.ts`, `typography.ts`) | `ColorScheme`, `Typography` |

## Deferred on purpose — IMPLEMENT LATER

Consciously excluded from v1. Do not implement during the migration.

- [ ] **IMPLEMENT LATER — Chat message cap**: RN appends messages forever (unbounded memory on long streams). Cap the list at the latest ~500 messages. Deliberately kept at parity (unbounded) during migration to avoid introducing bugs.
- [ ] **IMPLEMENT LATER — CI**: GitHub Actions running build + lint + tests on PRs.
- [ ] **IMPLEMENT LATER — Tests**: JUnit + MockK + Turbine + MockWebServer unit tests for ViewModels/repositories; a few Compose UI tests (login redirect parsing, chat rendering with emotes).
- [ ] **IMPLEMENT LATER — Lint / format tooling**: ktlint + detekt + Android Lint.
- [ ] **IMPLEMENT LATER — Auth via Chrome Custom Tabs**: replace WebView login; requires App Links (`assetlinks.json` on `streamhelpermobile.com`) and re-adding the `autoVerify` intent-filter. Consider authorization-code flow.
- [ ] **IMPLEMENT LATER — Background chat**: foreground service with persistent notification to keep chat connected in background.
- [ ] **IMPLEMENT LATER — Dark mode**: add a dark `ColorScheme` (theme is already structured for it).
- [ ] **IMPLEMENT LATER — Polls & predictions** (OAuth scopes already requested).
- [ ] **IMPLEMENT LATER — StreamInfo and Raid screens** (placeholders in v1).
- [ ] **IMPLEMENT LATER — iOS**: separate native port, not planned yet.
