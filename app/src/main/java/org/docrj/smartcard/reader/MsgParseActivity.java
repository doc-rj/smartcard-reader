package org.docrj.smartcard.reader;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.support.v7.widget.ShareActionProvider;
import android.widget.SpinnerAdapter;
import android.widget.TextView;


public class MsgParseActivity extends ActionBarActivity {

    String mHtml;
    String mActivityName;
    int mTestMode;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle b = intent.getBundleExtra("parsed_msg");
        String msgName = b.getString("name");
        String msgText = b.getString("text");
        mHtml = b.getString("html");
        mActivityName = b.getString("activity");
        mTestMode = b.getInt("test_mode");

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                /*| ActionBar.DISPLAY_SHOW_HOME*/ | ActionBar.DISPLAY_HOME_AS_UP);
        SpinnerAdapter sAdapter = ArrayAdapter.createFromResource(this,
                R.array.test_modes, R.layout.spinner_dropdown_action_bar);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(sAdapter, new ActionBar.OnNavigationListener() {
            String[] strings = getResources().getStringArray(R.array.test_modes);
            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                String testMode = strings[position];
                if (!testMode.equals(mActivityName)) {
                    new Launcher(MsgParseActivity.this).launch(testMode, true, false);
                    // finish activity so it does not remain on back stack
                    finish();
                    overridePendingTransition(0, 0);
                }
                return true;
            }
        });

        setContentView(R.layout.activity_msg_parse);
        TextView heading = (TextView) findViewById(R.id.heading);
        TextView contents = (TextView) findViewById(R.id.msg_text);

        heading.setText(msgName);
        contents.setText(msgText);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setSelectedNavigationItem(mTestMode);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_msg_parse, menu);
        MenuItem item = menu.findItem(R.id.menu_share_msgs);
        ShareActionProvider sp = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(mHtml));
        String subject = getString(R.string.app_name) + ": " + mActivityName;
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("text/html");
        sp.setShareIntent(sendIntent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
