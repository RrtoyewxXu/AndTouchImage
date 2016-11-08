package com.rrtoyewx.touchimageview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.rrtoyewx.touchimageviewlibrary.TouchImageView;

public class MainActivity extends AppCompatActivity {
    Button changeImageResourceBtn;
    TouchImageView mTouchImage;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeImageResourceBtn = (Button) findViewById(R.id.btn_main_change_image_resources);
        mTouchImage = (TouchImageView) findViewById(R.id.tiv_main_image);
        changeImageResourceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTouchImage.setImageResource(count++ % 2 == 0 ? R.drawable.image_1 : R.drawable.image_2);
            }
        });
    }
}
