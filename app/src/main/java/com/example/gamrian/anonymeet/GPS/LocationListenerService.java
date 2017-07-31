package com.example.gamrian.anonymeet.GPS;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class LocationListenerService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GpsStatus.Listener {

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private static GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    public static Location mCurrentLocation;
    static String gender;

    private static String username;
    private static DatabaseReference onlineUsers;

    public static boolean providerEnabled;
    LocationManager locationManager;
    static NotificationManager notificationManager;
    public static Context ctx;

    static boolean visible;
    static boolean isActive;

    public LocationListenerService() {
    }

    public static GoogleApiClient getApi() {
        return mGoogleApiClient;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        isActive = true;

        gender = getSharedPreferences("data", MODE_PRIVATE).getString("gender", "");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.d("TAG", "ariel onStartCommand");

        visible = true;

        ctx = this;

        username = getSharedPreferences("data", MODE_PRIVATE).getString("nickname", "");

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        onlineUsers = FirebaseDatabase.getInstance().getReference().child("OnlineUsers");

        buildGoogleApiClient();

        mGoogleApiClient.connect();

        setupGPS();

        if (FindPeopleActivity.isRunning())
            cancelNotification();

        refresh();

        return super.onStartCommand(intent, flags, startId);
    }

    private void setupGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.addGpsStatusListener(this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        if (username != null)
            onlineUsers.child(username).runTransaction(new Transaction.Handler() {
                public Transaction.Result doTransaction(MutableData mutableData) {
                    mutableData.setValue(null);
                    return Transaction.success(mutableData);
                }
                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                }
            });
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (visible) {



            mCurrentLocation = location;
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            GeoFire geoFire = new GeoFire(onlineUsers);
            GeoLocation targetLocation = new GeoLocation(latitude, longitude);
            geoFire.setLocation(username, targetLocation);



            if (FindPeopleActivity.isRunning())
                FindPeopleActivity.updateList();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void buildNotification() {

        if (ctx.getSharedPreferences("data", MODE_PRIVATE).getString("nickname", "") == "")
            return;

        Intent intent = new Intent(ctx, StatusReceiver.class);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder n;
        n = new Notification.Builder(ctx)
                .setContentTitle("Anonymeet")
                .setContentText("Status: online")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setShowWhen(false)
                .setVibrate(new long[]{Long.valueOf(0)})
                .setSound(null)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hide me", pendingIntent2);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            n.setColor(Color.parseColor("#ff5722"));

        TaskStackBuilder t = TaskStackBuilder.create(ctx);
        Intent i = new Intent(ctx, FindPeopleActivity.class);
        t.addNextIntent(i);
        PendingIntent pendingIntent = t.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        n.setContentIntent(pendingIntent);

        notificationManager.notify(0, n.build());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    public static Location getLocation() {
        return mCurrentLocation;
    }

    public static void cancelNotification() {
        try {
            notificationManager.cancel(0);
        } catch (NullPointerException e) {
        }
    }

    @Override
    public void onDestroy() {
        isActive = false;
        visible = false;
        LocationListenerService.cancelNotification();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        hideMe();
        if (FindPeopleActivity.isRunning())
            FindPeopleActivity.clearAdapter();

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                providerEnabled = true;
                mGoogleApiClient.connect();
                if (FindPeopleActivity.isRunning()) {
                    FindPeopleActivity.updateMessage();
                }
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                providerEnabled = false;
                if (FindPeopleActivity.isRunning())
                    FindPeopleActivity.updateMessage();

                stopSelf();
                break;
        }
    }

    private void hideMe() {
        onlineUsers.child(username).runTransaction(new Transaction.Handler() {
            public Transaction.Result doTransaction(MutableData mutableData) {
                mutableData.setValue(null);
                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }

        });
    }

    public static void refresh() {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (visible && mCurrentLocation != null) {

            double latitude = mCurrentLocation.getLatitude();
            double longitude = mCurrentLocation.getLongitude();

            onlineUsers.child(username).child("latitude").setValue(latitude);
            onlineUsers.child(username).child("longitude").setValue(longitude);
            onlineUsers.child(username).child("gender").setValue(gender);

            if (FindPeopleActivity.isRunning())
                FindPeopleActivity.updateList();
        }
    }
}
