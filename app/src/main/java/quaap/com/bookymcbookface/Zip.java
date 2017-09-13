package quaap.com.bookymcbookface;

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
 * Created by tom on 9/12/17.
 */

public class Zip {

    public static List<File> unzip(File srcFile, File destDir) {
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
}
