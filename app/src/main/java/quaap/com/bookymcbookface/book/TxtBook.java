package quaap.com.bookymcbookface.book;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tom on 9/16/17.
 */

public class TxtBook extends Book {
    List<String> l = new ArrayList<>();

    public TxtBook(Context context) {
        super(context);
    }

    @Override
    protected void load() throws IOException {
        if (!getFile().exists() || !getFile().canRead()) {
            throw new FileNotFoundException(getFile() + " doesn't exist or not readable");
        }
        File outFile = getBookFile();

        if (!outFile.exists()) {

            try (BufferedReader reader = new BufferedReader(new FileReader(getFile()))) {
                try (Writer out = new FileWriter(outFile)) {
                    StringBuilder para = new StringBuilder(4096);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.matches("^\\s*$")) {
                            para.append(System.lineSeparator());
                            para.append(System.lineSeparator());
                            out.write(para.toString());
                            para.delete(0, para.length());
                        } else {
                            para.append(line);
                            if (!line.matches(".*\\s+$")) {
                                para.append(" ");
                            }
                            //if (line.matches("[.?!\"]\\s*$")) {
                            //    para.append(System.lineSeparator());
                            //}
                        }
                    }
                }
            }
        }

    }

    @NonNull
    private File getBookFile() {
        return new File(getThisBookDir(), getFile().getName());
    }

    @Override
    public Map<String, String> getToc() {
        return null;
    }

    @Override
    protected BookMetadata getMetaData() throws IOException {
        BookMetadata metadata = new BookMetadata();
        metadata.setFilename(getFile().getPath());

        try (BufferedReader reader = new BufferedReader(new FileReader(getFile()))) {
            int c = 0;
            String line;
            Pattern titlerx = Pattern.compile("^\\s*(?i:title)[:= \\t]+(.+)");
            Pattern authorrx = Pattern.compile("^\\s*(?i:author|by)[:= \\t]+(.+)");
            Pattern titleauthorrx = Pattern.compile("^(?xi: \\s* (.+),? \\s+ (?:translated\\s+)? by \\s+ (.+) )");

            boolean foundtitle = false;
            boolean foundauthor = false;
            String ptitle = null;
            String pauthor = null;

            while( (line=reader.readLine())!=null) {
                Matcher tam = titleauthorrx.matcher(line);
                if (tam.find()) {
                    ptitle = tam.group(1);
                    pauthor = tam.group(2);
                }

                Matcher tm = titlerx.matcher(line);
                if (!foundtitle && tm.find()) {
                    metadata.setTitle(tm.group(1));
                    foundtitle = true;
                }
                Matcher am = authorrx.matcher(line);
                if (!foundauthor && am.find()) {
                    metadata.setAuthor(am.group(1));
                    foundauthor = true;
                }
                if (c++>1000 || foundauthor && foundtitle) {
                    break;
                }

            }

            if (!foundtitle && ptitle!=null) {
                metadata.setTitle(ptitle);
                foundtitle = true;
            }
            if (!foundauthor && pauthor!=null) {
                metadata.setAuthor(pauthor);
            }

            if (!foundtitle) {
                metadata.setTitle(getFile().getName());
            }
        }

        //metadata.setTitle(getFile().getName());
        return metadata;
    }

    @Override
    protected List<String> getSectionIds() {

        l.add("1");
        return l;
    }

    @Override
    protected File getFileForSectionID(String id) {
        return getBookFile();
    }

    @Override
    protected File getFileForSection(String section) {
        return getBookFile();
    }

    @Override
    protected String getSectionIDForSection(String section) {
        return "1";
    }



}
