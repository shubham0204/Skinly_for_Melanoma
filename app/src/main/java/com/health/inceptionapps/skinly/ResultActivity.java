package com.health.inceptionapps.skinly;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.health.inceptionapps.skinly.TFRecognizerClasses.SkinDiseaseRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ResultActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 123 ;
    private Bitmap[] compareImages ;

    private SkinDiseaseRecognizer recognizer ;

    private static final int DETECTION_RESULT_POSITIVE = 0 ;
    private static final int DETECTION_RESULT_NEGATIVE = 1 ;
    private static final int K = 3 ;

    private ImageView sampleImageView ;
    private TextView resultTextView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getWindow().setStatusBarColor( getColor( android.R.color.white ) ) ;

        sampleImageView = findViewById( R.id.sampleImageView ) ;
        resultTextView = findViewById( R.id.resultText ) ;

        recognizer = new SkinDiseaseRecognizer( this ) ;

        try {
            launchCameraIntent();
            new ImagesLoadTask().execute( getImagePaths() ) ;
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    private void launchCameraIntent(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE) ;
        startActivityForResult( cameraIntent , CAMERA_REQUEST_CODE );
    }

    private String[] getImagePaths() {
        File rootDir = new File( Environment.getExternalStorageDirectory() + "/Android/data/" +
                getPackageName() + "/images" );
        File[] imageFiles = rootDir.listFiles() ;
        Arrays.sort(imageFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo( o2.getName() );
            }
        });
        String[] paths = new String[ imageFiles.length ] ;
        for ( int i = 0 ; i < imageFiles.length ; i ++ ) {
            paths[i] = imageFiles[i].getAbsolutePath() ;
            Log.e( ResultActivity.class.getSimpleName() , "RESULT PATH : " + imageFiles[i].getAbsolutePath()  ) ;
        }
        return paths ;
    }


    public void onBackButtonClick(View view ) {
        finish();
    }


    public void onRecheckButtonClick( View view ) {
        launchCameraIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == CAMERA_REQUEST_CODE ) {
            if ( resultCode == RESULT_OK ) {
                try {
                    Bitmap image = ( Bitmap ) data.getExtras().get( "data" ) ;
                    sampleImageView.setImageBitmap( image   ) ;
                    showResultsForImage( scaleCenterCrop( image , 16 , 16 )  ) ;
                }
                catch ( Exception e ){
                    e.printStackTrace();
                }
            }
            else {
                finish();
            }
        }
    }


    private Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);
        return Bitmap.createScaledBitmap( dest , 32 , 32 , false ) ;
    }


    private void showResultsForImage( Bitmap image ) {
        int detection = detectImageInSample( image ) ;
        Log.e( ResultActivity.class.getSimpleName() , "DETECTION : " + detection) ;
        if ( detection == DETECTION_RESULT_POSITIVE ) {
            resultTextView.setText( getString( R.string.melanoma_result  )) ;
        }
        else if ( detection == DETECTION_RESULT_NEGATIVE ) {
            Log.e( ResultActivity.class.getSimpleName() , "NO DISEASE" ) ;
            resultTextView.setText( getString( R.string.no_disease_found_result  ));
        }
    }

    private int detectImageInSample ( Bitmap hostImage ){
        ArrayList<Float> similarityScores = new ArrayList<>() ;
        for ( Bitmap sampleImage : compareImages ) {
            float score = recognizer.getSimilarity( hostImage , sampleImage ) ;
            if ( score != SkinDiseaseRecognizer.RECOGNIZER_ERROR_CODE ) {
                similarityScores.add( score );
            }
            else {
                Log.e( ResultActivity.class.getSimpleName() , "ERROR in Recognizer" ) ;
                break;
            }
        }
        Log.e( ResultActivity.class.getSimpleName() , "SCORES : " + similarityScores.toString() ) ;
        ArrayList<Float> scoresClone = ( ArrayList<Float> )similarityScores.clone() ;
        Collections.sort(scoresClone, new Comparator<Float>() {
            @Override
            public int compare(Float o1, Float o2) {
                return o2.compareTo( o1 ) ;
            }
        });
        ArrayList<Integer> classes = new ArrayList<>() ;
        for ( int i = 0 ; i < K ; i ++ ) {
            int index = similarityScores.indexOf( scoresClone.get(i) )  ;
            if ( index < similarityScores.size() / 2 ) {
                classes.add( 0 );
            }
            else {
                classes.add( 1 );
            }
        }
        HashMap< Integer , Integer > freqCounts = new HashMap<>() ;
        for ( Integer value : classes ) {
            int count = 0 ;
            for ( Integer x : classes ) {
                if (value.equals(x)){
                    count += 1 ;
                }
            }
            freqCounts.put( count , value ) ;
        }
        Integer maxKey = Integer.MIN_VALUE ;
        for ( Integer x : freqCounts.keySet() ) {
            if ( x > maxKey ) {
                maxKey = x ;
            }
        }
        int class_ = freqCounts.get( maxKey ) ;
        if ( class_ == 0 ){
            return DETECTION_RESULT_NEGATIVE ;
        }
        else {
            return DETECTION_RESULT_POSITIVE ;
        }
    }


    private class ImagesLoadTask extends AsyncTask< String[] , Void , Bitmap[]>  {

        @Override
        protected Bitmap[] doInBackground(String[]... strings) {
            Bitmap[] images = new Bitmap[ strings[0].length ] ;
            for ( int i = 0 ; i < strings[0].length ; i ++ ) {
                try {
                    FileInputStream fileInputStream =
                            new FileInputStream( new File( strings[0][i] ) ) ;
                    Bitmap image = BitmapFactory.decodeStream( fileInputStream ) ;
                    images[i] = image ;
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return images ;
        }

        @Override
        protected void onPostExecute(Bitmap[] bitmaps) {
            super.onPostExecute(bitmaps);
            compareImages = bitmaps ;
        }

    }

}
