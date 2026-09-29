package com.nova.offline;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * WebBridge - Secure JavaScript interface injected into Android WebView as 'window.NOVA'.
 * Connects the web UI to LocalAI, ModelManager, ChatDatabase, and Android system services.
 */
public class WebBridge {

    private static final String TAG = "NOVA_WebBridge";
    private static final String PREFS_NAME = "nova_offline_settings";

    private final Activity mActivity;
    private final WebView mWebView;
    private final LocalAI mLocalAI;
    private final ModelManager mModelManager;
    private final ChatDatabase mChatDatabase;
    private final SharedPreferences mPrefs;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private String mCurrentSessionId = null;

    public interface ModelImportTrigger {
        void triggerModelFilePicker();
    }
    private ModelImportTrigger mImportTrigger;

    public WebBridge(Activity activity, WebView webView, LocalAI localAI, ModelManager modelManager, ChatDatabase chatDatabase) {
        this.mActivity = activity;
        this.mWebView = webView;
        this.mLocalAI = localAI;
        this.mModelManager = modelManager;
        this.mChatDatabase = chatDatabase;
        this.mPrefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Attempt initial model check
        checkAndLoadCurrentModel();
    }

    public void setModelImportTrigger(ModelImportTrigger trigger) {
        this.mImportTrigger = trigger;
    }

    public void checkAndLoadCurrentModel() {
        File modelFile = mModelManager.getActiveModelFile();
        if (modelFile != null && modelFile.exists()) {
            int ctxLen = mPrefs.getInt("context_length", 2048);
            mLocalAI.loadModel(modelFile.getAbsolutePath(), ctxLen, 4);
        }
    }

    // =========================================================================
    // JavaScript Interface Methods (called from web UI via window.NOVA.*)
    // =========================================================================

    @JavascriptInterface
    public void generate(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            notifyError("Prompt cannot be empty.");
            return;
        }

        // Ensure session exists
        if (mCurrentSessionId == null) {
            String initialTitle = prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt;
            mCurrentSessionId = mChatDatabase.createSession(initialTitle);
        }

        // Persist User Message
        mChatDatabase.addMessage(mCurrentSessionId, "user", prompt.trim(), 0);

        // Load inference parameters from settings
        float temp = mPrefs.getFloat("temperature", 0.7f);
        int maxTokens = mPrefs.getInt("max_tokens", 512);
        String sysPrompt = mPrefs.getString("system_prompt",
                "You are NOVA OFFLINE, a private on-device AI assistant. Answer accurately, concisely, and format all code blocks cleanly.");

        // Create Assistant Message Placeholder in DB
        final String assistantMsgId = mChatDatabase.addMessage(mCurrentSessionId, "assistant", "", 0);

        final StringBuilder responseAccumulator = new StringBuilder();

