

package com.neurosky.algo_sdk_sample;

//mongodb mobile imports

import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;

// Packages needed to interact with MongoDB and Stitch
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

// Necessary component for working with MongoDB Mobile
import com.mongodb.stitch.android.services.mongodb.local.LocalMongoDbService;

import org.bson.*;

import android.media.MediaPlayer;
import android.net.Uri;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.res.AssetManager;
import android.app.AlertDialog;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import com.mongodb.client.FindIterable;
import com.neurosky.AlgoSdk.NskAlgoConfig;
import com.neurosky.AlgoSdk.NskAlgoDataType;
import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.AlgoSdk.NskAlgoSignalQuality;
import com.neurosky.AlgoSdk.NskAlgoState;
import com.neurosky.AlgoSdk.NskAlgoType;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;
import com.androidplot.xy.*;



public class MainActivity extends Activity {

    final String TAG = "MainActivityTag";

    // COMM SDK handles
    private TgStreamReader tgStreamReader;
    private BluetoothAdapter mBluetoothAdapter;

    // canned data variables
    private short raw_data[] = {0};
    private int raw_data_index= 0;

    private Button neurosky_connect_btn;
    private TextView attValue;
    private TextView forced_blink_strength_text;
    private TextView direction_text;
    private TextView state_text;
    private TextView sqText;
    private TextView test_textview;
    private NskAlgoSdk nskAlgoSdk;
    private TextView avg_att;
    private TextView avg_blink;
    long previous_click_time;

    //Additional Bluetooth Connect Variables HC-06
    private final String DEVICE_ADDRESS = "B8:27:EB:93:92:2D"; //MAC Address of Bluetooth Module
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private BluetoothSocket socket;

    private OutputStream outputStream;
    private InputStream inputStream;
    boolean connected = false;
    boolean isRunning = false;
    String command;
    Button bt_connect_btn;
    Button soundbutton;


    public int OldMax =100;
    public int OldMin =0;
    public int NewMax =15;
    public int NewMin =0;
    public int global_att;
    public int OldRange = (OldMax - OldMin);
    public int NewRange = (NewMax - NewMin);


    private Context mContext;
    private Activity mActivity;

    private LinearLayout mRootLayout;
    private LinearLayout COLOR_WALA;

    private Button mBtnSetMediaVolume;
    private TextView mTVStats;

    private AudioManager mAudioManager;
    private Random mRandom = new Random();
    public int att_global;
    //////

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_connect_btn = (Button) findViewById(R.id.bt_connect_btn);

        neurosky_connect_btn = (Button) findViewById(R.id.neurosky_connect_btn);

        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.setInitialScale(50);

        //myWebView.loadUrl("http://192.168.43.122/html");


        ListView lv = (ListView) findViewById(R.id.MongoDoc);


        avg_att = findViewById(R.id.avg_att);
        avg_blink = findViewById(R.id.avg_blink);


        //mongodb client initialize

        final StitchAppClient client = Stitch.initializeDefaultAppClient("<APPLICATION_ID >");

       // Create a Client for MongoDB Mobile (initializing MongoDB Mobile)
        final MongoClient mobileClient = client.getServiceClient(LocalMongoDbService.clientFactory);


        // Point to the target collection and insert a document
        MongoCollection<Document> localCollection =
                mobileClient.getDatabase("brain").getCollection("attention");

        final String[] directions = { "FORWARD", "REVERSE", "LEFT", "RIGHT" };


        // Get the application context
        mContext = getApplicationContext();
        mActivity = MainActivity.this;

        // Get the widget reference from xml layout
        mRootLayout = findViewById(R.id.root_layout);
        COLOR_WALA = findViewById(R.id.color_wala);



        soundbutton = findViewById(R.id.sound);
        //final MediaPlayer mp = MediaPlayer.create(this, R.raw.water);

