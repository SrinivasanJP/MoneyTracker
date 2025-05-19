package dev.roxs.moneytracker.page;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import dev.roxs.moneytracker.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import dev.roxs.moneytracker.MainActivity;
import dev.roxs.moneytracker.R;

public class SplashScreen extends AppCompatActivity {
        private ImageView logoImage;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_splash_screen);

            logoImage = findViewById(R.id.logoImage);

            runLogoAnimation();
        }

        private void runLogoAnimation() {
            logoImage.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(this::checkForUpdate)
                    .start();
        }

    private void checkForUpdate() {
        new FetchLatestVersionTask().execute("https://srinivasan-jp-portfolio.vercel.app/appVersionAPI.json");
    }

    private class FetchLatestVersionTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                reader.close();
                urlConnection.disconnect();

                JSONObject jsonObject = new JSONObject(json.toString());
                return jsonObject.getString("version");

            } catch (Exception e) {
                e.printStackTrace();
                return null;  // On failure, skip update check
            }
        }

        @Override
        protected void onPostExecute(String latestVersion) {
            Log.d("UT", "onPostExecute: "+ latestVersion+" bc:"+BuildConfig.VERSION_NAME);
            if (latestVersion == null) {
                proceedToMainApp(); // Proceed even if failed to fetch
                return;
            }

            String currentVersion = BuildConfig.VERSION_NAME;

            if (!currentVersion.equals(latestVersion)) {
                showUpdateDialog();
            } else {
                proceedToMainApp();
            }
        }
    }


        private void showUpdateDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Update Available")
                    .setMessage("A new version of the app is available. Please update to continue.")
                    .setCancelable(false)
                    .setPositiveButton("Update", (dialog, which) -> {
                        String updateUrl = "https://srinivasan-jp-portfolio.vercel.app/appVersionAPI.json";
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        startActivity(intent);
                        finish(); // Close the splash screen
                    })
                    .setNegativeButton("Exit", (dialog, which) -> finish())
                    .show();
        }

        private void proceedToMainApp() {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
