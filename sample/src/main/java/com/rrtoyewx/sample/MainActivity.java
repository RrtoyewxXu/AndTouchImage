package com.rrtoyewx.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.rrtoyewx.touchimageviewlibrary.TouchImageView;

public class MainActivity extends AppCompatActivity {
    TouchImageView mTouchImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTouchImageView = (TouchImageView) findViewById(R.id.tiv_main_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scale_image:
                mTouchImageView.scaleImage(1.4f);
                break;
            case R.id.action_rotate_image:
                Log.e("TAG", "rotate");
                mTouchImageView.rotateImage(90);
                break;
            case R.id.action_scroll_image:
                mTouchImageView.translateImage(200, 200, 1000);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
