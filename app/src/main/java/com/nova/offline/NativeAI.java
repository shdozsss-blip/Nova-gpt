package com.nova.offline;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class NativeAI implements LocalAI {

    private static final String TAG = "NOVA_NativeAI";

    private static boolean sLibraryLoaded = false;
    private static boolean sNativeInitialized = false;

    static {
        try {
            System.loadLibrary("nova_native");
            sLibraryLoaded = true;
            Log.i(TAG, "nova_native library loaded.");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load nova_native.", e);

            try {
                System.loadLibrary("llama-android");
                sLibraryLoaded = true;
                Log.i(TAG, "llama-android library loaded.");
            } catch (UnsatisfiedLinkError e2) {
                Log.e(TAG, "No native AI library available.", e2);
                sLibraryLoaded = false;
            }
        }
    }

    private boolean mIsModelLoaded = false;
    private String mModelPath = null;

    private int mContextLength = 2048;
    private int mThreads = 4;

    private final AtomicBoolean mGenerating =
            new AtomicBoolean(false);

    public interface NativeTokenCallback {
        void onNativeToken(String token);
        void onNativeDone();
        void onNativeError(String error);
    }

    private static native boolean nativeInit();

    private static native boolean nativeLoadModel(
            String modelPath,
            int contextLength,
            int nThreads
    );

    private static native void nativeUnloadModel();

    private static native void nativeGenerate(
            String prompt,
            String systemPrompt,
            float temperature,
            int maxTokens,
            NativeTokenCallback callback
    );

    private static native void nativeStop();


    public NativeAI() {

        if (sLibraryLoaded && !sNativeInitialized) {

            try {
                sNativeInitialized = nativeInit();

                Log.i(
                        TAG,
                        "Native initialization: "
                                + sNativeInitialized
                );

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Native initialization failed.",
                        e
                );

                sNativeInitialized = false;
            }
        }
    }


    @Override
    public boolean loadModel(
            String modelPath,
            int contextLength,
            int nThreads
    ) {

        if (modelPath == null || modelPath.trim().isEmpty()) {

            Log.e(TAG, "Model path is empty.");

            return false;
        }

        mModelPath = modelPath;

        mContextLength =
                contextLength > 0
                        ? contextLength
                        : 2048;

        mThreads =
                nThreads > 0
                        ? nThreads
                        : 4;


        if (!sLibraryLoaded || !sNativeInitialized) {

            Log.e(
                    TAG,
                    "Native backend is not available."
            );

            mIsModelLoaded = false;

            return false;
        }


        try {

            Log.i(
                    TAG,
                    "Loading model: " + modelPath
            );

            Log.i(
                    TAG,
                    "Context: " + mContextLength
            );

            Log.i(
                    TAG,
                    "Threads: " + mThreads
            );


            boolean success = nativeLoadModel(
                    modelPath,
                    mContextLength,
                    mThreads
            );


            mIsModelLoaded = success;


            if (success) {

                Log.i(
                        TAG,
                        "Model loaded successfully."
                );

            } else {

                Log.e(
                        TAG,
                        "Model loading failed."
                );
            }


            return success;

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Exception while loading model.",
                    e
            );

            mIsModelLoaded = false;

            return false;
        }
    }


    @Override
    public void unloadModel() {

        try {

            if (sLibraryLoaded && sNativeInitialized) {
                nativeUnloadModel();
            }

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Error unloading model.",
                    e
            );
        }

        mIsModelLoaded = false;
        mModelPath = null;
    }


    @Override
    public boolean isModelLoaded() {
        return mIsModelLoaded;
    }


    @Override
    public void generate(
            String prompt,
            String systemPrompt,
            float temperature,
            int maxTokens,
            TokenCallback callback
    ) {

        if (callback == null) {
            Log.e(TAG, "TokenCallback is null.");
            return;
        }


        if (prompt == null) {
            prompt = "";
        }

        if (systemPrompt == null) {
            systemPrompt = "";
        }


        if (temperature <= 0) {
            temperature = 0.7f;
        }

        if (maxTokens <= 0) {
            maxTokens = 256;
        }


        if (!sLibraryLoaded ||
                !sNativeInitialized ||
                !mIsModelLoaded) {

            callback.onError(
                    "Native AI model is not loaded."
            );

            return;
        }


        if (!mGenerating.compareAndSet(
                false,
                true
        )) {

            callback.onError(
                    "Generation is already running."
            );

            return;
        }


        final String finalPrompt = prompt;
        final String finalSystemPrompt = systemPrompt;
        final float finalTemperature = temperature;
        final int finalMaxTokens = maxTokens;


        new Thread(() -> {

            try {

                NativeTokenCallback nativeCallback =
                        new NativeTokenCallback() {

                            @Override
                            public void onNativeToken(
                                    String token
                            ) {

                                if (token == null) {
                                    return;
                                }

                                callback.onToken(token);
                            }


                            @Override
                            public void onNativeDone() {

                                mGenerating.set(false);

                                callback.onComplete();
                            }


                            @Override
                            public void onNativeError(
                                    String error
                            ) {

                                mGenerating.set(false);

                                callback.onError(
                                        error != null
                                                ? error
                                                : "Unknown native error."
                                );
                            }
                        };


                nativeGenerate(
                        finalPrompt,
                        finalSystemPrompt,
                        finalTemperature,
                        finalMaxTokens,
                        nativeCallback
                );

            } catch (Throwable e) {

                mGenerating.set(false);

                Log.e(
                        TAG,
                        "Generation failed.",
                        e
                );

                callback.onError(
                        "Native generation failed: "
                                + e.getMessage()
                );
            }

        }).start();
    }


    @Override
    public void stopGeneration() {

        try {

            if (sLibraryLoaded &&
                    sNativeInitialized) {

                nativeStop();
            }

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Error stopping generation.",
                    e
            );
        }

        mGenerating.set(false);
    }


    @Override
    public String getBackendName() {

        if (sLibraryLoaded &&
                sNativeInitialized) {

            return "NOVA Native llama.cpp";
        }

        return "Native backend unavailable";
    }


    @Override
    public String getModelPath() {
        return mModelPath;
    }


    @Override
    public int getContextLength() {
        return mContextLength;
    }


    @Override
    public boolean isGenerating() {
        return mGenerating.get();
    }
}