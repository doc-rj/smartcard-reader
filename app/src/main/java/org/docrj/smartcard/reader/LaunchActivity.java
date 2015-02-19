package org.docrj.smartcard.reader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;


public class LaunchActivity extends Activity {

    // test modes
    static final int TEST_MODE_AID_ROUTE = 0;
    static final int TEST_MODE_EMV_READ = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences ss = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int testMode = ss.getInt("test_mode", TEST_MODE_AID_ROUTE);
        Class<?> cls;
        switch(testMode) {
            case TEST_MODE_AID_ROUTE:
                cls = AidRouteActivity.class;
                break;
            case TEST_MODE_EMV_READ:
                cls = EmvReadActivity.class;
                break;
            default:
                cls = AidRouteActivity.class;
                break;
        }
        startActivity(new Intent(this, cls));
        // finish activity so it does not remain on back stack
        finish();
    }
}
