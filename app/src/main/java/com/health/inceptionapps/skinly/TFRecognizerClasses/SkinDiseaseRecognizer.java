package com.health.inceptionapps.skinly.TFRecognizerClasses;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SkinDiseaseRecognizer {

    private final Context context ;
    public static final float RECOGNIZER_ERROR_CODE = 145f ;

    public SkinDiseaseRecognizer( Context context) {
        this.context = context ;
    }

    public float getSimilarity ( Bitmap image1 , Bitmap image2 ) {
        try {
            Interpreter interpreter = new Interpreter( loadModelFile() ) ;
            int INPUT_DIM = 32;
            float[][] x1 = new float[][]{mapBitmapToFloatArray( resizeBitmap( image1 , INPUT_DIM) , INPUT_DIM) };
            float[][] x2 = new float[][]{mapBitmapToFloatArray( resizeBitmap( image2 , INPUT_DIM) , INPUT_DIM) };
            Object[] inputs = { x1 , x2 } ;
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, new float[1][1] );
            interpreter.runForMultipleInputsOutputs( inputs , outputs ) ;
            float[][] similarity = ( float[][] )outputs.get( 0 ) ;
            return similarity[0][0] ;
        }
        catch (IOException e) {
            e.printStackTrace();
            return RECOGNIZER_ERROR_CODE;
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        String MODEL_ASSETS_PATH = "recog_model.tflite";
        AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(MODEL_ASSETS_PATH) ;
        FileInputStream fileInputStream = new FileInputStream( assetFileDescriptor.getFileDescriptor() ) ;
        FileChannel fileChannel = fileInputStream.getChannel() ;
        long startoffset = assetFileDescriptor.getStartOffset() ;
        long declaredLength = assetFileDescriptor.getDeclaredLength() ;
        return fileChannel.map( FileChannel.MapMode.READ_ONLY , startoffset , declaredLength ) ;
    }

    private Bitmap resizeBitmap(Bitmap image , int dimension ) {
        return Bitmap.createScaledBitmap( image , dimension , dimension , true ) ;
    }

    private float[] mapBitmapToFloatArray( Bitmap image , int dimension ) {
        ArrayList< Float > imageArray = new ArrayList<>() ;
        for ( int x = 0 ; x < dimension ; x ++ ) {
            for ( int y = 0 ; y < dimension ; y ++ ) {
                float R = ( float ) Color.red( image.getPixel( x , y ) );
                float G = ( float )Color.green( image.getPixel( x , y ) );
                float B = ( float )Color.blue( image.getPixel( x , y ) );
                imageArray.add( R / 255 ) ;
                imageArray.add( G / 255 ) ;
                imageArray.add( B / 255 ) ;
            }
        }
        float[] imageArrayFloat = new float[ dimension * dimension * 3 ] ;
        for ( int i = 0 ; i < imageArray.size() ; i ++ ){
            imageArrayFloat[i] = imageArray.get( i ) ;
        }
        return imageArrayFloat ;
    }


}
