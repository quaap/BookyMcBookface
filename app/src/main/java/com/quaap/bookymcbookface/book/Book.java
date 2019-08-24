package com.quaap.bookymcbookface.book;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.quaap.bookymcbookface.FsTools;


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

public abstract class Book {
    private static final String FONTSIZE = "fontsize";
    private static final String SECTION_ID_OFFSET = "sectionIDOffset";
    private static final String SECTION_ID = "sectionID";
    private static final String BG_COLOR = "BG_COLOR";
    private String title;
    private File file;

    private final File dataDir;
    private SharedPreferences data;
    private final Context context;

    private List<String> sectionIDs;
    private int currentSectionIDPos = 0;

    private String subbook;
    private File thisBookDir;

    Book(Context context) {
        this.dataDir = context.getFilesDir();
        this.context = context;
        sectionIDs = new ArrayList<>();
    }

    protected abstract void load() throws IOException;

    public abstract Map<String,String> getToc();

    protected abstract BookMetadata getMetaData() throws IOException;

    protected abstract List<String> getSectionIds();

    protected abstract Uri getUriForSectionID(String id);

    //protected abstract Uri getUriForSection(String section);

    //protected abstract String getSectionIDForSection(String section);

    protected abstract ReadPoint locateReadPoint(String section);

    public void load(File file) {
        this.file = file;
        data = getStorage(context, file);

        thisBookDir = getBookDir(context, file);
        thisBookDir.mkdirs();
        try {
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sectionIDs = getSectionIds();
        restoreCurrentSectionID();
    }

    public boolean hasDataDir() {
        return data!=null;
    }

    public Uri getFirstSection() {
        clearSectionOffset();
        currentSectionIDPos = 0;
        saveCurrentSectionID();
        return getUriForSectionID(sectionIDs.get(currentSectionIDPos));
    }

    public Uri getCurrentSection() {
        try {
            restoreCurrentSectionID();
            if (currentSectionIDPos >= sectionIDs.size()) {
                currentSectionIDPos = 0;
                saveCurrentSectionID();
            }

            if (sectionIDs.size() == 0) {
                return null;
            }
            return getUriForSectionID(sectionIDs.get(currentSectionIDPos));
        } catch (Throwable t) {
            Log.e("Booky", t.getMessage(), t);
            return null;
        }
    }

    public void setFontsize(int fontsize) {
        data.edit().putInt(FONTSIZE, fontsize).apply();
    }

    public int getFontsize() {
        return data.getInt(FONTSIZE, -1);
    }

    public void clearFontsize() {
        data.edit().remove(FONTSIZE).apply();
    }

    public void setSectionOffset(int offset) {
        data.edit().putInt(SECTION_ID_OFFSET, offset).apply();
    }

    public int getSectionOffset() {
        if (data==null) {
            return 0;
        }
        return data.getInt(SECTION_ID_OFFSET, -1);
    }

    private void clearSectionOffset() {
        data.edit().remove(SECTION_ID_OFFSET).apply();
    }


    public void setBackgroundColor(int color) {
        data.edit().putInt(BG_COLOR, color).apply();
    }

    public int getBackgroundColor() {
        return data.getInt(BG_COLOR, Integer.MAX_VALUE);
    }

    public void clearBackgroundColor() {
        data.edit().remove(BG_COLOR).apply();
    }


    public void setFlag(String key, boolean value) {
        data.edit().putBoolean(key, value).apply();
    }

    public boolean getFlag(String key, boolean value) {
        return data.getBoolean(key, value);
    }


    public Uri getNextSection() {
        try {
            if (currentSectionIDPos + 1 < sectionIDs.size()) {
                clearSectionOffset();
                currentSectionIDPos++;
                saveCurrentSectionID();
                return getUriForSectionID(sectionIDs.get(currentSectionIDPos));
            }
        } catch (Throwable t) {
            Log.e("Booky", t.getMessage(), t);
        }
        return null;
    }

    public Uri getPreviousSection() {
        try {
            if (currentSectionIDPos - 1 >= 0) {
                clearSectionOffset();
                currentSectionIDPos--;
                saveCurrentSectionID();
                return getUriForSectionID(sectionIDs.get(currentSectionIDPos));
            }
        } catch (Throwable t) {
            Log.e("Booky", t.getMessage(), t);
        }
        return null;
    }

    private void gotoSectionID(String id) {
        try {
            int pos = sectionIDs.indexOf(id);
            if (pos > -1 && pos < sectionIDs.size()) {
                currentSectionIDPos = pos;
                saveCurrentSectionID();
                getUriForSectionID(sectionIDs.get(currentSectionIDPos));
            }
        } catch (Throwable t) {
            Log.e("Booky", t.getMessage(), t);
        }
    }

    public Uri handleClickedLink(String clickedLink) {
        ReadPoint readPoint = locateReadPoint(clickedLink);

        if (readPoint!=null) {
            gotoSectionID(readPoint.getId());
            clearSectionOffset();
            return readPoint.getPoint();
        }
        return null;
    }


    private void saveCurrentSectionID() {
        Log.d("Book", "saving section " + currentSectionIDPos);
        data.edit().putInt(SECTION_ID, currentSectionIDPos).apply();
    }

    private void restoreCurrentSectionID() {
        currentSectionIDPos = data.getInt(SECTION_ID, currentSectionIDPos);
        Log.d("Book", "Loaded section " + currentSectionIDPos);
    }

    private static String makeOldFName(File file) {
        return file.getPath().replaceAll("[/\\\\]","_");
    }

    private static final String reservedChars = "[/\\\\:?\"'*|<>+\\[\\]()]";

    private static String makeFName(File file) {
        String fname = file.getPath().replaceAll(reservedChars,"_");
        if (fname.getBytes().length>60) {
            //for very long names, we take the first part and the last part and the crc.
            // should be unique.
            int len = 30;
            if (fname.length()<=len) {  //just in case I'm missing some utf bytes vs length weirdness here
                len = fname.length()-1;
            }
            fname = fname.substring(0,len) + fname.substring(fname.length()-len/2) + crc32(fname);
        }
        return fname;
    }

    private static long crc32(String input) {
        byte[] bytes = input.getBytes();
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        return checksum.getValue();
    }

    //fix long/invalid filenames while maintaining those that somehow worked.
    private static String getProperFName(Context context, File file) {
        String fname;
        if (hasOldBookDir(context, file)) {
            fname = makeOldFName(file);
            Log.d("Book", "using old fname " + fname);
        } else {
            fname = makeFName(file);
            Log.d("Book", "using new fname " + fname);
        }
        return fname;
    }

    private static boolean hasOldBookDir(Context context, File file) {
        String subbook = "book" + makeOldFName(file);
        return new File(context.getFilesDir(), subbook).exists();
    }

    private static File getBookDir(Context context, File file) {
        String fname = getProperFName(context, file);
        String subbook = "book" + fname;
        return new File(context.getFilesDir(), subbook);
    }

    private static SharedPreferences getStorage(Context context, File file) {
        String fname = getProperFName(context, file);
        return context.getSharedPreferences(fname, Context.MODE_PRIVATE);
    }

    public static void remove(Context context, File file) {
        try {
            FsTools.deleteDir(getBookDir(context, file));
            String fname = getProperFName(context, file);
            if (Build.VERSION.SDK_INT >= 24) {
                context.deleteSharedPreferences(fname);
            } else {
                getStorage(context, file).edit().clear().commit();
            }
        } catch (Exception e) {
            Log.e("Book", e.getMessage(),e);
        }
    }

    public boolean remove() {
        FsTools.deleteDir(getThisBookDir());
        return data.edit().clear().commit();
    }

    File getThisBookDir() {
        return thisBookDir;
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    File getFile() {
        return file;
    }

    private void setFile(File file) {
        this.file = file;
    }


    public File getDataDir() {
        return dataDir;
    }

    protected Context getContext() {
        return context;
    }

    SharedPreferences getSharedPreferences() {
        return data;
    }



    public static String getFileExtensionRX() {
        return ".*\\.(epub|txt|html?)";
    }

    public static Book getBookHandler(Context context, String filename) throws IOException {
        Book book = null;
        if (filename.toLowerCase().endsWith(".epub")) {
            book = new EpubBook(context);
        } else if (filename.toLowerCase().endsWith(".txt")) {
            book = new TxtBook(context);
        } else if (filename.toLowerCase().endsWith(".html") || filename.toLowerCase().endsWith(".htm")) {
            book = new HtmlBook(context);
        }

        return book;

    }

    public static BookMetadata getBookMetaData(Context context, String filename) throws IOException {

        Book book = getBookHandler(context, filename);
        if (book!=null) {
            book.setFile(new File(filename));

            return book.getMetaData();
        }

        return null;

    }

    protected class ReadPoint {
        private String id;
        private Uri point;

        String getId() {
            return id;
        }

        void setId(String id) {
            this.id = id;
        }

        Uri getPoint() {
            return point;
        }

        void setPoint(Uri point) {
            this.point = point;
        }
    }
}
