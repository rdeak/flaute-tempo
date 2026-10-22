# Play Console answers for Flute Assistant

Facts the answers rest on: the app runs entirely on the device. The microphone is used only
while the Tuner tab is open; audio is analysed in memory for pitch and never stored, logged or
transmitted. There is no account, no network access, no analytics, no ads, no SDK that
collects data. Presets and settings are stored locally on the device.

## Store listing

| Field | Value |
|---|---|
| App name | Flute Assistant |
| Short description | Metronome and chromatic tuner for flute practice. |
| Full description | See below |
| App icon | `play-listing/icon-512.png` (512x512) |
| Feature graphic | `play-listing/feature-graphic-1024x500.png` |
| Screenshots | At least 2 phone screenshots (metronome, tuner); 16:9 or 9:16, 320 to 3840 px |
| Category | Music & Audio |
| Contact email | Required; shown publicly |
| Privacy policy URL | Required; host `privacy-policy.md` (for example on GitHub Pages) |

Full description draft:

> A focused practice companion for flutists. The metronome runs from 30 to 300 BPM with tap
> tempo, 2/4, 3/4, 4/4, 5/4 and 6/8 meters, eighth, sixteenth, triplet and shuffle subdivisions,
> three click sounds and a beat flasher synced to the audio. Save your favourite settings as
> presets. The chromatic tuner shows the note and cents on a needle gauge with an adjustable
> A4 reference (415 to 466 Hz). The metronome keeps running while you read sheet music in
> another app, with a notification to stop it. No account, no ads, no data leaves your device.

## App content

### Privacy policy
Required. Link to the hosted copy of `privacy-policy.md`.

### Ads
Does the app contain ads? **No.**

### App access
No login or restricted parts. **All functionality is available without special access.**

### Content rating (IARC questionnaire)
Category: **Utility, Productivity, Communication, or Other**. Answer No to every content
question (violence, sexuality, language, controlled substances, gambling, user interaction,
location sharing, purchases). Expected rating: Everyone / PEGI 3.

### Target audience and content
Target age group: **18 and over** (13+ is also defensible). The app is not designed for
children, so the Families policy does not apply. Could it unintentionally appeal to children?
**No.**

### News app
**No.**

### Data safety

- Does your app collect or share any of the required user data types? **No.**
  Play defines collection as transmitting data off the device. Microphone audio is processed
  on the device in memory and discarded; nothing is stored or sent, so no data type is
  collected or shared.
- Encryption in transit and deletion requests are not asked when nothing is collected.
  Uninstalling removes presets and settings.

Resulting public label: "No data collected" and "No data shared with third parties".

### Government apps
**No.**

### Financial features
**None** (no financial features).

### Health
**No health features.**

### Advertising ID
Does your app use advertising ID? **No.** The manifest does not declare
`com.google.android.gms.permission.AD_ID` and no ads SDK is included.

### Foreground service permissions (declaration for Android 14+)

The app declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.

- Foreground service type: **Media playback**.
- Description to enter: "The metronome keeps clicking while the screen is off or another app
  (for example sheet music) is in front. A notification shows the current tempo and offers a
  Stop action. The service runs only while the metronome is playing and stops itself when
  playback ends."
- A link to a short video demonstrating the feature is required: record starting the
  metronome, pressing Home, showing the notification, and stopping from it.

### Photo and video permissions
Not requested. **No.**

## Permissions declared in the manifest

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Chromatic tuner (runtime permission, requested when the Tuner tab opens) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Metronome keeps playing in the background |
| `POST_NOTIFICATIONS` | Playback notification with Stop (runtime permission on Android 13+) |

None of these needs a separate sensitive-permission declaration form, apart from the
foreground service type above.

## Testing requirement for newer personal developer accounts

Personal accounts created after November 2023 must run a closed test with at least 12 testers
for 14 days before applying for production access. Plan the closed track first.