        final MediaPlayer mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_ALARM);
        try {
            mp.setDataSource(this,Uri.parse("android.resource://"+mContext.getPackageName()+"/"+R.raw.water)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mp.start();

        soundbutton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v) {
                mp.start();
            }
        });


        nskAlgoSdk = new NskAlgoSdk();


        try
        {
            // (1) Make sure that the device supports Bluetooth and Bluetooth is on
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Toast.makeText(
                        this,
                        "Please enable your Bluetooth and re-run this program !",
                        Toast.LENGTH_LONG).show();
                //finish();
            }
        }

        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
            return;
        }

        attValue = (TextView)this.findViewById(R.id.attText);
        forced_blink_strength_text = (TextView) this.findViewById(R.id.forced_blink_strength_text);
        test_textview = (TextView) this.findViewById(R.id.test_textview);
        sqText = (TextView)this.findViewById(R.id.sqText);
        sqText.setText("AGAIN");

        int algoTypes = 0;

        algoTypes += NskAlgoType.NSK_ALGO_TYPE_MED.value;

        algoTypes += NskAlgoType.NSK_ALGO_TYPE_ATT.value;

        algoTypes += NskAlgoType.NSK_ALGO_TYPE_BLINK.value;

        algoTypes += NskAlgoType.NSK_ALGO_TYPE_BP.value;


        int ret = nskAlgoSdk.NskAlgoInit(algoTypes, getFilesDir().getAbsolutePath());

        nskAlgoSdk.NskAlgoStart(false);

        // state_text.setText("--");

        // Raspberry piBLUETOOTH CONNECT BUTTON

        bt_connect_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {

                if(BTinit())
                {
                    BTconnect();
                }
            }

        });

        neurosky_connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                raw_data = new short[512];
                raw_data_index = 0;

                tgStreamReader = new TgStreamReader(mBluetoothAdapter,callback);

                if(tgStreamReader != null && tgStreamReader.isBTConnected()){

                    // Prepare for connecting
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }

                tgStreamReader.connect();
                //state_text.setText("STANDBY");
            }
        });

        nskAlgoSdk.setOnSignalQualityListener(new NskAlgoSdk.OnSignalQualityListener() {
            @Override
            public void onSignalQuality(final int level) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String sqStr = NskAlgoSignalQuality.values()[level].toString();
                        sqText.setText(sqStr);

                        if(sqText.getText().toString().equals("NOT DETECTED") || sqText.getText().toString().equals("POOR") || sqText.getText().toString().equals("MEDIUM"))
                        {
                            command = "NOT_GOOD_SIGNAL";

                            try
                            {
                                outputStream.write(command.getBytes());
                            }
                            catch(IOException e)
                            {
                                e.printStackTrace();
                            }


                            if(isRunning = true)
                            {

                            }

                        }

                        else
                        {
                            //state_text.setText("GOOD Quality");
                            int cond=1;

                        }

                    }
                });
            }
        });



        nskAlgoSdk.setOnAttAlgoIndexListener(new NskAlgoSdk.OnAttAlgoIndexListener()
        {
            @Override
            public void onAttAlgoIndex(int value)
            {
                global_att = value;

                String send_med_value = String.valueOf(value);
                String text2="ATTENTION=";
                text2=text2+send_med_value+"/n";

                try
                {
                    outputStream.write(send_med_value.getBytes());
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }

                Log.d(TAG, "NskAlgoAttAlgoIndexListener: Attention:" + value);
                String attStr = "[" + value + "]";
                final String finalAttStr = attStr;
                att_global=value;
                //mBtnSetMediaVolume.performClick();


                    Document doc2 = new Document();
                    doc2.put("date", Calendar.getInstance().getTime());
                    doc2.put("attention", att_global);
                    doc2.put("eye-blink", 0);

                    localCollection.insertOne(doc2);


                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // change UI elements here

                        attValue.setText(finalAttStr);

                        //RED
                        if(global_att > 0 && global_att < 15)
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#FF0000"));
                        }
                        //ORANGE
                        else if (global_att > 16 && global_att < 30)
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#FF7F00"));

                        }
                        //YELLOW
                        else if (global_att > 31 && global_att < 45)
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#FFFF00"));

                        }
                        //GREEN
                        else if (global_att > 46 && global_att < 60)
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#00FF00"));

                        }
                        //BLUE
                        else if (global_att > 61 && global_att < 75)
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#0000FF"));

                        }
                        //INDIGO
                        else if (global_att > 76 && global_att < 90)
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#4B0082"));

                        }

                        //VIOLET
                        else
                        {
                            COLOR_WALA.setBackgroundColor(Color.parseColor("#9400D3"));

                        }

                        FindIterable<Document> cursor = localCollection.find();

            List<Document> results = (ArrayList<Document>) cursor.into(new ArrayList<Document>());

                        int len = results.size();
                        int total=0;
                        int avg = 0;
                        int avg_att_java;
                        int i;

                        for(i=0; i<len; i++)
                        {
//                            avg_att_java = results.get(i).get("attention");
                            avg_att_java = results.get(i).getInteger("attention");

//                            attis = avg_att_java;
                            total=total+avg_att_java;
                        }
                        avg = total/len;

                        avg_att.setText(String.valueOf(avg));


                        Collections.reverse(results);

            ArrayAdapter<Document> arrayAdapter = new ArrayAdapter<Document>(
                    MainActivity.this,
                    android.R.layout.simple_list_item_1,
                    results );
            lv.setAdapter(arrayAdapter);

                    }
                });
            }
        });


        nskAlgoSdk.setOnEyeBlinkDetectionListener(new NskAlgoSdk.OnEyeBlinkDetectionListener() {
            @Override
            public void onEyeBlinkDetect(int strength) {
                Log.d(TAG, "NskAlgoEyeBlinkDetectionListener: Eye blink detected: " + strength);
                final int final_strength = strength;
                final String double_blink = "Double Blink Detected";
                final String send_eye_blink_value = String.valueOf(strength);
//                 String text1 = "Eye Blink Detected ----";




                    Document doc3 = new Document();
                    doc3.put("date", Calendar.getInstance().getTime());
                    doc3.put("attention", 0);
                    doc3.put("eye-blink", final_strength);


                    localCollection.insertOne(doc3);
                    //Document doc = localCollection.find();


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text1 = "BLINK_DETECTED";
                        soundbutton.performClick();
                        if (final_strength < 1) {
                            //normal_blink_strength_text.setText(String.valueOf(final_strength));
                        } else {
                            forced_blink_strength_text.setText(String.valueOf(final_strength));

                            try {
                                outputStream.write(text1.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }



                            long temp = System.currentTimeMillis();

                            if (previous_click_time != 0) {
                                if (temp - previous_click_time < 1000) {
                                    test_textview.setText("DOUBLE_BLINK");
                                    soundbutton.performClick();
                                    command="DOUBLE_BLINK";
//

                                    try {
                                        outputStream.write(command.getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    test_textview.setText("NO DOUBLE BLINK");
                                }
                            }

                            previous_click_time = temp;
                        }



                        //mongo update

                        FindIterable<Document> cursor = localCollection.find();

                        List<Document> results = (ArrayList<Document>) cursor.into(new ArrayList<Document>());

                        int len = results.size();
                        int attis;
                        int total=0;
                        int avg = 0;
                        int avg_blink_java;
                        int i;
                        int count=0;

                        for(i=0; i<len; i++)
                        {

                            avg_blink_java = results.get(i).getInteger("eye-blink");
                            if(avg_blink_java != 0)
                            {
                                total = total + avg_blink_java;
                                count++;

                            }
                        }
                        avg = total/count;

                        avg_blink.setText(String.valueOf(avg));


                        Collections.reverse(results);


                        ArrayAdapter<Document> arrayAdapter = new ArrayAdapter<Document>(
                                MainActivity.this,
                                android.R.layout.simple_list_item_1,
                                results );
                        lv.setAdapter(arrayAdapter);



                    }

                });

            }
        });

    } // END OF ONCREATE

    public boolean BTinit()
    {
        boolean found = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            Toast.makeText(getApplicationContext(), "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
        }

        if(!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter,0);

            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            Toast.makeText(getApplicationContext(), "Please pair the device first", Toast.LENGTH_SHORT).show();
        }
        else
        {
            for(BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }

        return found;
    }




    public boolean BTconnect()
    {
        try
        {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Creates a socket to handle the outgoing connection
            socket.connect();

            Toast.makeText(getApplicationContext(),
                    "Connection to Raspberry Pi successful", Toast.LENGTH_LONG).show();
            connected = true;
            neurosky_connect_btn.setEnabled(true);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            connected = false;
        }

        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            try
            {
                inputStream = socket.getInputStream(); //gets the input stream of the socket
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return connected;
    }

    @Override
    public void onBackPressed() {
        nskAlgoSdk.NskAlgoUninit();
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    tgStreamReader.start();
                    showToast("Connection to Neurosky Mindwave Mobile successful", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    showToast("Get data time out!", Toast.LENGTH_SHORT);

                    break;
                case ConnectionStates.STATE_STOPPED:
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    break;
                case ConnectionStates.STATE_ERROR:
                    break;
                case ConnectionStates.STATE_FAILED:
                    break;
            }
        }

        @Override
        public void onRecordFail(int flag) {
            // You can handle the record error message here
            Log.e(TAG,"onRecordFail: " +flag);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // You can handle the bad packets here.
        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // You can handle the received data here
            // You can feed the raw data to algo sdk here if necessary.
            //Log.i(TAG,"onDataReceived");
            switch (datatype) {
                case MindDataType.CODE_ATTENTION:

                    short attValue[] = {(short)data};
//                    String send4="["+data+"]";
//                    attValue.setText(send4);
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_ATT.value, attValue, 1);
                    break;
                case MindDataType.CODE_MEDITATION:
                    short medValue[] = {(short)data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_MED.value, medValue, 1);
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    short pqValue[] = {(short)data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_PQ.value, pqValue, 1);
                    //sample.setText(String.valueOf(pqValue));
                    break;
                case MindDataType.CODE_RAW:
                    raw_data[raw_data_index++] = (short)data;
                    if (raw_data_index == 512) {
                        nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_EEG.value, raw_data, raw_data_index);
                        raw_data_index = 0;
                    }
                    break;
                default:
                    break;
            }
        }

        public void showToast(final String msg, final int timeStyle) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, timeStyle).show();
                }

            });
        }
    };}
























