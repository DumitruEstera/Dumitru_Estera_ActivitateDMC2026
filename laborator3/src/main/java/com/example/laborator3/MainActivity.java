package com.example.laborator3;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.i(TAG, "onCreate() called");
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.w(TAG, "onStart() called");
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.v(TAG, "onResume() called");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.wtf(TAG, "onStop() called");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.e(TAG, "onDestroy() called");
    }
}