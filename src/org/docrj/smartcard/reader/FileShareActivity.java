package org.docrj.smartcard.reader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.view.Gravity;
import android.widget.Toast;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

public class FileShareActivity extends Activity {
    private static final String TAG = "smartcard-reader";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        Spanned text = (Spanned) intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (text != null &&
            Intent.ACTION_SEND.equals(intent.getAction()) &&
            "text/html".equals(intent.getType())) {
            // check external storage
            if (isExternalStorageWritable()) {
                // write string to file
                try {
                    File file = new File(getExternalFilesDir(null),
                        "smartcard_reader_" + System.currentTimeMillis() + ".html");
                    Log.d(TAG, "abs file path: " + file.getAbsolutePath());
                    OutputStream os = new FileOutputStream(file);
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    osw.write(Html.toHtml(text));
                    osw.close();
                    showToast(getString(R.string.saved_to, file.getName()));

                    // add file to media library for viewing via mtp
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(file));
                    sendBroadcast(scanIntent);
                } catch (IOException e) {
                    Log.e(TAG, "failed to write file: " + e.toString());
                    showToast(getString(R.string.save_exception));
                }
            } else {
                showToast(getString(R.string.save_not_mounted));                
            }
        }

        finish();
    }
    
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(FileShareActivity.this, text,
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, -100);
        toast.show();
    }

    /*
    private boolean hasSavedFiles() {
        File dir = getExternalFilesDir(null);
        File[] files = dir.listFiles();
        return files.length > 0;
    }

    private void deleteSavedFiles() {
        File dir = getExternalFilesDir(null);
        File[] files = dir.listFiles();
        if (files.length > 0) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
            showToast("deleted saved files");
        } else {
            showToast("no saved files");
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                // recursion
                deleteRecursively(child);
            }
        }
        file.delete();
    }
    */
}
