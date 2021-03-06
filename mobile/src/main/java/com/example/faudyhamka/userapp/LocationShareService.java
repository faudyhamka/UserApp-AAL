package com.example.faudyhamka.userapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationShareService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{
    public LocationShareService() {
    }

    public static final String myPreference = "homepref";
    public static final String mypreference = "mypref";
    public static final String inputIP = "input_IP";
    public static final String inputLat = "input_Lat";
    public static final String inputLon = "input_Lon";
    GoogleApiClient client;
    LocationRequest request;
    LatLng latLngCurrent;
    String lat, lng, ip, Lat, Lon;
    RequestQueue queue;
    SharedPreferences sharedpreferences, sharedPreferences;
    boolean a = false, checka = false;
    private RemoteSensorManager remoteSensorManager;

    NotificationCompat.Builder notification;
    public final int uniqueId = 654321;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        queue = Volley.newRequestQueue(this);
        notification = new NotificationCompat.Builder(this);
        notification.setAutoCancel(false);
        notification.setOngoing(true);

        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        client.connect();

        sharedpreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);
        sharedPreferences = getSharedPreferences(myPreference, Context.MODE_PRIVATE);
        Lat = sharedPreferences.getString(inputLat, "");
        Lon = sharedPreferences.getString(inputLon, "");
        ip = sharedpreferences.getString(inputIP, "");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        request = new LocationRequest().create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(2000);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(client, request, this);

        notification.setSmallIcon(android.R.drawable.ic_menu_mylocation);
        notification.setTicker("Notification.");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Family Tracker App");
        notification.setContentText("You are sharing your location.!");
        notification.setDefaults(Notification.DEFAULT_SOUND);

        Intent intent = new Intent(getApplicationContext(),MyGPS.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0,intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);

        // Build the nofification
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(uniqueId,notification.build());
        // display notification
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        latLngCurrent = new LatLng(location.getLatitude(), location.getLongitude());
        if (sharedPreferences.contains(inputLat) && sharedPreferences.contains(inputLon)) {
            lat = String.valueOf(latLngCurrent.latitude);
            lng = String.valueOf(latLngCurrent.longitude);
            if ((Math.abs(Double.parseDouble(Lat) - latLngCurrent.latitude) > 0.0005) || (Math.abs(Double.parseDouble(Lon) - latLngCurrent.longitude) > 0.0005)) {
                try {
                    JSONObject loc = new JSONObject();
                    loc.put("lat", lat);
                    loc.put("lon", lng);
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, "http://" + ip + ":3000/location", loc,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                        }
                    });
                    request.setTag("Map");
                    queue.add(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                a = true;
            } else if ((Math.abs(Double.parseDouble(Lat) - latLngCurrent.latitude) < 0.0005) || (Math.abs(Double.parseDouble(Lon) - latLngCurrent.longitude) < 0.0005)) {
                a = false;
            }
//        if (checka != a) { if (a) {remoteSensorManager.StartXYZ();} else {remoteSensorManager.StopXYZ();} checka = a;}
        }
    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
        client.disconnect();

        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(uniqueId);
    }
}
