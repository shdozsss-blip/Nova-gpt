package com.nova.offline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

/**
 * ChatDatabase - SQLite database for persistent offline storage of chat sessions,
 * conversation message history, and token counts.
 */
public class ChatDatabase extends SQLiteOpenHelper {

    private static final String TAG = "NOVA_ChatDatabase";
    private static final String DATABASE_NAME = "nova_offline_chat.db";
    private static final int DATABASE_VERSION = 1;

    // Table Sessions
    public static final String TABLE_SESSIONS = "sessions";
    public static final String COL_SESSION_ID = "id";
    public static final String COL_SESSION_TITLE = "title";
    public static final String COL_SESSION_CREATED_AT = "created_at";
    public static final String COL_SESSION_UPDATED_AT = "updated_at";

    // Table Messages
    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MSG_ID = "id";
    public static final String COL_MSG_SESSION_ID = "session_id";
    public static final String COL_MSG_ROLE = "role";
    public static final String COL_MSG_CONTENT = "content";
    public static final String COL_MSG_TIMESTAMP = "timestamp";
    public static final String COL_MSG_TOKENS = "tokens";

    public ChatDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                COL_SESSION_ID + " TEXT PRIMARY KEY, " +
                COL_SESSION_TITLE + " TEXT, " +
                COL_SESSION_CREATED_AT + " INTEGER, " +
                COL_SESSION_UPDATED_AT + " INTEGER" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_MESSAGES + " (" +
                COL_MSG_ID + " TEXT PRIMARY KEY, " +
                COL_MSG_SESSION_ID + " TEXT, " +
                COL_MSG_ROLE + " TEXT, " +
                COL_MSG_CONTENT + " TEXT, " +
                COL_MSG_TIMESTAMP + " INTEGER, " +
                COL_MSG_TOKENS + " INTEGER, " +
                "FOREIGN KEY(" + COL_MSG_SESSION_ID + ") REFERENCES " + TABLE_SESSIONS + "(" + COL_SESSION_ID + ") ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE INDEX idx_messages_session ON " + TABLE_MESSAGES + "(" + COL_MSG_SESSION_ID + ", " + COL_MSG_TIMESTAMP + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
    }

    public synchronized String createSession(String title) {
        SQLiteDatabase db = getWritableDatabase();
        String sessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(COL_SESSION_ID, sessionId);
        values.put(COL_SESSION_TITLE, (title != null && !title.trim().isEmpty()) ? title.trim() : "New Chat");
        values.put(COL_SESSION_CREATED_AT, now);
        values.put(COL_SESSION_UPDATED_AT, now);

        db.insert(TABLE_SESSIONS, null, values);
        return sessionId;
    }

    public synchronized void updateSessionTitle(String sessionId, String title) {
        if (sessionId == null || title == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SESSION_TITLE, title.trim());
        values.put(COL_SESSION_UPDATED_AT, System.currentTimeMillis());
        db.update(TABLE_SESSIONS, values, COL_SESSION_ID + " = ?", new String[]{sessionId});
    }

    public synchronized void touchSession(String sessionId) {
        if (sessionId == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SESSION_UPDATED_AT, System.currentTimeMillis());
        db.update(TABLE_SESSIONS, values, COL_SESSION_ID + " = ?", new String[]{sessionId});
    }

    public synchronized String addMessage(String sessionId, String role, String content, int tokens) {
        SQLiteDatabase db = getWritableDatabase();
        String msgId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(COL_MSG_ID, msgId);
        values.put(COL_MSG_SESSION_ID, sessionId);
        values.put(COL_MSG_ROLE, role);
        values.put(COL_MSG_CONTENT, content);
        values.put(COL_MSG_TIMESTAMP, now);
        values.put(COL_MSG_TOKENS, tokens);

        db.insert(TABLE_MESSAGES, null, values);
        touchSession(sessionId);

        return msgId;
    }

    public synchronized JSONArray getAllSessions() {
        JSONArray array = new JSONArray();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT s.id, s.title, s.created_at, s.updated_at, COUNT(m.id) as msg_count " +
                     "FROM " + TABLE_SESSIONS + " s " +
                     "LEFT JOIN " + TABLE_MESSAGES + " m ON s.id = m.session_id " +
                     "GROUP BY s.id " +
                     "ORDER BY s.updated_at DESC";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                JSONObject obj = new JSONObject();
                obj.put("id", cursor.getString(0));
                obj.put("title", cursor.getString(1));
                obj.put("createdAt", cursor.getLong(2));
                obj.put("updatedAt", cursor.getLong(3));
                obj.put("messageCount", cursor.getInt(4));
                array.put(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying sessions: " + e.getMessage());
        }
        return array;
    }

    public synchronized JSONArray getMessages(String sessionId) {
        JSONArray array = new JSONArray();
        if (sessionId == null) return array;

        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT id, session_id, role, content, timestamp, tokens FROM " + TABLE_MESSAGES +
                     " WHERE " + COL_MSG_SESSION_ID + " = ? ORDER BY " + COL_MSG_TIMESTAMP + " ASC";

        try (Cursor cursor = db.rawQuery(sql, new String[]{sessionId})) {
            while (cursor.moveToNext()) {
                JSONObject obj = new JSONObject();
                obj.put("id", cursor.getString(0));
                obj.put("sessionId", cursor.getString(1));
                obj.put("role", cursor.getString(2));
                obj.put("content", cursor.getString(3));
                obj.put("timestamp", cursor.getLong(4));
                obj.put("tokens", cursor.getInt(5));
                array.put(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying messages for session " + sessionId + ": " + e.getMessage());
        }
        return array;
    }

    public synchronized void deleteSession(String sessionId) {
        if (sessionId == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MESSAGES, COL_MSG_SESSION_ID + " = ?", new String[]{sessionId});
        db.delete(TABLE_SESSIONS, COL_SESSION_ID + " = ?", new String[]{sessionId});
    }

    public synchronized void clearAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
        db.delete(TABLE_SESSIONS, null, null);
    }
}
