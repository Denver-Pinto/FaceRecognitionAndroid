package com.example.completeapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button populateButton;
    private Button predictButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        populateButton = (Button) findViewById(R.id.populatebutton);
        predictButton = (Button) findViewById(R.id.predictbutton);

    }

    /* Called when user clicks populatebutton */
    public void populate(View view){
        Intent intent = new Intent(this, PopulateActivity.class);
        startActivity(intent);

    }

    /* Called when user clicks predictbutton */
    public void predict(View view){
        Intent intent = new Intent(this, PredictActivity.class);
        startActivity(intent);

    }
}
