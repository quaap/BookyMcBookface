package com.quaap.bookymcbookface;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
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

    private SharedPreferences data;


    private ViewGroup listHolder;
    private ScrollView listScroller;

    private BookListAdderHandler viewAdder;
    private TextView tv;

    private BookDb db;
    private int recentread;
    private boolean showingSearch;

    private int showStatus = BookDb.STATUS_ANY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        listHolder = findViewById(R.id.book_list_holder);
        listScroller = findViewById(R.id.book_list_scroller);
        tv = findViewById(R.id.progress_text);
        checkStorageAccess(false);

        data = getSharedPreferences("booklist", Context.MODE_PRIVATE);

        if (!data.contains(SORTORDER_KEY)) {
            setSortOrder(SortOrder.Default);
        }

        //getApplicationContext().deleteDatabase(BookDb.DBNAME);

        db = BookyApp.getDB(this);

        viewAdder = new BookListAdderHandler(this);
        update();

        recentread = db.getMostRecentlyRead();

        listScroller.postDelayed(new Runnable() {
            @Override
            public void run() {

                populateBooks();
            }
        }, 100);
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateViewTimes();

        int seen = 40;
        String seennewsKey = "seennews";

        if (seen != data.getInt(seennewsKey, -1)) {
            viewAdder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BookListActivity.this, "New! !"+ getString(R.string.search_your_books) + "!", Toast.LENGTH_LONG).show();
                }
            },3000);

            data.edit().putInt(seennewsKey, seen).apply();
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

    private void update() {
        String UPDATED = "UPDATE_DONE";
        if (data.getBoolean(UPDATED, false)) return;

        String NEXTID_KEY = "nextid";
        String TITLE_ORDER_KEY = "title_order";
        String AUTHOR_ORDER_KEY = "author_order";
        String FILENAME_SUFF = ".filename";
        String TITLE_SUFF = ".title";
        String AUTHOR_SUFF = ".author";
        String LASTREAD_SUFF = ".lastread";
        String ID_KEY = ".id";
        String BOOK_PREFIX = "book.";
        //String LASTREAD_KEY = "lastread";


        int nextid = data.getInt(NEXTID_KEY,0);

        for (int i = 0; i < nextid; i++) {
            String bookidstr = BOOK_PREFIX + i;
            String filename = data.getString(bookidstr + FILENAME_SUFF, null);


            if (filename!=null) {
                //Log.d("Book", "Filename "  + filename);
                Log.d("Book", "Updating Filename "  + filename);

                String title = data.getString(bookidstr + TITLE_SUFF, null);
                String author = data.getString(bookidstr + AUTHOR_SUFF, null);

                int id = db.addBook(filename,title,author);

                if (id>-1) {
                    long lastread = data.getLong(bookidstr + LASTREAD_SUFF, Long.MIN_VALUE);

                    if (lastread > 0) {
                        db.updateLastRead(id, lastread);
                    }

                    data.edit()
                            .remove(bookidstr + ID_KEY)
                            .remove(bookidstr + TITLE_SUFF)
                            .remove(bookidstr + AUTHOR_SUFF)
                            .remove(bookidstr + FILENAME_SUFF)
                            .remove(bookidstr + LASTREAD_SUFF)
                       .apply();

                }

            }
        }

        data.edit().remove(TITLE_ORDER_KEY).remove(AUTHOR_ORDER_KEY).apply();

        data.edit().putBoolean(UPDATED, true).apply();
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
        showStatus = status;
        SortOrder sortorder = getSortOrder();
        final List<Integer> books = db.getBookIds(sortorder, status);

        boolean showRecent = false;
        int title = R.string.app_name;
        switch (status) {
            case BookDb.STATUS_ANY:
                title = R.string.book_status_any;
                showRecent = true;
                break;
            case BookDb.STATUS_NONE:
                title = R.string.book_status_none;
                break;
            case BookDb.STATUS_STARTED:
                title = R.string.book_status_started;
                if (sortorder==SortOrder.Default) {
                    showRecent = true;
                }
                break;
            case BookDb.STATUS_DONE:
                title = R.string.book_status_completed2;
                break;
            case BookDb.STATUS_LATER:
                title = R.string.book_status_later2;
                break;

        }
        BookListActivity.this.setTitle(title);
        populateBooks(books,  showRecent);
        invalidateOptionsMenu();
    }


    private void searchBooks(String searchfor, boolean stitle, boolean sauthor) {
        showStatus = BookDb.STATUS_ANY;
        List<Integer> books = db.searchBooks(searchfor, stitle, sauthor);
        populateBooks(books, false);
        BookListActivity.this.setTitle(getString(R.string.search_res_title, searchfor, books.size()));
        showingSearch = true;
        invalidateOptionsMenu();
    }

    private void populateBooks(final List<Integer> books, boolean showRecent) {

        showProgress(0);
        Thread.yield();

        listHolder.removeAllViews();

        if (showRecent) {
            recentread = db.getMostRecentlyRead();
            if (recentread >= 0) {
                viewAdder.displayBook(recentread);
                books.remove((Integer) recentread);
            }
        }

        new DisplayBooksTask(this, books).execute();
    }

    private static class DisplayBooksTask extends  AsyncTask<Void,Void,Void> {

        private WeakReference<BookListActivity> blactref;
        private List<Integer> books;

        DisplayBooksTask(BookListActivity blact, List<Integer> books) {
            blactref = new WeakReference<>(blact);
            this.books = books;
        }

        //int p = 0;
        @Override
        protected Void doInBackground(Void... voids) {

            BookListActivity blact = blactref.get();
            if (blact!=null) {
                for (Integer bookid : books) {

                    blact.viewAdder.displayBook(bookid);

//                    if (p++%3==0) {
//                        viewAdder.showProgress(0);
//                    }
                }
                blact.viewAdder.hideProgress();
            }
            return null;
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


    private void updateViewTimes() {
        for (int i=0; i<listHolder.getChildCount(); i++) {
            View child = listHolder.getChildAt(i);
            if (child!=null) {
                Integer id = (Integer)child.getTag();
                if (id !=null) {

                    BookDb.BookRecord book = db.getBookRecord(id);
                    updateBookDisplay(book, child);

                }
            }
        }
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
        }

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("Booky", "onPrepareOptionsMenu called, showingSearch=" + showingSearch);
        super.onPrepareOptionsMenu(menu);

        for (int i=0; i<menu.size()-4; i++) {
            menu.getItem(i).setVisible(!(showingSearch || showStatus!=BookDb.STATUS_ANY));
        }

        menu.findItem(R.id.menu_all_books).setVisible(true);
        menu.findItem(R.id.menu_open_books).setVisible(true);
        menu.findItem(R.id.menu_unopen_books).setVisible(true);

        menu.findItem(R.id.menu_completed_books).setVisible(true);

        menu.findItem(R.id.menu_later_books).setVisible(true);

        menu.findItem(R.id.menu_search_books).setVisible(true);



        switch (showStatus) {
            case BookDb.STATUS_ANY:
                menu.findItem(R.id.menu_all_books).setVisible(false);
                break;
            case BookDb.STATUS_DONE:
                menu.findItem(R.id.menu_completed_books).setVisible(false);
                break;
            case BookDb.STATUS_LATER:
                menu.findItem(R.id.menu_later_books).setVisible(false);
                break;
            case BookDb.STATUS_NONE:
                menu.findItem(R.id.menu_unopen_books).setVisible(false);
                break;
            case BookDb.STATUS_STARTED:
                menu.findItem(R.id.menu_open_books).setVisible(false);
                break;
        }




        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

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
            case R.id.menu_get_books:
                Intent intent = new Intent(this, GetBooksActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_completed_books:
                pop = true;
                showStatus = BookDb.STATUS_DONE;
                break;
            case R.id.menu_later_books:
                pop = true;
                showStatus = BookDb.STATUS_LATER;
                break;
            case R.id.menu_open_books:
                pop = true;
                showStatus = BookDb.STATUS_STARTED;
                break;
            case R.id.menu_unopen_books:
                pop = true;
                showStatus = BookDb.STATUS_NONE;
                break;
            case R.id.menu_search_books:
                showSearch();
                break;
            case R.id.menu_all_books:
                pop = true;
                showStatus = BookDb.STATUS_ANY;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        if (pop) {
            listHolder.postDelayed(new Runnable() {
                @Override
                public void run() {
                   populateBooks(showStatus);
                    invalidateOptionsMenu();
                }
            }, 120);
        }

        invalidateOptionsMenu();
        return true;
    }


    private static String maxlen(String text, int maxlen) {
        if (text!=null && text.length() > maxlen) {
            int minus = text.length()>3?3:0;

            return text.substring(0, maxlen-minus) + "...";
        }
        return text;
    }

    private void displayBookListEntry(int bookid) {
        BookDb.BookRecord book = db.getBookRecord(bookid);

        if (book!=null && book.filename!=null) {
            //Log.d("Book", "Filename "  + filename);

            ViewGroup listEntry = (ViewGroup)getLayoutInflater().inflate(R.layout.book_list_item, listHolder, false);
            TextView titleView = listEntry.findViewById(R.id.book_title);
            TextView authorView = listEntry.findViewById(R.id.book_author);

            titleView.setText(maxlen(book.title, 120));
            authorView.setText(maxlen(book.author, 50));

            long lastread = updateBookDisplay(book, listEntry);

            listEntry.setTag(bookid);
            listEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    readBook((int)view.getTag());
                }
            });

            listEntry.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    longClickBook(view);
                    return false;
                }
            });

            SortOrder sortOrder = getSortOrder();

            if (sortOrder==SortOrder.Default) {
                if (book.id == recentread) {
                    listHolder.addView(listEntry, 0);
                } else {
                    if (book.status == 0 && lastread > 0 && listHolder.getChildCount() > 0) {
                        listHolder.addView(listEntry, 1);
                    } else {
                        listHolder.addView(listEntry);
                    }
                }
            } else {
                listHolder.addView(listEntry);
            }
        }
    }

    private long updateBookDisplay(BookDb.BookRecord book, View listEntry) {
        long lastread = book.lastread;

        if (book.status==BookDb.STATUS_DONE) {
            updateBookStatus(listEntry, lastread, R.string.book_status_completed);
        } else if (book.status==BookDb.STATUS_LATER) {
            updateBookStatus(listEntry, 0, R.string.book_status_later);
        } else if (lastread>0) {
            updateBookStatus(listEntry, lastread, R.string.book_viewed_on);
        } else {
            updateBookStatus(listEntry, book.added, R.string.book_added_on);
        }
        return lastread;
    }

    private void readBook(final int bookid) {

        final BookDb.BookRecord book = db.getBookRecord(bookid);

        if (book!=null && book.filename!=null) {
            //data.edit().putString(LASTREAD_KEY, BOOK_PREFIX + book.id).apply();

            final long now = System.currentTimeMillis();
            db.updateLastRead(bookid, now);
            recentread = bookid;

            listHolder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (int i=0; i<listHolder.getChildCount(); i++) {
                        View child = listHolder.getChildAt(i);
                        if (child!=null) {
                            Integer id = (Integer)child.getTag();
                            if (id !=null && id == bookid) {
                                updateBookStatus(child, now, R.string.book_viewed_on);

                                listScroller.smoothScrollTo(0,0);

                                listHolder.removeView(child);
                                listHolder.addView(child, 0);

                                listScroller.smoothScrollTo(0,0);

                                break;
                            }
                        }
                    }

                    Intent main = new Intent(BookListActivity.this, ReaderActivity.class);
                    main.putExtra(ReaderActivity.FILENAME, book.filename);
                    startActivity(main);

                }
            }, 300);
        }
    }

    private void updateBookStatus(View child, long time, int text) {
        TextView statusView = child.findViewById(R.id.book_status);
        CharSequence rtime = android.text.format.DateUtils.getRelativeTimeSpanString(time);

        statusView.setTextSize(12);

        if (text==R.string.book_viewed_on) {
            statusView.setTextSize(14);
        }
        statusView.setText(getString(text, rtime));
    }

    private void removeBook(int bookid, boolean delete, boolean resettime) {
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
        } else if (resettime) {
            db.updateLastRead(bookid, -1);
        }
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
        private WeakReference<BookListActivity> blactref;
        private File dir;


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
                    updateBookStatus(bookid, view, BookDb.STATUS_DONE);
                    if (lastread > 0) {
                        removeBook(bookid, false, false);
                    }
                    return false;
                }
            });
        }

        if (status!=BookDb.STATUS_LATER) {
            menu.getMenu().add(R.string.mark_later).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    updateBookStatus(bookid, view, BookDb.STATUS_LATER);
                    return false;
                }
            });
        }

        if (status!=BookDb.STATUS_NONE && status!=BookDb.STATUS_STARTED) {
            menu.getMenu().add(R.string.un_mark).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {

                    updateBookStatus(bookid, view, lastread>0 ? BookDb.STATUS_STARTED : BookDb.STATUS_NONE);
                    return false;
                }
            });
        }


        if (lastread>0) {

            menu.getMenu().add(R.string.close_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    removeBook(bookid, false, true);
                    updateViewTimes();
                    listHolder.invalidate();
                    return false;
                }
            });
        }


        menu.getMenu().add(R.string.remove_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                ((ViewGroup)view.getParent()).removeView(view);
                removeBook(bookid, true, false);
                return false;
            }
        });
        menu.show();
    }

    private void updateBookStatus(int bookid, View view, int status) {
        db.updateStatus(bookid, status);
//        listHolder.removeView(view);
//        listHolder.addView(view);
        updateViewTimes();
        listHolder.invalidate();
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

    public static void showMsg(Context context, String title, String message) {
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

    public void showSearch() {
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
                data.edit()
                        .putString("__LAST_SEARCH_STR__", searchfor)
                        .putBoolean("__LAST_TITLE__", title.isChecked())
                        .putBoolean("__LAST_AUTHOR__", author.isChecked())
                        .apply();

                if (!searchfor.trim().isEmpty()) {
                    boolean stitle = title.isChecked() || authortitle.isChecked();
                    boolean sauthor = author.isChecked() || authortitle.isChecked();

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

        private static final int ADD_BOOK = 1001;
        private static final int SHOW_PROGRESS = 1002;
        private static final int HIDE_PROGRESS = 1003;
        private final WeakReference<BookListActivity> weakReference;

        BookListAdderHandler(BookListActivity blInstance) {
            weakReference = new WeakReference<>(blInstance);
        }

        void displayBook(int bookid) {
            Message msg=new Message();
            msg.arg1 = BookListAdderHandler.ADD_BOOK;
            msg.arg2 = bookid;
            sendMessage(msg);
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
                    case ADD_BOOK:
                        blInstance.displayBookListEntry(msg.arg2);
                        break;
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
