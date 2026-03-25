package com.s17labs.notesapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BoardUtils {

    private static final int DEFAULT_COLORS[] = {
        0xFF6B6B6B,
        0xFF4CAF50,
        0xFF2196F3
    };

    public static List<BoardColumnAdapter.BoardColumn> getDefaultColumns(Context context) {
        List<BoardColumnAdapter.BoardColumn> columns = new ArrayList<>();
        columns.add(new BoardColumnAdapter.BoardColumn(0, "To Do", DEFAULT_COLORS[0], false));
        columns.add(new BoardColumnAdapter.BoardColumn(1, "In Progress", DEFAULT_COLORS[1], false));
        columns.add(new BoardColumnAdapter.BoardColumn(2, "Done", DEFAULT_COLORS[2], false));
        return columns;
    }

    public static List<BoardColumnAdapter.BoardColumn> getAndSaveDefaultColumns(Context context) {
        List<BoardColumnAdapter.BoardColumn> columns = getDefaultColumns(context);
        saveColumns(context, columns);
        return columns;
    }

    public static List<BoardColumnAdapter.BoardColumn> loadColumns(Context context, SQLiteDatabase db) {
        String json = NoteDbHelper.getBoardColumnsJson(context);
        
        if (json == null || json.isEmpty()) {
            return getAndSaveDefaultColumns(context);
        }

        try {
            JSONArray jsonArray = new JSONArray(json);
            if (jsonArray.length() == 0) {
                return getAndSaveDefaultColumns(context);
            }
            
            List<BoardColumnAdapter.BoardColumn> columns = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                BoardColumnAdapter.BoardColumn column = new BoardColumnAdapter.BoardColumn(
                    obj.getInt("id"),
                    obj.getString("title"),
                    obj.getInt("color"),
                    obj.getBoolean("completed")
                );
                columns.add(column);
            }
            
            return columns;
        } catch (Exception e) {
            e.printStackTrace();
            return getAndSaveDefaultColumns(context);
        }
    }

    public static void saveColumns(Context context, List<BoardColumnAdapter.BoardColumn> columns) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (BoardColumnAdapter.BoardColumn column : columns) {
                JSONObject obj = new JSONObject();
                obj.put("id", column.id);
                obj.put("title", column.title);
                obj.put("color", column.color);
                obj.put("completed", column.completed);
                jsonArray.put(obj);
            }
            
            NoteDbHelper.setBoardColumnsJson(context, jsonArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addNewColumn(Context context, String title, int color) {
        String json = NoteDbHelper.getBoardColumnsJson(context);
        List<BoardColumnAdapter.BoardColumn> columns = new ArrayList<>();
        
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    BoardColumnAdapter.BoardColumn column = new BoardColumnAdapter.BoardColumn(
                        obj.getInt("id"),
                        obj.getString("title"),
                        obj.getInt("color"),
                        obj.getBoolean("completed")
                    );
                    columns.add(column);
                }
            } catch (Exception e) {
                columns = new ArrayList<>();
            }
        }
        
        int maxId = -1;
        for (BoardColumnAdapter.BoardColumn column : columns) {
            if (column.id > maxId) {
                maxId = column.id;
            }
        }
        int newId = maxId + 1;
        
        BoardColumnAdapter.BoardColumn newColumn = new BoardColumnAdapter.BoardColumn(newId, title, color, false);
        columns.add(newColumn);
        
        String newJson = null;
        try {
            JSONArray jsonArray = new JSONArray();
            for (BoardColumnAdapter.BoardColumn column : columns) {
                JSONObject obj = new JSONObject();
                obj.put("id", column.id);
                obj.put("title", column.title);
                obj.put("color", column.color);
                obj.put("completed", column.completed);
                jsonArray.put(obj);
            }
            newJson = jsonArray.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (newJson != null) {
            NoteDbHelper.setBoardColumnsJson(context, newJson);
        }
    }

    public static List<BoardItemAdapter.BoardItem> loadItemsForColumn(SQLiteDatabase db, int columnId) {
        List<BoardItemAdapter.BoardItem> items = new ArrayList<>();
        
        Cursor cursor = NoteDbHelper.getBoardItemsByColumn(db, columnId);

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            int colId = cursor.getInt(1);
            String text = cursor.getString(2);
            int completed = cursor.getInt(3);

            items.add(new BoardItemAdapter.BoardItem(id, text, completed, colId));
        }
        
        cursor.close();
        return items;
    }

    public static long addItemToColumn(SQLiteDatabase db, int columnId, String text) {
        return NoteDbHelper.insertBoardItem(db, columnId, text);
    }

    public static void updateItemText(SQLiteDatabase db, long itemId, String text) {
        NoteDbHelper.updateBoardItemText(db, itemId, text);
    }

    public static void updateItemCompleted(SQLiteDatabase db, long itemId, boolean completed) {
        NoteDbHelper.updateBoardItemCompleted(db, itemId, completed ? 1 : 0);
    }

    public static void deleteItem(SQLiteDatabase db, long itemId) {
        NoteDbHelper.deleteBoardItem(db, itemId);
    }

    public static void moveItemToColumn(SQLiteDatabase db, long itemId, int newColumnId) {
        NoteDbHelper.updateBoardItemColumnId(db, itemId, newColumnId);
    }

    public static void updateItemPosition(SQLiteDatabase db, long itemId, int position) {
        NoteDbHelper.updateBoardItemPosition(db, itemId, position);
    }

    public static void updateColumnTitle(Context context, int columnId, String newTitle) {
        try {
            String json = NoteDbHelper.getBoardColumnsJson(context);
            if (json != null && !json.isEmpty()) {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (obj.getInt("id") == columnId) {
                        obj.put("title", newTitle);
                        break;
                    }
                }
                NoteDbHelper.setBoardColumnsJson(context, jsonArray.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getPresetsKey(Context context) {
        return "board_presets_" + context.getPackageName();
    }

    public static void savePreset(Context context, String name, List<BoardColumnAdapter.BoardColumn> columns) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("board_prefs", Context.MODE_PRIVATE);
            String presetsJson = prefs.getString(getPresetsKey(context), "[]");
            JSONArray presets = new JSONArray(presetsJson);

            JSONArray columnsJson = new JSONArray();
            for (BoardColumnAdapter.BoardColumn column : columns) {
                JSONObject obj = new JSONObject();
                obj.put("id", column.id);
                obj.put("title", column.title);
                obj.put("color", column.color);
                obj.put("completed", column.completed);
                columnsJson.put(obj);
            }

            JSONObject preset = new JSONObject();
            preset.put("name", name);
            preset.put("columns", columnsJson);

            boolean found = false;
            for (int i = 0; i < presets.length(); i++) {
                if (presets.getJSONObject(i).getString("name").equals(name)) {
                    presets.put(i, preset);
                    found = true;
                    break;
                }
            }
            if (!found) {
                presets.put(preset);
            }

            prefs.edit().putString(getPresetsKey(context), presets.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getPresetNames(Context context) {
        List<String> names = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences("board_prefs", Context.MODE_PRIVATE);
            String presetsJson = prefs.getString(getPresetsKey(context), "[]");
            JSONArray presets = new JSONArray(presetsJson);

            for (int i = 0; i < presets.length(); i++) {
                names.add(presets.getJSONObject(i).getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }

    public static List<BoardColumnAdapter.BoardColumn> loadPreset(Context context, String name) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("board_prefs", Context.MODE_PRIVATE);
            String presetsJson = prefs.getString(getPresetsKey(context), "[]");
            JSONArray presets = new JSONArray(presetsJson);

            for (int i = 0; i < presets.length(); i++) {
                JSONObject preset = presets.getJSONObject(i);
                if (preset.getString("name").equals(name)) {
                    JSONArray columnsJson = preset.getJSONArray("columns");
                    List<BoardColumnAdapter.BoardColumn> columns = new ArrayList<>();

                    for (int j = 0; j < columnsJson.length(); j++) {
                        JSONObject obj = columnsJson.getJSONObject(j);
                        BoardColumnAdapter.BoardColumn column = new BoardColumnAdapter.BoardColumn(
                            obj.getInt("id"),
                            obj.getString("title"),
                            obj.getInt("color"),
                            obj.getBoolean("completed")
                        );
                        columns.add(column);
                    }
                    return columns;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearColumns(Context context) {
        NoteDbHelper.setBoardColumnsJson(context, "[]");
    }
}
