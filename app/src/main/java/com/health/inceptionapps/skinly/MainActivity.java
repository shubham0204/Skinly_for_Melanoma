package com.health.inceptionapps.skinly;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.bottomappbar.BottomAppBar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.health.inceptionapps.skinly.FragmentClasses.BottomNavFragment;
import com.health.inceptionapps.skinly.ViewModelClasses.TitleViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100 ;
    private StorageReference rootStorageRef ;
    private DatabaseReference rootDatabaseRef ;
    private ArrayList<String> imagePaths ;
    private AlertDialog downloadDialog ;
    private SharedPreferences sharedPreferences;

    private TextView titleTextView;

    private int START_IMAGE_DOWNLOAD_INDEX = 0 ;
    private boolean isDownloadCompleted = false ;
    private TextView progressText ;
    private ProgressBar downloadProgress ;
    private ProgressDialog fetchPathDialog ;
    private File imagesDirectory ;
    private boolean isDownloadCancelled = false ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomAppBar bottomAppBar = findViewById( R.id.bottomAppBar ) ;
        Button takePictureButton = findViewById(R.id.take_picture_button);
        titleTextView = findViewById( R.id.titleTextView ) ;


        bottomAppBar.replaceMenu( R.menu.bottom_app_bar_menu ) ;
        FirebaseStorage.getInstance().setMaxDownloadRetryTimeMillis(5000);
        rootStorageRef = FirebaseStorage.getInstance().getReference() ;
        rootDatabaseRef = FirebaseDatabase.getInstance().getReference() ;
        sharedPreferences = getSharedPreferences( getString( R.string.app_name ) , Context.MODE_PRIVATE );
        imagesDirectory = new File( Environment.getExternalStorageDirectory() + "/Android/data/" +
                getPackageName() + "/images" );

        ObjectAnimator fabAnimatorX = ObjectAnimator.ofFloat(takePictureButton, "scaleX" , 0.0f , 1.0f);
        ObjectAnimator fabAnimatorY = ObjectAnimator.ofFloat(takePictureButton, "scaleY" , 0.0f , 1.0f);
        fabAnimatorX.setInterpolator( new DecelerateInterpolator() ) ;
        fabAnimatorY.setInterpolator( new DecelerateInterpolator() ) ;
        fabAnimatorX.setDuration( 750 ) ;
        fabAnimatorY.setDuration( 750 ) ;
        AnimatorSet animatorSet = new AnimatorSet() ;
        animatorSet.playTogether( fabAnimatorX , fabAnimatorY ) ;
        animatorSet.start();

        TitleViewModel viewModel = ViewModelProviders.of( this ).get( TitleViewModel.class ) ;
        viewModel.getTitle().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                ObjectAnimator fadeIn =
                        ObjectAnimator.ofFloat(titleTextView, "alpha" , 0.0f , 1.0f ) ;
                fadeIn.setDuration( 750 ) ;
                fadeIn.start();
                titleTextView.setText( s ) ;
            }
        });

        bottomAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomNavFragment fragment = new BottomNavFragment();
                fragment.show( getSupportFragmentManager() , fragment.getTag() ) ;
            }
        });

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( sharedPreferences.getBoolean( getString( R.string.download_completed_key) , false ) ){
                    startActivity( new Intent( MainActivity.this , ResultActivity.class ) ) ;
                }
                else {
                    START_IMAGE_DOWNLOAD_INDEX =
                            sharedPreferences.getInt( getString( R.string.download_index_key ) , 0 );
                    requestImageDownloads( true );
                }
            }
        });

        if ( sharedPreferences.getBoolean( getString( R.string.app_first_open_key ) , true ) ){
            sharedPreferences.edit().putBoolean( getString( R.string.app_first_open_key ) , false ).apply();
            requestImageDownloads( false );
            authenticateAnonymously();
        }

    }

    private void authenticateAnonymously() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance() ;
        mAuth.signInAnonymously() ;
    }

    private void requestImageDownloads( boolean isContinuing ){
        AlertDialog.Builder builder = new AlertDialog.Builder( this ) ;
        String text;
        if ( isContinuing ) {
            builder.setTitle( "Continue Image Downloads") ;
            builder.setMessage( getString( R.string.continue_image_download_message ) ) ;
            text = "resume downloads" ;
        }
        else {
            builder.setTitle("Download Sample Images") ;
            builder.setMessage(getString(R.string.download_images_message));
            text = "grant permissions" ;
        }
        builder.setPositiveButton(text , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startImageDownloads();
            }
        }) ;
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                isDownloadCompleted = false ;
                Toast.makeText(MainActivity.this,
                        "The app will not function if the images are not downloaded",
                        Toast.LENGTH_LONG).show();
            }
        }) ;
        builder.setCancelable( false );
        AlertDialog dialog = builder.create() ;
        dialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnims ;
        dialog.show();
    }

    private interface FetchPathCallback {

        void onPathsFetched( HashMap<String,String> path )  ;
        void onErrorWhileFetching( String errorMessage ) ;

    }

    private final FetchPathCallback fetchPathCallback = new FetchPathCallback() {

        @Override
        public void onPathsFetched(HashMap<String, String> data) {
            fetchPathDialog.dismiss();
            imagePaths = new ArrayList<>(data.values());
            try {
                initiateImageDownloads( imagePaths , true , START_IMAGE_DOWNLOAD_INDEX ) ;
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        @Override
        public void onErrorWhileFetching(String errorMessage) {
            isDownloadCompleted = false ;
            sharedPreferences.edit().putBoolean(
                    getString( R.string.download_completed_key) , isDownloadCompleted).apply();
            Toast.makeText(MainActivity.this, errorMessage , Toast.LENGTH_LONG).show();
        }

    } ;


    private void fetchImagePaths ( ){
        try {
            fetchPathDialog = new ProgressDialog( this )  ;
            fetchPathDialog.setMessage( "Fetching image paths ..." ) ;
            fetchPathDialog.show();
            DatabaseReference imagePathsRef = rootDatabaseRef ;
            imagePathsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap mess = ( HashMap )dataSnapshot.getValue() ;
                    fetchPathCallback.onPathsFetched( mess ) ;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    fetchPathCallback.onErrorWhileFetching( databaseError.getMessage() ) ;
                }
            }) ;
        }
        catch ( Exception e ) {
            fetchPathCallback.onErrorWhileFetching( e.getMessage() ) ;
        }

    }

    private void showDownloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this )  ;
        View dialogView = getLayoutInflater().inflate( R.layout.download_images_dialog , null , false ) ;
        progressText = dialogView.findViewById( R.id.dialogImageCount ) ;
        downloadProgress = dialogView.findViewById( R.id.downloadProgressbar ) ;
        Button cancelDownloadButton = dialogView.findViewById(R.id.dismissDownloadButton);
        builder.setView( dialogView ) ;
        builder.setCancelable( false ) ;
        downloadDialog = builder.create() ;
        downloadDialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnims ;
        downloadDialog.show();
        cancelDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDownloadCancelled = true ;
                isDownloadCompleted = false ;
                sharedPreferences.edit().putBoolean( getString( R.string.download_completed_key) , isDownloadCompleted).apply();
                downloadDialog.dismiss();
                Toast.makeText(MainActivity.this, "Download process cancelled", Toast.LENGTH_LONG).show();
            }
        });
        isDownloadCancelled = false ;
    }

    private void showNoConnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this )  ;
        View dialogView = getLayoutInflater().inflate( R.layout.no_connection_dialog , null , false ) ;
        Button openSettings = dialogView.findViewById( R.id.openSettingsButton );
        final AlertDialog dialog = builder.setView( dialogView ).create() ;
        openSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                dialog.dismiss();
            }
        });
        dialog.getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnims ;
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( requestCode == STORAGE_PERMISSION_CODE ) {
            if ( grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                if ( isConnected() ) {
                    fetchImagePaths() ;
                }
                else {
                    showNoConnectionDialog();
                }
            }
        }
    }

    interface DownloadInterface {
        void onDownloadFinished( int index ) ;
        void onErrorWhileDownloading(int index) ;
    }

    private final DownloadInterface downloadInterface = new DownloadInterface() {

        @Override
        public void onDownloadFinished( int index ) {
            try {
                START_IMAGE_DOWNLOAD_INDEX = index;
                sharedPreferences.edit().putInt( getString( R.string.download_index_key) ,
                        START_IMAGE_DOWNLOAD_INDEX ).apply();
                if ( index < (imagePaths.size() - 1) ) {
                    if ( !isDownloadCancelled ) {
                        initiateImageDownloads( imagePaths , false , index + 1);
                    }
                }
                else {
                    downloadDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Images downloaded.", Toast.LENGTH_LONG).show();
                    isDownloadCompleted = true ;
                    sharedPreferences.edit().putBoolean( getString( R.string.download_completed_key) , isDownloadCompleted).apply();
                }
            }
            catch ( Exception e ){
                e.printStackTrace();
            }
        }

        @Override
        public void onErrorWhileDownloading(int index) {
            START_IMAGE_DOWNLOAD_INDEX = index ;
            isDownloadCompleted = false ;
            sharedPreferences.edit().putBoolean( getString( R.string.download_completed_key) , isDownloadCompleted).apply();
            downloadDialog.dismiss();
            if ( index == 0 ) {
                Toast.makeText(MainActivity.this, "Could not download the images.", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(MainActivity.this, "Could not download some images.", Toast.LENGTH_LONG).show();
            }
        }
    } ;

    private boolean isConnected () {
        ConnectivityManager manager = ( ConnectivityManager )getSystemService( CONNECTIVITY_SERVICE ) ;
        NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void initiateImageDownloads(ArrayList<String> imagePaths , boolean showDialog , final int index )  {
        if ( showDialog ) {
            showDownloadDialog() ;
        }
        progressText.setText( "Downloading images ( " + (index + 1) + " / "
                + imagePaths.size() +" )");
        StorageReference imageRef = rootStorageRef.child( imagePaths.get(index) ) ;
        if ( !imagesDirectory.exists() ) {
            imagesDirectory.mkdirs() ;
        }
        File imageFile = new File( imagesDirectory.getAbsolutePath() + "/" +
                imagePaths.get(index) ) ;
        final FileDownloadTask task = imageRef.getFile(Uri.fromFile(imageFile)) ;
        task.addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                downloadInterface.onDownloadFinished( index );
            }
        }) ;
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                downloadInterface.onErrorWhileDownloading( index ) ;
            }
        }) ;
        task.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                long progress = ( taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount() ) * 100 ;
                downloadProgress.setProgress( ( int )progress );
            }
        });
    }

    private void startImageDownloads() {
        if (ActivityCompat.checkSelfPermission( this , Manifest.permission.WRITE_EXTERNAL_STORAGE )!=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions( this , new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE} ,
                    STORAGE_PERMISSION_CODE ) ;
        }
        else {
            if ( isConnected() ) {
                fetchImagePaths() ;
            }
            else {
                showNoConnectionDialog();
            }
        }
    }

}
