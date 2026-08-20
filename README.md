# 🎙️ Gemini Live Voice Assistant (Android) + Multi-Key Auto-Rotate

Native Android app jo **Gemini Live API** (voice-to-voice) use karta hai with **smart API key rotation**.

## ✨ Features

- 🎤 **Voice-to-voice** (no separate TTS/STT)
- 🔑 **Multi-key support** — Add unlimited API keys
- � **Auto-rotation** — Jab ek key ka limit khatam ho, automatically next key use hogi
- 📊 **Live stats** — Dekho kaunsi key active hai, kitni use hui
- 🌐 **Hindi + English** mix support
- 🎨 **Dark theme UI**

## 📁 Project Structure

```
GeminiLiveApp/
├── app/
│   ├── build.gradle.kts          (dependencies)
│   └── src/main/
│       ├── AndroidManifest.xml    (permissions + 2 activities)
│       ├── java/com/example/geminilive/
│       │   ├── MainActivity.kt           (home screen + mic)
│       │   ├── SettingsActivity.kt       (API key management)
│       │   ├── KeysAdapter.kt            (RecyclerView adapter)
│       │   ├── KeyManager.kt             (rotation + storage logic)
│       │   ├── GeminiLiveClient.kt       (WebSocket + auto-rotation)
│       │   └── AudioPlayer.kt            (speaker playback)
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml     (home screen)
│           │   ├── activity_settings.xml (settings UI)
│           │   ├── item_key.xml          (each key card)
│           │   └── dialog_add_key.xml    (add key form)
│           └── values/
│               ├── colors.xml
│               └── strings.xml
```

## 🚀 Setup

### Step 1: Android Studio mein khol
1. Android Studio install kar (laptop pe)
2. "Open" karke `GeminiLiveApp` folder select kar
3. Gradle sync hone de (1-2 min)

### Step 2: API Keys add kar (App ke andar)
1. App khol → ⚙️ Settings button tap kar
2. "Add API Key" tap kar
3. Apni Gemini API key paste kar (e.g. `AIzaSy...`)
4. Label de (e.g. "Personal Account", "Work Account")
5. Repeat for 2nd account

### Step 3: Use kar! 🎉
1. Back jaa home pe
2. 🎙 Mic tap kar → Permission allow kar
3. Bolke sawaal puch → Response awaaz mein aayega!

## 🔄 Auto-Rotation Logic

- **Round-robin**: Sab keys equally use hoti hain (least-recently-used pick hota hai)
- **Rate limit detect**: Jab 429 aata hai, key ko 1 min ke liye mark kar deta hai
- **Auto-fallback**: Next available key pe seamless switch
- **Visual feedback**: Screen pe dikhta hai "🔄 Rotated to Account 2"

## 🔑 Gemini API Key Kahan Se?

1. [Google AI Studio](https://aistudio.google.com/) → Sign in
2. "Get API Key" → Create new
3. Copy karke app mein paste kar

**Note**: Free tier mein Gemini Live API preview ke liye access chahiye — agar available nahi toh [waitlist join kar](https://aistudio.google.com/).

## ⚠️ Troubleshooting

| Error | Fix |
|-------|-----|
| 401 | API key galat — check kar |
| 403 | Live API access nahi hai account pe |
| 429 | Rate limit — auto-rotation handle karega |
| No sound | Mic permission grant kar, volume check kar |

## 🛠️ Build Issues?

- Gradle sync fail → "File → Invalidate Caches" kar
- Missing dependencies → Internet check kar
- Compile error → Sab files copy hui hain ya nahi verify kar

## 📱 Phone pe Test (bina laptop)

- Termux mein Android SDK install karke `gradle assembleDebug` chala sakta hai
- Ya phir **AIDE** app use kar (Play Store) — direct phone pe build hoga
