# 🐱 Floppa Store (Android)

An open-source Android App Store and Private Playtest Publishing Platform featuring:
- **Dual App Store Security**: Integrated **FloppaSecurity** (Threat Database of 72,000+ signatures & Bytecode Heuristics) and **VirusTotal** (72 cloud antivirus vendor engines).
- **Developer Hub**: APK Upload & Publishing Studio, Private Playtesting links & beta codes, real developer analytics.
- **Floppa AI Store Assistant**: Intelligent recommendations and device compatibility audit.
- **Wi-Fi Download Guard**: Automatic detection requiring Wi-Fi for heavy downloads.

---

## 🚀 GitHub Actions CI/CD (Compile & Publish)

This repository includes automated GitHub Actions workflows in `.github/workflows/`:

1. **`build_and_publish.yml`**:
   - **Automatic on Git Tags**: Pushing any tag like `v1.0.0` will automatically compile the APK, compute cryptographic SHA-256 hashes, and publish a new **GitHub Release** with the downloadable `floppa-store-debug.apk` attached.
   - **Manual Trigger (`workflow_dispatch`)**: Go to **Actions** → **Build and Publish Floppa Store** → **Run workflow**, choose `debug` or `release`, and trigger an immediate build.
   - **Artifacts**: Every build uploads the compiled APK to GitHub Action Run Artifacts (retained for 30 days).

2. **`ci.yml`**:
   - Compiles and verifies the Android codebase on every pull request and push to `main`/`master`.

---

## ⚙️ Enabling GitHub Releases Publishing

By default, newly created GitHub repositories grant workflows "Read-only" permissions. To allow GitHub Actions to automatically publish releases:

1. Open your repository on GitHub.
2. Go to **Settings** → **Actions** → **General**.
3. Scroll down to **Workflow permissions**.
4. Select **"Read and write permissions"** and click **Save**.
5. The compiled APK will now automatically be published to the **Releases** section on every build! (The APK is also always available immediately in the **Artifacts** section of each workflow run).

---

## 🛠️ Local Compilation

To build locally from the terminal:

```bash
# Decode the debug keystore
base64 -d debug.keystore.base64 > debug.keystore

# Compile the Debug APK
gradle assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔐 Optional Release Keystore Configuration

To sign release builds in GitHub Actions, add these repository secrets in **Settings** → **Secrets and variables** → **Actions**:

- `KEYSTORE_BASE64`: Base64 encoded release keystore file (`.jks` or `.keystore`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key alias password
