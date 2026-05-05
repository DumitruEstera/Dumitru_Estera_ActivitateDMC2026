package com.example.laborator10;

import android.os.AsyncTask;
import android.os.Bundle;

import com.example.laborator10.BuildConfig;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = BuildConfig.ACCUWEATHER_API_KEY;

    private EditText etCity;
    private Spinner spinnerDays;
    private Button btnSearch;
    private TextView tvResult;

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

        etCity = findViewById(R.id.etCity);
        spinnerDays = findViewById(R.id.spinnerDays);
        btnSearch = findViewById(R.id.btnSearch);
        tvResult = findViewById(R.id.tvResult);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.days_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDays.setAdapter(adapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = etCity.getText().toString().trim();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Introduceti un oras", Toast.LENGTH_SHORT).show();
                    return;
                }
                tvResult.setText("Se cauta...");
                new CitySearchTask().execute(city);
            }
        });
    }

    private String getDaysEndpoint() {
        int pos = spinnerDays.getSelectedItemPosition();
        if (pos == 1) return "5day";
        if (pos == 2) return "10day";
        return "1day";
    }

    // cauta codul orasului
    private class CitySearchTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String city = URLEncoder.encode(params[0], "UTF-8");
                String urlStr = "https://dataservice.accuweather.com/locations/v1/cities/search"
                        + "?apikey=" + API_KEY + "&q=" + city;
                String response = httpGet(urlStr);
                if (response == null) return null;

                JSONArray array = new JSONArray(response);
                if (array.length() == 0) return null;
                JSONObject first = array.getJSONObject(0);
                return first.getString("Key");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String cityKey) {
            if (cityKey == null) {
                tvResult.setText("Orasul nu a fost gasit sau eroare la cerere.");
                return;
            }
            tvResult.setText("Cod oras: " + cityKey + "\nSe obtine prognoza...");
            new ForecastTask().execute(cityKey, getDaysEndpoint());
        }
    }

    // obtine prognoza
    private class ForecastTask extends AsyncTask<String, Void, String> {

        private String cityKey;

        @Override
        protected String doInBackground(String... params) {
            try {
                cityKey = params[0];
                String days = params[1];
                String urlStr = "https://dataservice.accuweather.com/forecasts/v1/daily/"
                        + days + "/" + cityKey
                        + "?apikey=" + API_KEY + "&metric=true";
                String response = httpGet(urlStr);
                if (response == null) return null;

                JSONObject root = new JSONObject(response);
                JSONArray forecasts = root.getJSONArray("DailyForecasts");

                StringBuilder sb = new StringBuilder();
                sb.append("Cod oras: ").append(cityKey).append("\n\n");
                for (int i = 0; i < forecasts.length(); i++) {
                    JSONObject day = forecasts.getJSONObject(i);
                    String date = day.getString("Date").substring(0, 10);
                    JSONObject temp = day.getJSONObject("Temperature");
                    double min = temp.getJSONObject("Minimum").getDouble("Value");
                    double max = temp.getJSONObject("Maximum").getDouble("Value");
                    sb.append("Ziua ").append(i + 1).append(" (").append(date).append("):\n")
                            .append("  Min: ").append(min).append(" °C\n")
                            .append("  Max: ").append(max).append(" °C\n\n");
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                tvResult.setText("Eroare la obtinerea prognozei.\n"
                        + "Planul gratuit AccuWeather suporta doar 1 zi si 5 zile. "
                        + "Pentru 10 zile e necesar un plan platit.");
            } else {
                tvResult.setText(result);
            }
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
