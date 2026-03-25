package com.s17labs.notesapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class BoardActivity extends AppCompatActivity implements 
        BoardColumnAdapter.OnColumnHeaderListener, 
        BoardColumnAdapter.OnBoardItemListener {

    private NoteDbHelper dbHelper;
    private RecyclerView boardRecyclerView;
    private BoardColumnAdapter boardAdapter;
    private TextView toolbarTitle;
    private ImageButton backButton;
    private TextView emptyView;
    private FloatingActionButton fab;
    private ImageButton addColumnButton;
    private ImageButton menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        getWindow().setBackgroundDrawable(null);

        dbHelper = new NoteDbHelper(this);

        toolbarTitle = findViewById(R.id.toolbarTitle);
        backButton = findViewById(R.id.backButton);
        boardRecyclerView = findViewById(R.id.boardRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        fab = findViewById(R.id.fab);
        addColumnButton = findViewById(R.id.addColumnButton);
        menuButton = findViewById(R.id.menuButton);

        backButton.setOnClickListener(v -> finish());
        addColumnButton.setOnClickListener(v -> showAddColumnDialog());
        menuButton.setOnClickListener(v -> showBoardMenuDialog());

        fab.setVisibility(View.GONE);

        boardRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBoardView();
    }

    private void loadBoardView() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<BoardColumnAdapter.BoardColumn> columns = BoardUtils.loadColumns(this, db);

        for (BoardColumnAdapter.BoardColumn column : columns) {
            column.items = BoardUtils.loadItemsForColumn(db, column.id);
        }

        if (columns.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            boardRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            boardRecyclerView.setVisibility(View.VISIBLE);
        }

        if (boardAdapter == null) {
            boardAdapter = new BoardColumnAdapter(this, this, this);
            boardRecyclerView.setAdapter(boardAdapter);
        }
        boardAdapter.setColumns(columns);
    }

    @Override
    public void onColumnIndicatorClick(int columnId, int currentColor) {
        showColumnColorDialog(columnId, currentColor);
    }

    @Override
    public void onColumnIndicatorLongClick(int columnId) {
        Toast.makeText(this, "Tap to change color", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onColumnTitleClick(int columnId, String currentTitle) {
        showEditColumnTitleDialog(columnId, currentTitle);
    }

    @Override
    public void onColumnMenuClick(int columnId, View anchorView) {
        showColumnMenuPopup(columnId, anchorView);
    }

    private void showColumnMenuPopup(int columnId, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.inflate(R.menu.menu_column);

        List<BoardColumnAdapter.BoardColumn> columns = boardAdapter.getColumns();
        int columnIndex = -1;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).id == columnId) {
                columnIndex = i;
                break;
            }
        }

        MenuItem moveLeft = popup.getMenu().findItem(R.id.action_move_left);
        MenuItem moveRight = popup.getMenu().findItem(R.id.action_move_right);
        
        if (columnIndex <= 0 && moveLeft != null) {
            moveLeft.setVisible(false);
        }
        if (columnIndex < 0 || columnIndex >= columns.size() - 1 || moveRight == null) {
            if (moveRight != null) moveRight.setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_move_left) {
                moveColumn(columnId, -1);
                return true;
            } else if (itemId == R.id.action_move_right) {
                moveColumn(columnId, 1);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteColumnDialog(columnId);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void moveColumn(int columnId, int direction) {
        List<BoardColumnAdapter.BoardColumn> columns = boardAdapter.getColumns();
        int columnIndex = -1;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).id == columnId) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex < 0) return;

        int newIndex = columnIndex + direction;
        if (newIndex < 0 || newIndex >= columns.size()) {
            Toast.makeText(this, "Cannot move column", Toast.LENGTH_SHORT).show();
            return;
        }

        BoardColumnAdapter.BoardColumn temp = columns.get(columnIndex);
        columns.set(columnIndex, columns.get(newIndex));
        columns.set(newIndex, temp);

        boardAdapter.setColumns(columns);
        BoardUtils.saveColumns(this, columns);
    }

    private void showDeleteColumnDialog(int columnId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        TextView titleText = dialogView.findViewById(R.id.deleteTitle);
        TextView messageText = dialogView.findViewById(R.id.deleteMessage);
        
        titleText.setText("Delete Column?");
        messageText.setText("This will delete the column and all its items.");
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            deleteColumn(columnId);
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout(
            (int)(280 * getResources().getDisplayMetrics().density), 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void deleteColumn(int columnId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("board_items", "column_id=?", new String[]{String.valueOf(columnId)});
        
        List<BoardColumnAdapter.BoardColumn> columns = boardAdapter.getColumns();
        List<BoardColumnAdapter.BoardColumn> newColumns = new ArrayList<>();
        for (BoardColumnAdapter.BoardColumn column : columns) {
            if (column.id != columnId) {
                newColumns.add(column);
            }
        }
        boardAdapter.setColumns(newColumns);
        BoardUtils.saveColumns(this, newColumns);
    }

    private void showEditColumnTitleDialog(int columnId, String currentTitle) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        EditText editText = dialogView.findViewById(R.id.editText);
        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        titleText.setText("Edit Column Title");
        editText.setText(currentTitle);
        editText.setSelection(editText.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTitle = editText.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                updateColumnTitle(columnId, newTitle);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnShowListener(d -> {
            editText.requestFocus();
            editText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        });

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void updateColumnTitle(int columnId, String newTitle) {
        if (boardAdapter == null) return;

        List<BoardColumnAdapter.BoardColumn> columns = boardAdapter.getColumns();
        for (BoardColumnAdapter.BoardColumn column : columns) {
            if (column.id == columnId) {
                column.title = newTitle;
                break;
            }
        }
        BoardUtils.saveColumns(this, columns);
        boardAdapter.notifyDataSetChanged();
    }

    private void showColumnColorDialog(int columnId, int currentColor) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_column_color, null);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        View colorRed = dialogView.findViewById(R.id.colorRed);
        View colorOrange = dialogView.findViewById(R.id.colorOrange);
        View colorYellow = dialogView.findViewById(R.id.colorYellow);
        View colorGreen = dialogView.findViewById(R.id.colorGreen);
        View colorTeal = dialogView.findViewById(R.id.colorTeal);
        View colorBlue = dialogView.findViewById(R.id.colorBlue);
        View colorPurple = dialogView.findViewById(R.id.colorPurple);
        View colorGray = dialogView.findViewById(R.id.colorGray);
        android.widget.CheckBox checkboxCompleted = dialogView.findViewById(R.id.checkboxCompleted);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        boolean currentCompleted = false;
        if (boardAdapter != null) {
            for (BoardColumnAdapter.BoardColumn column : boardAdapter.getColumns()) {
                if (column.id == columnId) {
                    boolean hasItems = column.items.size() > 0;
                    if (hasItems) {
                        currentCompleted = true;
                        for (BoardItemAdapter.BoardItem item : column.items) {
                            if (item.completed != 1) {
                                currentCompleted = false;
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        checkboxCompleted.setChecked(currentCompleted);

        final int[] colors = {0xFF4136, 0xFFFF851B, 0xFFFFDC00, 0xFF2ECC40,
                              0xFF39CCCC, 0xFF0074D9, 0xFFB10DC9, 0xFF6B6B6B};
        View[] colorViews = {colorRed, colorOrange, colorYellow, colorGreen,
                            colorTeal, colorBlue, colorPurple, colorGray};

        for (int i = 0; i < colorViews.length; i++) {
            final int color = colors[i];
            colorViews[i].setOnClickListener(v -> {
                updateColumnColor(columnId, color);
                dialog.dismiss();
            });
        }

        checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateColumnCompleted(columnId, isChecked);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void updateColumnColor(int columnId, int color) {
        if (boardAdapter == null) return;

        List<BoardColumnAdapter.BoardColumn> columns = boardAdapter.getColumns();
        for (BoardColumnAdapter.BoardColumn column : columns) {
            if (column.id == columnId) {
                column.color = color;
                break;
            }
        }
        BoardUtils.saveColumns(this, columns);
        boardAdapter.notifyDataSetChanged();
    }

    private void updateColumnCompleted(int columnId, boolean completed) {
        if (boardAdapter == null) return;

        List<BoardColumnAdapter.BoardColumn> columns = boardAdapter.getColumns();
        for (BoardColumnAdapter.BoardColumn column : columns) {
            if (column.id == columnId) {
                for (BoardItemAdapter.BoardItem item : column.items) {
                    item.completed = completed ? 1 : 0;
                    BoardUtils.updateItemCompleted(dbHelper.getWritableDatabase(), item.id, completed);
                }
                break;
            }
        }
        BoardUtils.saveColumns(this, columns);
        boardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(long itemId, String currentText) {
        showEditItemDialog(itemId, currentText);
    }

    @Override
    public void onItemCompletedChanged(long itemId, boolean completed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        BoardUtils.updateItemCompleted(db, itemId, completed);
        boardAdapter.updateItemInColumn(itemId, completed);
    }

    @Override
    public void onItemDeleteRequested(long itemId) {
        showDeleteConfirmationDialog(itemId);
    }

    @Override
    public void onAddItemClick(int columnId) {
        showAddItemDialog(columnId);
    }

    private void showDeleteConfirmationDialog(long itemId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        TextView titleText = dialogView.findViewById(R.id.deleteTitle);
        TextView messageText = dialogView.findViewById(R.id.deleteMessage);
        
        titleText.setText("Delete Item?");
        messageText.setText("This item will be permanently deleted.");
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            BoardUtils.deleteItem(db, itemId);
            boardAdapter.removeItemFromColumn(itemId);
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout(
            (int)(280 * getResources().getDisplayMetrics().density), 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    @Override
    public void onItemMoved(long itemId, int fromColumnId, int toColumnId, int newPosition) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        BoardUtils.moveItemToColumn(db, itemId, toColumnId);
    }

    @Override
    public void onItemPositionChanged(long itemId, int newPosition) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        BoardUtils.updateItemPosition(db, itemId, newPosition);
    }

    private void showAddItemDialog(int columnId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        EditText editText = dialogView.findViewById(R.id.editText);
        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        titleText.setText("Add Item");
        editText.setHint("Enter item text...");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                long newItemId = BoardUtils.addItemToColumn(db, columnId, text);
                BoardItemAdapter.BoardItem newItem = new BoardItemAdapter.BoardItem(newItemId, text, 0, columnId);
                boardAdapter.addItemToColumn(columnId, newItem);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Item cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnShowListener(d -> {
            editText.requestFocus();
            editText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        });

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void showEditItemDialog(long itemId, String currentText) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        EditText editText = dialogView.findViewById(R.id.editText);
        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        titleText.setText("Edit Item");
        editText.setText(currentText);
        editText.setSelection(editText.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                BoardUtils.updateItemText(db, itemId, text);
                dialog.dismiss();
                loadBoardView();
            } else {
                Toast.makeText(this, "Item cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnShowListener(d -> {
            editText.requestFocus();
            editText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        });

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void showAddColumnDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        EditText editText = dialogView.findViewById(R.id.editText);
        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        titleText.setText("Add Column");
        editText.setHint("Enter column title...");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = editText.getText().toString().trim();
            if (!title.isEmpty()) {
                int newColor = getUnusedColor();
                BoardUtils.addNewColumn(this, title, newColor);
                dialog.dismiss();
                loadBoardView();
                boardRecyclerView.postDelayed(() -> {
                    LinearLayoutManager lm = (LinearLayoutManager) boardRecyclerView.getLayoutManager();
                    if (lm != null) {
                        lm.scrollToPosition(lm.getItemCount() - 1);
                    }
                }, 100);
            } else {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnShowListener(d -> {
            editText.requestFocus();
            editText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        });

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int getUnusedColor() {
        int[] colors = {0xFF4136, 0xFFFF851B, 0xFFFFDC00, 0xFF2ECC40, 0xFF39CCCC, 0xFF0074D9, 0xFFB10DC9, 0xFF6B6B6B};
        boolean[] used = new boolean[colors.length];
        
        for (BoardColumnAdapter.BoardColumn column : boardAdapter.getColumns()) {
            for (int i = 0; i < colors.length; i++) {
                if (column.color == colors[i]) {
                    used[i] = true;
                }
            }
        }
        
        for (int i = 0; i < colors.length; i++) {
            if (!used[i]) {
                return colors[i];
            }
        }
        
        return colors[(int) (Math.random() * colors.length)];
    }

    private int getNextColumnId() {
        int maxId = -1;
        for (BoardColumnAdapter.BoardColumn column : boardAdapter.getColumns()) {
            if (column.id > maxId) {
                maxId = column.id;
            }
        }
        return maxId + 1;
    }

    private void showBoardMenuDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_board_menu, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        dialogView.findViewById(R.id.btnSavePreset).setOnClickListener(v -> {
            dialog.dismiss();
            showSavePresetDialog();
        });
        dialogView.findViewById(R.id.btnLoadPreset).setOnClickListener(v -> {
            dialog.dismiss();
            showLoadPresetDialog();
        });
        dialogView.findViewById(R.id.btnResetDefault).setOnClickListener(v -> {
            dialog.dismiss();
            confirmResetToDefault();
        });

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void showSavePresetDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        EditText editText = dialogView.findViewById(R.id.editText);
        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        titleText.setText("Save Preset");
        editText.setHint("Preset name");
        editText.setVisibility(View.VISIBLE);

        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnSave = dialogView.findViewById(R.id.btnSave);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        btnSave.setOnClickListener(v -> {
            String presetName = editText.getText().toString().trim();
            if (!presetName.isEmpty()) {
                BoardUtils.savePreset(this, presetName, boardAdapter.getColumns());
                Toast.makeText(this, "Preset saved", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void showLoadPresetDialog() {
        List<String> presetNames = BoardUtils.getPresetNames(this);
        if (presetNames.isEmpty()) {
            Toast.makeText(this, "No presets available", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_load_preset, null);
        ListView presetList = dialogView.findViewById(R.id.presetList);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, R.layout.item_preset, presetNames);
        presetList.setAdapter(adapter);

        presetList.setOnItemClickListener((parent, view, position, id) -> {
            String presetName = presetNames.get(position);
            List<BoardColumnAdapter.BoardColumn> columns = BoardUtils.loadPreset(this, presetName);
            if (columns != null) {
                BoardUtils.saveColumns(this, columns);
                NoteDbHelper.clearBoardItems(this);
                loadBoardView();
                Toast.makeText(this, "Preset loaded", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(300 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void confirmResetToDefault() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        TextView titleText = dialogView.findViewById(R.id.deleteTitle);
        titleText.setText("Reset to Default");
        TextView messageText = dialogView.findViewById(R.id.deleteMessage);
        messageText.setText("This will clear all columns and items. Continue?");

        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            BoardUtils.clearColumns(this);
            NoteDbHelper.clearBoardItems(this);
            loadBoardView();
            Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(
            (int)(340 * getResources().getDisplayMetrics().density),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }
}
