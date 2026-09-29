package com.nova.offline;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * NativeAI - Implements LocalAI using a JNI native interface to llama.cpp or GGUF runtime.
 * Provides a resilient bridge: connects to libnova_native.so when compiled,
 * while maintaining graceful diagnostics and simulated token streaming if the native
 * binary has not yet been built on the current target device ABI.
 */
public class NativeAI implements LocalAI {

    private static final String TAG = "NOVA_NativeAI";
    private static final String NATIVE_LIB_NAME = "nova_native";
    private static final String ALT_LIB_NAME = "llama-android";

    private static boolean sLibraryLoaded = false;
    private static String sLibraryLoadError = null;

    static {
        try {
            System.loadLibrary(NATIVE_LIB_NAME);
            sLibraryLoaded = true;
            Log.i(TAG, "Successfully loaded native library: " + NATIVE_LIB_NAME);
        } catch (UnsatisfiedLinkError e1) {
            try {
                System.loadLibrary(ALT_LIB_NAME);
                sLibraryLoaded = true;
                Log.i(TAG, "Successfully loaded alternative native library: " + ALT_LIB_NAME);
            } catch (UnsatisfiedLinkError e2) {
                sLibraryLoaded = false;
                sLibraryLoadError = e1.getMessage();
                Log.w(TAG, "Native GGUF libraries (" + NATIVE_LIB_NAME + " / " + ALT_LIB_NAME 
                        + ") not present in APK. Running in architectural fallback mode: " + e1.getMessage());
            }
        }
    }

    // Native JNI interface definitions for llama.cpp runtime
    private static native boolean nativeInit();
    private static native boolean nativeLoadModel(String modelPath, int contextLength, int nThreads);
    private static native void nativeUnloadModel();
    private static native void nativeGenerate(String prompt, String systemPrompt, float temperature, int maxTokens, NativeTokenCallback callback);
    private static native void nativeStop();

    public interface NativeTokenCallback {
        void onNativeToken(String token);
        void onNativeDone();
        void onNativeError(String error);
    }

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Future<?> mCurrentTask = null;
    private volatile boolean mIsCancelled = false;
    private boolean mIsModelLoaded = false;
    private String mLoadedModelPath = null;
    private int mContextLength = 2048;

    public NativeAI() {
        if (sLibraryLoaded) {
            try {
                nativeInit();
            } catch (Throwable t) {
                Log.e(TAG, "nativeInit failed: " + t.getMessage(), t);
            }
        }
    }

    @Override
    public boolean isReady() {
        return sLibraryLoaded;
    }

    @Override
    public boolean isModelLoaded() {
        return mIsModelLoaded;
    }

    @Override
    public synchronized boolean loadModel(String modelPath, int contextLength, int nThreads) {
        if (modelPath == null || !new File(modelPath).exists()) {
            Log.e(TAG, "Model file not found: " + modelPath);
            mIsModelLoaded = false;
            mLoadedModelPath = null;
            return false;
        }

        mContextLength = contextLength > 0 ? contextLength : 2048;

        if (sLibraryLoaded) {
            try {
                boolean success = nativeLoadModel(modelPath, mContextLength, nThreads > 0 ? nThreads : 4);
                mIsModelLoaded = success;
                if (success) {
                    mLoadedModelPath = modelPath;
                }
                return success;
            } catch (Throwable t) {
                Log.e(TAG, "nativeLoadModel error: " + t.getMessage(), t);
                mIsModelLoaded = false;
                return false;
            }
        } else {
            // Emulated readiness: file exists and has been validated by ModelManager
            mIsModelLoaded = true;
            mLoadedModelPath = modelPath;
            Log.i(TAG, "Model registered at: " + modelPath + " (Native lib unlinked, emulated mode active)");
            return true;
        }
    }

    @Override
    public synchronized void unloadModel() {
        if (sLibraryLoaded && mIsModelLoaded) {
            try {
                nativeUnloadModel();
            } catch (Throwable t) {
                Log.e(TAG, "nativeUnloadModel error: " + t.getMessage(), t);
            }
        }
        mIsModelLoaded = false;
        mLoadedModelPath = null;
    }

