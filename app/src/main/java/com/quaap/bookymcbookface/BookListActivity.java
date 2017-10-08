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
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        listHolder = (ViewGroup)findViewById(R.id.book_list_holder);
        listScroller = (ScrollView)findViewById(R.id.book_list_scroller);
        tv = (TextView)findViewById(R.id.progress_text);
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

        return SortOrder.valueOf(data.getString(SORTORDER_KEY, SortOrder.Default.name()));
    }


    private void populateBooks() {

        showProgress(0);
        Thread.yield();
        SortOrder sortorder = getSortOrder();

        final List<Integer> books = db.getBookIds(sortorder);

        recentread = db.getMostRecentlyRead();

        listHolder.removeAllViews();

        if (recentread>=0) {
            viewAdder.displayBook(recentread);
            books.remove((Integer) recentread);
        }

        new AsyncTask<Void,Void,Void>() {
            //int p = 0;
            @Override
            protected Void doInBackground(Void... voids) {

                for (Integer bookid : books) {

                    viewAdder.displayBook(bookid);

//                    if (p++%3==0) {
//                        viewAdder.showProgress(0);
//                    }
                }
                viewAdder.hideProgress();
                return null;
            }


            @Override
            protected void onCancelled(Void aVoid) {
                viewAdder.hideProgress();
                super.onCancelled(aVoid);
            }
        }.execute();
    }


    private void updateViewTimes() {
        for (int i=0; i<listHolder.getChildCount(); i++) {
            View child = listHolder.getChildAt(i);
            if (child!=null) {
                Integer id = (Integer)child.getTag();
                if (id !=null) {
                    long rt = db.getLastReadTime(id);
                    if (rt>0) {
                        updateBookStatus(child, rt, R.string.book_viewed_on);
                    }
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
    public boolean onOptionsItemSelected(MenuItem item) {

        boolean pop = false;
        switch (item.getItemId()) {
            case R.id.menu_add:
            case R.id.menu_add2:
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
            default:
                return super.onOptionsItemSelected(item);
        }

        if (pop) {
            listHolder.postDelayed(new Runnable() {
                @Override
                public void run() {
                   populateBooks();
                }
            }, 120);
        }
        return true;
    }


    private static String maxlen(String text, int maxlen) {
        if (text!=null && text.length() > maxlen) {
            return text.substring(0, maxlen-3) + "...";
        }
        return text;
    }

    private void displayBookListEntry(int bookid) {
        BookDb.BookRecord book = db.getBookRecord(bookid);

        if (book!=null && book.filename!=null) {
            //Log.d("Book", "Filename "  + filename);

            ViewGroup listEntry = (ViewGroup)getLayoutInflater().inflate(R.layout.book_list_item, listHolder, false);
            TextView titleView = (TextView)listEntry.findViewById(R.id.book_title);
            TextView authorView = (TextView)listEntry.findViewById(R.id.book_author);

            titleView.setText(maxlen(book.title, 120));
            authorView.setText(maxlen(book.author, 50));
            long lastread = book.lastread;

            if (lastread>0) {
                updateBookStatus(listEntry, lastread, R.string.book_viewed_on);
            } else {
                updateBookStatus(listEntry, book.added, R.string.book_added_on);
            }
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

            if (book.id == recentread) {
                listHolder.addView(listEntry,0);
            } else {
                if (lastread>0 && listHolder.getChildCount()>0 && getSortOrder()==SortOrder.Default) {
                    listHolder.addView(listEntry, 1);
                } else {
                    listHolder.addView(listEntry);
                }
            }
        }
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

    private void updateBookStatus(View child, long lastread, int text) {
        TextView statusView = (TextView)child.findViewById(R.id.book_status);
        CharSequence rtime;
        if (text==R.string.book_viewed_on) {
            statusView.setTextSize(14);
            rtime = android.text.format.DateUtils.getRelativeTimeSpanString(lastread);
        } else {
            statusView.setTextSize(10);
            rtime = android.text.format.DateUtils.getRelativeTimeSpanString(this, lastread);
        }
        statusView.setText(getString(text, rtime));
    }

    private void removeBook(int bookid) {
        BookDb.BookRecord book = db.getBookRecord(bookid);
        if (book.filename!=null && book.filename.length()>0) {
            Book.remove(this, new File(book.filename));
        }
        db.removeBook(bookid);
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


    private void addDir(final File dir) {

        viewAdder.showProgress(0);
        new AsyncTask<Void,Void,Void>() {
            volatile int added=0;
            @Override
            protected Void doInBackground(Void... voids) {
                long time = System.currentTimeMillis();
                for(final File file:dir.listFiles()) {
                    if (file.isFile() && file.getName().matches(Book.getFileExtensionRX())) {
                        if (addBook(file.getPath(), false, time)) {
                            added++;
                        }
                        viewAdder.showProgress(added);

                    }
                }
                return null;
            }


            @Override
            protected void onPostExecute(Void aVoid) {
                viewAdder.hideProgress();
                Toast.makeText(BookListActivity.this, getString(R.string.books_added, added), Toast.LENGTH_SHORT).show();
                populateBooks();
            }

            @Override
            protected void onCancelled(Void aVoid) {
                viewAdder.hideProgress();
                super.onCancelled(aVoid);
            }
        }.execute();
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
        menu.getMenu().add(R.string.remove_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                removeBook(bookid);
                ((ViewGroup)view.getParent()).removeView(view);
                return false;
            }
        });
        menu.show();
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
        messageview.setPadding(16,8,16,8);

        final SpannableString s = new SpannableString(message);
        Linkify.addLinks(s, Linkify.ALL);
        messageview.setText(s);
        messageview.setMovementMethod(LinkMovementMethod.getInstance());

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
