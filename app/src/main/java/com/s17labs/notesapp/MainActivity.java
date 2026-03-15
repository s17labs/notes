package com.s17labs.notesapp;

import android.animation.Animator;
import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {
    
    private NoteDbHelper dbHelper;
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private FloatingActionButton fab;
    private TextView emptyView;
    private EditText searchEditText;
    private ImageButton clearButton;
    private ImageButton sortButton;
    private int sortBy = 0;
    private int sortOrder = 0;
    private PopupWindow sortPopup;
    private boolean isSortMenuOpen = false;
    private static final int SORT_BY_CREATED = 0;
    private static final int SORT_BY_MODIFIED = 1;
    private static final int SORT_NEWEST = 0;
    private static final int SORT_OLDEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NoteDbHelper(this);
        
        recyclerView = findViewById(R.id.notesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NoteActivity.class));
        });
        
        emptyView = findViewById(R.id.emptyView);
        
        searchEditText = findViewById(R.id.searchEditText);
        clearButton = findViewById(R.id.clearButton);
        sortButton = findViewById(R.id.sortButton);
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes(s.toString());
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        
        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
            hideKeyboard();
            loadNotes("");
        });

        sortButton.setOnClickListener(this::showSortMenu);
    }

    private void showSortMenu(View anchor) {
        if (sortPopup != null && sortPopup.isShowing()) {
            closeSortMenu();
            return;
        }

        isSortMenuOpen = true;
        animateSortToX();

        View popupView = LayoutInflater.from(this).inflate(R.layout.sort_popup, null);
        
        RadioGroup sortByGroup = popupView.findViewById(R.id.sortByGroup);
        RadioButton sortByCreated = popupView.findViewById(R.id.sortByCreated);
        RadioButton sortByModified = popupView.findViewById(R.id.sortByModified);
        
        ImageButton btnDescending = popupView.findViewById(R.id.btnDescending);
        ImageButton btnAscending = popupView.findViewById(R.id.btnAscending);

        sortByCreated.setChecked(sortBy == SORT_BY_CREATED);
        sortByModified.setChecked(sortBy == SORT_BY_MODIFIED);

        updateOrderButtons(btnDescending, btnAscending, sortOrder);

        sortByGroup.setOnCheckedChangeListener((group, checkedId) -> {
            sortBy = checkedId == R.id.sortByCreated ? SORT_BY_CREATED : SORT_BY_MODIFIED;
            loadNotes(searchEditText.getText().toString());
        });

        btnAscending.setOnClickListener(v -> {
            if (sortOrder != SORT_NEWEST) {
                sortOrder = SORT_NEWEST;
                updateOrderButtons(btnDescending, btnAscending, sortOrder);
                loadNotes(searchEditText.getText().toString());
            }
        });

        btnDescending.setOnClickListener(v -> {
            if (sortOrder != SORT_OLDEST) {
                sortOrder = SORT_OLDEST;
                updateOrderButtons(btnDescending, btnAscending, sortOrder);
                loadNotes(searchEditText.getText().toString());
            }
        });

        sortPopup = new PopupWindow(popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true);
        sortPopup.setBackgroundDrawable(null);
        sortPopup.setOutsideTouchable(true);
        sortPopup.setAnimationStyle(android.R.style.Animation_Dialog);
        
        sortPopup.setOnDismissListener(() -> {
            if (isSortMenuOpen) {
                closeSortMenu();
            }
        });
        
        sortPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, 
            anchor.getRight() - 220, 
            anchor.getBottom() + 114);
    }

    private void closeSortMenu() {
        isSortMenuOpen = false;
        animateXToSort();
    }

    private void updateOrderButtons(ImageButton btnDescending, ImageButton btnAscending, int currentOrder) {
        int accentColor = getResources().getColor(R.color.colorAccent, getTheme());
        int secondaryColor = getResources().getColor(R.color.textSecondary, getTheme());
        
        if (currentOrder == SORT_OLDEST) {
            btnDescending.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            btnAscending.setColorFilter(new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN));
        } else {
            btnDescending.setColorFilter(new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN));
            btnAscending.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
        }
    }

    private void animateSortToX() {
        sortButton.setRotation(0f);
        sortButton.animate()
            .rotation(180f)
            .setDuration(150)
            .setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    sortButton.setImageResource(R.drawable.ic_clear);
                    sortButton.setRotation(0f);
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            })
            .start();
    }

    private void animateXToSort() {
        sortButton.setRotation(0f);
        sortButton.animate()
            .rotation(-180f)
            .setDuration(150)
            .setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    sortButton.setImageResource(R.drawable.ic_sort);
                    sortButton.setRotation(0f);
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            })
            .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(searchEditText.getText().toString());
    }

    private void loadNotes(String searchQuery) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        
        String sortColumn = sortBy == SORT_BY_CREATED ? "created_at" : "modified_at";
        String sortDirection = sortOrder == SORT_NEWEST ? "DESC" : "ASC";
        String sortOrderSql = sortColumn + " " + sortDirection;
        
        if (searchQuery != null && !searchQuery.isEmpty()) {
            cursor = db.rawQuery(
                "SELECT * FROM notes WHERE title LIKE ? OR note_text LIKE ? ORDER BY " + sortOrderSql,
                new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%"}
            );
        } else {
            cursor = db.rawQuery("SELECT * FROM notes ORDER BY " + sortOrderSql, null);
        }
        
        adapter = new NoteAdapter(this, cursor, this);
        recyclerView.setAdapter(adapter);
        
        if (cursor.getCount() == 0) {
            if (searchQuery != null && !searchQuery.isEmpty()) {
                emptyView.setText("No matching notes");
            } else {
                emptyView.setText("No notes yet.\nTap + to create one!");
            }
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    @Override
    public void onNoteClick(long id) {
        Intent intent = new Intent(MainActivity.this, NoteActivity.class);
        intent.putExtra("NOTE_ID", id);
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(long id) {
        showDeleteDialog(id);
    }

    private void showDeleteDialog(long noteId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            dbHelper.getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            loadNotes(searchEditText.getText().toString());
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
