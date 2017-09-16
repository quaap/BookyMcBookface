package quaap.com.bookymcbookface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import quaap.com.bookymcbookface.book.Book;
import quaap.com.bookymcbookface.book.BookMetadata;

public class BookListActivity extends Activity {

    private SharedPreferences data;

    private int nextid = 0;

    private ViewGroup listHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        listHolder = findViewById(R.id.book_list_holder);

        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findFile();
            }
        });

        checkStorageAccess(true);

        data = getSharedPreferences("booklist", Context.MODE_PRIVATE);
        nextid = data.getInt("nextid",0);

        for (int i=0; i<nextid; i++) {
            displayBookListEntry(i);
        }
    }

    private void displayBookListEntry(int bookid) {
        String bookidstr = "book." + bookid;
        String title = data.getString(bookidstr + ".title", null);
        String author = data.getString(bookidstr + ".author", null);
        String filename = data.getString(bookidstr + ".filename", null);

        Log.d("Book", "Filename "  + filename);

        if (filename!=null) {
            ViewGroup listEntry = (ViewGroup)getLayoutInflater().inflate(R.layout.book_list_item, listHolder, false);
            TextView titleView = listEntry.findViewById(R.id.book_title);
            TextView authorView = listEntry.findViewById(R.id.book_author);
            TextView statusView = listEntry.findViewById(R.id.book_status);

            titleView.setText(title);
            //titleView.setText(filename);
            authorView.setText(author);
            listEntry.setTag(bookidstr);
            listEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    readBook((String)view.getTag());

                }
            });

            listEntry.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    longClickBook(view);
                    return false;
                }
            });

            listHolder.addView(listEntry);
        }
    }

    private void readBook(String bookid) {
        String filename = data.getString(bookid + ".filename",null);
        if (filename!=null) {
            Intent main = new Intent(BookListActivity.this, MainActivity.class);
            main.putExtra("filename", filename);
            startActivity(main);
        }
    }

    private void removeBook(String bookid) {
        data.edit()
                .remove(bookid + ".title")
                .remove(bookid + ".author")
                .remove(bookid + ".filename")
         .apply();
    }

    private void addBook(String filename) {
        try {
            BookMetadata metadata = Book.getBookMetaData(filename);

            if (metadata!=null) {
                String bookid = "book." + nextid;

                if (data.contains(bookid + ".filename")) {
                    Toast.makeText(this,"Book already added",Toast.LENGTH_LONG).show();
                }
                data.edit()
                        .putString(bookid + ".title", metadata.getTitle())
                        .putString(bookid + ".author", metadata.getAuthor())
                        .putString(bookid + ".filename", metadata.getFilename())
                .apply();

                displayBookListEntry(nextid);
                nextid++;
                data.edit().putInt("nextid",nextid).apply();
            } else {
                Toast.makeText(this,"Couldn't add " + filename,Toast.LENGTH_LONG).show();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findFile() {

        FsTools fsTools = new FsTools(this);

        fsTools.selectExternalLocation(new FsTools.SelectionMadeListener() {
            @Override
            public void selected(File selection) {
                addBook(selection.getPath());

            }
        }, "epub", false, ".*\\.epub");
    }


    private void longClickBook(final View view) {
        final String bookid = (String)view.getTag();
        PopupMenu menu = new PopupMenu(this, view);
        menu.getMenu().add("Open book").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                readBook(bookid);
                return false;
            }
        });
        menu.getMenu().add("Remove book").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                removeBook(bookid);
                ((ViewGroup)view.getParent()).removeView(view);
                return false;
            }
        });
        menu.show();
    }

    private boolean checkStorageAccess(boolean yay) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    yay? REQUEST_READ_EXTERNAL_STORAGE : REQUEST_READ_EXTERNAL_STORAGE_NOYAY);
            return false;
        }
        return true;
    }

    private static final int REQUEST_READ_EXTERNAL_STORAGE_NOYAY = 4333;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 4334;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean yay = true;
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE_NOYAY:
                yay = false;
            case REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (yay) Toast.makeText(this, "Yay", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Boo", Toast.LENGTH_LONG).show();
                }

        }
    }

}
