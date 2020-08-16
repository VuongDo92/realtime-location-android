/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firebase.geofire.example.geo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is a JUnit rule that can be used for hooking up Geofire with a real database instance.
 */
public final class GeoFireSetLocation { //geofire

    private static final String TAG = "GeoFireTestingRule";

    private static final String DEFAULT_DATABASE_URL = "/geofire";

    static final long DEFAULT_TIMEOUT_SECONDS = 5;

    private DatabaseReference databaseReference;

    public final String databaseUrl;

    /** Timeout in seconds. */
    public final long timeout;

    public GeoFireSetLocation() {
        this (DEFAULT_DATABASE_URL, DEFAULT_TIMEOUT_SECONDS);
    }

    public GeoFireSetLocation(final String databaseUrl) {
        this(databaseUrl, DEFAULT_TIMEOUT_SECONDS);
    }

    public GeoFireSetLocation(final String databaseUrl, final long timeout) {
        this.databaseUrl = databaseUrl;
        this.timeout = timeout;
    }

    public void before(Context context) {
        this.databaseReference = FirebaseDatabase.getInstance().getReference(databaseUrl);
    }

    public void after() {
        this.databaseReference.setValue(null);
        this.databaseReference = null;
    }

    /** This will return you a new Geofire instance that can be used for testing. */
    public GeoFire newTestGeoFire() {
        return new GeoFire(databaseReference);
    }

    /**
     * Sets a given location key from the latitude and longitude on the provided Geofire instance.
     * This operation will run asychronously.
     */
    public void setLocation(GeoFire geoFire, String key, double latitude, double longitude) {
        setLocation(geoFire, key, latitude, longitude, false);
    }

    /**
     * Removes a location on the provided Geofire instance.
     * This operation will run asychronously.
     */
    public void removeLocation(GeoFire geoFire, String key) {
        removeLocation(geoFire, key, false);
    }

    /** Sets the value on the given databaseReference and waits until the operation has successfully finished. */
    public void setValueAndWait(DatabaseReference databaseReference, Object value) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        databaseReference.setValue(value, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                futureError.put(databaseError);
            }
        });
        try {
            futureError.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Sets a given location key from the latitude and longitude on the provided Geofire instance.
     * This operation will run asychronously or synchronously depending on the wait boolean.
     */
    public void setLocation(GeoFire geoFire, String key, double latitude, double longitude, boolean wait) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        geoFire.setLocation(key, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                futureError.put(error);
            }
        });
        if (wait) {
            try {
                futureError.get(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * Removes a location on the provided Geofire instance.
     * This operation will run asychronously or synchronously depending on the wait boolean.
     */
    public void removeLocation(GeoFire geoFire, String key, boolean wait) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        geoFire.removeLocation(key, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                futureError.put(error);
            }
        });
        if (wait) {
            try {
                futureError.get(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /** This lets you blockingly wait until the onGeoFireReady was fired on the provided Geofire instance. */
    public void waitForGeoFireReady(GeoFire geoFire) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        geoFire.getDatabaseReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                semaphore.release();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });

        Log.i(TAG, "Timeout occured!");
        semaphore.tryAcquire(timeout, TimeUnit.SECONDS);
    }
}