    @Override
    public void generate(String prompt, String systemPrompt, float temperature, int maxTokens, GenerationCallback callback) {
        stopGeneration();
        mIsCancelled = false;

        mCurrentTask = mExecutor.submit(() -> {
            if (sLibraryLoaded && mIsModelLoaded) {
                // Execute real native inference through llama.cpp JNI bindings
                try {
                    StringBuilder fullResponse = new StringBuilder();
                    nativeGenerate(prompt, systemPrompt, temperature, maxTokens, new NativeTokenCallback() {
                        @Override
                        public void onNativeToken(String token) {
                            if (!mIsCancelled) {
                                fullResponse.append(token);
                                mMainHandler.post(() -> callback.onToken(token));
                            }
                        }

                        @Override
                        public void onNativeDone() {
                            if (!mIsCancelled) {
                                mMainHandler.post(() -> callback.onComplete(fullResponse.toString()));
                            }
                        }

                        @Override
                        public void onNativeError(String error) {
                            mMainHandler.post(() -> callback.onError(error));
                        }
                    });
                } catch (Throwable t) {
                    Log.e(TAG, "nativeGenerate execution failed: " + t.getMessage(), t);
                    mMainHandler.post(() -> callback.onError("Native inference error: " + t.getMessage()));
                }
            } else {
                // Architectural demonstration & fallback engine
                // Allows UI token streaming, testing, and full app functionality
                runArchitecturalFallback(prompt, systemPrompt, temperature, maxTokens, callback);
            }
        });
    }

    private void runArchitecturalFallback(String prompt, String systemPrompt, float temperature, int maxTokens, GenerationCallback callback) {
        StringBuilder fullText = new StringBuilder();

        String statusNotice;
        if (!mIsModelLoaded) {
            statusNotice = "> **[NOVA OFFLINE Notice]**: No `.gguf` model file is currently active in memory.\n"
                    + "> Go to the **Model Manager** tab to import a 1–2 GB GGUF model (e.g. TinyLlama, Qwen2.5, Phi-2) into app storage.\n\n";
        } else if (!sLibraryLoaded) {
            statusNotice = "> **[NOVA OFFLINE Native Status]**: Model file detected (`" 
                    + (mLoadedModelPath != null ? new File(mLoadedModelPath).getName() : "model.gguf") 
                    + "`), but native JNI shared object (`libnova_native.so`) is not yet compiled into this APK build.\n"
                    + "> *To compile full C++ llama.cpp inference, build `app/src/main/cpp/nova_native.cpp` with Android NDK.* (See `README.md`)\n\n";
        } else {
            statusNotice = "";
        }

        String intelligentResponse = generateFallbackAnswer(prompt);
        String completeOutput = statusNotice + intelligentResponse;

        // Stream tokens realistically (word by word with slight delay)
        String[] words = completeOutput.split("(?<=\\s)|(?<=\\n)");
        for (String word : words) {
            if (mIsCancelled || Thread.currentThread().isInterrupted()) {
                Log.d(TAG, "Generation cancelled by user.");
                return;
            }

            fullText.append(word);
            mMainHandler.post(() -> callback.onToken(word));

            try {
                // Approximate 25-45ms per token streaming cadence
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Log.d(TAG, "Generation thread interrupted.");
                return;
            }
        }

        mMainHandler.post(() -> callback.onComplete(fullText.toString()));
    }

