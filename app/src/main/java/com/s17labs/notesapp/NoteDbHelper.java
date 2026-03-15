package com.s17labs.notesapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoteDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 3;

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, note_text TEXT, date_text TEXT, created_at INTEGER, modified_at INTEGER);");
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
    }
}
