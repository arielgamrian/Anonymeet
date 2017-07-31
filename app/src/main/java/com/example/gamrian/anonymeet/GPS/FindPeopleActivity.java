package com.example.gamrian.anonymeet.GPS;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.Tapp.Anonymeet1.R;
import com.example.gamrian.anonymeet.FireBaseChat.HelperDB;
import com.example.gamrian.anonymeet.FireBaseChat.MessagesFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;


public class FindPeopleActivity extends AppCompatActivity implements GpsStatus.Listener {

    private Toolbar toolbar;
    static boolean isRunning;
    static ViewPager pager;
    static FindPeopleFragment f1;
    static MessagesFragment f2;
    static HelperDB db;
    public static boolean providerEnabled;
    static LocationManager locationManager;

    public static FindPeopleFragment getF1() {
        return f1;
    }

    public static MessagesFragment getF2() {
        return f2;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_people_activity);

        PagerTitleStrip p = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
        p.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        db = new HelperDB(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        FragmentManager manager = getSupportFragmentManager();
        f1 = new FindPeopleFragment();
        f2 = new MessagesFragment();
        pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(new FragmentPagerAdapter(manager) {
            @Override
            public CharSequence getPageTitle(int position) {
                if(position == 0) return "Find People";
                else return "My Messages";

            }

            @Override
            public Fragment getItem(int position) {

                if (position == 0)
                    return f1;
                return f2;
            }

            @Override
            public int getCount() {
                return 2;
            }
        });

        if(getIntent().getBooleanExtra("fromNoti", false)) {
            pager.setCurrentItem(1);
        }



        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            setupGPS();
    }

    private void setupGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.addGpsStatusListener(this);
    }

    @Override
    protected void onUserLeaveHint() {

            FindPeopleFragment.exit();
        super.onUserLeaveHint();
    }

    public static boolean isOnMessagesFragment() {
        return (pager.getCurrentItem() == 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isRunning = true;
        try {
            LocationListenerService.cancelNotification();
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        isRunning = false;
    }

    public static boolean showMyself() {
        return f1.showMyself();
    }

    public static void updateMessage() {
        f1.updateMessage();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static void updateList() {
        f1.updateList();
    }

    public static void clearAdapter() {
        if (isRunning)
            f1.clearAdapter();
    }

    public void enableLocationServices(View view) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || LocationListenerService.getApi() == null) return;

        if (Build.VERSION.SDK_INT >= 22) locationChecker(LocationListenerService.getApi(), this);

        else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);

            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            GpsStatus.Listener listener = new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    if (event == GpsStatus.GPS_EVENT_STARTED) {
                        finishActivity(0);
                        updateMessage();
                    }

                }
            };

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "permission error", Toast.LENGTH_SHORT).show();
                return;
            }

            manager.addGpsStatusListener(listener);
        }
    }

    public void locationChecker(GoogleApiClient mGoogleApiClient, final Activity activity) {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5 * 1000);
        locationRequest.setFastestInterval(2 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                                     @Override
                                     public void onResult(LocationSettingsResult result) {
                                         final Status status = result.getStatus();
                                         switch (status.getStatusCode()) {
                                             case LocationSettingsStatusCodes.SUCCESS:
                                                 break;
                                             case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                                 try {
                                                     status.startResolutionForResult(
                                                             activity, 1000);
                                                 } catch (IntentSender.SendIntentException e) {
                                                 }
                                                 break;
                                             case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                                 break;
                                         }
                                     }
                                 }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gps_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout_item)
            logoutMessage();

        return true;
    }

    private void logoutMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Are you sure?");
        builder.setMessage("You will lose all of your data.");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                f1.onLogout();
            }
        });
        builder.setNegativeButton("Cancel", null);

        Dialog dialog = builder.create();
        dialog.show();
    }

    public static void syncContacts() {
        f2.syncContacts();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                providerEnabled = true;
                startService(new Intent(this, LocationListenerService.class));
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 1)
            pager.setCurrentItem(0);
        else {
            FindPeopleFragment.exit();
            super.onBackPressed();
        }
    }

    public static void hideMessage() {
        f1.hideMessage();
    }
}