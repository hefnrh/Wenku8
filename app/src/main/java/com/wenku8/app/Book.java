package com.wenku8.app;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wenku8.app.com.wenku8.app.util.WenkuHttpClient;
import org.apache.http.Header;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class Book extends ActionBarActivity {

    private BookAdapter adapter;
    private String baseURL;

    private class BookAdapter extends BaseAdapter {

        private List<String> data = new ArrayList<>();
        private List<Boolean> dataIsLink = new ArrayList<>();
        private List<String> urls = new ArrayList<>();

        public void addBook(String title) {
            data.add(title);
            dataIsLink.add(false);
            urls.add("");
            notifyDataSetChanged();
        }

        public void addChapter(String title, String url) {
            data.add(title);
            dataIsLink.add(true);
            urls.add(url);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (dataIsLink.get(position)) {
                convertView = new Button(Book.this);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(Book.this, Content.class);
                        intent.putExtra("url", urls.get(position));
                        startActivity(intent);
                    }
                });
            } else {
                convertView = new TextView(Book.this);
            }
            ((TextView) convertView).setText(data.get(position));
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);
        adapter = new BookAdapter();
        ((ListView) findViewById(R.id.chapter_list_view)).setAdapter(adapter);
        String url = getIntent().getStringExtra("url");
        WenkuHttpClient.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String res;
                try {
                    res = new String(bytes, getString(R.string.charset));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                parseDetail(res);
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                System.out.println(i);
                System.out.println("murippoi mk2");
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.book, menu);
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

    private void parseDetail(String page) {
        int startPos, endPos;
        startPos = page.indexOf("<title>") + 7;
        endPos = page.indexOf("</title>", startPos);
        String bookName = page.substring(startPos, endPos);
        startPos = page.indexOf("小说作者：") + 5;
        endPos = page.indexOf("<", startPos);
        String author = page.substring(startPos, endPos);
        startPos = page.indexOf("最后更新：") + 5;
        endPos = page.indexOf("<", startPos);
        String updateDate = page.substring(startPos, endPos);
        Node summary, catalogueURL;
        try {
            Parser parser = new Parser(page);
            summary = parser.parse(new StringFilter("内容简介")).elementAt(0).getParent().getNextSibling().getNextSibling();
            parser = new Parser(page);
            catalogueURL = parser.parse(new StringFilter("小说目录")).elementAt(0).getParent();
        } catch (ParserException e) {
            e.printStackTrace();
            return;
        }
        setTitle(bookName);
        ((TextView) findViewById(R.id.author_text_view)).setText(author);
        ((TextView) findViewById(R.id.update_data_text_view)).setText(updateDate);
        ((TextView) findViewById(R.id.summary_text_view)).setText(summary.toPlainTextString());

        String tag = catalogueURL.toHtml();
        startPos = tag.indexOf("href") + 6;
        endPos = tag.indexOf("\"", startPos);
        String catalogURL = tag.substring(startPos, endPos);
        baseURL = catalogURL.substring(0, catalogURL.length() - 9);

        WenkuHttpClient.get(tag.substring(startPos, endPos), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String page;
                try {
                    page = new String(bytes, getString(R.string.charset));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                parseChapter(page);
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                System.out.println(i);
                System.out.println("murippoi mk3");
            }
        });
    }

    private void parseChapter(String page) {
        NodeList nodeList;
        try {
            Parser parser = new Parser(page);
            nodeList = parser.parse(new OrFilter(new HasAttributeFilter("class", "vcss"), new HasAttributeFilter("class", "ccss")));
        } catch (ParserException e) {
            e.printStackTrace();
            return;
        }
        for (int i = 0, j = nodeList.size(); i < j; ++i) {
            Node n = nodeList.elementAt(i);
            String tag = n.toHtml();
            int start = tag.indexOf("href");
            if (start < 0) {
                if (tag.contains("vcss")) {
                    adapter.addBook(n.toPlainTextString());
                }
            } else {
                start += 6;
                int end = tag.indexOf("\"", start);
                adapter.addChapter(n.toPlainTextString(), baseURL + tag.substring(start, end));
            }
        }
    }
}
