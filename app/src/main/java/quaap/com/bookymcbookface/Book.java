package quaap.com.bookymcbookface;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * Created by tom on 9/12/17.
 */

public abstract class Book {
    private String title;
    private File file;
    private int pages;
    private int currentPage;
    private File dataDir;
    private SharedPreferences data;
    private Context context;


    public Book(Context context, File dataDir) {
        this.dataDir = dataDir;
        this.context = context;
    }

    protected abstract void load();
    public abstract Page getPage(int page);
    public abstract Page getContents();



    public void load(File file) {
        this.file = file;
        data = context.getSharedPreferences(file.getName(), Context.MODE_PRIVATE);
        load();
    }

    public Page getCurrentPage() {
        return getPage(currentPage);
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

    public int getNumPages() {
        return pages;
    }

    protected void setNumPages(int pages) {
        this.pages = pages;
    }

    public int getCurrentPageNum() {
        return currentPage;
    }

    protected void setCurrentPageNum(int currentPage) {
        this.currentPage = currentPage;
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
