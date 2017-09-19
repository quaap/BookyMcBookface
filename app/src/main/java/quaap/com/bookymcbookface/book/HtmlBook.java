package quaap.com.bookymcbookface.book;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tom on 9/16/17.
 */

public class HtmlBook extends Book {
    List<String> l = new ArrayList<>();
    Map<String,String> toc;

    public HtmlBook(Context context) {
        super(context);
    }

    @Override
    protected void load() throws IOException {
        if (!getFile().exists() || !getFile().canRead()) {
            throw new FileNotFoundException(getFile() + " doesn't exist or not readable");
        }

        toc = new LinkedHashMap<>();

        SharedPreferences bookdat = getSharedPreferences();
        if (bookdat.contains("ordercount")) {
            int toccount = bookdat.getInt("ordercount", 0);

            for (int i = 0; i < toccount; i++) {
                String label = bookdat.getString("toc.label." + i, "");
                String point = bookdat.getString("toc.content." + i, "");

                toc.put(point, label);
                Log.d("EPUB", "TOC: " + label + ". File: " + point);

            }

        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(getFile()))) {
                int c = 0;
                String line;

                Pattern idlinkrx = Pattern.compile("<a\\s+[^>]*\\b(?i:name|id)=\"([^\"]+)\"[^>]*>(?:(.+?)</a>)?");
                Pattern hidlinkrx = Pattern.compile("<h[1-3]\\s+[^>]*\\bid=\"([^\"]+)\"[^>]*>(.+?)</h");

                SharedPreferences.Editor bookdatedit = bookdat.edit();

                while ((line = reader.readLine()) != null) {
                    String id = null;
                    String text = null;
                    Matcher t = idlinkrx.matcher(line);
                    if (t.find()) {
                        id = t.group(1);
                        text = t.group(2);
                    }
                    Matcher t2 = hidlinkrx.matcher(line);
                    if (t2.find()) {
                        id = t2.group(1);
                        text = t2.group(2);
                    }
                    if (id != null) {
                        if (text==null) text=id;
                        bookdatedit.putString("toc.label."+c, text);
                        bookdatedit.putString("toc.content."+c, "#"+id);
                        toc.put("#"+id, text);
                        c++;
                    }


                }
                bookdatedit.putInt("ordercount", c);

                bookdatedit.apply();
            }
        }

    }

    @Override
    public Map<String, String> getToc() {
        return toc;
    }

    @Override
    protected BookMetadata getMetaData() throws IOException {
        BookMetadata metadata = new BookMetadata();
        metadata.setFilename(getFile().getPath());

        try (Reader reader = new FileReader(getFile())) {

            char[] header = new char[8196];
            Pattern titlerx = Pattern.compile("(?is:<title.*?>\\s*(.+?)\\s*</title>)");

            boolean foundtitle = false;

            if(reader.read(header)>0) {
                String line = new String(header);
                Matcher tm = titlerx.matcher(line);
                if (tm.find()) {
                    metadata.setTitle(tm.group(1));
                    foundtitle = true;
                }

            }

            if (!foundtitle) {
                metadata.setTitle(getFile().getName());
            }
        }


        return metadata;
    }

    @Override
    protected List<String> getSectionIds() {

        l.add("1");
        return l;
    }

    @Override
    protected Uri getUriForSectionID(String id) {
        return Uri.fromFile(getFile());
    }

    @Override
    protected ReadPoint locateReadPoint(String section) {
        ReadPoint readPoint = new ReadPoint();
        readPoint.setId("1");

        Uri suri = Uri.parse(section);

        if (suri.isRelative()) {
            suri = new Uri.Builder().scheme("file").path(getFile().getPath()).fragment(suri.getFragment()).build();
        }

        readPoint.setPoint(suri);
        return readPoint;
    }


//    @Override
//    protected Uri getUriForSection(String section) {
//        return Uri.fromFile(getFile());
//    }
//
//    @Override
//    protected String getSectionIDForSection(String section) {
//        return "1";
//    }
//


}
