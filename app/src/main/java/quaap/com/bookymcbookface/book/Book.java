package quaap.com.bookymcbookface.book;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.List;
import java.util.Map;

import quaap.com.bookymcbookface.FsTools;


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
    private int currentSectionIDPos = 0;

    private String subbook;
    private File thisBookDir;

    public Book(Context context, File dataDir) {
        this.dataDir = dataDir;
        this.context = context;
    }

    protected abstract void load() throws FileNotFoundException;

    public abstract Map<String,String> getToc();

    protected abstract BookMetadata getMetaData() throws IOException;

    protected abstract List<String> getSectionIds();

    protected abstract File getFileForSectionID(String id);

    protected abstract File getFileForSection(String section);

    protected abstract String getSectionIDForSection(String section);

    public void load(File file) {
        this.file = file;
        data = getStorage(context, file);
        subbook = "book" + makeFName(getFile());
        thisBookDir = new File(getDataDir(), subbook);
        thisBookDir.mkdirs();
        try {
            load();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        sectionIDs = getSectionIds();
        restoreCurrentSectionID();
    }

    public File getFirstSection() {
        currentSectionIDPos = 0;
        saveCurrentSectionID();
        return getFileForSectionID(sectionIDs.get(currentSectionIDPos));
    }

    public File getCurrentSection() {
        restoreCurrentSectionID();
        if (currentSectionIDPos > sectionIDs.size()) {
            currentSectionIDPos = 0;
            saveCurrentSectionID();
        }

        return getFileForSectionID(sectionIDs.get(currentSectionIDPos));
    }


    public void setSectionOffset(int offset) {
        data.edit().putInt("sectionIDOffset", offset).apply();
    }

    public int getSectionOffset() {
        return data.getInt("sectionIDOffset", 0);
    }

    public File getNextSection() {
        if (currentSectionIDPos + 1< sectionIDs.size()) {
            currentSectionIDPos++;
            saveCurrentSectionID();
            return getFileForSectionID(sectionIDs.get(currentSectionIDPos));
        }
        return null;
    }

    public File getPreviousSection() {
        if (currentSectionIDPos - 1 > 0) {
            currentSectionIDPos--;
            saveCurrentSectionID();
            return getFileForSectionID(sectionIDs.get(currentSectionIDPos));
        }
        return null;
    }

    public File gotoSectionID(String id) {
        int pos = sectionIDs.indexOf(id);
        if (pos>-1 && pos < sectionIDs.size()) {
            currentSectionIDPos = pos;
            saveCurrentSectionID();
            return getFileForSectionID(sectionIDs.get(currentSectionIDPos));
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


    private void saveCurrentSectionID() {
        Log.d("Book", "saving section " + currentSectionIDPos);
        data.edit().putInt("sectionID", currentSectionIDPos).apply();
    }

    private void restoreCurrentSectionID() {
        currentSectionIDPos = data.getInt("sectionID", currentSectionIDPos);
        Log.d("Book", "Loaded section " + currentSectionIDPos);
    }


    private static String makeFName(File file) {
        return file.getPath().replaceAll("/|\\\\","_");
    }

    public static SharedPreferences getStorage(Context context, File file) {
        return context.getSharedPreferences(makeFName(file), Context.MODE_PRIVATE);
    }

    public static boolean remove(Context context, File file) {
        if (Build.VERSION.SDK_INT>=24) {
            return context.deleteSharedPreferences(makeFName(file));
        } else {
            return getStorage(context, file).edit().clear().commit();
        }
    }

    public boolean remove() {
        FsTools.deleteDir(getThisBookDir());
        return data.edit().clear().commit();
    }

    public File getThisBookDir() {
        return thisBookDir;
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



    public static String getFileExtensionRX() {
        return ".*\\.(epub|txt|html?)";
    }

    public static Book getBookHandler(Context context, String filename) throws IOException {
        Book book = null;
        if (filename.toLowerCase().endsWith(".epub")) {
            book = new EpubBook(context, context.getFilesDir());
        } else if (filename.toLowerCase().endsWith(".txt")) {
            book = new TxtBook(context, context.getFilesDir());
        }

        return book;

    }

    public static BookMetadata getBookMetaData(Context context, String filename) throws IOException {

        Book book = getBookHandler(context, filename);
        if (book!=null) {
            book.setFile(new File(filename));

            return book.getMetaData();
        }

        return null;

    }
}
