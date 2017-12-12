package com.example.megh.simplewearapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private static final String WEAR_MESSAGE_PATH = "/message";
    private static long timeDelayMillis = 5000;
    private static long lastTriggerMillis = 0;

    private GoogleApiClient mApiClient;
    private ArrayAdapter<String> mAdapter;
    private ListView mListView;
    private static Camera mCamera = null;
    private CameraPreview mPreview;
    private boolean safeToTakePicture = false;
    private static int currentHeartRate = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        lastTriggerMillis = System.currentTimeMillis();
        initGoogleApiClient();
        mCamera = Camera.open(0);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {

        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
                    String msg = new String(messageEvent.getData());
                    if(msg.equals("Stop"))
                    {
                        finish();
                    }
                    else {
                        getHeartRate(msg);
                    }
                }
            }
        });
    }

    private void getHeartRate(String msg)
    {

        String [] hrStr = msg.split(":");
        int hr = Integer.parseInt(hrStr[1]);
        long currentMillis = System.currentTimeMillis();
        if ((currentMillis - lastTriggerMillis) >= timeDelayMillis) {
            if (safeToTakePicture) {
                currentHeartRate = hr;
                mCamera.setDisplayOrientation(90);
                mCamera.takePicture(null, null, mPicture);
                mAdapter.add(msg + "(click)");
                mAdapter.notifyDataSetChanged();
                safeToTakePicture = false;
                lastTriggerMillis = currentMillis;
                mPreview.destroyDrawingCache();
            } else {
                mAdapter.add(msg + "(fail)");
                mAdapter.notifyDataSetChanged();
            }
        }
        else{
            mAdapter.add(msg);
            mAdapter.notifyDataSetChanged();
            mPreview = new CameraPreview(this, mCamera);
        }
    }

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null){
                Log.d("Simple Wear App", "Error creating media file, check storage permissions: " );
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                safeToTakePicture = true;
            } catch (FileNotFoundException e) {
                Log.d("Simple Wear App", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Simple Wear App", "Error accessing file: " + e.getMessage());
            }
        }
    };

    /** Create a File for saving the image*/
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SimpleWearAppHR2");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Simple Wear App", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + "_"+currentHeartRate+".jpg");
        return mediaFile;
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    private void init() {
        mListView = (ListView) findViewById(R.id.list_view );
        mAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1 );
        mListView.setAdapter( mAdapter );
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener( mApiClient, this );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /** The Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
                safeToTakePicture = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
                safeToTakePicture = true;

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}