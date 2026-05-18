package com.example.laborator11;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class ChartActivity extends AppCompatActivity {

    private PieChartView pieChart;
    private ColumnChartView columnChart;
    private BarChartView barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chart);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chartRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            finish();
            return;
        }

        ArrayList<String> names = bundle.getStringArrayList(MainActivity.EXTRA_NAMES);
        float[] values = bundle.getFloatArray(MainActivity.EXTRA_VALUES);

        if (names == null || values == null || names.isEmpty()) {
            finish();
            return;
        }

        pieChart = findViewById(R.id.pieChart);
        columnChart = findViewById(R.id.columnChart);
        barChart = findViewById(R.id.barChart);

        pieChart.setData(names, values);
        columnChart.setData(names, values);
        barChart.setData(names, values);

        Spinner spinner = findViewById(R.id.spinnerChartType);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.chart_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showChart(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showChart(int position) {
        pieChart.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        columnChart.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        barChart.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }
}
