package com.quaap.bookymcbookface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

public class FsTools {

    private Context mContext;

    public FsTools(Context context) {
        mContext = context;
    }

    private String [] listDirsInDir(File extdir, final String matchRE, final boolean onlyDirs) {

        FilenameFilter filterdirs = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                return sel.isDirectory();
            }

        };

        List<String> dirs = new ArrayList<>(Arrays.asList(extdir.list(filterdirs)));

        if (extdir.getParent()!=null && !extdir.equals(Environment.getExternalStorageDirectory())) {
            dirs.add(0, "..");
        }

        Collections.sort(dirs, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareToIgnoreCase(t1);
            }
        });

        if (!onlyDirs) {
            FilenameFilter filterfiles = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isFile() && (matchRE == null || filename.matches(matchRE));
                }

            };

            List<String> files = new ArrayList<>(Arrays.asList(extdir.list(filterfiles)));

            Collections.sort(files, new Comparator<String>() {
                @Override
                public int compare(String s, String t1) {
                    return s.compareToIgnoreCase(t1);
                }
            });

            dirs.addAll(files);
        }

        String [] fileslist = dirs.toArray(new String[0]);
        for (int i=0; i<fileslist.length; i++) {
            File sel = new File(extdir, fileslist[i]);
            if (sel.isDirectory()) fileslist[i] = fileslist[i] + "/";
            //Log.d("DD", fileslist[i]);
        }
        return fileslist;

    }

    public void selectExternalLocation(final SelectionMadeListener listener, String title, boolean chooseDir) {
        selectExternalLocation(listener, title, null, chooseDir, null);
    }

    public void selectExternalLocation(final SelectionMadeListener listener, String title, boolean chooseDir, String matchRE) {
        selectExternalLocation(listener, title, null, chooseDir, matchRE);
    }


    public void selectExternalLocation(final SelectionMadeListener listener, final String title, String startdir, final boolean chooseDir, final String matchRE) {
        final File currentDir = startdir==null ? Environment.getExternalStorageDirectory() :  new File(startdir);

        final String [] items = listDirsInDir(currentDir, matchRE, chooseDir);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(title + "\n" +
                currentDir.getPath().replaceFirst(Pattern.quote(Environment.getExternalStorageDirectory().getPath()) + "/?", "/"));

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                try {
                    File selFile = new File(currentDir,items[i]).getCanonicalFile();
                    if (selFile.isDirectory()) {
                        selectExternalLocation(listener, title, selFile.getPath(), chooseDir, matchRE);
                    } else {
                        listener.selected(selFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        if (chooseDir) {
            builder.setPositiveButton(R.string.select_thisfolder, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    listener.selected(currentDir);
                }
            });
        }
        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }


    public interface SelectionMadeListener {
        void selected(File selection);
    }


    public static boolean deleteDir(File dir) {

        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return dir.delete();
    }

    public static File copyFileToDir(File srcFile, File destDir){
        if (!destDir.isDirectory()) throw new IllegalArgumentException("Destination must be a directory");

        return copyFile(srcFile, new File(destDir,srcFile.getName()));
    }

    public static File copyFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, false, false);
    }

    public static File compressFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, true, false);
    }


    public static File decompressFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, false, true);
    }



    public static File copyFile(File srcFile, File destFile, boolean compress, boolean decompress){
        if (destFile.exists() && !destFile.isFile()) throw new IllegalArgumentException("Destination must be a normal file");
        try {

            InputStream fis = new FileInputStream(srcFile);

            try {
                if (decompress) {
                    fis = new GZIPInputStream(fis);
                }

                OutputStream output = new FileOutputStream(destFile);

                try {
                    if (compress) {
                        output = new GZIPOutputStream(output);
                    }

                    // Transfer bytes from the inputfile to the outputfile
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }

                    output.flush();
                    return destFile;
                } finally {
                    try {output.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
                }
            } finally {
                try {fis.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
            }

        } catch (IOException e) {
            Log.e("DB", "Copy failed", e);
        }
        return null;
    }

    /**
     *
     * @param files first files are source, last file is destination file.
     * @return
     */
    public static File compressFiles(File destFile, File ... files) {

        if (files.length<1) throw new IllegalArgumentException("Need at least one source file and exactly one destination file.");
        if (destFile.exists() && !destFile.isFile()) throw new IllegalArgumentException("Destination must be a normal file");

        try {
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(destFile));
            try {

                for (int i=0; i< files.length; i++) {
                    InputStream fis = new FileInputStream(files[i]);
                    try {

                        zout.putNextEntry(new ZipEntry(files[i].getName()));

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zout.write(buffer, 0, length);
                        }


                        zout.closeEntry();

                    } finally {
                        try {fis.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
                    }

                }
            } finally {
                try {zout.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
            }


        } catch (IOException e) {
            Log.e("DB", "Copy failed", e);
        }
        return destFile;
    }


    public static List<File> uncompressFiles(File srcFile, File destDir) {

        List<File> files = new ArrayList<>();


        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(srcFile));
            try {
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) !=null) {
                    File destFile = new File(destDir, zipEntry.getName());

                    if (!destFile.getParentFile().exists()) {
                        if (!destFile.getParentFile().mkdirs()) continue;
                    }
                    if (zipEntry.isDirectory()) {
                        continue;
                    }

                    files.add(destFile);

                    OutputStream zout = new FileOutputStream(destFile);
                    try {

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            zout.write(buffer, 0, length);
                        }

                    } finally {
                        try {zout.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
                    }

                }
            } finally {
                try {zipInputStream.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
            }


        } catch (IOException e) {
            Log.e("DB", "Copy failed", e);
        }
        return files;
    }

    public static Map<String,CharSequence> extractFilesAsString(File srcFile, String ... searchFiles) {

        Map<String,CharSequence> files = new HashMap<>();

        List<String> searchFilesList = Arrays.asList(searchFiles);

        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(srcFile));
            try {
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) !=null) {
                    String fname = zipEntry.getName();
                    Log.d("FsTools", "zip entry " + fname);

                    if (zipEntry.isDirectory()) {
                        continue;
                    }

                    if (searchFilesList.contains(fname)) {
                        StringBuilder buff = new StringBuilder(4096);

                        BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));

                        String line;
                        while ( (line =reader.readLine())!=null) {
                            buff.append(line);
                            buff.append(System.lineSeparator());
                        }
                        files.put(fname, buff);
                    }

                }
            } finally {
                try {zipInputStream.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
            }


        } catch (IOException e) {
            Log.e("FsTools", "extract failed", e);
        }
        return null;
    }


    public static File saveSharedPreferencesToFile(SharedPreferences pref, File destFile) {

        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(destFile));

            output.writeObject(pref.getAll());

        } catch (IOException e) {
            Log.e("FsTools", e.getMessage(), e);
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException e) {
                Log.e("FsTools", e.getMessage(), e);
            }
        }
        return destFile;
    }


    @SuppressWarnings({"unchecked"})
    public static boolean loadSharedPreferencesFromFile(SharedPreferences pref, File src) {
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = pref.edit();
            prefEdit.clear();

            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    prefEdit.putBoolean(key, (Boolean) v);
                } else if (v instanceof Float) {
                    prefEdit.putFloat(key, (Float) v);
                } else if (v instanceof Integer) {
                    prefEdit.putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    prefEdit.putLong(key, (Long) v);
                } else if (v instanceof String) {
                    prefEdit.putString(key, ((String) v));
                } else if (v instanceof Set) {
                    prefEdit.putStringSet(key, ((Set<String>) v));
                } else {
                    Log.d("FsTools", "unknown type for '" + key + "': " + v.getClass());
                }

            }
            if (prefEdit.commit()) {
                Log.d("FsTools", "prefs updated");
            }
            res = true;
        } catch (IOException | ClassNotFoundException e) {
            Log.e("FsTools", e.getMessage(), e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                Log.e("FsTools", e.getMessage(), e);
            }
        }
        return res;
    }

}
