package quaap.com.bookymcbookface;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by tom on 9/12/17.
 */

public abstract class Book {
    private String title;
    private File file;

    private File dataDir;
    private SharedPreferences data;
    private Context context;


    public Book(Context context, File dataDir) {
        this.dataDir = dataDir;
        this.context = context;
    }

    protected abstract void load();

    public abstract Map<String,String> getToc();

    public abstract List<String> getSectionIds();

    public abstract File getFileForSectionID(String id);

    public abstract File getFileForSection(String section);

    public void load(File file) {
        this.file = file;
        data = context.getSharedPreferences(file.getName(), Context.MODE_PRIVATE);
        load();
    }



    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public File getFile() {
        return file;
    }

    protected void setFile(File file) {
        this.file = file;
    }


    public File getDataDir() {
        return dataDir;
    }

    protected Context getContext() {
        return context;
    }

    protected SharedPreferences getSharedPreferences() {
        return data;
    }
}
