package quaap.com.bookymcbookface.book;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tom on 9/16/17.
 */

public class HtmlBook extends Book {
    List<String> l = new ArrayList<>();

    public HtmlBook(Context context) {
        super(context);
    }

    @Override
    protected void load() throws IOException {
        if (!getFile().exists() || !getFile().canRead()) {
            throw new FileNotFoundException(getFile() + " doesn't exist or not readable");
        }
    }

    @Override
    public Map<String, String> getToc() {
        return null;
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
        readPoint.setPoint(Uri.parse(section));
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
