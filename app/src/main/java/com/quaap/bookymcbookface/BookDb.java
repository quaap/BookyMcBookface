package com.quaap.bookymcbookface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tom on 10/6/17.
 */

public class BookDb extends SQLiteOpenHelper {

    public final static String DBNAME = "bookdb";
    private final static int DBVERSION = 1;

    private final static String BOOK_TABLE = "book";
    private final static String BOOK_ID = "id";
    private final static String BOOK_TITLE = "title";
    private final static String BOOK_LIB_TITLE = "libtitle";
    private final static String BOOK_AUTHOR = "author";
    private final static String BOOK_LIB_AUTHOR = "libauthor";
    private final static String BOOK_FILENAME = "filename";
    private final static String BOOK_ADDED = "added";
    private final static String BOOK_LASTREAD = "lastread";




    public BookDb(Context context) {
        super(context, DBNAME, null, DBVERSION);
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
                        BOOK_LASTREAD + " INTEGER" +
                 ")";
        db.execSQL(createbooktable);

//        String [] indexcolums = {BOOK_LIB_TITLE, BOOK_LIB_AUTHOR, BOOK_FILENAME, BOOK_ADDED, BOOK_LASTREAD};
//
//        for (String col: indexcolums) {
//
//        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

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

        if (filename==null || containsBook(filename)) return -1;

        if (title==null) title=filename.replaceAll(".*/","");
        if (author==null) author="Unknown";

        String libtitle = title.toLowerCase();
        {
            Matcher titlematch = Pattern.compile("^(a|an|the)\\s+(.+)$", Pattern.CASE_INSENSITIVE).matcher(libtitle);
            if (titlematch.find()) {
                libtitle = titlematch.group(2) + ", " + titlematch.group(1);
            }
        }

        String libauthor = author.toLowerCase();

        if (!libauthor.contains(",")) {

            Matcher authmatch = Pattern.compile("^\\s*(?:(?i:sir|lady|rev|doctor)\\s+)? (.+?) \\s+ ((?:(?:V[ao]n|De)\\s+)? \\S+ (?:\\s+(?i:jr|sr|m\\.?d|ph\\.?d|j\\.?d|[IVX]+|1st|2nd|3rd)\\.?)?)$", Pattern.COMMENTS).matcher(libauthor);
            if (authmatch.find()) {
                libauthor = authmatch.group(2) + ", " + authmatch.group(1);
            }
        }

        //Log.d("AddBook", "libauthor=" + libauthor);

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues data = new ContentValues();
        data.put(BOOK_TITLE, title);
        data.put(BOOK_LIB_TITLE, libtitle);
        data.put(BOOK_AUTHOR, author);
        data.put(BOOK_LIB_AUTHOR, libauthor);
        data.put(BOOK_FILENAME, filename);
        data.put(BOOK_ADDED, System.currentTimeMillis());
        data.put(BOOK_LASTREAD, -1);

        return (int)db.insert(BOOK_TABLE,null, data);

    }

    public void updateLastRead(int id, long lastread) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues data = new ContentValues();
        data.put(BOOK_LASTREAD, lastread);

        db.update(BOOK_TABLE, data, BOOK_ID + "=?", new String[]{ id + ""});
    }

    public BookRecord getBookRecord(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor = db.query(BOOK_TABLE, new String[] {BOOK_ID, BOOK_FILENAME, BOOK_TITLE, BOOK_AUTHOR, BOOK_LASTREAD}, BOOK_ID + "=?", new String[] {""+id}, null, null, null)) {

            if (bookscursor.moveToNext()) {
                return getBookRecord(bookscursor);
            }
        }
        return null;
    }

    public int getMostRecentlyRead() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor bookscursor =
                     db.rawQuery(
                           "select " + BOOK_ID + " from " + BOOK_TABLE +
                             " where " + BOOK_LASTREAD + " = (select max(" + BOOK_LASTREAD +") from " + BOOK_TABLE + ")", null)) {

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

        try (Cursor bookscursor = db.query(BOOK_TABLE,new String[] {BOOK_ID, BOOK_FILENAME, BOOK_TITLE, BOOK_AUTHOR, BOOK_LASTREAD},null, null, null, null, orderby)) {

            while (bookscursor.moveToNext()) {
                BookRecord br = getBookRecord(bookscursor);
                books.add(br);
            }
        }

        return books;
    }

    public List<Integer> getBookIds(SortOrder sortOrder) {
        SQLiteDatabase db = this.getReadableDatabase();

        List<Integer> books = new ArrayList<>();

        String orderby = BOOK_ADDED;
        switch (sortOrder) {
            case Title: orderby = BOOK_LIB_TITLE; break;
            case Author: orderby = BOOK_LIB_AUTHOR; break;
        }

        try (Cursor bookscursor = db.query(BOOK_TABLE,new String[] {BOOK_ID},null, null, null, null, orderby)) {

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

    }




}
