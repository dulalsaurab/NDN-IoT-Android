package com.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import NDNLiteSupport.BLEFace.BLEFace;
import NDNLiteSupport.BLEUnicastConnectionMaintainer.BLEUnicastConnectionMaintainer;
import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.NDNLiteSupportInit;
import NDNLiteSupport.SignOnBasicControllerBLE.SignOnBasicControllerBLE;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResultCodes;
import NDNLiteSupport.transport.ble.BLEAdvertiser;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_256;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.SecurityHelpers.asnEncodeRawECPublicKeyBytes;
import static com.example.HARDCODED_EXPERIMENTATION_SIGN_ON_BLE_ECC_256.*;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // constants for the permission request broadcasts
    private final int REQUEST_COARSE_LOCATION = 11;
    // counter for how many times we failed to get permission from user; if it goes over MAX_PERMISSION_REQUEST_FAILURES,
    // just print a failure message
    private int m_permission_request_failures = 0;
    private int MAX_PERMISSION_REQUEST_FAILURES = 10;

    // Log for tagging.
    private final String TAG = MainActivity.class.getSimpleName();

    // Reference to interact with the secure sign on controller over BLE singleton.
    private SignOnBasicControllerBLE m_SignOnBasicControllerBLE;

    // Reference to interact with the ble unicast connection maintainer. Both the
    // BLEFace and the SignOnBasicControllerBLE object depend on this to proactively
    // maintain connections to devices using the ndn lite library. You MUST initialize
    // this for the SignOnBasicControllerBLE and BLEFace to work; if you do not initialize
    // this, then you will never actually connect to any devices over BLE.
    private BLEUnicastConnectionMaintainer m_BLEUnicastConnectionMaintainer;
    private BLEAdvertiser m_BLEAdvertiser;


    private BLEFace mBLEFace;

    // References to UI objects.
    private TextView m_log;
    private Button m_btn;
    private Button m_btnMacAddress;
    private TextInputEditText m_MacAdress;

    private SensorManager mSensorManager;



    private int hearRate = -1;

    // Callback for when an interest is received. In this example, the nRf52840 sends an interest to
    // us after sign on is complete, and triggers this callback.
    OnInterestCallback onInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filter) {
            logMessage(TAG, "onInterest got called, prefix of interest: " + prefix.toUri());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{Manifest.permission.BODY_SENSORS}, 1);
        } else {
            Log.d(TAG, "ALREADY GRANTED");
        }

        // Iniitalize elements of the UI.
        initializeUI();



        NDNLiteSupportInit.NDNLiteSupportInit();

        CertificateV2 trustAnchorCertificate = new CertificateV2();

        // initializing the BLEUnicastConnectionMaintainer
        // (YOU MUST DO THIS FOR SecureSignOnControllerBLE AND BLEFace TO FUNCTION AT ALL)
        m_BLEUnicastConnectionMaintainer = BLEUnicastConnectionMaintainer.getInstance();
        m_BLEUnicastConnectionMaintainer.initialize(this);

        m_BLEAdvertiser = new BLEAdvertiser(this);
        m_BLEAdvertiser.startAdvertising();



        m_btnMacAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String macAddress = m_MacAdress.getText().toString();
                Log.d(TAG,macAddress);
                mBLEFace = new BLEFace(macAddress, new OnInterestCallback() {
                    @Override
                    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {

//                        mBLEFace.putData();
//                        Log.d(TAG,interest.toUri());
                        m_log.append(interest.toUri()+"\n");
                    }
                });

            }
        });
;
        m_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_log.setText(new Date().toString()+"\n");
//                m_log.append("HeartRate : "+hearRate+"\n");
                mBLEFace.expressInterest(new Interest("/ble/test"), new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        Log.d(TAG,interest.toUri().toString());
                        Log.d(TAG,data.toString());
                    }
                });


            }
        });


    }

    private void initializeUI() {
        m_log = (TextView) findViewById(R.id.ui_log);
        m_btnMacAddress=(Button)findViewById(R.id.ui_btnSetMac);
        m_btn = (Button) findViewById(R.id.ui_btnSendData);
        m_MacAdress=findViewById((R.id.ui_text_input));



    }

    private void logMessage(String TAG, String msg) {
        Log.d(TAG, msg);
        logMessageUI(TAG, msg);
    }

    private void logMessageUI(final String TAG, final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_log.append(TAG + ":" + "\n");
                m_log.append(msg + "\n");
                m_log.append("------------------------------" + "\n");
            }
        });
    }







}