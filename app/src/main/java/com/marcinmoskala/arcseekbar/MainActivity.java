package com.marcinmoskala.arcseekbar;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArcSeekBar arcSeekBar = findViewById(R.id.seekArc);

        arcSeekBar.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int progress) {
                Log.i("SeekBar", "Value is " + progress);
            }
        });

        int[] intArray = getResources().getIntArray(R.array.progressGradientColors);
        arcSeekBar.setProgressGradient(intArray);
    }
}
