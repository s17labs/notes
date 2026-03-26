package com.s17labs.notesapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class NoteDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 8;

    public static final String PREFS_NAME = "NotesAppPrefs";
    public static final String PREF_AUTO_SAVE = "auto_save";
    public static final String PREF_GRID_VIEW = "grid_view";
    public static final String PREF_BOARD_VIEW = "board_view";
    public static final String PREF_BOARD_COLUMNS = "board_columns";

    public static final int DEFAULT_COLUMN_ID = 0;
    public static final int COLUMN_NOT_STARTED = 0;
    public static final int COLUMN_IN_PROGRESS = 1;
    public static final int COLUMN_DONE = 2;

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, note_text TEXT, date_text TEXT, created_at INTEGER, modified_at INTEGER, pinned INTEGER DEFAULT 0, color INTEGER DEFAULT 0, deleted INTEGER DEFAULT 0, column_id INTEGER DEFAULT 0, completed INTEGER DEFAULT 0, board_enabled INTEGER DEFAULT 0);");
        db.execSQL("CREATE TABLE IF NOT EXISTS board_items (_id INTEGER PRIMARY KEY AUTOINCREMENT, column_id INTEGER DEFAULT 0, text TEXT, completed INTEGER DEFAULT 0, position INTEGER DEFAULT 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN title TEXT DEFAULT ''");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE notes ADD COLUMN created_at INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN modified_at INTEGER DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER DEFAULT 0");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER DEFAULT 0");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE notes ADD COLUMN deleted INTEGER DEFAULT 0");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE notes ADD COLUMN column_id INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN completed INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN board_enabled INTEGER DEFAULT 0");
        }
        if (oldVersion < 8) {
            db.execSQL("CREATE TABLE IF NOT EXISTS board_items (_id INTEGER PRIMARY KEY AUTOINCREMENT, column_id INTEGER DEFAULT 0, text TEXT, completed INTEGER DEFAULT 0, position INTEGER DEFAULT 0);");
        }
    }

    public static long insertBoardItem(SQLiteDatabase db, int columnId, String text) {
        Cursor cursor = db.rawQuery("SELECT MAX(position) FROM board_items WHERE column_id = ?", new String[]{String.valueOf(columnId)});
        int maxPosition = 0;
        if (cursor.moveToFirst()) {
            maxPosition = cursor.getInt(0);
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put("column_id", columnId);
        values.put("text", text);
        values.put("completed", 0);
        values.put("position", maxPosition + 1);
        return db.insert("board_items", null, values);
    }

    public static void updateBoardItemText(SQLiteDatabase db, long itemId, String text) {
        ContentValues values = new ContentValues();
        values.put("text", text);
        db.update("board_items", values, "_id=?", new String[]{String.valueOf(itemId)});
    }

    public static void updateBoardItemCompleted(SQLiteDatabase db, long itemId, int completed) {
        ContentValues values = new ContentValues();
        values.put("completed", completed);
        db.update("board_items", values, "_id=?", new String[]{String.valueOf(itemId)});
    }

    public static void deleteBoardItem(SQLiteDatabase db, long itemId) {
        db.delete("board_items", "_id=?", new String[]{String.valueOf(itemId)});
    }

    public static void clearBoardItems(Context context) {
        SQLiteDatabase db = new NoteDbHelper(context).getWritableDatabase();
        db.delete("board_items", null, null);
    }

    public static void updateBoardItemColumnId(SQLiteDatabase db, long itemId, int columnId) {
        ContentValues values = new ContentValues();
        values.put("column_id", columnId);
        db.update("board_items", values, "_id=?", new String[]{String.valueOf(itemId)});
    }

    public static void updateBoardItemPosition(SQLiteDatabase db, long itemId, int position) {
        ContentValues values = new ContentValues();
        values.put("position", position);
        db.update("board_items", values, "_id=?", new String[]{String.valueOf(itemId)});
    }

    public static Cursor getBoardItemsByColumn(SQLiteDatabase db, int columnId) {
        return db.rawQuery("SELECT * FROM board_items WHERE column_id = ? ORDER BY position ASC", new String[]{String.valueOf(columnId)});
    }

    public static boolean isAutoSaveEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_SAVE, true);
    }

    public static void setAutoSaveEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AUTO_SAVE, enabled).apply();
    }

    public static boolean isGridView(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_GRID_VIEW, false);
    }

    public static void setGridView(Context context, boolean isGrid) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_GRID_VIEW, isGrid).apply();
    }

    public static boolean isBoardView(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_BOARD_VIEW, false);
    }

    public static void setBoardView(Context context, boolean isBoard) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_BOARD_VIEW, isBoard).apply();
    }

    public static String getBoardColumnsJson(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_BOARD_COLUMNS, null);
    }

    public static void setBoardColumnsJson(Context context, String json) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_BOARD_COLUMNS, json).apply();
    }

    public static void updateNoteColumnId(SQLiteDatabase db, long noteId, int columnId) {
        ContentValues values = new ContentValues();
        values.put("column_id", columnId);
        values.put("board_enabled", 1);
        db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
    }

    public static void updateNoteCompleted(SQLiteDatabase db, long noteId, int completed) {
        ContentValues values = new ContentValues();
        values.put("completed", completed);
        db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
    }

    public static JSONArray exportBoardColumns(Context context) {
        JSONArray columnsArray = new JSONArray();
        try {
            String json = getBoardColumnsJson(context);
            if (json != null && !json.isEmpty()) {
                columnsArray = new JSONArray(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return columnsArray;
    }

    public static void importBoardColumns(Context context, JSONArray columnsArray) {
        try {
            setBoardColumnsJson(context, columnsArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONArray exportBoardItems(SQLiteDatabase db) {
        JSONArray itemsArray = new JSONArray();
        try {
            Cursor cursor = db.rawQuery("SELECT _id, column_id, text, completed, position FROM board_items", null);
            if (cursor.moveToFirst()) {
                do {
                    JSONObject item = new JSONObject();
                    item.put("id", cursor.getLong(0));
                    item.put("column_id", cursor.getInt(1));
                    item.put("text", cursor.getString(2));
                    item.put("completed", cursor.getInt(3));
                    item.put("position", cursor.getInt(4));
                    itemsArray.put(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemsArray;
    }

    public static void importBoardItems(SQLiteDatabase db, JSONArray itemsArray) {
        try {
            db.delete("board_items", null, null);
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject item = itemsArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put("column_id", item.optInt("column_id", 0));
                values.put("text", item.optString("text", ""));
                values.put("completed", item.optInt("completed", 0));
                values.put("position", item.optInt("position", i));
                db.insert("board_items", null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONArray exportBoardPresets(Context context) {
        JSONArray presetsArray = new JSONArray();
        try {
            SharedPreferences prefs = context.getSharedPreferences("board_prefs", Context.MODE_PRIVATE);
            String presetsJson = prefs.getString("board_presets_" + context.getPackageName(), "[]");
            presetsArray = new JSONArray(presetsJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return presetsArray;
    }

    public static void importBoardPresets(Context context, JSONArray presetsArray) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("board_prefs", Context.MODE_PRIVATE);
            prefs.edit().putString("board_presets_" + context.getPackageName(), presetsArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
