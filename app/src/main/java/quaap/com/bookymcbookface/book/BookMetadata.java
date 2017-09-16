package quaap.com.bookymcbookface.book;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tom on 9/16/17.
 */

public class BookMetadata {
    private String title;
    private String author;
    private String filename;

    private Map<String,String> alldata = new LinkedHashMap<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Map<String, String> getAlldata() {
        return alldata;
    }

    public void setAlldata(Map<String, String> alldata) {
        this.alldata = alldata;
    }
}
