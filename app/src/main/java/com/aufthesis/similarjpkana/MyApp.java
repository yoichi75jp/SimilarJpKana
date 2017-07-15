package com.aufthesis.similarjpkana;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

// Created by yoichi75jp2 on 2017/04/20.
public class MyApp extends Application {
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    private static FirebaseAnalytics m_FirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-64731121-10"); // Replace with actual tracker/property Id
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);

        m_FirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    public static FirebaseAnalytics getFirebaseAnalytics() {
        return m_FirebaseAnalytics;
    }
}
