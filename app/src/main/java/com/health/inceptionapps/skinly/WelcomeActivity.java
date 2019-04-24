package com.health.inceptionapps.skinly;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class WelcomeActivity extends AppCompatActivity {

    private Handler handler ;
    private Runnable runnable ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        try {
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    startActivity( new Intent( WelcomeActivity.this , MainActivity.class ) );
                    overridePendingTransition( android.R.anim.fade_in , android.R.anim.fade_out ) ;
                    finish();
                }
            };
            handler.postDelayed( runnable , 1500 ) ;
        }
        catch ( Exception e ){
            e.printStackTrace();
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.removeCallbacks( runnable ) ;
        finish();
    }

}
