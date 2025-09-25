package com.example.gsong;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeezerApi {

    public interface DeezerCallback {
        void onResult(String previewUrl);
    }

    public static void getPreviewUrl(String songTitle, DeezerCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String query = songTitle.replace(" ", "+");
                    URL url = new URL("https://api.deezer.com/search?q=" + query);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(result.toString());
                    JSONArray data = json.getJSONArray("data");
                    if (data.length() > 0) {
                        JSONObject firstSong = data.getJSONObject(0);
                        return firstSong.getString("preview"); // παίρνουμε το demo
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String previewUrl) {
                if (callback != null) {
                    callback.onResult(previewUrl);
                }
            }
        }.execute();
    }
}
