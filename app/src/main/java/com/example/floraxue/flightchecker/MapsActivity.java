package com.example.floraxue.flightchecker;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONObject;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MapsActivity extends Activity implements OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private String url_clean_source = "";
    private String cleaned_source = "";
    private String uncleaned_source = "";
    private String url_clean_destination = "";
    private String cleaned_destination = "";
    private String uncleaned_destination = "";
    private String source_weather = "";
    private String destination_weather = "";
    private String result;
    private static String url_head =
            "http://api.apixu.com/v1/current.json?key=43b694b695254668be533703162404&q=";
    private static String url_tail =
            "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
    private String[] dangerWeather = new String[]{"Blizzard", "Heavy rain", "Light freezing rain",
            "Moderate or heavy freezing rain", "Patchy moderate snow", "Moderate snow",
            "Patchy heavy snow", "Heavy snow", "Ice pellets", "Moderate or heavy rain shower",
            "Torrential rain shower", "Light sleet showers", "Moderate or heavy sleet showers",
            "Moderate or heavy snow showers", "Light showers of ice pellets",
            "Moderate or heavy showers of ice pellets", "Patchy light rain in area with thunder",
            "Moderate or heavy rain in area with thunder", "Patchy light snow in area with thunder",
            "Moderate or heavy snow in area with thunder"};
    private HashSet<String> dangerWeatherSet = new HashSet<>();
    private boolean delayed = false;
    private List<Polyline> allLines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setUpMapIfNeeded();

        setContentView(R.layout.activity_maps);
        final ImageButton searchButton = (ImageButton) this.findViewById(R.id.search_button);
        final EditText source_box = (EditText) this.findViewById(R.id.source);
        final EditText destination_box = (EditText) this.findViewById(R.id.destination);
        final TextView result_box = (TextView) this.findViewById(R.id.result);
        final MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayed = false;
                url_clean_source = cleanString(source_box.getText().toString());
                cleaned_source = cleanString2(source_box.getText().toString());
                uncleaned_source = source_box.getText().toString();
                url_clean_destination = cleanString(destination_box.getText().toString());
                cleaned_destination = cleanString2(destination_box.getText().toString());
                uncleaned_destination = destination_box.getText().toString();
                Network n = new Network();
                n.execute();
                if (delayed) {
//                    result_box.setText("The flight is DELAYED\n" + uncleaned_source + " weather: " +
//                    source_weather + ", " + uncleaned_destination + " weather: " +
//                    destination_weather);
                    result_box.setText("The flight is DELAYED");
                } else {
//                    result_box.setText("The flight is ON TIME\n" + uncleaned_source + " weather: " +
//                    source_weather + ", " + uncleaned_destination + " weather: " +
//                            destination_weather);
                    result_box.setText("The flight is ON TIME");
                }
                mapFragment.getMapAsync(MapsActivity.this);
            }
        };
        searchButton.setOnClickListener(listener);
        for (String s : dangerWeather) {
            dangerWeatherSet.add(s);
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        setUpMapIfNeeded();
//    }

    private class Network extends AsyncTask {
        @Override
        protected Boolean doInBackground(Object[] params) {
            String query_url_source = url_head + url_clean_source;
            String query_url_destination = url_head + url_clean_destination;
            JSONObject jsonObject = null;
            String response = null;
            try {
                URL url = new URL(query_url_source);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                try {
                    c.setRequestMethod("GET");
                    c.setRequestProperty("Content-length", "0");
                    c.setUseCaches(false);
                    c.setAllowUserInteraction(false);
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(5000);
                    c.connect();
                    int status = c.getResponseCode();
                    Log.d("network", query_url_source);


                    switch (status) {
                        case 200:
                        case 201:
                            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line+"\n");
                            }
                            br.close();
                            response = sb.toString();
                            jsonObject = new JSONObject(response);
                            JSONObject j1 = (JSONObject) jsonObject.get("current");
                            JSONObject j2 = (JSONObject) j1.get("condition");
                            String j3 = (String) j2.get("text");
                            Log.d("network", url_clean_source + " " + j3);
                            source_weather = j3;
                            if (dangerWeatherSet.contains(j3)) {
                                delayed = true;
                            }
                    }
                } finally {
                    c.disconnect();
                }
            } catch (Exception e) {
                Log.e("network", e.toString());
            }

            // Do it again for url_clean_destination
            try {
                URL url = new URL(query_url_destination);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                try {
                    c.setRequestMethod("GET");
                    c.setRequestProperty("Content-length", "0");
                    c.setUseCaches(false);
                    c.setAllowUserInteraction(false);
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(5000);
                    c.connect();
                    int status = c.getResponseCode();

                    switch (status) {
                        case 200:
                        case 201:
                            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line+"\n");
                            }
                            br.close();
                            response = sb.toString();
                            jsonObject = new JSONObject(response);
                            JSONObject j1 = (JSONObject) jsonObject.get("current");
                            JSONObject j2 = (JSONObject) j1.get("condition");
                            String j3 = (String) j2.get("text");
                            Log.d("network", url_clean_destination + " " + j3);
                            destination_weather = j3;
                            if (dangerWeatherSet.contains(j3)) {
                                delayed = true;
                            }
                    }
                } finally {
                    c.disconnect();
                }
            } catch (Exception e) {
                Log.e("network", e.toString());
            }
            return true;
        }
    }

    private static String cleanString(String s) {
        s = s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
        s = s.replaceAll("[ ]", "%20");
        return s;
    }

    private static String cleanString2(String s) {
        s = s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
//        s = s.replaceAll("[ ]", "%20");
        return s;
    }

    @Override
    public void onMapReady(GoogleMap map) {

        double southwest_x = 0;
        double southwest_y = 0;
        double northeast_x = 0;
        double northeast_y = 0;
        LatLng latLng_source = null;
        LatLng latLng_destination = null;

        if(Geocoder.isPresent() && url_clean_source.length() > 0 && url_clean_destination.length() > 0){
            try {
//                String location = "theNameOfTheLocation";
                Geocoder gc = new Geocoder(this);
                List<Address> addresses_source = gc.getFromLocationName(cleaned_source, 5); // get the found Address Objects
                List<Address> addresses_destination = gc.getFromLocationName(cleaned_destination, 5); // get the found Address Objects
                latLng_source = new LatLng(addresses_source.get(0).getLatitude(),
                        addresses_source.get(0).getLongitude());
                latLng_destination = new LatLng(addresses_destination.get(0).getLatitude(),
                        addresses_destination.get(0).getLongitude());
                southwest_y = Math.min(latLng_source.latitude, latLng_destination.latitude);
                northeast_y = Math.max(latLng_source.latitude, latLng_destination.latitude);
                double source_lon = latLng_source.longitude;
                double destination_lon = latLng_destination.longitude;
                if (Math.max(source_lon, destination_lon)
                        - Math.min(source_lon, destination_lon) > 180) {
                    southwest_x = Math.max(latLng_source.longitude, latLng_destination.longitude);
                    northeast_x = Math.min(latLng_source.longitude, latLng_destination.longitude);
                } else {
                    northeast_x = Math.max(latLng_source.longitude, latLng_destination.longitude);
                    southwest_x = Math.min(latLng_source.longitude, latLng_destination.longitude);
                }
                Log.d("location", "southwest_y: " + southwest_y + " southwest_x " + southwest_x);
                Log.d("location", "northeast_y: " + northeast_y + " northeast_x " + northeast_x);
//                List<LatLng> ll = new ArrayList<LatLng>(addresses_source.size()); // A list to save the coordinates if they are available
//                for (Address a : addresses_source) {
//                    if(a.hasLatitude() && a.hasLongitude()){
//                        ll.add(new LatLng(a.getLatitude(), a.getLongitude()));
//                    }
//                }
            } catch (IOException e) {
                // handle the exception
                Log.e("location", e.toString());
            }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    new LatLngBounds(new LatLng(southwest_y, southwest_x),
                            new LatLng(northeast_y, northeast_x)), 50));
            // Polylines are useful for marking paths and routes on the map.
//        map.addPolyline(new PolylineOptions().geodesic(true)
//                .add(new LatLng(-33.866, 151.195))  // Sydney
//                .add(new LatLng(-18.142, 178.431))  // Fiji
//                .add(new LatLng(21.291, -157.821))  // Hawaii
//                .add(new LatLng(37.423, -122.091))  // Mountain View

            for (Polyline p : allLines) {
                p.remove();
            }
            Polyline polyline = map.addPolyline(new PolylineOptions().geodesic(true)
                    .add(latLng_source)
                    .add(latLng_destination));
            allLines.add(polyline);
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.8716667, -122.2716667), 2));
            map.addMarker(new MarkerOptions().position(new LatLng(37.8716667, -122.2716667)).title("Marker"));

        }

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
//    private void setUpMapIfNeeded() {
//        // Do a null check to confirm that we have not already instantiated the map.
//        if (mMap == null) {
//            // Try to obtain the map from the SupportMapFragment.
//            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
//                    .getMap();
//            // Check if we were successful in obtaining the map.
//            if (mMap != null) {
//                setUpMap();
//            }
//        }
//    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
}
