package com.nova.offline;

/**
 * LocalAI - Interface defining the local on-device AI inference engine contract.
 * Allows decoupling the WebView UI and bridge from the underlying native runtime
 * (such as llama.cpp, ggml, or custom native bindings).
 */
public interface LocalAI {

    /**
     * Callback interface for streaming token generation and status notifications.
     */
    interface GenerationCallback {
        /**
         * Called when a new token has been generated.
         * @param token Text token produced by the model
         */
        void onToken(String token);

        /**
         * Called when generation has completed successfully.
         * @param fullResponse Complete generated text
         */
        void onComplete(String fullResponse);

        /**
         * Called when an error occurs during inference.
         * @param error Descriptive error message
         */
        void onError(String error);
    }

    /**
     * Check if the runtime engine is ready (native libraries initialized).
     */
    boolean isReady();

    /**
     * Check if a GGUF model is currently loaded in memory.
     */
    boolean isModelLoaded();

    /**
     * Load a GGUF model into memory.
     * @param modelPath Absolute file path to the .gguf model file
     * @param contextLength Context window size (e.g. 2048, 4096)
     * @param nThreads Number of CPU inference threads to utilize
     * @return true if loaded successfully, false otherwise
     */
    boolean loadModel(String modelPath, int contextLength, int nThreads);

    /**
     * Unload the currently loaded model and free memory.
     */
    void unloadModel();

    /**
     * Generate response for the given user prompt.
     * @param prompt User query
     * @param systemPrompt Instructions/persona for the model
     * @param temperature Sampling temperature (0.1 - 1.5)
     * @param maxTokens Maximum number of tokens to generate
     * @param callback Token and completion handler
     */
    void generate(String prompt, String systemPrompt, float temperature, int maxTokens, GenerationCallback callback);

    /**
     * Stop generation if inference is currently active.
     */
    void stopGeneration();

    /**
     * Returns the name of the active inference backend.
     */
    String getBackendName();

    /**
     * Returns a human-readable status message regarding engine readiness and model state.
     */
    String getStatusMessage();
}
