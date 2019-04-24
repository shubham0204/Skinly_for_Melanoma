package com.health.inceptionapps.skinly;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class ApplicationMain extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled( true ) ;
    }
}