    private String generateFallbackAnswer(String prompt) {
        String p = prompt.trim().toLowerCase();

        if (p.contains("hello") || p.contains("hi") || p.contains("hey")) {
            return "Hello! I am **NOVA OFFLINE**, your private on-device AI assistant.\n\n"
                    + "I operate completely locally without sending any data over the internet. You can ask me questions, request code snippets, or manage your offline models directly from the **Model Manager**.";
        } else if (p.contains("code") || p.contains("python") || p.contains("script")) {
            return "Here is a clean Python example showcasing offline text processing and tokenization:\n\n"
                    + "```python\n"
                    + "# Offline Token Processing Example\n"
                    + "import sys\n\n"
                    + "def process_local_stream(prompt: str, max_tokens: int = 512):\n"
                    + "    print(f\"Processing prompt locally: '{prompt}'\")\n"
                    + "    tokens = prompt.split()\n"
                    + "    for i, token in enumerate(tokens[:max_tokens]):\n"
                    + "        yield f\"{token} \"\n\n"
                    + "for t in process_local_stream(\"" + escapeForMarkdown(prompt) + "\"):\n"
                    + "    sys.stdout.write(t)\n"
                    + "    sys.stdout.flush()\n"
                    + "print(\"\\n[Offline stream completed]\")\n"
                    + "```\n\n"
                    + "This script runs without external network dependencies, mirroring our on-device architecture.";
        } else if (p.contains("model") || p.contains("gguf") || p.contains("llama") || p.contains("install")) {
            return "### About GGUF Models & Offline Inference\n\n"
                    + "NOVA OFFLINE supports quantized **GGUF** format models tailored for low-RAM mobile devices:\n\n"
                    + "1. **TinyLlama 1.1B Chat (Q4_K_M)** — ~670 MB (Minimal RAM footprint, 4GB phone friendly)\n"
                    + "2. **Qwen2.5 1.5B Instruct (Q4_K_M)** — ~1.1 GB (Superb reasoning & coding)\n"
                    + "3. **Phi-2 2.7B (Q4_K_M)** — ~1.6 GB (High knowledge density)\n\n"
                    + "To load your model, navigate to **Model Manager** and click **Import GGUF Model**.";
        } else if (p.contains("who are you") || p.contains("about")) {
            return "I am **NOVA OFFLINE** (v1.0), an autonomous on-device AI chatbot designed for Android.\n\n"
                    + "- **Privacy:** Zero network requests, 100% on-device processing.\n"
                    + "- **Format:** GGUF (Quantized 4-bit / 8-bit).\n"
                    + "- **Engine:** llama.cpp native inference layer with clean Java abstraction.\n"
                    + "- **UI:** Hardware-accelerated WebView interface with SQLite history persistence.";
        } else {
            return "Here is what you should know about **" + escapeForMarkdown(prompt) + "**:\n\n"
                    + "1. **Core Concept:** Offline execution guarantees complete privacy and deterministic speed independent of network connectivity.\n"
                    + "2. **Edge Architecture:** By leveraging 4-bit quantization (GGUF Q4_K_M), multi-billion parameter models fit inside 1–2 GB of RAM on typical 4GB Android smartphones.\n"
                    + "3. **Next Steps:** Open the **Settings** menu to adjust generation parameters like Temperature, Max Tokens, and System Prompt to fine-tune responses.";
        }
    }

    private String escapeForMarkdown(String text) {
        if (text == null) return "";
        return text.replace("`", "'").replace("\n", " ");
    }

    @Override
    public synchronized void stopGeneration() {
        mIsCancelled = true;
        if (sLibraryLoaded && mIsModelLoaded) {
            try {
                nativeStop();
            } catch (Throwable t) {
                Log.e(TAG, "nativeStop error: " + t.getMessage(), t);
            }
        }
        if (mCurrentTask != null && !mCurrentTask.isDone()) {
            mCurrentTask.cancel(true);
        }
    }

    @Override
    public String getBackendName() {
        if (sLibraryLoaded && mIsModelLoaded) {
            return "llama.cpp Native JNI (Active GGUF)";
        } else if (sLibraryLoaded) {
            return "llama.cpp Native JNI (Ready, No Model)";
        } else if (mIsModelLoaded) {
            return "NOVA Fallback Engine (Model Installed, Native Lib Unlinked)";
        } else {
            return "NOVA Local Engine (Standby)";
        }
    }

    @Override
    public String getStatusMessage() {
        if (sLibraryLoaded && mIsModelLoaded) {
            return "Native runtime loaded. Model: " + (mLoadedModelPath != null ? new File(mLoadedModelPath).getName() : "Active");
        } else if (sLibraryLoaded) {
            return "Native runtime loaded. Waiting for GGUF model import.";
        } else if (mIsModelLoaded) {
            return "GGUF model present. Native JNI library awaiting compilation.";
        } else {
            return "Ready. Import a GGUF model in Model Manager.";
        }
    }
}
