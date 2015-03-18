package com.wenku8.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wenku8.app.com.wenku8.app.util.WenkuHttpClient;
import org.apache.http.Header;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by GuYifan on 2015/3/16.
 */
public class ListFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static ListFragment[] frags = new ListFragment[4];
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String[] URLS = {"http://www.wenku8.cn/modules/article/toplist.php?sort=lastupdate&page=",
            "http://www.wenku8.cn/modules/article/toplist.php?sort=goodnum&page=",
            "http://www.wenku8.cn/modules/article/toplist.php?sort=anime&page=",
            "http://www.wenku8.cn/modules/article/search.php?searchtype=articlename&searchkey="};
    private BookItemAdapter adapter;
    private int pageNumber = 1;
    private int sortType;
    private String keyword;
    private Map<String, String> titleURLMap = new HashMap<>(25, (float) 0.875);

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ListFragment newInstance(int sectionNumber, Context context) {
        if (frags[sectionNumber] != null && sectionNumber < 3) {
            return frags[sectionNumber];
        }
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.adapter = fragment.new BookItemAdapter(context);
        fragment.sortType = sectionNumber;
        frags[sectionNumber] = fragment;
        return fragment;
    }

    public String getSearchKeyword(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("书名");
        final StringBuilder sb = new StringBuilder();
        final EditText editText = new EditText(context);
        builder.setView(editText);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    keyword = URLEncoder.encode(editText.getText().toString(), context.getString(R.string.charset));
                    loadPage();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.show();
        return sb.toString();
    }

    private class BookItemAdapter extends BaseAdapter {

        private List<String> data = new ArrayList<>(24);
        private LayoutInflater inflater;

        public BookItemAdapter(Context context) {
            super();
            inflater = LayoutInflater.from(context);
        }

        public void add(String title) {
            data.add(title);
            notifyDataSetChanged();
        }

        public void clear() {
            data.clear();
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
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.book_item_layout, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.book_item_text_view)).setText(data.get(position));
            convertView.findViewById(R.id.book_item_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), Book.class);
                    intent.putExtra("url", titleURLMap.get(data.get(position)));
                    startActivity(intent);
                }
            });
            return convertView;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recent, container, false);
        ((ListView) rootView.findViewById(R.id.recent_list)).setAdapter(adapter);
        rootView.findViewById(R.id.recent_last_page_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                --pageNumber;
                loadPage();
            }
        });
        rootView.findViewById(R.id.recent_next_page_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++pageNumber;
                loadPage();
            }
        });
        if (sortType < 3) {
            loadPage();
        } else {
            getSearchKeyword(getActivity());
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void loadPage() {
        String url = URLS[sortType];
        if (sortType == 3) {
            url += keyword + "&page=";
        }
        WenkuHttpClient.get(url + pageNumber, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String res;
                try {
                    res = new String(bytes, getString(R.string.charset));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                NodeList nodeList;
                try {
                    Parser parser = new Parser(res);
                    AndFilter filter = new AndFilter(new HasAttributeFilter("href"), new HasAttributeFilter("title"));
                    filter = new AndFilter(new TagNameFilter("a"), filter);
                    nodeList = parser.parse(filter);
                } catch (ParserException e) {
                    e.printStackTrace();
                    return;
                }
                adapter.clear();
                titleURLMap.clear();
                for (int j = 1 /*remove link to home page*/, k = nodeList.size(); j < k; ++j) {
                    String tag = nodeList.elementAt(j).toHtml();
                    int titleStart = tag.indexOf("title") + 7;
                    int titleEnd = tag.indexOf("\"", titleStart);
                    String title = tag.substring(titleStart, titleEnd);
                    adapter.add(title);
                    titleStart = tag.indexOf("href") + 6;
                    titleEnd = tag.indexOf("\"", titleStart);
                    titleURLMap.put(title, tag.substring(titleStart, titleEnd));
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                if (i == 302) {
                    for (Header h : headers) {
                        if (h.getName().equals("Location")) {
                            Intent intent = new Intent();
                            intent.setClass(getActivity(), Book.class);
                            intent.putExtra("url", h.getValue());
                            startActivity(intent);
                            return;
                        }
                    }
                }
                System.out.println(i);
                System.out.println("murippoi");
            }
        });
    }
}
