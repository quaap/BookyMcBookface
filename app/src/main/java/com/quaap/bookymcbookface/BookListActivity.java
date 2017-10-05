package com.quaap.bookymcbookface;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String NEXTID_KEY = "nextid";
    private static final String TITLE_ORDER_KEY = "title_order";
    private static final String AUTHOR_ORDER_KEY = "author_order";
    private static final String BOOK_PREFIX = "book.";
    private static final String FILENAME_SUFF = ".filename";
    private static final String TITLE_SUFF = ".title";
    private static final String AUTHOR_SUFF = ".author";
    private static final String LASTREAD_SUFF = ".lastread";
    private static final String LASTREAD_KEY = "lastread";
    private static final String ID_KEY = ".id";

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

        checkStorageAccess(false);

        data = getSharedPreferences("booklist", Context.MODE_PRIVATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        populateBooks();

    }

    private enum SortOrder {Default, Title, Author}

    private void setSortOrder(SortOrder sortOrder) {
        data.edit().putString(SORTORDER_KEY,sortOrder.name()).apply();
    }

    @NonNull
    private SortOrder getSortOrder() {
        if (!data.contains(SORTORDER_KEY)) {
            setSortOrder(SortOrder.Default);
        }
        return SortOrder.valueOf(data.getString(SORTORDER_KEY, SortOrder.Default.name()));
    }

    private void populateBooks() {

        SortOrder sortorder = getSortOrder();
        nextid = data.getInt(NEXTID_KEY,0);

        final int [] order = new int[nextid];

        for (int i = 0; i < nextid; i++) {
            order[i] = -1;
        }
        if (sortorder==SortOrder.Default) {
            for (int i = 0; i < nextid; i++) {
                order[i] = nextid - 1 - i;
            }

        } else {
            Set<String> list;
            if (sortorder==SortOrder.Title) {
                list = data.getStringSet(TITLE_ORDER_KEY, null);
            } else {
                list = data.getStringSet(AUTHOR_ORDER_KEY, null);
            }
            if (list!=null) {
                int i = 0;
                List<String> alist = new ArrayList<>(list);
                Collections.sort(alist);
                for(String p: alist) {
                    Matcher m = Pattern.compile("\\.(\\d+)$").matcher(p);
                    if (m.find()) {
                        order[i++] = Integer.parseInt(m.group(1));
                    }

                }
            }
        }


        listHolder.removeAllViews();
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                for (int i=0; i<order.length; i++) {
                    final int id = order[i];
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
//        menu.findItem(R.id.menu_sort_default).setChecked(sortorder==SortOrder.Default);
//        menu.findItem(R.id.menu_sort_author).setChecked(sortorder==SortOrder.Author);
//        menu.findItem(R.id.menu_sort_title).setChecked(sortorder==SortOrder.Title);



        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_add:
            case R.id.menu_add2:
                findFile();
                return true;
            case R.id.menu_add_dir:
                findDir();
                return true;
            case R.id.menu_about:
                showMsg(BookListActivity.this,getString(R.string.about), getString(R.string.about_app));
                return true;
            case R.id.menu_sort_default:
                setSortOrder(SortOrder.Default);
                populateBooks();
                item.setChecked(true);
                return true;
            case R.id.menu_sort_author:
                setSortOrder(SortOrder.Author);
                populateBooks();
                item.setChecked(true);
                return true;
            case R.id.menu_sort_title:
                setSortOrder(SortOrder.Title);
                populateBooks();
                item.setChecked(true);
                return true;
            case R.id.menu_gutenberg:
                Uri uri = Uri.parse("http://m.gutenberg.org/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private static String maxlen(String text, int maxlen) {
        if (text!=null && text.length() > maxlen) {
            return text.substring(0, maxlen-3) + "...";
        }
        return text;
    }

    private void displayBookListEntry(int bookid) {
        String bookidstr = BOOK_PREFIX + bookid;
        String filename = data.getString(bookidstr + FILENAME_SUFF, null);


        if (filename!=null) {
            Log.d("Book", "Filename "  + filename);
            String title = data.getString(bookidstr + TITLE_SUFF, null);
            String author = data.getString(bookidstr + AUTHOR_SUFF, null);
            ViewGroup listEntry = (ViewGroup)getLayoutInflater().inflate(R.layout.book_list_item, listHolder, false);
            TextView titleView = (TextView)listEntry.findViewById(R.id.book_title);
            TextView authorView = (TextView)listEntry.findViewById(R.id.book_author);
            TextView statusView = (TextView)listEntry.findViewById(R.id.book_status);

            titleView.setText(maxlen(title, 120));
            authorView.setText(maxlen(author, 50));
            long lastread = data.getLong(bookidstr + LASTREAD_SUFF, Long.MIN_VALUE);

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

            if (data.getString(LASTREAD_KEY, "").equals(bookidstr)) {
                listHolder.addView(listEntry,0);
            } else {
                //if (lastread!=Long.MIN_VALUE && listHolder.getChildCount()>0) {
                //    listHolder.addView(listEntry, 1);
               // } else {
                    listHolder.addView(listEntry);
                //}
            }
        }
    }

    private void readBook(String bookid) {
        String filename = data.getString(bookid + FILENAME_SUFF,null);
        if (filename!=null) {
            data.edit().putLong(bookid + LASTREAD_SUFF, System.currentTimeMillis()).putString(LASTREAD_KEY, bookid).apply();

            Intent main = new Intent(BookListActivity.this, ReaderActivity.class);
            main.putExtra(ReaderActivity.FILENAME, filename);
            startActivity(main);
        }
    }

    private void removeBook(String bookid) {
        String file = data.getString(bookid + FILENAME_SUFF, null);
        String title = data.getString(bookid + TITLE_SUFF, null);
        String author = data.getString(bookid + AUTHOR_SUFF, null);
        int id = data.getInt(bookid + ID_KEY, -1);

        if (file!=null) {
            Book.remove(this, new File(file));
        }

        Set<String> titles = new TreeSet<>(data.getStringSet(TITLE_ORDER_KEY, new TreeSet<String>()));
        titles.remove(title + "." + id );

        Set<String> authors = new TreeSet<>(data.getStringSet(AUTHOR_ORDER_KEY, new TreeSet<String>()));
        authors.remove(author + "." + id);


        data.edit()
                .remove(bookid + ID_KEY)
                .remove(bookid + TITLE_SUFF)
                .remove(bookid + AUTHOR_SUFF)
                .remove(bookid + FILENAME_SUFF)
                .putStringSet(TITLE_ORDER_KEY,titles)
                .putStringSet(AUTHOR_ORDER_KEY,authors)
         .apply();
    }

    private boolean addBook(String filename) {
        return addBook(filename, true);
    }

    private boolean addBook(String filename, boolean showToastWarnings) {
        if (data.getAll().values().contains(filename)) {

            if (showToastWarnings) {
                Toast.makeText(this, getString(R.string.already_added, new File(filename).getName()), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        try {

            BookMetadata metadata = Book.getBookMetaData(this, filename);

            if (metadata!=null) {
                String bookid = BOOK_PREFIX + nextid;

                String title = metadata.getTitle() != null ? metadata.getTitle().toLowerCase():"_" ;
                Set<String> titles = new TreeSet<>(data.getStringSet(TITLE_ORDER_KEY, new TreeSet<String>()));
                titles.add(title + "." + nextid );

                String author = metadata.getAuthor() != null ? metadata.getAuthor().toLowerCase():"_" ;

                String [] authparts = author.split("\\s+");
                if (authparts.length>1) {
                    author = authparts[authparts.length-1];
                    for (int i=0; i<authparts.length-1; i++) {
                        author += " " + authparts[i];
                    }
                }

                Set<String> authors = new TreeSet<>(data.getStringSet(AUTHOR_ORDER_KEY, new TreeSet<String>()));
                authors.add(author + "." + nextid);

                data.edit()
                        .putInt(bookid + ID_KEY, nextid)
                        .putString(bookid + TITLE_SUFF, metadata.getTitle())
                        .putString(bookid + AUTHOR_SUFF, metadata.getAuthor())
                        .putString(bookid + FILENAME_SUFF, metadata.getFilename())
                        .putStringSet(TITLE_ORDER_KEY,titles)
                        .putStringSet(AUTHOR_ORDER_KEY,authors)
                .apply();

                //displayBookListEntry(nextid);
                nextid++;
                data.edit().putInt(NEXTID_KEY,nextid).apply();
                return true;
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

    private void addDir(final File dir) {
        final TextView tv = (TextView)findViewById(R.id.progress_text);
        tv.setVisibility(View.VISIBLE);
        new AsyncTask<Void,Void,Void>() {
            volatile int added=0;
            @Override
            protected Void doInBackground(Void... voids) {

                for(final File file:dir.listFiles()) {
                    if (file.isFile() && file.getName().matches(Book.getFileExtensionRX())) {
                        if (addBook(file.getPath(), false)) {
                            added++;
                        }
                        publishProgress();
//                        listScroller.post(new Runnable() {
//                            @Override
//                            public void run() {
//
//                            }
//                        });
                    }
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
                tv.setText("" + added);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(BookListActivity.this, getString(R.string.books_added, added), Toast.LENGTH_SHORT).show();
                tv.setVisibility(View.GONE);
                populateBooks();
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
}
