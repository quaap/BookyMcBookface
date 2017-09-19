package quaap.com.bookymcbookface;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

public class ZipReader implements Closeable {

    private ZipInputStream zipInputStream;
    private ZipEntry zipEntry;

    public ZipReader(File srcFile) throws IOException {
        zipInputStream = new ZipInputStream(new FileInputStream(srcFile));
    }


    public boolean moveNext() throws IOException {
        zipEntry = zipInputStream.getNextEntry();
        return zipEntry!=null;
    }

    public String getFilename() {
        if (zipEntry==null) return null;
        return zipEntry.getName();
    }

    public boolean isDirectory() {
        if (zipEntry==null) return false;
        return zipEntry.isDirectory();
    }

    public long getFileSize() {
        if (zipEntry==null) return -1;
        return zipEntry.getSize();
    }

    public long getCrc() {
        if (zipEntry==null) return -1;
        return zipEntry.getCrc();
    }

    public long getFiletime() {
        if (zipEntry==null) return -1;
        return zipEntry.getTime();
    }

    public String getComment() {
        if (zipEntry==null) return null;
        return zipEntry.getComment();
    }


    public CharSequence getFileAsText() throws IOException {
        StringBuilder buff = new StringBuilder(4096);

        Reader reader = new InputStreamReader(zipInputStream);

        char[] buffer = new char[1024];
        int length;
        while ((length = reader.read()) != -1 ) {
            buff.append(buffer, 0, length);
        }
        return buff;
    }

    public byte[] getFileAsBytes() throws IOException {

        ByteArrayOutputStream zout = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;
        while ((length = zipInputStream.read(buffer)) != -1) {
            zout.write(buffer, 0, length);
        }

        return zout.toByteArray();
    }


    @Override
    public void close() throws IOException {
        zipInputStream.close();
    }


//    public static Map<String,CharSequence> extractFilesAsString(File srcFile, String ... searchFiles) {
//
//        Map<String,CharSequence> files = new HashMap<>();
//
//        List<String> searchFilesList = Arrays.asList(searchFiles);
//
//        try {
//            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(srcFile));
//            try {
//                ZipEntry zipEntry;
//                while ((zipEntry = zipInputStream.getNextEntry()) !=null) {
//                    String fname = zipEntry.getName();
//                    Log.d("FsTools", "zip entry " + fname);
//
//                    if (zipEntry.isDirectory()) {
//                        continue;
//                    }
//
//                    if (searchFilesList.contains(fname)) {
//                        StringBuilder buff = new StringBuilder(4096);
//
//                        BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
//
//                        String line;
//                        while ( (line =reader.readLine())!=null) {
//                            buff.append(line);
//                            buff.append(System.lineSeparator());
//                        }
//                        files.put(fname, buff);
//                    }
//
//                }
//            } finally {
//                try {zipInputStream.close();} catch (Exception e) {Log.e("Fs",e.getMessage(),e);}
//            }
//
//
//        } catch (IOException e) {
//            Log.e("FsTools", "extract failed", e);
//        }
//        return null;
//    }


}
