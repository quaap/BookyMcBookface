package com.quaap.bookymcbookface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

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

public class BookListActivity extends Activity {

    private SharedPreferences data;

    private int nextid = 0;

    private ViewGroup listHolder;
    private ScrollView listScroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        listHolder = (ViewGroup)findViewById(R.id.book_list_holder);
        listScroller = (ScrollView)findViewById(R.id.book_list_scroller);

        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findFile();
            }
        });
        findViewById(R.id.add_dir_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findDir();
            }
        });

        checkStorageAccess(false);

        data = getSharedPreferences("booklist", Context.MODE_PRIVATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        nextid = data.getInt("nextid",0);

        listHolder.removeAllViews();
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                for (int i=0; i<nextid; i++) {
                    final int id = i;
                    listScroller.post(new Runnable() {
                        @Override
                        public void run() {

                            displayBookListEntry(id);
                        }
                    });
                }
                return null;
            }
        }.execute();

    }

    private void displayBookListEntry(int bookid) {
        String bookidstr = "book." + bookid;
        String title = data.getString(bookidstr + ".title", null);
        String author = data.getString(bookidstr + ".author", null);
        String filename = data.getString(bookidstr + ".filename", null);


        if (filename!=null) {
            Log.d("Book", "Filename "  + filename);
            ViewGroup listEntry = (ViewGroup)getLayoutInflater().inflate(R.layout.book_list_item, listHolder, false);
            TextView titleView = (TextView)listEntry.findViewById(R.id.book_title);
            TextView authorView = (TextView)listEntry.findViewById(R.id.book_author);
            TextView statusView = (TextView)listEntry.findViewById(R.id.book_status);

            titleView.setText(title);
            authorView.setText(author);
            long lastread = data.getLong(bookidstr + ".lastread", Long.MIN_VALUE);

            if (lastread!=Long.MIN_VALUE) {

                statusView.setText(getString(R.string.book_viewed_on, android.text.format.DateUtils.getRelativeTimeSpanString(lastread)));
                //statusView.setText(getString(R.string.book_viewed_on, new SimpleDateFormat("YYYY-MM-dd HH:mm", Locale.getDefault()).format(new Date(lastread))));
            }
            listEntry.setTag(bookidstr);
            listEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    readBook((String)view.getTag());
                }
            });

            listEntry.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    longClickBook(view);
                    return false;
                }
            });

            if (data.getString("lastread", "").equals(bookidstr)) {
                listHolder.addView(listEntry,0);
            } else {
                listHolder.addView(listEntry);
            }
        }
    }

    private void readBook(String bookid) {
        String filename = data.getString(bookid + ".filename",null);
        if (filename!=null) {
            data.edit().putLong(bookid + ".lastread", System.currentTimeMillis()).putString("lastread", bookid).apply();

            Intent main = new Intent(BookListActivity.this, ReaderActivity.class);
            main.putExtra("filename", filename);
            startActivity(main);
        }
    }

    private void removeBook(String bookid) {
        String file = data.getString(bookid + ".filename", null);

        if (file!=null) {
            Book.remove(this, new File(file));
        }

        data.edit()
                .remove(bookid + ".title")
                .remove(bookid + ".author")
                .remove(bookid + ".filename")
         .apply();
    }

    private boolean addBook(String filename) {
        return addBook(filename, true);
    }

    private boolean addBook(String filename, boolean showAlreadyAddedWarning) {
        if (data.getAll().values().contains(filename)) {

            if (showAlreadyAddedWarning) {
                Toast.makeText(this, getString(R.string.already_added, new File(filename).getName()), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        try {

            BookMetadata metadata = Book.getBookMetaData(this, filename);

            if (metadata!=null) {
                String bookid = "book." + nextid;

                data.edit()
                        .putString(bookid + ".title", metadata.getTitle())
                        .putString(bookid + ".author", metadata.getAuthor())
                        .putString(bookid + ".filename", metadata.getFilename())
                .apply();

                displayBookListEntry(nextid);
                nextid++;
                data.edit().putInt("nextid",nextid).apply();
                return true;
            } else {
                Toast.makeText(this,getString(R.string.coulndt_add_book, new File(filename).getName()),Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Log.e("BookList", e.getMessage(), e);
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

                }
            }, getString(R.string.find_book), false, Book.getFileExtensionRX());
        }
    }

    private void addDir(final File dir) {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                for(final File file:dir.listFiles()) {
                    if (file.isFile() && file.getName().matches(Book.getFileExtensionRX())) {
                        listScroller.post(new Runnable() {
                            @Override
                            public void run() {
                                addBook(file.getPath(), false);
                            }
                        });
                    }
                }
                return null;
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
        final String bookid = (String)view.getTag();
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

}
