// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.firebase.geofire.example;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.firebase.geofire.example.geo.GeoFireSetLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;

import java.util.Timer;
import java.util.TimerTask;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
// [START maps_marker_on_map_ready]
public class MapsMarkerActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final int DEFAULT_ZOOM = 15;
    private static final String TAG_LOCATION = "TAG_LOCATION";

    private GeoFireSetLocation geoFireSetLocation;

    private GeoLocation lastKnownGeoLocation;

    private MarkerOptions markerOptions;
    private GoogleMap map;

    // [START_EXCLUDE]
    // [START maps_marker_get_map_async]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        geoFireSetLocation = new GeoFireSetLocation();
        geoFireSetLocation.before(this);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final GeoFire geoFire = geoFireSetLocation.newTestGeoFire();

//        GeoQuery geoQuery = geoFireSetLocation.newTestGeoFire().queryAtLocation()

        geoFireSetLocation.newTestGeoFire().getLocation("firebase-hq", new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                lastKnownGeoLocation = location;
                Log.i(TAG_LOCATION, "Location Changed Latitude with key: " + key);
                Log.i(TAG_LOCATION, "Location Changed Latitude Marker_Activity: " + location.latitude + "\tLongitude : " + location.longitude);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        waitUntil(this, new WaitUntil() {
            @Override
            public boolean cond() {
                return lastKnownGeoLocation != null;
            }

            @Override
            public void todo() {
                GeoQuery geoQuery = geoFire.queryAtLocation(lastKnownGeoLocation, 20);
                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {
                        lastKnownGeoLocation = location;
                        map.clear();
                        if(markerOptions != null) {
                            markerOptions.position(new LatLng(location.latitude, location.longitude));
                        } else {
                            markerOptions = new MarkerOptions().position(new LatLng(location.latitude, location.longitude));
                        }
                        map.addMarker(markerOptions);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.latitude, location.longitude), DEFAULT_ZOOM));

                        Log.i(TAG_LOCATION, "onKeyEntered key = " + key);
                        Log.i(TAG_LOCATION, "onKeyEntered lat = " + location.latitude);
                        Log.i(TAG_LOCATION, "onKeyEntered lng = " + location.longitude);
                    }

                    @Override
                    public void onKeyExited(String key) {
                        Log.i(TAG_LOCATION, "onKeyExited key = " + key);
                    }

                    @Override
                    public void onKeyMoved(String key, GeoLocation location) {
                        lastKnownGeoLocation = location;
                        if(markerOptions != null) {
                            markerOptions.position(new LatLng(location.latitude, location.longitude));
                            map.clear();
                            map.addMarker(markerOptions);
                        } else {
                            markerOptions = new MarkerOptions().position(new LatLng(location.latitude, location.longitude));
                        }
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.latitude, location.longitude), DEFAULT_ZOOM));

                        Log.i(TAG_LOCATION, "onKeyMoved key = " + key);
                        Log.i(TAG_LOCATION, "onKeyMoved lat = " + location.latitude);
                        Log.i(TAG_LOCATION, "onKeyMoved lng = " + location.longitude);
                    }

                    @Override
                    public void onGeoQueryReady() {
                        Log.i(TAG_LOCATION, "onGeoQueryReady");
                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {
                        Log.i(TAG_LOCATION, "onGeoQueryError");
                    }
                });
            }
        });

    }
    // [END maps_marker_get_map_async]
    // [END_EXCLUDE]

    // [START_EXCLUDE silent]
    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    // [END_EXCLUDE]
    // [START maps_marker_on_map_ready_add_marker]
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // [START_EXCLUDE silent]
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        // [END_EXCLUDE]
        this.map = googleMap;
        LatLng sydney = new LatLng(-33.852, 151.211);
        markerOptions = new MarkerOptions()
                .position(sydney)
                .title("Marker in Sydney");
        googleMap.addMarker(markerOptions);
        // [START_EXCLUDE silent]
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, DEFAULT_ZOOM));
//        Marker marker = new Marker();
//        animateMarker(marker, new LatLng(-33.852, 151.211), false);
        // [END_EXCLUDE]
    }
    // [END maps_marker_on_map_ready_add_marker]

    public static void waitUntil(final Activity ctx, final WaitUntil wu) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (wu.cond()) {
                            wu.todo();
                            cancel();
                        }
                    }
                });

            }
        }, 1000);
    }

    public void animateMarker(final Marker marker, final LatLng toPosition, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = map.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }
}
// [END maps_marker_on_map_ready]
