package com.nova.offline;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ModelManager - Handles local storage, validation, copying, and deletion
 * of GGUF model files in the private app sandbox (/files/models/).
 */
public class ModelManager {

    private static final String TAG = "NOVA_ModelManager";
    private static final String MODELS_DIR_NAME = "models";
    private static final byte[] GGUF_MAGIC = new byte[]{(byte) 0x47, (byte) 0x47, (byte) 0x55, (byte) 0x46}; // "GGUF"

    private final Context mContext;
    private final File mModelsDir;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public interface ImportProgressListener {
        void onProgress(int percent, long bytesCopied, long totalBytes, String status);
        void onComplete(File importedFile);
        void onError(String errorMessage);
    }

    public ModelManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mModelsDir = new File(mContext.getFilesDir(), MODELS_DIR_NAME);
        if (!mModelsDir.exists()) {
            boolean created = mModelsDir.mkdirs();
            Log.d(TAG, "Models directory created: " + created + " at " + mModelsDir.getAbsolutePath());
        }
    }

    public File getModelsDir() {
        return mModelsDir;
    }

    public boolean isModelInstalled() {
        File active = getActiveModelFile();
        return active != null && active.exists() && active.length() > 0;
    }

    public File getActiveModelFile() {
        if (!mModelsDir.exists() || !mModelsDir.isDirectory()) {
            return null;
        }
        File[] files = mModelsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf"));
        if (files != null && files.length > 0) {
            // Return largest or first GGUF file
            File largest = files[0];
            for (File f : files) {
                if (f.length() > largest.length()) {
                    largest = f;
                }
            }
            return largest;
        }
        return null;
    }

    public JSONObject getModelInfo() {
        JSONObject json = new JSONObject();
        try {
            File active = getActiveModelFile();
            if (active != null && active.exists()) {
                json.put("installed", true);
                json.put("fileName", active.getName());
                json.put("fileSizeBytes", active.length());
                json.put("fileSizeFormatted", formatFileSize(active.length()));
                json.put("filePath", active.getAbsolutePath());
                json.put("isValidGguf", validateGgufHeader(active));
                json.put("lastModified", active.lastModified());
            } else {
                json.put("installed", false);
                json.put("fileName", "None");
                json.put("fileSizeBytes", 0);
                json.put("fileSizeFormatted", "0 MB");
                json.put("filePath", new File(mModelsDir, "model.gguf").getAbsolutePath());
                json.put("isValidGguf", false);
                json.put("lastModified", 0);
            }
            json.put("modelsDirPath", mModelsDir.getAbsolutePath());
        } catch (JSONException e) {
            Log.e(TAG, "Error building model info JSON: " + e.getMessage());
        }
        return json;
    }

    /**
     * Checks if the file starts with the 4-byte GGUF magic header (0x46554747 in LE = "GGUF").
     */
    public static boolean validateGgufHeader(File file) {
        if (file == null || !file.exists() || file.length() < 4) {
            return false;
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[4];
            int read = is.read(header);
            if (read == 4) {
                return header[0] == GGUF_MAGIC[0] &&
                       header[1] == GGUF_MAGIC[1] &&
                       header[2] == GGUF_MAGIC[2] &&
                       header[3] == GGUF_MAGIC[3];
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not read header of " + file.getName() + ": " + e.getMessage());
        }
        return false;
    }

    public void importModelAsync(Uri uri, String originalName, ImportProgressListener listener) {
        mExecutor.submit(() -> {
            String fileName = sanitizeFileName(originalName != null ? originalName : queryFileName(uri));
            if (!fileName.toLowerCase().endsWith(".gguf")) {
                if (listener != null) {
                    listener.onError("Invalid file extension. Selected file must end with .gguf (found: " + fileName + ")");
                }
                return;
            }

            File destFile = new File(mModelsDir, fileName);
            File tempFile = new File(mModelsDir, fileName + ".importing");

            long totalBytes = queryFileSize(uri);
            long bytesCopied = 0;

            if (listener != null) {
                listener.onProgress(0, 0, totalBytes, "Starting import of " + fileName + "...");
            }

            byte[] buffer = new byte[65536]; // 64 KB buffer
            try (InputStream in = mContext.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempFile)) {

                if (in == null) {
                    throw new IOException("Failed to open input stream for URI: " + uri);
                }

                int read;
                int lastReportedPercent = -1;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    bytesCopied += read;

                    int percent = totalBytes > 0 ? (int) ((bytesCopied * 100) / totalBytes) : -1;
                    if (percent != lastReportedPercent && percent >= 0) {
                        lastReportedPercent = percent;
                        if (listener != null) {
                            String status = "Importing " + fileName + ": " + formatFileSize(bytesCopied) + " / " + formatFileSize(totalBytes);
                            listener.onProgress(percent, bytesCopied, totalBytes, status);
                        }
                    }
                }
                out.flush();

                // Validate GGUF Magic Header
                boolean isValidGguf = validateGgufHeader(tempFile);
                if (!isValidGguf) {
                    tempFile.delete();
                    if (listener != null) {
                        listener.onError("GGUF header validation failed. The file is missing the 'GGUF' magic identifier.");
                    }
                    return;
                }

                // If previous model exists, remove it or overwrite
                if (destFile.exists()) {
                    destFile.delete();
                }

                boolean renamed = tempFile.renameTo(destFile);
                if (!renamed) {
                    throw new IOException("Failed to move temporary import file to " + destFile.getAbsolutePath());
                }

                Log.i(TAG, "GGUF model successfully imported: " + destFile.getAbsolutePath() + " (" + formatFileSize(destFile.length()) + ")");
                if (listener != null) {
                    listener.onProgress(100, bytesCopied, totalBytes, "Model imported successfully!");
                    listener.onComplete(destFile);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error importing model: " + e.getMessage(), e);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                if (listener != null) {
                    listener.onError("Import failed: " + e.getMessage());
                }
            }
        });
    }

    public synchronized boolean deleteCurrentModel() {
        File active = getActiveModelFile();
        if (active != null && active.exists()) {
            boolean deleted = active.delete();
            Log.i(TAG, "Deleted model: " + active.getName() + " result=" + deleted);
            return deleted;
        }
        return false;
    }

    public synchronized void clearAllModels() {
        if (mModelsDir.exists() && mModelsDir.isDirectory()) {
            File[] files = mModelsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }

    private String queryFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not resolve file name from URI: " + e.getMessage());
            }
        }
        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    result = path.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "model.gguf";
    }

    private long queryFileSize(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        return cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not query file size: " + e.getMessage());
            }
        }
        return -1;
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "model.gguf";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
