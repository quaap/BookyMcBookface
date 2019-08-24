package com.quaap.bookymcbookface;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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

public class Zip {

    public static List<File> unzip(File srcFile, File destDir) {
        List<File> files = new ArrayList<>();


        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(srcFile));
            try {
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) !=null) {
                    File destFile = new File(destDir.getCanonicalPath(), zipEntry.getName());
                    String canonicalPath = destFile.getCanonicalPath();
                    if (!canonicalPath.startsWith(destDir.getCanonicalPath())) {
                        continue;
                    }

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
}
