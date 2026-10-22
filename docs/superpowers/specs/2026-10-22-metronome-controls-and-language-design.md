# Metronome controls and in-app language: design

Date: 2026-10-22. Status: built, reviewed by the user, revised. The first build (−/+ beside
the ring, a ten-button scroller for the beats, flag buttons above the content) was rejected
as too packed; the user then chose, from options offered, a stepper pill for the beats, an
in-place wheel instead of the −/+ buttons, and the language switch in a top app bar. The
calls marked *my call* are the builder's.

## Intent

Four follow-ups to the kid-friendly UI, all asked for in one request:

1. A language switch inside the app, so the app can be in Croatian on an English phone
   (a child's phone is often set up by a parent in another language) or vice versa.
2. The − and + tempo buttons go; tapping the tempo number gives a scroller instead.
3. More "beats in a bar" choices than 2/4, 3/4 and 4/4, in one simple component.
4. A volume control on the metronome screen.

## Goals

- Switching language takes one tap, works on every supported Android version
  (minSdk 24), survives restarts and does not stop a running metronome.
- The ring stays the hero; fine tempo changes need no extra row of buttons.
- Every time signature the engine knows is a few taps away in one compact control, with
  the three a beginner meets first next to each other.
- The screen is no fuller than before: the user rejected a packed first attempt.
- Volume is a slider with no number, in line with the rest of the screen.

## Non-goals

- Changing the audio engine, persistence or ViewModel contracts beyond exposing what
  already exists (`setVolume`, `setTimeSignature`).
- A settings screen. The language switch sits on the main screen; an "advanced mode"
  remains the home for everything else that is still hidden.
- Translating into a third language.

## Anti-goals

- Numbers or jargon on the screen: no percentage on the volume, no "BPM".
- A language switch that only works on Android 13+.
- A layout that overflows a 360 dp wide phone.

## What the child sees

### Language switch

In the top app bar, as its action: two small round buttons showing a flag, 🇬🇧 and 🇭🇷; the
active one is ringed in coral and the other is faded. The bar also carries the app name.
*My call:* flags, because a child who cannot read the current language still finds their
own, and the two buttons need no translated label. The accessibility labels are the
language names in their own language ("English", "Hrvatski"), which are therefore
untranslatable strings.

The choice is stored by the platform, not by the app (per-app language on Android 13+;
AppCompat's backport with `autoStoreLocales` below that). The screen re-creates itself
with the new language; the ViewModels and the metronome engine survive that, so a
running beat keeps going. Below Android 13 only the activity is relocalised, so the
playback notification keeps the phone's language there. *My call:* acceptable, it is a
system surface read by adults.

### Tempo

The ring is unchanged: 250 dp, drag around it, one degree is one beat. The − / + buttons
are gone. A faint up-down chevron sits above the number as a hint; tapping the number
swaps it, in the same spot, for a three-value wheel: the chosen tempo big in the middle,
the neighbours small and faded above and below. Scrolling snaps one beat per notch with
a haptic tick and the ring, the level pills and a running metronome follow live. Tapping
a neighbour scrolls to it; tapping the middle value, or touching anywhere else on the
screen, brings the plain number back (the touched control still works).

### Beats in a bar

One pill, 64 dp tall and full width: an arrow, the current signature written the way it
is in sheet music, an arrow. The arrows step through the engine's display order and wrap
at both ends: 2/4, 3/4, 4/4, 5/4, 3/8, 6/8, 7/8, 9/8, 12/8, 2/2. The beat dots above show
the result at once and shrink for bars with more than six beats so 12 dots still fit.

New engine entries (*my call* on the accents, which follow the usual textbook groupings):
3/8 and 2/2 simple, 7/8 as 2+2+3 (strong, weak, medium, weak, medium, weak, weak),
9/8 as 3+3+3 and 12/8 as 3+3+3+3 with a medium accent on each group. Every beat still
gets one click at the chosen tempo, as the engine already does for 6/8.

### Volume

> Removed again later the same day: the phone's volume buttons already do this and the screen
> is sparser without it. `MetronomeViewModel.setVolume` and the stored value stay for an
> advanced mode, but neither screen reads the stored value any more.

Under the play button: a small speaker icon, a Material slider, a loud speaker icon. No
label, no number; the accessibility name is "Volume" / "Glasnoća". Moving it calls the
existing `MetronomeViewModel.setVolume`, which already persists and forwards to the
engine while playing.

### Order on the metronome tab

Beat dots, tempo ring, level pills, beats pill, play button, volume, with the original
24 dp side padding and spacing. The whole tab fits a 1080×2400 phone without scrolling.

## Code changes (the how, for the builder)

- `gradle/libs.versions.toml`, `app/build.gradle.kts`: add `androidx.appcompat:appcompat`.
- `MainActivity` extends `AppCompatActivity`; `res/values/themes.xml` moves to
  `Theme.AppCompat.DayNight.NoActionBar` (the night variant file goes away); the
  manifest declares `AppLocalesMetadataHolderService` with `autoStoreLocales`.
- `ui/common/AppLanguage.kt`: enum `ENGLISH` / `CROATIAN` with the language tag, flag and
  endonym, plus `fromLocale` (anything that is not Croatian counts as English, since
  English is the fallback resource set). `ui/common/LanguageSwitch.kt`: the stateless
  two-button toggle. `MainScreen` puts it in a `TopAppBar`, reads the current language
  from `LocalConfiguration` and wires the switch to `AppCompatDelegate.setApplicationLocales`.
- `domain/model/MetronomeConfig.kt`: new `TimeSignature` entries in display order.
  Persistence is by name, so reordering and adding are safe. `ui/metronome/MetronomeUiState.kt`
  adds the wrapping `next()` / `previous()` the stepper uses.
- `ui/metronome/TempoWheel.kt`: the snapping three-value drum (a `LazyColumn` with
  `rememberSnapFlingBehavior`, the item nearest the viewport centre is the value).
- `ui/metronome/TempoDial.kt`: the number is tappable with a hint icon; while `editing` it
  shows the wheel and reports the wheel's position so the screen can close it on a touch
  elsewhere (observed on the initial pointer pass at the screen root).
- `ui/metronome/MetronomeScreen.kt`: stepper pill, volume row, `onVolumeChange` callback on
  `MetronomeContent`; `onAdjustBpm` and the hold-to-repeat buttons are gone (the
  ViewModel keeps `adjustBpm` for an advanced mode).
- `ui/metronome/BeatIndicator.kt`: sizes scale down above six beats.
- `ui/common/AppIcons.kt`: speaker icons, chevrons and the unfold hint; the + / − icons go.
- Strings: `volume`, `language`, `tempo_tap`, `tempo_wheel`, `previous_time_signature`,
  `next_time_signature`; `decrease_tempo` / `increase_tempo` go.
- Tests: `AppLanguageTest`, `TimeSignatureTest`, `TimeSignatureStepTest` (JVM);
  `LanguageSwitchTest` and the extended `MetronomeScreenTest` (instrumented).

## Verification

- `./gradlew testDebugUnitTest` green.
- `./gradlew :app:connectedDebugAndroidTest` green on the headless emulator, one class at
  a time.
- Screenshots of the metronome tab in English and Croatian, light and dark, with the wheel
  open, and after switching language on the emulator while the metronome plays.
