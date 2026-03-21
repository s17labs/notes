package com.s17labs.notesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoteDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 6;

    public static final String PREFS_NAME = "NotesAppPrefs";
    public static final String PREF_AUTO_SAVE = "auto_save";

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, note_text TEXT, date_text TEXT, created_at INTEGER, modified_at INTEGER, pinned INTEGER DEFAULT 0, color INTEGER DEFAULT 0, deleted INTEGER DEFAULT 0);");
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
    }

    public static boolean isAutoSaveEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_SAVE, true);
    }

    public static void setAutoSaveEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AUTO_SAVE, enabled).apply();
    }
}
