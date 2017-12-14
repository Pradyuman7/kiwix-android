/*
 * Copyright 2013  Elad Keyshawn <elad.keyshawn@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */


package org.kiwix.kiwixmobile.bookmarks_view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BookmarksActivity extends BaseActivity
        implements AdapterView.OnItemClickListener, BookmarksViewCallback {

    private ArrayList<String> bookmarks;
    private ArrayList<String> bookmarkUrls;
    @BindView(R.id.bookmarks_list)
    ListView bookmarksList;
    private BookmarksArrayAdapter adapter;
    @BindView(R.id.bookmarks_activity_layout)
    CoordinatorLayout snackbarLayout;
    @BindView(R.id.bookmarks_none_linlayout)
    LinearLayout noBookmarksLayout;
    @Inject
    BookmarksPresenter presenter;
    private ActionModeListener actionModeListener;
    private Context mContext;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (KiwixSettingsActivity.nightMode(sharedPreferences)) {
            setTheme(R.style.AppTheme_Night);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        ButterKnife.bind(this);

        setUpToolbar();
        bookmarks = new ArrayList<>();
        bookmarkUrls = new ArrayList<>();
        mContext = this;
        actionModeListener = new ActionModeListener();
        adapter = new BookmarksArrayAdapter(getApplicationContext(), R.layout.bookmarks_row, R.id.bookmark_title, bookmarks);
        bookmarksList.setAdapter(adapter);
        bookmarksList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        bookmarksList.setMultiChoiceModeListener(actionModeListener);
        bookmarksList.setOnItemClickListener(this);

        presenter.attachView(this);
        presenter.loadBookmarks(this);


    }

    @Override
    protected void setupDagger(ApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onResume() {
        super.onResume();

        new AsyncTask<Void, Void, String>() {


            @Override
            protected String doInBackground(Void... voids) {
                Process logcat;
                final StringBuilder log = new StringBuilder();
                String path = Environment.getExternalStorageDirectory() + "/" + "MyApp/";
                String fullName = path + "crash-log.txt";

                // Extract to file.
                File appDirectory = new File( Environment.getExternalStorageDirectory() + "/KiwixApp" );
                File logDirectory = new File( appDirectory + "/log" );
                File logFile = new File( logDirectory, "logcat.txt" );

                StringBuilder text = new StringBuilder();

                try {
                    BufferedReader br = new BufferedReader(new FileReader(logFile));
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                }
                catch (IOException e) {
                    //You'll need to add proper error handling here
                }

                return text.toString();
            }

            @Override
            protected void onPostExecute(String log) {
                super.onPostExecute(log);
                Toast.makeText(mContext, "Length:"  + log.length(), Toast.LENGTH_SHORT).show();
                Toast.makeText(mContext, log, Toast.LENGTH_SHORT).show();
            }
        }.execute();




//    String mailto = "mailto:bob@example.org" +
//            "?cc=" + "alice@example.com" +
//            "&subject=" + Uri.encode("Kiwix log") +
//            "&body=" + Uri.encode(log.toString());
//
//    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
//    emailIntent.setData(Uri.parse(mailto));
//    startActivity(emailIntent);

    }

    private void setNoBookmarksState() {
        if (bookmarksList.getCount() == 0) {
            noBookmarksLayout.setVisibility(View.VISIBLE);
        } else {
            noBookmarksLayout.setVisibility(View.GONE);
        }
    }


    private void deleteSelectedItems() {
        SparseBooleanArray sparseBooleanArray = bookmarksList.getCheckedItemPositions();
        for (int i = sparseBooleanArray.size() - 1; i >= 0; i--) {
            deleteBookmark(bookmarkUrls.get(sparseBooleanArray.keyAt(i)));
            bookmarks.remove(sparseBooleanArray.keyAt(i));
            bookmarkUrls.remove(sparseBooleanArray.keyAt(i));
        }
        adapter.notifyDataSetChanged();
        setNoBookmarksState();
    }

    private void deleteBookmark(String article) {
        presenter.deleteBookmark(article);
    }


    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.menu_bookmarks_list));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, KiwixMobileActivity.class);
        if (!bookmarkUrls.get(position).equals("null")) {
            intent.putExtra("choseXURL", bookmarkUrls.get(position));
        } else {
            intent.putExtra("choseXTitle", bookmarks.get(position));
        }
        intent.putExtra("bookmarkClicked", true);
        int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
        if (value == 1) {
            startActivity(intent);
            finish();
        } else {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
        Intent startIntent = new Intent(this, KiwixMobileActivity.class);
        startIntent.putExtra("bookmarkClicked", false);

        if (value == 1) { // means there's only 1 activity in stack so start new
            startActivity(startIntent);

        } else { // we have a parent activity waiting...
            setResult(RESULT_OK, startIntent);
            finish();
        }
    }

    @Override
    public void showBookmarks(ArrayList<String> bookmarks, ArrayList<String> bookmarkUrls) {
        this.bookmarks.clear();
        this.bookmarkUrls.clear();
        this.bookmarks.addAll(bookmarks);
        this.bookmarkUrls.addAll(bookmarkUrls);
        adapter.notifyDataSetChanged();
        setNoBookmarksState();
    }

    @Override
    public void popDeleteBookmarksSnackbar() {
        Snackbar bookmarkDeleteSnackbar =
                Snackbar.make(snackbarLayout, actionModeListener.getNumOfSelected() + " " + getString(R.string.deleted_message), Snackbar.LENGTH_LONG);


        bookmarkDeleteSnackbar.setActionTextColor(getResources().getColor(R.color.white));
        bookmarkDeleteSnackbar.show();
    }


    class ActionModeListener implements AbsListView.MultiChoiceModeListener {
        private ArrayList<String> selected = new ArrayList<>();
        private int numOfSelected = 0;

        public ActionModeListener() {

        }

        public int getNumOfSelected() {
            return numOfSelected;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            if (checked) {
                selected.add(bookmarks.get(position));
                numOfSelected++;
                mode.setTitle(Integer.toString(numOfSelected));
            } else if (selected.contains(bookmarks.get(position))) {
                selected.remove(bookmarks.get(position));
                numOfSelected--;
                if (numOfSelected == 0) {
                    mode.finish();
                } else {
                    mode.setTitle(Integer.toString(numOfSelected));
                }
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_bookmarks, menu);
            numOfSelected = 0;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_bookmarks_delete:
                    deleteSelectedItems();
                    popDeleteBookmarksSnackbar();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }


    class BookmarksArrayAdapter extends ArrayAdapter<String> {

        public BookmarksArrayAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.bookmarks_row, null);
            }
            return super.getView(position, convertView, parent);
        }
    }

}
