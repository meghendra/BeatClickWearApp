package com.example.megh.simplewearapp;

/**
 * Created by megh on 12/3/17.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements MessageApi.MessageListener, SensorEventListener, GoogleApiClient.ConnectionCallbacks {

    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private GoogleApiClient mApiClient;

    private TextView mTextView;
    private Button mSendButton;
    private boolean startFlag = false;

    private static long timeDelayMillis = 5000;
    private static long lastTriggerMillis = 0;

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGoogleApiClient();
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTextView = (TextView) findViewById(R.id.heartRateText);
        mSendButton = (Button) findViewById(R.id.btn_send );

        mSendButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String text = "Moto says hi!";
//                sendMessage(WEAR_MESSAGE_PATH, text);
                if (!startFlag) {
                    mTextView.setText("Please wait...");
                    mSendButton.setText("STOP");
                    sendMessage(START_ACTIVITY,"");
                    startMeasure();
                    startFlag=true;
                }
                else{
                    mTextView.setText("---");
                    mSendButton.setText("START");
                    startFlag=false;
                    stopMeasure();
                }
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
    }

    private void startMeasure() {
        boolean sensorRegistered = mSensorManager.registerListener(this, mHeartRateSensor, 5000000, 5000000);
        Log.d("Sensor Status:", " Sensor registered: " + (sensorRegistered ? "yes" : "no"));
    }

    private void stopMeasure() {
        mSensorManager.unregisterListener(this);
        sendMessage(WEAR_MESSAGE_PATH, "Stop");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float mHeartRateFloat = event.values[0];

        int mHeartRate = Math.round(mHeartRateFloat);

        mTextView.setText(Integer.toString(mHeartRate));
        String text = "HR:"+mHeartRate;
        sendMessage(WEAR_MESSAGE_PATH, text);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    private void initGoogleApiClient() {

        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();

        mApiClient.connect();

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {    }

    @Override
    public void onConnected(Bundle bundle) {
        sendMessage(START_ACTIVITY, "");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}