# Releasing Flute Assistant

Application id `hr.bertlab.fluteassistant`. Play requires an Android App Bundle (AAB) signed
with the **upload key**; Google holds the app signing key (Play App Signing).

## 1. Version

Edit `gradle.properties`:

```
fluteAssistant.versionCode=1      # must increase on every upload
fluteAssistant.versionName=1.0.0  # shown to people
```

CI can override both: `./gradlew bundleRelease -PfluteAssistant.versionCode=42`.

## 2. Signing credentials

`app/build.gradle.kts` signs the release build when it finds credentials, in this order:

1. Environment variables `FLUTE_ASSISTANT_STORE_FILE`, `FLUTE_ASSISTANT_STORE_PASSWORD`,
   `FLUTE_ASSISTANT_KEY_ALIAS` (default `upload`), `FLUTE_ASSISTANT_KEY_PASSWORD`
   (defaults to the store password).
2. `keystore.properties` in the project root (gitignored): `storeFile`, `storePassword`,
   `keyAlias`, `keyPassword`.

Without either, the release build is produced **unsigned** so the project still compiles anywhere.

The upload keystore lives at `keystore/flute-assistant-upload.jks` (PKCS12, RSA 2048, alias
`upload`, valid until 2054). Both the keystore directory and `keystore.properties` are
gitignored. Never commit them and never paste the password into a chat or a ticket.

### Backup in AWS Secrets Manager

Keep the passwords and the keystore file itself in the secret
`flute-assistant/upload-keystore` in `eu-west-1` as JSON:

```
{"storePassword": "...", "keyPassword": "...", "keyAlias": "upload",
 "keystoreFileName": "flute-assistant-upload.jks", "keystoreBase64": "..."}
```

Create or refresh the backup from a machine that has the files. The password is read from the
properties file and never typed or displayed:

```sh
python3 - <<'PY' > /tmp/upload-keystore.json
import base64, json, pathlib
props = dict(l.split("=", 1) for l in pathlib.Path("keystore.properties").read_text().splitlines()
             if "=" in l and not l.startswith("#"))
print(json.dumps({"storePassword": props["storePassword"], "keyPassword": props["keyPassword"],
                  "keyAlias": props["keyAlias"], "keystoreFileName": "flute-assistant-upload.jks",
                  "keystoreBase64": base64.b64encode(pathlib.Path(props["storeFile"]).read_bytes()).decode()}))
PY
aws secretsmanager create-secret --region eu-west-1 --name flute-assistant/upload-keystore \
  --description "Android upload keystore for Flute Assistant: passwords, alias and the PKCS12 file as base64. Static credential, no rotation." \
  --secret-string file:///tmp/upload-keystore.json \
  --tags Key=Application,Value=FluteAssistant Key=Purpose,Value=android-upload-keystore
rm /tmp/upload-keystore.json
```

Use `put-secret-value` instead of `create-secret` to refresh an existing secret.

Restore on a new machine:

```sh
aws secretsmanager get-secret-value --region eu-west-1 --secret-id flute-assistant/upload-keystore \
  --query SecretString --output text | python3 -c '
import base64, json, os, pathlib, sys
s = json.load(sys.stdin); os.umask(0o077); pathlib.Path("keystore").mkdir(exist_ok=True)
pathlib.Path("keystore/" + s["keystoreFileName"]).write_bytes(base64.b64decode(s["keystoreBase64"]))
lines = ["storeFile=keystore/" + s["keystoreFileName"], "storePassword=" + s["storePassword"],
         "keyAlias=" + s["keyAlias"], "keyPassword=" + s["keyPassword"]]
pathlib.Path("keystore.properties").write_text("\n".join(lines) + "\n")'
```

With a secret-resolving launcher such as `asm-exec`, skip the properties file and inject the
environment variables at build time instead:

```sh
asm-exec \
  FLUTE_ASSISTANT_STORE_FILE=keystore/flute-assistant-upload.jks \
  FLUTE_ASSISTANT_STORE_PASSWORD='{{resolve:secretsmanager:flute-assistant/upload-keystore:SecretString:storePassword}}' \
  FLUTE_ASSISTANT_KEY_PASSWORD='{{resolve:secretsmanager:flute-assistant/upload-keystore:SecretString:keyPassword}}' \
  -- ./gradlew bundleRelease
```

If the upload key is ever lost, Play Console lets you request an upload key reset
(Setup > App signing); the app signing key held by Google is unaffected.

## 3. Build and verify

```sh
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab (upload this)
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk (sideload testing)
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

The certificate SHA-256 printed by `apksigner` must match the upload certificate shown in
Play Console > Setup > App signing after the first upload.

Before uploading, run the full check: `./gradlew testDebugUnitTest lintRelease bundleRelease`.

## 4. Play Console

See `play-console.md` for the store listing, data safety, permission and content rating
answers, and `privacy-policy.md` for the policy text to host.

## 5. Launcher icon, store graphics and screenshots

The icon is a vector adaptive icon (`res/drawable/ic_launcher_*.xml`) plus legacy PNGs for
Android 7 (`res/mipmap-*/`). `tools/IconGen.java` renders the PNGs, the 512 px Play icon and the
1024x500 feature graphic from the same geometry:

```sh
java -Djava.awt.headless=true tools/IconGen.java /tmp/icons
```

Phone screenshots come from the emulator: with the AVD running (`-no-audio`, see CLAUDE.md) and
a debug APK built, `tools/play-screenshots.sh` drives the four tabs in English and Croatian and
writes one 1080x1920 JPEG per tab (Play asks for 9:16) into `docs/release/play-listing/screenshots/`.
Re-run it after any visible change before uploading.
