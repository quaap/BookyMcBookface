package com.quaap.bookymcbookface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class GetBooksActivity extends Activity implements View.OnClickListener{

    EditText nameBox;
    EditText urlBox;

    LinearLayout list;

    BookDb db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_books);

        db = BookyApp.getDB(this);

        list = (LinearLayout)findViewById(R.id.webs_list);

        nameBox = (EditText)findViewById(R.id.web_name);
        urlBox = (EditText)findViewById(R.id.web_url);
        Button add = (Button)findViewById(R.id.web_add);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (urlBox.getText().length()>0) {
                    String url = urlBox.getText().toString();
                    String name = url.replaceAll("^https?://([\\w\\-.])(/.*)","$1");
                    if (nameBox.getText().length()>0) {
                        name = nameBox.getText().toString();
                    }
                    db.addWebsite(name, url);
                    displayWeb(name, url, true);
                }
            }
        });

        findViewById(R.id.web_new).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetBooksActivity.this.findViewById(R.id.web_add_layout).setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
            }
        });



        Map<String,String> webs = db.getWebSites();

        for (Map.Entry<String,String> web: webs.entrySet()) {
            displayWeb(web.getValue(), web.getKey());
        }

    }

    private void displayWeb(String name, String url) {
        displayWeb(name, url, false);
    }

    private void displayWeb(String name, String url, boolean first) {
        TextView v = new TextView(this);
        v.setTextSize(24);
        v.setTextColor(Color.BLUE);
        v.setPadding(16,16,8,8);
        v.setText(name);
        v.setTag(url);
        v.setOnClickListener(this);
        if (first) {
            list.addView(v, 0);
        } else {
            list.addView(v);
        }

    }

    @Override
    public void onClick(View v) {
        try {
            String url = (String) v.getTag();
            if (url != null) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e("Webs", e.getMessage(), e);
        }
    }
}
