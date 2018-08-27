package com.quaap.bookymcbookface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final Context mContext;

    public FsTools(Context context) {
        mContext = context;
    }

    private Map<File,String> getDrives() {
        List<File> roots = new ArrayList<>();
        roots.add(new File("/storage"));
        //roots.add(new File("/mnt"));

        try {
            roots.addAll(Arrays.asList(mContext.getExternalFilesDirs(null)));
        } catch (Throwable t) {
            Log.e("storage", t.getMessage(), t);
        }


        Map<File,String> files = new LinkedHashMap<>();

        for(File r: roots) {
            try {
                int sd = 1;
                if (r!=null) {
                    for (File e : r.listFiles()) {
                        try {
                            Log.d("storage", e.getPath() + " " + e.isDirectory());
                            if (e.isDirectory()) { // && !e.getName().equals("emulated") && !e.getName().equals("self")) {
                                boolean removable = false;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    removable = Environment.isExternalStorageRemovable(e);
                                }
                                String name = "SD";
                                if (sd++ > 1) name += sd;
                                files.put(e, removable ? name : e.getName());
                                //Log.d("storage", name + " " + e.getPath());
                            }
                        } catch (IllegalArgumentException ex) {
                            Log.d("storage", e.getPath() + " is no good");
                        } catch (Throwable t) {
                            Log.e("storage", t.getMessage(), t);
                        }

                    }
                }
            } catch (Exception e) {
                Log.e("storage", e.getMessage(), e);
            }
        }


        File ext = Environment.getExternalStorageDirectory();
        if (Environment.isExternalStorageEmulated()) {
            files.put(ext, "Internal");
        } else if (Environment.isExternalStorageRemovable()) {
            files.put(ext, "SD");
        }

        return files;
    }

    private Map<File,String> listDirsInDir(File extdir, final String matchRE, final boolean onlyDirs) {
        Map<File,String> allfiles = new LinkedHashMap<>();

        Map<File,String> drives = getDrives();

        if (extdir==null) {
            return drives;
        }

        List<File> dirs;
        if (!extdir.exists() || !extdir.isDirectory()) {
            dirs = new ArrayList<>();
        } else {
            FilenameFilter filterdirs = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isDirectory();
                }

            };

            try {
                dirs = new ArrayList<>(Arrays.asList(extdir.listFiles(filterdirs)));
            } catch (Exception e) {
                Log.d("FsTools", e.getMessage(), e);
                dirs = new ArrayList<>();
            }
        }

        Collections.sort(dirs, new Comparator<File>() {
            @Override
            public int compare(File s, File t1) {
                return s.getName().compareToIgnoreCase(t1.getName());
            }
        });

        if (extdir.getParent()!=null) {
            if (drives.keySet().contains(extdir)) {
                allfiles.put(null, "← ..");
            } else {
                allfiles.put(extdir.getParentFile(), "← ..");
            }
        }

        for (File dir: dirs) {
            allfiles.put(dir, "\uD83D\uDCC1 " + dir.getName());
        }


        if (!onlyDirs) {
            FilenameFilter filterfiles = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isFile() && (matchRE == null || filename.matches(matchRE));
                }

            };

            List<File> files;
            if (!extdir.exists() || !extdir.isDirectory()) {
                files = new ArrayList<>();
            } else {
                try {
                    files = new ArrayList<>(Arrays.asList(extdir.listFiles(filterfiles)));
                } catch (Exception e) {
                    Log.d("FsTools", e.getMessage(), e);
                    files = new ArrayList<>();
                }
            }

            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File s, File t1) {
                    return s.getName().compareToIgnoreCase(t1.getName());
                }
            });

            for(File file: files) {
                allfiles.put(file, "\uD83D\uDCC4 " + file.getName());
            }
        }

        return allfiles;

    }

    public void selectExternalLocation(final SelectionMadeListener listener, String title, boolean chooseDir) {
        selectExternalLocation(listener, title, null, chooseDir, null);
    }

    public void selectExternalLocation(final SelectionMadeListener listener, String title, boolean chooseDir, String matchRE) {
        selectExternalLocation(listener, title, null, chooseDir, matchRE);
    }


    private void selectExternalLocation(final SelectionMadeListener listener, final String title, final String startdir, final boolean chooseDir, final String matchRE) {

        String dname = startdir==null ? "" : startdir;

        Map<File,String> listDirsInDir = listDirsInDir(startdir==null?null:new File(startdir), matchRE, chooseDir);

        final File [] fileItems = new File[listDirsInDir.size()];
        final String [] showItems  = new String[listDirsInDir.size()];

        int i=0;
        for (Map.Entry<File,String> entry: listDirsInDir.entrySet()) {
            fileItems[i] = entry.getKey();
            showItems[i] = entry.getValue();

            //Log.d("storage2", fileItems[i] + " " + showItems[i]);
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(title + "\n" + dname);

        builder.setItems(showItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                try {
                    File selFile = fileItems[i];
                    if (selFile==null) {
                        selectExternalLocation(listener, title, null, chooseDir, matchRE);
                    } else if (selFile.isDirectory()) {
                        selectExternalLocation(listener, title, selFile.getPath(), chooseDir, matchRE);
                    } else {
                        listener.selected(selFile.getCanonicalFile());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        if (chooseDir && startdir!=null) {
            builder.setPositiveButton(R.string.select_thisfolder, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    listener.selected(new File(startdir));
                }
            });
        }
        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
        //Log.d("storage3", "Here!");
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

    private static File copyFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, false, false);
    }

    public static File compressFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, true, false);
    }


    public static File decompressFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, false, true);
    }



    private static File copyFile(File srcFile, File destFile, boolean compress, boolean decompress){
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

    /*
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
*/
}
