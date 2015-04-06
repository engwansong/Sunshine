package com.ewsme.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ews on 5/4/2015.
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private final String WEATHER_API_BASE_URL = "http://api.openweathermap.org";
    private final String WEATHER_API_PATH = "/data/2.5/forecast/daily";
    private final String WEATHER_API_QUERY_POSTAL = "q";
    private final String WEATHER_API_QUERY_MODE = "mode";
    private final String WEATHER_API_QUERY_UNITS = "units";
    private final String WEATHER_API_QUERY_COUNT = "cnt";
    private static WeatherDataParser weatherDataParser = new WeatherDataParser();
    private ArrayAdapter<String> forecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
            fetchWeatherTask.execute("94043");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/46",
                "Wed - Cloudy - 72/63",
                "Thu - Rainy - 64/51",
                "Fri - Foggy - 70/46",
                "Sat - Sunny - 76/68"
        };

        ArrayList<String> forecastData = new ArrayList<String>(
                Arrays.asList(forecastArray)
        );

        forecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_text_view,
                forecastData);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);

        return rootView;
    }

    private class FetchWeatherTask extends AsyncTask<String, Integer, String[]> {
        private Map<String, String> query = new HashMap<String, String>();

        public FetchWeatherTask() {
            query.put(WEATHER_API_QUERY_POSTAL, "02114");
            query.put(WEATHER_API_QUERY_MODE, "json");
            query.put(WEATHER_API_QUERY_UNITS, "metric");
            query.put(WEATHER_API_QUERY_COUNT, "7");
        }

        private Uri buildURI(String... params) {
            // Defaults
            if(params.length != 0) {
                query.put(WEATHER_API_QUERY_POSTAL, params[0]);
            }

            Uri.Builder builder = Uri.parse(WEATHER_API_BASE_URL).buildUpon();
            builder.path(WEATHER_API_PATH);
            for(Map.Entry<String, String> entry : query.entrySet()) {
                builder.appendQueryParameter(entry.getKey(), entry.getValue());
            }

            return builder.build();
        }

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri builtUri = buildURI(params);
                URL url = new URL(builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return weatherDataParser.getWeatherDataFromJson(forecastJsonStr, Integer.parseInt(query.get(WEATHER_API_QUERY_COUNT)));
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] forecastData) {
            if(forecastData != null) {
                forecastAdapter.clear();
                for (String data : forecastData) {
                    forecastAdapter.add(data);
                }
            } else {
                forecastAdapter.add("Error contacting server");
            }
        }
    }

}