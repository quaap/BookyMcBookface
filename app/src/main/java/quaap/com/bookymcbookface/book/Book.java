package quaap.com.bookymcbookface.book;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
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

    private List<String> sectionIDs;
    private int currentSectionID = 0;


    public Book(Context context, File dataDir) {
        this.dataDir = dataDir;
        this.context = context;
    }

    protected abstract void load();

    public abstract Map<String,String> getToc();

    protected abstract List<String> getSectionIds();

    protected abstract File getFileForSectionID(String id);

    protected abstract File getFileForSection(String section);

    protected abstract String getSectionIDForSection(String section);

    public void load(File file) {
        this.file = file;
        data = context.getSharedPreferences(file.getName(), Context.MODE_PRIVATE);
        load();
        sectionIDs = getSectionIds();
    }

    public File getFirstSection() {
        currentSectionID = 0;
        return getFileForSectionID(sectionIDs.get(currentSectionID));
    }


    public File getNextSection() {
        if (currentSectionID + 1< sectionIDs.size()) {
            currentSectionID++;
            return getFileForSectionID(sectionIDs.get(currentSectionID));
        }
        return null;
    }

    public File getPreviousSection() {
        if (currentSectionID - 1 > 0) {
            currentSectionID--;
            return getFileForSectionID(sectionIDs.get(currentSectionID));
        }
        return null;
    }

    public File gotoSectionID(String id) {
        int pos = sectionIDs.indexOf(id);
        if (pos>-1 && pos < sectionIDs.size()) {
            currentSectionID = pos;
            return getFileForSectionID(sectionIDs.get(currentSectionID));
        }
        return null;
    }

    public File gotoSectionFile(String file) {
        String sectionID = getSectionIDForSection(file);
        if (sectionID!=null) {
            gotoSectionID(sectionID);
            return getFileForSection(file);
        }
        return null;
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
