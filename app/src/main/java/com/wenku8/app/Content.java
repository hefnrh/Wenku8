package com.wenku8.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wenku8.app.com.wenku8.app.util.WenkuHttpClient;
import org.apache.http.Header;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.ParserException;

import java.io.UnsupportedEncodingException;


public class Content extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        System.out.println(getIntent().getStringExtra("url"));
        WenkuHttpClient.get(getIntent().getStringExtra("url"), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String page;
                try {
                    page = new String(bytes, getString(R.string.charset));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                String content;
                try {
                    content = new Parser(page).parse(new HasAttributeFilter("id", "content")).elementAt(0).toPlainTextString();
                } catch (ParserException e) {
                    e.printStackTrace();
                    return;
                }
                content = content.replaceAll("&nbsp;", " ");
                ((TextView) findViewById(R.id.content_text_view)).setText(content);
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                System.out.println(i);
                System.out.println("murippoi mk4");
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.content, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
