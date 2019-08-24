package com.quaap.bookymcbookface;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.quaap.bookymcbookface.book.Book;
import com.quaap.bookymcbookface.book.BookMetadata;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of BookyMcBookface and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

public class BookListActivity extends AppCompatActivity {

    private static final String SORTORDER_KEY = "sortorder";
    private static final String LASTSHOW_STATUS_KEY = "LastshowStatus";
    private static final String STARTWITH_KEY = "startwith";
    private static final String ENABLE_SCREEN_PAGE_KEY = "screenpaging";
    private static final String ENABLE_DRAG_SCROLL_KEY = "dragscroll";

    private static final int STARTLASTREAD = 1;
    private static final int STARTOPEN = 2;
    private static final int STARTALL = 3;

    private static final String ACTION_SHOW_OPEN = "com.quaap.bookymcbookface.SHOW_OPEN_BOOKS";
    private static final String ACTION_SHOW_UNREAD = "com.quaap.bookymcbookface.SHOW_UNREAD_BOOKS";
    public static final String ACTION_SHOW_LAST_STATUS = "com.quaap.bookymcbookface.SHOW_LAST_STATUS";

    private SharedPreferences data;

    private BookAdapter bookAdapter;

    private BookListAdderHandler viewAdder;
    private TextView tv;

    private BookDb db;
    private int recentread;
    private boolean showingSearch;

    private int showStatus = BookDb.STATUS_ANY;

    public final String SHOW_STATUS = "showStatus";

    public final static String prefname = "booklist";

    private boolean openLastread = false;
    private static boolean alreadyStarted=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        tv = findViewById(R.id.progress_text);
        checkStorageAccess(false);

        data = getSharedPreferences(prefname, Context.MODE_PRIVATE);

        viewAdder = new BookListAdderHandler(this);

        if (!data.contains(SORTORDER_KEY)) {
            setSortOrder(SortOrder.Default);
        }

        //getApplicationContext().deleteDatabase(BookDb.DBNAME);

        db = BookyApp.getDB(this);

        RecyclerView listHolder = findViewById(R.id.book_list_holder);
        listHolder.setLayoutManager(new LinearLayoutManager(this));
        listHolder.setItemAnimator(new DefaultItemAnimator());

