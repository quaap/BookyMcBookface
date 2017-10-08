package com.quaap.bookymcbookface;

import android.app.Application;
import android.content.Context;

/**
 * Created by tom on 10/8/17.
 */

public class BookyApp extends Application {

    private BookDb db;

    @Override
    public void onCreate() {
        super.onCreate();

        db = new BookDb(this);
    }

    public static BookDb getDB(Context context) {
        return ((BookyApp)context.getApplicationContext()).db;
    }


    @Override
    public void onTerminate() {
        if (db!=null) db.close();

        super.onTerminate();
    }
}
