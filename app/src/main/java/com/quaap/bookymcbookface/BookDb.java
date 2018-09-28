package com.quaap.bookymcbookface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class BookDb extends SQLiteOpenHelper {

    private final static String DBNAME = "bookdb";
    private final static int DBVERSION = 2;

    private final static String BOOK_TABLE = "book";
    private final static String BOOK_ID = "id";
    private final static String BOOK_TITLE = "title";
    private final static String BOOK_LIB_TITLE = "libtitle";
    private final static String BOOK_AUTHOR = "author";
    private final static String BOOK_LIB_AUTHOR = "libauthor";
    private final static String BOOK_FILENAME = "filename";
    private final static String BOOK_ADDED = "added";
    private final static String BOOK_LASTREAD = "lastread";
    private final static String BOOK_STATUS = "status";


    private final static String WEBS_TABLE = "webs";
    private final static String WEBS_NAME = "name";
    private final static String WEBS_URL = "url";

    private final Context context;

    private final Pattern authorRX;
    private final Pattern titleRX;

    public final static int STATUS_DONE = 128;
    public final static int STATUS_LATER = 32;
    public final static int STATUS_STARTED = 8;
    public final static int STATUS_NONE = 0;
    public final static int STATUS_ANY = -1;
    public final static int STATUS_SEARCH = -2;



    public BookDb(Context context) {
        super(context, DBNAME, null, DBVERSION);
        this.context = context;

        String namePrefixRX="sir|lady|rev(?:erend)?|doctor|dr|mr|ms|mrs|miss";
        String nameSuffixRX="jr|sr|\\S{1,5}\\.d|[jm]\\.?d|[IVX]+|1st|2nd|3rd|esq";
        String nameInfixRX="V[ao]n|De|St\\.?";

        authorRX = Pattern.compile("^\\s*(?:(?i:" + namePrefixRX + ")\\.?\\s+)? (.+?) (?:\\s+|d')?((?:(?:" + nameInfixRX + ")\\s+)? \\S+ (?:\\s+(?i:" + nameSuffixRX + ")\\.?)?)$", Pattern.COMMENTS);
        titleRX = Pattern.compile("^(a|an|the|la|el|le|eine?|der|die)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createbooktable =
                "create table " + BOOK_TABLE + "( " +
                        BOOK_ID + " INTEGER PRIMARY KEY," +
                        BOOK_TITLE + " TEXT," +
                        BOOK_LIB_TITLE + " TEXT," +
                        BOOK_AUTHOR + " TEXT," +
                        BOOK_LIB_AUTHOR + " TEXT," +
                        BOOK_FILENAME + " TEXT," +
                        BOOK_ADDED    + " INTEGER," +
                        BOOK_LASTREAD + " INTEGER," +
                        BOOK_STATUS  + " INTEGER" +
                 ")";
        db.execSQL(createbooktable);

        String [] indexcolums = {BOOK_LIB_TITLE, BOOK_LIB_AUTHOR, BOOK_FILENAME, BOOK_ADDED, BOOK_LASTREAD};

        for (String col: indexcolums) {
            db.execSQL("create index ind_" + col + " on " + BOOK_TABLE + " (" + col + ")");
        }

        String createwebstable =
                "create table " + WEBS_TABLE + "( " +
                        WEBS_URL + " TEXT PRIMARY KEY," +
                        WEBS_NAME + " TEXT" +
                    ")";
        db.execSQL(createwebstable);

        String [] wnames = context.getResources().getStringArray(R.array.getbook_names);
        String [] wurls = context.getResources().getStringArray(R.array.getbook_urls);

        for (int i=0; i<wnames.length; i++) {
            addWebsite(db, wnames[i], wurls[i]);
        }


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion<2) {
            db.execSQL("alter table " + BOOK_TABLE + " add column " + BOOK_STATUS + " INTEGER");

            ContentValues data = new ContentValues();
            data.put(BOOK_STATUS, STATUS_NONE);
            db.update(BOOK_TABLE,data,null, null);

            data = new ContentValues();
            data.put(BOOK_STATUS, STATUS_STARTED);
            db.update(BOOK_TABLE,data,BOOK_LASTREAD + ">0", null);
        }

    }


    public boolean containsBook(String filename) {
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor bookcursor = db.query(BOOK_TABLE,new String[] {BOOK_ID},BOOK_FILENAME + "=?", new String[] {filename}, null, null, null)) {

            return bookcursor.moveToNext();
        }

    }

    public boolean removeBook(String filename) {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(BOOK_TABLE, BOOK_FILENAME + "=?", new String[] {filename})>0;
    }

    public boolean removeBook(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(BOOK_TABLE, BOOK_ID + "=?", new String[] {""+id})>0;
    }

    public int addBook(String filename, String title, String author) {
        return addBook(filename, title, author, System.currentTimeMillis());
    }

    public int addBook(String filename, String title, String author, long dateadded) {

        if (filename==null || containsBook(filename)) return -1;

        if (title==null || title.trim().length()==0) title=filename.replaceAll(".*/","");
        if (author==null || author.trim().length()==0) author="Unknown";

        String libtitle = title.toLowerCase();
        {
            Matcher titlematch = titleRX.matcher(libtitle);
            if (titlematch.find()) {
                libtitle = titlematch.group(2) + ", " + titlematch.group(1);
            }
        }

        String libauthor = author;

        if (!libauthor.contains(",")) {


            Matcher authmatch = authorRX.matcher(libauthor);
            if (authmatch.find()) {
                libauthor = authmatch.group(2) + ", " + authmatch.group(1);
            }
        }
        libauthor = libauthor.toLowerCase();

        //Log.d("AddBook", "libauthor=" + libauthor);

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues data = new ContentValues();
        data.put(BOOK_TITLE, title);
        data.put(BOOK_LIB_TITLE, libtitle);
        data.put(BOOK_AUTHOR, author);
        data.put(BOOK_LIB_AUTHOR, libauthor);
        data.put(BOOK_FILENAME, filename);
        data.put(BOOK_ADDED, dateadded);
        data.put(BOOK_LASTREAD, -1);
        data.put(BOOK_STATUS, STATUS_NONE);

        return (int)db.insert(BOOK_TABLE,null, data);

    }

    public void updateLastRead(int id, long lastread) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues data = new ContentValues();
        data.put(BOOK_LASTREAD, lastread);
        db.update(BOOK_TABLE, data, BOOK_ID + "=?", new String[]{ id + ""});

        // Only change the status if it is NONE
        data = new ContentValues();
        data.put(BOOK_STATUS, STATUS_STARTED);
        db.update(BOOK_TABLE, data,BOOK_ID + "=? and " + BOOK_STATUS + "=" + STATUS_NONE, new String[]{ id + ""});
    }

    public void updateStatus(int id, int status) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues data = new ContentValues();
        data.put(BOOK_STATUS, status);

        db.update(BOOK_TABLE, data, BOOK_ID + "=?", new String[]{ id + ""});
    }


    public BookRecord getBookRecord(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor = db.query(BOOK_TABLE, new String[] {BOOK_ID, BOOK_FILENAME, BOOK_TITLE, BOOK_AUTHOR, BOOK_LASTREAD, BOOK_ADDED, BOOK_STATUS}, BOOK_ID + "=?", new String[] {""+id}, null, null, null)) {

            if (bookscursor.moveToNext()) {
                return getBookRecord(bookscursor);
            }
        }
        return null;
    }

    public long getLastReadTime(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor = db.query(BOOK_TABLE, new String[] {BOOK_LASTREAD}, BOOK_ID + "=?", new String[] {""+id}, null, null, null)) {

            if (bookscursor.moveToNext()) {
                return bookscursor.getLong(bookscursor.getColumnIndex(BOOK_LASTREAD));
            }
        }
        return -1;
    }


    public long getAddedTime(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor = db.query(BOOK_TABLE, new String[] {BOOK_ADDED}, BOOK_ID + "=?", new String[] {""+id}, null, null, null)) {

            if (bookscursor.moveToNext()) {
                return bookscursor.getLong(bookscursor.getColumnIndex(BOOK_ADDED));
            }
        }
        return -1;
    }


    public int getStatus(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor = db.query(BOOK_TABLE, new String[] {BOOK_STATUS}, BOOK_ID + "=?", new String[] {""+id}, null, null, null)) {

            if (bookscursor.moveToNext()) {
                return bookscursor.getInt(bookscursor.getColumnIndex(BOOK_STATUS));
            }
        }
        return 0;
    }


    public int getMostRecentlyRead() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor =
                     db.rawQuery(
                           "select " + BOOK_ID + " from " + BOOK_TABLE +
                             " where " + BOOK_LASTREAD +
                                   " = (select max(" + BOOK_LASTREAD +") from " + BOOK_TABLE + " where " + BOOK_LASTREAD + ">0) and " + BOOK_STATUS +"=" + STATUS_STARTED, null)) {

            if (bookscursor.moveToNext()) {
                return bookscursor.getInt(bookscursor.getColumnIndex(BOOK_ID));
            }
        }
        return -1;
    }

    @NonNull
    private BookRecord getBookRecord(Cursor bookscursor) {
        BookRecord br = new BookRecord();
        br.id = bookscursor.getInt(bookscursor.getColumnIndex(BOOK_ID));
        br.filename = bookscursor.getString(bookscursor.getColumnIndex(BOOK_FILENAME));
        br.title = bookscursor.getString(bookscursor.getColumnIndex(BOOK_TITLE));
        br.author = bookscursor.getString(bookscursor.getColumnIndex(BOOK_AUTHOR));
        br.lastread = bookscursor.getLong(bookscursor.getColumnIndex(BOOK_LASTREAD));
        br.added = bookscursor.getLong(bookscursor.getColumnIndex(BOOK_ADDED));
        br.status = bookscursor.getInt(bookscursor.getColumnIndex(BOOK_STATUS));
        return br;
    }

    public List<BookRecord> getBooks(SortOrder sortOrder) {
        SQLiteDatabase db = this.getReadableDatabase();

        List<BookRecord> books = new ArrayList<>();

        String orderby = BOOK_ADDED;
        switch (sortOrder) {
            case Title: orderby = BOOK_LIB_TITLE; break;
            case Author: orderby = BOOK_LIB_AUTHOR; break;
        }

        try (Cursor bookscursor = db.query(BOOK_TABLE,new String[] {BOOK_ID, BOOK_FILENAME, BOOK_TITLE, BOOK_AUTHOR, BOOK_LASTREAD, BOOK_ADDED},null, null, null, null, orderby)) {

            while (bookscursor.moveToNext()) {
                BookRecord br = getBookRecord(bookscursor);
                books.add(br);
            }
        }

        return books;
    }

    public List<Integer> getBookIds(SortOrder sortOrder, int status) {
        SQLiteDatabase db = this.getReadableDatabase();

        List<Integer> books = new ArrayList<>();

        String where = null;
        if (status>=0) {
            where = BOOK_STATUS + "=" + status;
        }
        //System.out.println("where: " + where);

        String orderby = BOOK_STATUS + ", 2 desc, " + BOOK_LIB_TITLE + " asc";
        switch (sortOrder) {
            case Title: orderby = BOOK_LIB_TITLE + ", 2 desc"; break;
            case Author: orderby = BOOK_LIB_AUTHOR + ", " + BOOK_LIB_TITLE + ", 2 desc"; break;
            case Added: orderby = BOOK_ADDED + " desc, " + BOOK_LIB_TITLE + ", " + BOOK_LIB_AUTHOR ; break;
        }

        try (Cursor bookscursor = db.query(BOOK_TABLE,new String[] {BOOK_ID, BOOK_ADDED + "/80000"}, where, null, null, null, orderby)) {

            while (bookscursor.moveToNext()) {
                books.add(bookscursor.getInt(bookscursor.getColumnIndex(BOOK_ID)));
                //System.out.println(bookscursor.getInt(bookscursor.getColumnIndex(BOOK_STATUS)));
            }
        }

        return books;
    }

    public List<Integer> searchBooks(String text, boolean title, boolean author) {
        SQLiteDatabase db = this.getReadableDatabase();

        List<Integer> books = new ArrayList<>();

        String whereclause = null;

        List<String> whereargs = new ArrayList<>();
        String orderby = "2";

        if (title) {
            whereclause = BOOK_LIB_TITLE + " like ?";
            whereargs.add("%" + text + "%");
            orderby += "," + BOOK_LIB_TITLE;
        }

        if (author) {
            if (whereclause!=null) {
                whereclause += " or ";
            } else {
                whereclause = "";
            }
            whereclause += BOOK_LIB_AUTHOR + " like ?";
            whereargs.add("%" + text + "%");
            orderby += "," + BOOK_LIB_AUTHOR;
        }


        try (Cursor bookscursor = db.query(BOOK_TABLE,new String[] {BOOK_ID, BOOK_ADDED + "/90000"},
                whereclause, whereargs.toArray(new String[whereargs.size()])
                , null, null, orderby)) {

            while (bookscursor.moveToNext()) {
                books.add(bookscursor.getInt(bookscursor.getColumnIndex(BOOK_ID)));
            }
        }

        return books;
    }


    public class BookRecord {
        public int id;
        public String filename;
        public String title;
        public String author;
        public long lastread;
        public long added;
        public int status;

    }


//    String createwebstable =
//            "create table " + WEBS_TABLE + "( " +
//                    WEBS_URL + " TEXT PRIMARY KEY," +
//                    WEBS_NAME + " TEXT" +
//                    ")";


    public int addWebsite(String name, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        return addWebsite(db, name, url);
    }

    private int addWebsite(SQLiteDatabase db, String name, String url) {


        ContentValues data = new ContentValues();
        data.put(WEBS_NAME, name);
        data.put(WEBS_URL, url);

        return (int)db.insert(WEBS_TABLE,null, data);

    }

    public Map<String,String> getWebSites() {

        Map<String,String> webs = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.query(WEBS_TABLE,new String[] {WEBS_URL, WEBS_NAME},null, null, null, null, WEBS_NAME)) {

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(WEBS_NAME));
                String url = cursor.getString(cursor.getColumnIndex(WEBS_URL));
                webs.put(url, name);
            }
        }
        return webs;
    }

    public boolean deleteWebSite(String url) {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(WEBS_TABLE, WEBS_URL + "=?", new String[] {url})>0;
    }
}