        bookAdapter = new BookAdapter(this, db, new ArrayList<Integer>());
        bookAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readBook((int)view.getTag());
            }
        });
        bookAdapter.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                longClickBook(view);
                return false;
            }
        });

        listHolder.setAdapter(bookAdapter);

        processIntent(getIntent());

        //Log.d("BOOKLIST", "onCreate end");

    }

    @Override
    protected void onNewIntent(Intent intent) {
        //Log.d("BOOKLIST", "onNewIntent");
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {

        recentread = db.getMostRecentlyRead();

        showStatus = BookDb.STATUS_ANY;

        openLastread = false;

        boolean hadSpecialOpen = false;
        //Intent intent = getIntent();
        if (intent != null) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case ACTION_SHOW_OPEN:
                        showStatus = BookDb.STATUS_STARTED;
                        hadSpecialOpen = true;
                        break;
                    case ACTION_SHOW_UNREAD:
                        showStatus = BookDb.STATUS_NONE;
                        hadSpecialOpen = true;
                        break;
                    case ACTION_SHOW_LAST_STATUS:
                        showStatus = data.getInt(LASTSHOW_STATUS_KEY, BookDb.STATUS_ANY);
                        hadSpecialOpen = true;
                        break;
                }

            }
        }

        if (!hadSpecialOpen){

            switch (data.getInt(STARTWITH_KEY, STARTLASTREAD)) {
                case STARTLASTREAD:
                    if (recentread!=-1 && data.getBoolean(ReaderActivity.READEREXITEDNORMALLY, true)) openLastread = true;
                    break;
                case STARTOPEN:
                    showStatus = BookDb.STATUS_STARTED; break;
                case STARTALL:
                    showStatus = BookDb.STATUS_ANY;
            }
        }


    }

    @Override
    protected void onResume() {
        //Log.d("BOOKLIST", "onResume");
        super.onResume();
        if (openLastread) {
            openLastread = false;
            viewAdder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        BookDb.BookRecord book = db.getBookRecord(recentread);
                        getReader(book, true);
                        //finish();
                    } catch (Exception e) {
                        data.edit().putInt(STARTWITH_KEY, STARTALL).apply();
                    }
                }
            }, 200);
        } else {
            populateBooks(showStatus);
        }
    }

    @Override
    public void onBackPressed() {
        if (showingSearch || showStatus!=BookDb.STATUS_ANY) {
            setTitle(R.string.app_name);
            populateBooks();
            showingSearch = false;
        } else {
            super.onBackPressed();
        }
    }

    private void setSortOrder(SortOrder sortOrder) {
        data.edit().putString(SORTORDER_KEY,sortOrder.name()).apply();
    }

    @NonNull
    private SortOrder getSortOrder() {

        try {
            return SortOrder.valueOf(data.getString(SORTORDER_KEY, SortOrder.Default.name()));
        } catch (IllegalArgumentException e) {
            Log.e("Booklist", e.getMessage(), e);
            return SortOrder.Default;
        }
    }

    private void populateBooks() {
        populateBooks(BookDb.STATUS_ANY);
    }

    private void populateBooks(int status) {
        //Log.d("BOOKLIST", "populateBooks " + status);
        showStatus = status;
        data.edit().putInt(LASTSHOW_STATUS_KEY, showStatus).apply();

        boolean showRecent = false;
        int title = R.string.app_name;
        switch (status) {
            case BookDb.STATUS_SEARCH:
                String lastSearch = data.getString("__LAST_SEARCH_STR__","");
                if (!lastSearch.trim().isEmpty()) {
                    boolean stitle = data.getBoolean("__LAST_TITLE__", true);
                    boolean sauthor = data.getBoolean("__LAST_AUTHOR__", true);
                    searchBooks(lastSearch, stitle, sauthor);
                    return;
                }
            case BookDb.STATUS_ANY:
                title = R.string.book_status_any;
                showRecent = true;
                showingSearch = false;
                break;
            case BookDb.STATUS_NONE:
                title = R.string.book_status_none;
                showingSearch = false;
                break;
            case BookDb.STATUS_STARTED:
                title = R.string.book_status_started;
                showRecent = true;
                showingSearch = false;
                break;
            case BookDb.STATUS_DONE:
                title = R.string.book_status_completed2;
                showingSearch = false;
                break;
            case BookDb.STATUS_LATER:
                title = R.string.book_status_later2;
                showingSearch = false;
                break;
        }
        BookListActivity.this.setTitle(title);

        SortOrder sortorder = getSortOrder();
        final List<Integer> books = db.getBookIds(sortorder, status);
        populateBooks(books,  showRecent);

        invalidateOptionsMenu();
    }


    private void searchBooks(String searchfor, boolean stitle, boolean sauthor) {
        showStatus = BookDb.STATUS_SEARCH;
        data.edit().putInt(LASTSHOW_STATUS_KEY, showStatus).apply();
        List<Integer> books = db.searchBooks(searchfor, stitle, sauthor);
        populateBooks(books, false);
        BookListActivity.this.setTitle(getString(R.string.search_res_title, searchfor, books.size()));
        showingSearch = true;
        invalidateOptionsMenu();
    }

    private void populateBooks(final List<Integer> books, boolean showRecent) {

        if (showRecent) {
            recentread = db.getMostRecentlyRead();
            if (recentread >= 0) {
                //viewAdder.displayBook(recentread);
                books.remove((Integer) recentread);
                books.add(0, (Integer)recentread);
            }
        }

        bookAdapter.setBooks(books);
    }


    private void updateViewTimes() {
        bookAdapter.notifyItemRangeChanged(0, bookAdapter.getItemCount());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);

        SortOrder sortorder = getSortOrder();

        switch (sortorder) {
            case Default:
                menu.findItem(R.id.menu_sort_default).setChecked(true);
                break;
            case Author:
                menu.findItem(R.id.menu_sort_author).setChecked(true);
                break;
            case Title:
                menu.findItem(R.id.menu_sort_title).setChecked(true);
                break;
            case Added:
                menu.findItem(R.id.menu_sort_added).setChecked(true);
                break;
        }

        switch (data.getInt(STARTWITH_KEY, STARTLASTREAD)) {
            case STARTALL:
                menu.findItem(R.id.menu_start_all_books).setChecked(true);
                break;
            case STARTOPEN:
                menu.findItem(R.id.menu_start_open_books).setChecked(true);
                break;
            case STARTLASTREAD:
                menu.findItem(R.id.menu_start_last_read).setChecked(true);
                break;
        }

        return true;
    }


    private MenuItem enableScrollMenu;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Log.d("Booky", "onPrepareOptionsMenu called, showingSearch=" + showingSearch);
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.menu_add).setVisible(!showingSearch);
        menu.findItem(R.id.menu_add_dir).setVisible(!showingSearch);
        menu.findItem(R.id.menu_get_books).setVisible(!showingSearch);
        menu.findItem(R.id.menu_sort).setVisible(!showingSearch);

        MenuItem screenPaging = menu.findItem(R.id.menu_enable_screen_paging);
        screenPaging.setChecked(data.getBoolean(ENABLE_SCREEN_PAGE_KEY, true));

        enableScrollMenu = menu.findItem(R.id.menu_enable_scroll);
        enableScrollMenu.setChecked(data.getBoolean(ENABLE_DRAG_SCROLL_KEY, true));
        enableScrollMenu.setEnabled(screenPaging.isChecked());

        switch (showStatus) {
            case BookDb.STATUS_ANY:
                menu.findItem(R.id.menu_all_books).setChecked(true);
                break;
            case BookDb.STATUS_DONE:
                menu.findItem(R.id.menu_completed_books).setChecked(true);
                break;
            case BookDb.STATUS_LATER:
                menu.findItem(R.id.menu_later_books).setChecked(true);
                break;
            case BookDb.STATUS_NONE:
                menu.findItem(R.id.menu_unopen_books).setChecked(true);
                break;
            case BookDb.STATUS_STARTED:
                menu.findItem(R.id.menu_open_books).setChecked(true);
                break;
        }

        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int status = BookDb.STATUS_ANY;
        boolean pop = false;
        switch (item.getItemId()) {
            case R.id.menu_add:
            //case R.id.menu_add2:
                findFile();
                break;
            case R.id.menu_add_dir:
                findDir();
                break;
            case R.id.menu_about:
                showMsg(BookListActivity.this,getString(R.string.about), getString(R.string.about_app));
                break;
            case R.id.menu_sort_default:
                item.setChecked(true);
                setSortOrder(SortOrder.Default);
                pop = true;
                break;
            case R.id.menu_sort_author:
                item.setChecked(true);
                setSortOrder(SortOrder.Author);
                pop = true;
                break;
            case R.id.menu_sort_title:
                item.setChecked(true);
                setSortOrder(SortOrder.Title);
                pop = true;
                break;
            case R.id.menu_sort_added:
                item.setChecked(true);
                setSortOrder(SortOrder.Added);
                pop = true;
                break;
            case R.id.menu_get_books:
                Intent intent = new Intent(this, GetBooksActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_completed_books:
                pop = true;
                status = BookDb.STATUS_DONE;
                break;
            case R.id.menu_later_books:
                pop = true;
                status = BookDb.STATUS_LATER;
                break;
            case R.id.menu_open_books:
                pop = true;
                status = BookDb.STATUS_STARTED;
                break;
            case R.id.menu_unopen_books:
                pop = true;
                status = BookDb.STATUS_NONE;
                break;
            case R.id.menu_search_books:
                showSearch();
                break;
            case R.id.menu_all_books:
                pop = true;
                status = BookDb.STATUS_ANY;
                break;
            case R.id.menu_start_all_books:
                data.edit().putInt(STARTWITH_KEY, STARTALL).apply(); break;
            case R.id.menu_start_open_books:
                data.edit().putInt(STARTWITH_KEY, STARTOPEN).apply(); break;
            case R.id.menu_start_last_read:
                data.edit().putInt(STARTWITH_KEY, STARTLASTREAD).apply(); break;
            case R.id.menu_enable_screen_paging:
                item.setChecked(!item.isChecked());
                data.edit().putBoolean(ENABLE_SCREEN_PAGE_KEY, item.isChecked()).apply();
                if (enableScrollMenu!=null) enableScrollMenu.setEnabled(item.isChecked());
                break;
            case R.id.menu_enable_scroll:
                item.setChecked(!item.isChecked());
                data.edit().putBoolean(ENABLE_DRAG_SCROLL_KEY, item.isChecked()).apply(); break;
            default:

                return super.onOptionsItemSelected(item);
        }


        final int statusf = status;
        if (pop) {
            viewAdder.postDelayed(new Runnable() {
                @Override
                public void run() {
                   populateBooks(statusf);
                    invalidateOptionsMenu();
                }
            }, 120);
        }

        invalidateOptionsMenu();
        return true;
    }


    public static String maxlen(String text, int maxlen) {
        if (text!=null && text.length() > maxlen) {
            int minus = text.length()>3?3:0;

            return text.substring(0, maxlen-minus) + "...";
        }
        return text;
    }

    private void readBook(final int bookid) {

        final BookDb.BookRecord book = db.getBookRecord(bookid);

        if (book!=null && book.filename!=null) {
            //data.edit().putString(LASTREAD_KEY, BOOK_PREFIX + book.id).apply();

            final long now = System.currentTimeMillis();
            db.updateLastRead(bookid, now);
            recentread = bookid;

            viewAdder.postDelayed(new Runnable() {
                @Override
                public void run() {

                    getReader(book,true);

                }
            }, 300);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {

                try {

                    ShortcutManager shortcutManager = (ShortcutManager) getSystemService(Context.SHORTCUT_SERVICE);
                    if (shortcutManager!=null) {
                        Intent readBook = getReader(book,false);


                        ShortcutInfo readShortcut = new ShortcutInfo.Builder(this, "id1")
                                .setShortLabel(getString(R.string.shortcut_latest))
                                .setLongLabel(getString(R.string.shortcut_latest_title, maxlen(book.title, 24)))
                                .setIcon(Icon.createWithResource(BookListActivity.this, R.mipmap.ic_launcher_round))
                                .setIntent(readBook)
                                .build();



                        shortcutManager.setDynamicShortcuts(Collections.singletonList(readShortcut));
                    }
                } catch(Exception e) {
                    Log.e("Booky", e.getMessage(), e);
                }
            }


        }
    }

    private Intent getReader(BookDb.BookRecord book, boolean start) {
        Intent readBook = new Intent(BookListActivity.this, ReaderActivity.class);
        readBook.putExtra(ReaderActivity.FILENAME, book.filename);
        readBook.putExtra(ReaderActivity.SCREEN_PAGING, data.getBoolean(ENABLE_SCREEN_PAGE_KEY, true));
        readBook.putExtra(ReaderActivity.DRAG_SCROLL, data.getBoolean(ENABLE_DRAG_SCROLL_KEY, true));
        readBook.setAction(Intent.ACTION_VIEW);
        if (start) {
            bookAdapter.notifyItemIdChanged(book.id);
            startActivity(readBook);
        }
        return readBook;
    }


    private void removeBook(int bookid, boolean delete) {
        BookDb.BookRecord book = db.getBookRecord(bookid);
        if (book==null) {
            Toast.makeText(this, "Bug? The book doesn't seem to be in the database",Toast.LENGTH_LONG).show();
            return;
        }
        if (book.filename!=null && book.filename.length()>0) {
            Book.remove(this, new File(book.filename));
        }
        if (delete) {
            db.removeBook(bookid);
            if (bookAdapter!=null) bookAdapter.notifyItemIdRemoved(bookid);
        }
//        else if (status!=BookDb.STATUS_ANY) {
//            //db.updateLastRead(bookid, -1);
//            db.updateStatus(bookid, status);
//        }
        recentread = db.getMostRecentlyRead();
    }

    private boolean addBook(String filename) {
        return addBook(filename, true, System.currentTimeMillis());
    }

    private boolean addBook(String filename, boolean showToastWarnings, long dateadded) {

        try {
            if (db.containsBook(filename)) {

                if (showToastWarnings) {
                    Toast.makeText(this, getString(R.string.already_added, new File(filename).getName()), Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            BookMetadata metadata = Book.getBookMetaData(this, filename);

            if (metadata!=null) {

                return db.addBook(filename, metadata.getTitle(), metadata.getAuthor(), dateadded) > -1;

            } else if (showToastWarnings) {
                Toast.makeText(this,getString(R.string.coulndt_add_book, new File(filename).getName()),Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("BookList", "File: " + filename  + ", " + e.getMessage(), e);
        }
        return false;
    }

    private void findFile() {

        FsTools fsTools = new FsTools(this);

        if (checkStorageAccess(false)) {
            fsTools.selectExternalLocation(new FsTools.SelectionMadeListener() {
                @Override
                public void selected(File selection) {
                    addBook(selection.getPath());
                    populateBooks();

                }
            }, getString(R.string.find_book), false, Book.getFileExtensionRX());
        }
    }

    private void showProgress(int added) {

        if (tv.getVisibility() != View.VISIBLE) {
            tv.setVisibility(View.VISIBLE);
            tv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
        if (added>0) {
            tv.setText(getString(R.string.added_numbooks, added));
        } else {
            tv.setText(R.string.loading);
        }
    }

    private void hideProgress() {
        tv.setVisibility(View.GONE);
    }


    private void addDir( File dir) {

        viewAdder.showProgress(0);
        new AddDirTask(this, dir).execute(dir);
    }

    private static class AddDirTask extends  AsyncTask<File,Void,Void> {

        int added=0;
        private final WeakReference<BookListActivity> blactref;
        private final File dir;


        AddDirTask(BookListActivity blact,  File dir) {
            blactref = new WeakReference<>(blact);
            this.dir = dir;
        }

        @Override
        protected Void doInBackground(File... dirs) {
            BookListActivity blact = blactref.get();
            if (blact!=null && dirs!=null) {
                long time = System.currentTimeMillis();
                for (File d : dirs) {
                    try {
                        if (d == null || !d.isDirectory()) continue;
                        for (final File file : d.listFiles()) {
                            try {
                                if (file == null) continue;
                                if (file.isFile() && file.getName().matches(Book.getFileExtensionRX())) {
                                    if (blact.addBook(file.getPath(), false, time)) {
                                        added++;
                                    }
                                    blact.viewAdder.showProgress(added);

                                } else if (file.isDirectory()) {
                                    doInBackground(file);
                                }
                            } catch (Exception e) {
                                Log.e("Booky", e.getMessage(), e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Booky", e.getMessage(), e);
                    }
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            BookListActivity blact = blactref.get();
            if (blact!=null) {
                blact.viewAdder.hideProgress();
                Toast.makeText(blact, blact.getString(R.string.books_added, added), Toast.LENGTH_LONG).show();
                blact.populateBooks();
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            BookListActivity blact = blactref.get();
            if (blact!=null) {
                blact.viewAdder.hideProgress();
            }
            super.onCancelled(aVoid);
        }
    }

    private void findDir() {

        FsTools fsTools = new FsTools(this);

        if (checkStorageAccess(false)) {
            fsTools.selectExternalLocation(new FsTools.SelectionMadeListener() {
                @Override
                public void selected(File selection) {
                    addDir(selection);
                }
            }, getString(R.string.find_folder), true);
        }
    }


    private void longClickBook(final View view) {
        final int bookid = (int)view.getTag();
        PopupMenu menu = new PopupMenu(this, view);
        menu.getMenu().add(R.string.open_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                readBook(bookid);
                return false;
            }
        });

        final int status = db.getStatus(bookid);
        final long lastread = db.getLastReadTime(bookid);

        if (status!=BookDb.STATUS_DONE) {
            menu.getMenu().add(R.string.mark_completed).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (lastread > 0) {
                        removeBook(bookid, false);
                    } else {
                        db.updateLastRead(bookid, System.currentTimeMillis());
                    }
                    updateBookStatus(bookid, view, BookDb.STATUS_DONE);

                    return false;
                }
            });
        }

        if (status!=BookDb.STATUS_LATER && status!=BookDb.STATUS_DONE) {
            menu.getMenu().add(R.string.mark_later).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    updateBookStatus(bookid, view, BookDb.STATUS_LATER);
                    return false;
                }
            });
        }

        if (status==BookDb.STATUS_LATER || status==BookDb.STATUS_DONE) {
            menu.getMenu().add(R.string.un_mark).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {

                    updateBookStatus(bookid, view, lastread>0 ? BookDb.STATUS_STARTED : BookDb.STATUS_NONE);
                    return false;
                }
            });
        }


        if (status==BookDb.STATUS_STARTED) {

            menu.getMenu().add(R.string.close_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    removeBook(bookid, false);
                    updateBookStatus(bookid, view, BookDb.STATUS_NONE);
                    //updateViewTimes();
                    return false;
                }
            });
        }


        menu.getMenu().add(R.string.remove_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //((ViewGroup)view.getParent()).removeView(view);
                removeBook(bookid, true);
                return false;
            }
        });
        menu.show();
    }

    private void updateBookStatus(int bookid, View view, int status) {
        db.updateStatus(bookid, status);
        if (bookAdapter!=null) bookAdapter.notifyItemIdChanged(bookid);
//        listHolder.removeView(view);
//        listHolder.addView(view);
 //       updateViewTimes();
    }

    private boolean checkStorageAccess(boolean yay) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    yay? REQUEST_READ_EXTERNAL_STORAGE : REQUEST_READ_EXTERNAL_STORAGE_NOYAY);
            return false;
        }
        return true;
    }

    private static final int REQUEST_READ_EXTERNAL_STORAGE_NOYAY = 4333;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 4334;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean yay = true;
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE_NOYAY:
                yay = false;
            case REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (yay) Toast.makeText(this, "Yay", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Boo", Toast.LENGTH_LONG).show();
                }

        }
    }

    private static void showMsg(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        final TextView messageview = new TextView(context);
        messageview.setPadding(32,8,32,8);

        final SpannableString s = new SpannableString(message);
        Linkify.addLinks(s, Linkify.ALL);
        messageview.setText(s);
        messageview.setMovementMethod(LinkMovementMethod.getInstance());
        messageview.setTextSize(18);

        builder.setView(messageview);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSearch() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(android.R.string.search_go);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.search, null);
        builder.setView(dialogView);

        final EditText editText =  dialogView.findViewById(R.id.search_text);
        final RadioButton author = dialogView.findViewById(R.id.search_author);
        final RadioButton title = dialogView.findViewById(R.id.search_title);
        final RadioButton authortitle = dialogView.findViewById(R.id.search_authortitle);

        builder.setPositiveButton(android.R.string.search_go, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String searchfor = editText.getText().toString();

                if (!searchfor.trim().isEmpty()) {
                    boolean stitle = title.isChecked() || authortitle.isChecked();
                    boolean sauthor = author.isChecked() || authortitle.isChecked();
                    data.edit()
                            .putString("__LAST_SEARCH_STR__", searchfor)
                            .putBoolean("__LAST_TITLE__", stitle)
                            .putBoolean("__LAST_AUTHOR__", sauthor)
                            .apply();

                    searchBooks(searchfor, stitle, sauthor);
                } else {
                    dialogInterface.cancel();
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);

        editText.setFocusable(true);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        title.setChecked(data.getBoolean("__LAST_TITLE__", false));
        author.setChecked(data.getBoolean("__LAST_AUTHOR__", false));

        String lastSearch = data.getString("__LAST_SEARCH_STR__","");
        editText.setText(lastSearch);
        editText.setSelection(lastSearch.length());
        editText.setSelection(0, lastSearch.length());

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!lastSearch.isEmpty());

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setEnabled(!editText.getText().toString().trim().isEmpty());
            }
        });


        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editText.setImeActionLabel(getString(android.R.string.search_go), EditorInfo.IME_ACTION_SEARCH);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                } else if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || event == null
                        || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (!editText.getText().toString().trim().isEmpty()) {
                        editText.clearFocus();

                        if (imm != null) imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                    }
                    return true;
                }

                return false;
            }
        });

        editText.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (imm!=null) imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);

    }

    private static class BookListAdderHandler extends Handler {

        private static final int SHOW_PROGRESS = 1002;
        private static final int HIDE_PROGRESS = 1003;
        private final WeakReference<BookListActivity> weakReference;

        BookListAdderHandler(BookListActivity blInstance) {
            weakReference = new WeakReference<>(blInstance);
        }


        void showProgress(int progress) {
            Message msg=new Message();
            msg.arg1 = BookListAdderHandler.SHOW_PROGRESS;
            msg.arg2 = progress;
            sendMessage(msg);
        }
        void hideProgress() {
            Message msg=new Message();
            msg.arg1 = BookListAdderHandler.HIDE_PROGRESS;
            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            BookListActivity blInstance = weakReference.get();
            if (blInstance != null) {
                switch (msg.arg1) {

                    case SHOW_PROGRESS:
                        blInstance.showProgress(msg.arg2);
                        break;
                    case HIDE_PROGRESS:
                        blInstance.hideProgress();
                        break;
                }
            }
        }
    }

}
