# NOVA OFFLINE (v1.0)
### Standalone On-Device GGUF AI Chatbot for Android

NOVA OFFLINE is a 100% private, edge-computing AI chatbot engineered to run quantized Large Language Models (LLMs) locally on Android smartphones, including devices with only 4 GB RAM.

---

## Architecture

```
┌───────────────────────────────────────────┐
│     HTML5 / CSS / Vanilla JavaScript      │
│  (Modern Dark Theme, Markdown, Streaming) │
└─────────────────────┬─────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────┐
│    Android Hardware-Accelerated WebView   │
└─────────────────────┬─────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────┐
│   Java Bridge (window.NOVA / WebBridge)   │
└─────────────────────┬─────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────┐
│              LocalAI Layer                │
│    (Decoupled Interface for Inference)    │
└─────────────────────┬─────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────┐
│           NativeAI (JNI / C++)            │
│       (libnova_native.so / llama.cpp)     │
└─────────────────────┬─────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────┐
│    Quantized GGUF Model (~1–2 GB in RAM)  │
│  (/data/data/com.nova.offline/files/models)│
└───────────────────────────────────────────┘
```

---

## Features

- **100% Offline AI Execution:** No cloud servers, no telemetry, no tracking. Certified for Airplane Mode.
- **Low RAM Optimization (4GB Phones):** Tuned for 4-bit (Q4_K_M) models requiring ~600MB to 1.6GB RAM.
- **Model Manager:**
  - Import `.gguf` weights via Android Storage Access Framework.
  - Live progress tracking during file transfer.
  - Verification of the 4-byte `GGUF` magic header signature.
  - Model deletion and storage reporting.
- **Lightweight SQLite Database:** Persistent multi-session conversation history and message timestamps.
- **Streaming Response Architecture:** Real-time token delivery to the UI with animated cursor and stop button.
- **Full Markdown Rendering:** Headings, blockquotes, lists, bold/italics, and syntax-tagged code blocks with copy-to-clipboard buttons.

---

## Recommended Models

Download any of the following quantized GGUF models (e.g. from Hugging Face):

1. **TinyLlama 1.1B Chat (Q4_K_M)** — `~670 MB`  
   *Best for:* 2GB–4GB RAM phones, ultra-fast generation.
2. **Qwen2.5 1.5B Instruct (Q4_K_M)** — `~1.1 GB`  
   *Best for:* High quality logic, coding, and general instruction following.
3. **Phi-2 2.7B (Q4_K_M)** — `~1.6 GB`  
   *Best for:* Dense reasoning tasks on 4GB–6GB RAM devices.

Transfer the `.gguf` file to your device, open **Model Manager** in the app, and tap **Import GGUF Model**.

---

## Building Native GGUF Runtime (llama.cpp) with Android NDK

NOVA OFFLINE includes a clean abstraction layer in `LocalAI.java` and `NativeAI.java`, along with ready-to-build JNI bindings in `app/src/main/cpp/`.

### Option A: Build with CMake in Android Studio
1. Open the project in Android Studio.
2. Ensure Android NDK (r25c or higher) and CMake are installed via SDK Manager.
3. In `app/build.gradle`, uncomment the `externalNativeBuild` block:
   ```groovy
   externalNativeBuild {
       cmake {
           path file('src/main/cpp/CMakeLists.txt')
           version '3.22.1'
       }
   }
   ```
4. Build the APK. Gradle will automatically compile `libnova_native.so` for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

### Option B: Precompiled llama.cpp libraries
Alternatively, if you already build `libllama.so` or `libnova_native.so` using the official `llama.cpp` Android scripts:
Place the `.so` files in:
```
app/src/main/jniLibs/
  ├── arm64-v8a/libnova_native.so
  ├── armeabi-v7a/libnova_native.so
  └── x86_64/libnova_native.so
```
NOVA OFFLINE will automatically detect and link the native shared objects on boot.

---

## Project Structure

```
NOVA_OFFLINE/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── README.md
└── app/
    ├── build.gradle
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── cpp/
            │   ├── CMakeLists.txt
            │   └── nova_native.cpp
            ├── java/
            │   └── com/
            │       └── nova/
            │           └── offline/
            │               ├── MainActivity.java
            │               ├── LocalAI.java
            │               ├── NativeAI.java
            │               ├── ModelManager.java
            │               ├── ChatDatabase.java
            │               └── WebBridge.java
            ├── assets/
            │   └── web/
            │       ├── index.html
            │       ├── style.css
            │       ├── app.js
            │       └── settings.js
            └── res/
                ├── layout/
                │   └── activity_main.xml
                ├── values/
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                ├── drawable/
                └── mipmap/
```

---

## Opening in Android Studio

1. Extract `NOVA_OFFLINE.zip`.
2. Launch Android Studio.
3. Select **File > Open...** and choose the `NOVA_OFFLINE` folder.
4. Allow Gradle Sync to finish.
5. Select a target Android device or emulator (API 24+) and click **Run (Shift+F10)**.
