package com.example.proiect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.proiect.R;
import com.example.proiect.api.ApiClient;
import com.example.proiect.utils.PrefsManager;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView errorText;

    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(this);

        if (prefs.isLoggedIn()) {
            goToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.login_username);
        passwordInput = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.login_progress);
        errorText = findViewById(R.id.login_error);

        loginButton.setOnClickListener(v -> attemptLogin());

        // pentru a nu acoperi butoanele cu tastatura
        View loginRoot = findViewById(R.id.login_root);
        ViewCompat.setOnApplyWindowInsetsListener(loginRoot, (v, insets) -> {
            int bottom = insets.getInsets(
                    WindowInsetsCompat.Type.ime()
                            | WindowInsetsCompat.Type.systemBars()).bottom;
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void attemptLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.login_error_empty));
            return;
        }

        hideError();
        setLoading(true);

        final String serverUrl = prefs.getServerUrl();

        executor.execute(() -> {
            try {
                ApiClient client = new ApiClient(serverUrl, null);
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("password", password);

                ApiClient.ApiResponse resp = client.post("/api/login", body);

                if (resp.isSuccess()) {
                    JSONObject json = resp.asJson();
                    String token = json.optString("token",
                            json.optString("access_token", null));
                    JSONObject userObj = json.optJSONObject("user");
                    String role = (userObj != null) ? userObj.optString("role", "user") : "user";
                    String fullName = (userObj != null) ? userObj.optString("full_name", "") : "";
                    int userId = (userObj != null) ? userObj.optInt("id", -1) : -1;

                    if (token == null || token.isEmpty()) {
                        postError("Invalid server response (no token).");
                        return;
                    }

                    prefs.saveLogin(token, username, role, fullName, userId);
                    mainHandler.post(() -> {
                        setLoading(false);
                        goToDashboard();
                    });
                } else {
                    String message = "Login failed (" + resp.code + ")";
                    try {
                        JSONObject err = resp.asJson();
                        String detail = err.optString("detail",
                                err.optString("message", null));
                        if (detail != null && !detail.isEmpty()) {
                            message = detail;
                        }
                    } catch (Exception ignored) { }
                    postError(message);
                }
            } catch (Exception e) {
                postError("Network error: " + e.getMessage());
            }
        });
    }

    private void postError(final String message) {
        mainHandler.post(() -> {
            setLoading(false);
            showError(message);
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        usernameInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
