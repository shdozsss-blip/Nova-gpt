package com.nova.offline;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;

/**
 * MainActivity - Primary activity hosting the full-screen WebView and managing
 * the lifecycle of LocalAI, ModelManager, and ChatDatabase.
 */
public class MainActivity extends Activity implements WebBridge.ModelImportTrigger {

    private static final String TAG = "NOVA_MainActivity";
    private static final int REQUEST_CODE_PICK_GGUF = 1001;

    private WebView mWebView;
    private LocalAI mLocalAI;
    private ModelManager mModelManager;
    private ChatDatabase mChatDatabase;
    private WebBridge mWebBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply dark status and navigation bars
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(0xFF0B0F17);
            window.setNavigationBarColor(0xFF0B0F17);
        }

        setContentView(R.layout.activity_main);

        // Initialize Native & Database subsystems
        mLocalAI = new NativeAI();
        mModelManager = new ModelManager(this);
        mChatDatabase = new ChatDatabase(this);

        // Setup WebView
        mWebView = findViewById(R.id.webView);
        setupWebView();

        // Inject JavaScript Bridge as 'window.NOVA'
        mWebBridge = new WebBridge(this, mWebView, mLocalAI, mModelManager, mChatDatabase);
        mWebBridge.setModelImportTrigger(this);
        mWebView.addJavascriptInterface(mWebBridge, "NOVA");

        // Load entry point
        mWebView.loadUrl("file:///android_asset/web/index.html");
    }

    private void setupWebView() {
        mWebView.setBackgroundColor(0xFF0B0F17);
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setTextZoom(100);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "WebView finished loading: " + url);
                // Inform frontend of current model status
                mWebBridge.notifyModelStatusChanged();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient());
    }

    @Override
    public void triggerModelFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            // Also suggest .gguf or binary mime types
            String[] mimeTypes = {"application/octet-stream", "*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(intent, REQUEST_CODE_PICK_GGUF);
        } catch (Exception e) {
            Log.e(TAG, "File picker launch failed, trying fallback: " + e.getMessage());
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("*/*");
                startActivityForResult(fallback, REQUEST_CODE_PICK_GGUF);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open file picker: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_GGUF && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                Log.i(TAG, "User selected file URI: " + uri);
                mWebBridge.notifyImportProgress(0, 0, 0, "Reading selected file...");

                mModelManager.importModelAsync(uri, null, new ModelManager.ImportProgressListener() {
                    @Override
                    public void onProgress(int percent, long bytesCopied, long totalBytes, String status) {
                        mWebBridge.notifyImportProgress(percent, bytesCopied, totalBytes, status);
                    }

                    @Override
                    public void onComplete(File importedFile) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Model imported: " + importedFile.getName(), Toast.LENGTH_SHORT).show();
                            // Attempt loading into LocalAI
                            mLocalAI.loadModel(importedFile.getAbsolutePath(), 2048, 4);
                            mWebBridge.notifyImportComplete(true, "Model successfully installed!");
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            mWebBridge.notifyImportComplete(false, errorMessage);
                        });
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (mLocalAI != null) {
            mLocalAI.stopGeneration();
            mLocalAI.unloadModel();
        }
        if (mChatDatabase != null) {
            mChatDatabase.close();
        }
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }
}