        mLocalAI.generate(prompt, sysPrompt, temp, maxTokens, new LocalAI.GenerationCallback() {
            @Override
            public void onToken(String token) {
                responseAccumulator.append(token);
                evaluateJs("if(window.onNovaToken) window.onNovaToken(" + escapeJsString(token) + ", false, " + escapeJsString(assistantMsgId) + ");");
            }

            @Override
            public void onComplete(String fullResponse) {
                // Update final message in DB
                mChatDatabase.getWritableDatabase().execSQL(
                        "UPDATE " + ChatDatabase.TABLE_MESSAGES + " SET " + ChatDatabase.COL_MSG_CONTENT + " = ? WHERE " + ChatDatabase.COL_MSG_ID + " = ?",
                        new Object[]{fullResponse, assistantMsgId}
                );
                mChatDatabase.touchSession(mCurrentSessionId);

                evaluateJs("if(window.onNovaToken) window.onNovaToken(\"\", true, " + escapeJsString(assistantMsgId) + ");"
                        + "if(window.onNovaComplete) window.onNovaComplete(" + escapeJsString(fullResponse) + ", " + escapeJsString(assistantMsgId) + ");");
            }

            @Override
            public void onError(String error) {
                notifyError(error);
            }
        });
    }

    @JavascriptInterface
    public void stopGeneration() {
        mLocalAI.stopGeneration();
        evaluateJs("if(window.onNovaGenerationStopped) window.onNovaGenerationStopped();");
    }

    @JavascriptInterface
    public boolean isModelInstalled() {
        return mModelManager.isModelInstalled();
    }

    @JavascriptInterface
    public String getModelInfo() {
        JSONObject info = mModelManager.getModelInfo();
        try {
            info.put("backend", mLocalAI.getBackendName());
            info.put("statusMessage", mLocalAI.getStatusMessage());
            info.put("isLoaded", mLocalAI.isModelLoaded());
            info.put("isNativeReady", mLocalAI.isReady());
        } catch (JSONException ignored) {}
        return info.toString();
    }

    @JavascriptInterface
    public void importModel() {
        if (mImportTrigger != null) {
            mActivity.runOnUiThread(() -> mImportTrigger.triggerModelFilePicker());
        }
    }

    @JavascriptInterface
    public boolean deleteModel() {
        mLocalAI.unloadModel();
        boolean deleted = mModelManager.deleteCurrentModel();
        notifyModelStatusChanged();
        return deleted;
    }

    @JavascriptInterface
    public String getChatHistory() {
        JSONArray sessions = mChatDatabase.getAllSessions();
        return sessions.toString();
    }

    @JavascriptInterface
    public String getMessages(String sessionId) {
        JSONArray messages = mChatDatabase.getMessages(sessionId);
        return messages.toString();
    }

    @JavascriptInterface
    public String createNewChat(String title) {
        String newId = mChatDatabase.createSession(title);
        mCurrentSessionId = newId;
        return newId;
    }

    @JavascriptInterface
    public void setActiveSession(String sessionId) {
        this.mCurrentSessionId = sessionId;
    }

    @JavascriptInterface
    public String getActiveSession() {
        return mCurrentSessionId != null ? mCurrentSessionId : "";
    }

    @JavascriptInterface
    public void deleteChat(String sessionId) {
        mChatDatabase.deleteSession(sessionId);
        if (sessionId != null && sessionId.equals(mCurrentSessionId)) {
            mCurrentSessionId = null;
        }
    }

    @JavascriptInterface
    public void clearAllHistory() {
        mChatDatabase.clearAllHistory();
        mCurrentSessionId = null;
    }

    @JavascriptInterface
    public String getSettings() {
        JSONObject json = new JSONObject();
        try {
            json.put("mode", mPrefs.getString("mode", "offline"));
            json.put("temperature", mPrefs.getFloat("temperature", 0.7f));
            json.put("max_tokens", mPrefs.getInt("max_tokens", 512));
            json.put("context_length", mPrefs.getInt("context_length", 2048));
            json.put("system_prompt", mPrefs.getString("system_prompt",
                    "You are NOVA OFFLINE, a private on-device AI assistant. Answer accurately, concisely, and format all code blocks cleanly."));
        } catch (JSONException e) {
            Log.e(TAG, "Error packaging settings: " + e.getMessage());
        }
        return json.toString();
    }

    @JavascriptInterface
    public void saveSettings(String settingsJson) {
        try {
            JSONObject json = new JSONObject(settingsJson);
            SharedPreferences.Editor editor = mPrefs.edit();

            if (json.has("mode")) editor.putString("mode", json.getString("mode"));
            if (json.has("temperature")) editor.putFloat("temperature", (float) json.getDouble("temperature"));
            if (json.has("max_tokens")) editor.putInt("max_tokens", json.getInt("max_tokens"));
            if (json.has("context_length")) editor.putInt("context_length", json.getInt("context_length"));
            if (json.has("system_prompt")) editor.putString("system_prompt", json.getString("system_prompt"));

            editor.apply();

            // Re-apply context length if model is active
            checkAndLoadCurrentModel();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse settings JSON: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public void copyToClipboard(String text) {
        mActivity.runOnUiThread(() -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("NOVA OFFLINE", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Clipboard copy error: " + e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public String getAppInfo() {
        JSONObject info = new JSONObject();
        try {
            info.put("appName", "NOVA OFFLINE");
            info.put("version", "1.0");
            info.put("targetSdk", 35);
            info.put("minSdk", 24);
            info.put("deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
            info.put("androidVersion", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
            info.put("abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "arm64-v8a");
            info.put("storagePath", mModelManager.getModelsDir().getAbsolutePath());
        } catch (JSONException ignored) {}
        return info.toString();
    }

    @JavascriptInterface
    public void showToast(String message) {
        mActivity.runOnUiThread(() -> Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show());
    }

    // =========================================================================
    // Android -> JavaScript event dispatchers
    // =========================================================================

    public void notifyModelStatusChanged() {
        String info = getModelInfo();
        evaluateJs("if(window.onNovaModelStatusChanged) window.onNovaModelStatusChanged(" + escapeJsString(info) + ");");
    }

    public void notifyImportProgress(int percent, long bytesCopied, long totalBytes, String status) {
        evaluateJs("if(window.onNovaImportProgress) window.onNovaImportProgress(" + percent + ", " + bytesCopied + ", " + totalBytes + ", " + escapeJsString(status) + ");");
    }

    public void notifyImportComplete(boolean success, String message) {
        evaluateJs("if(window.onNovaImportComplete) window.onNovaImportComplete(" + success + ", " + escapeJsString(message) + ");");
        notifyModelStatusChanged();
    }

    public void notifyError(String error) {
        evaluateJs("if(window.onNovaError) window.onNovaError(" + escapeJsString(error) + ");");
    }

    private void evaluateJs(String script) {
        mMainHandler.post(() -> {
            if (mWebView != null) {
                mWebView.evaluateJavascript(script, null);
            }
        });
    }

    private static String escapeJsString(String s) {
        if (s == null) return "\"\"";
        return JSONObject.quote(s);
    }
}
