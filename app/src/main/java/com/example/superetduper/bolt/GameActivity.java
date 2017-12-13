package com.example.superetduper.bolt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class GameActivity extends AppCompatActivity {
    private final static String LOG_TAG = GameActivity.class.getSimpleName();

    private static final int BLUETOOTH_ENABLE_RESULT_CODE = 100;

    private static final int STATE_CONNECTING = 0;
    private static final int STATE_TESTING_CONNECTION = 1;
    private static final int STATE_INGAME = 2;
    private static final int STATE_GAME_ENDED = 3;

    private int mState = STATE_CONNECTING;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService;

    private TextView statusTextView;
    private LinearLayout statusBox;
    private TextView countDownTimer;

    private int mLedNumberToPress;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            mBluetoothService.connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate");
        setContentView(R.layout.activity_game);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "No Bluetooth Support", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindService(new Intent(this, BluetoothService.class), mServiceConnection, BIND_AUTO_CREATE);

        makeFullScreen();

        statusTextView = findViewById(R.id.status_text_view);
        statusBox = findViewById(R.id.progressBox);
        countDownTimer = findViewById(R.id.count_down_text_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(LOG_TAG, "onResume");

        if (!mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_ENABLE_RESULT_CODE);
        }

        registerReceiver(mBroadcastReceiver, getGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);

        Log.i(LOG_TAG, "onPause");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        Log.i(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == BLUETOOTH_ENABLE_RESULT_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null){
                Log.w(LOG_TAG, "Action is null");
            }

            switch (intent.getAction()) {
                case BluetoothService.MSG_PROGRESS:
                    String status = intent.getStringExtra(BluetoothService.EXTRA_PROGRESS_STRING);
                    updateStatus(status);
                    Log.i(LOG_TAG, "Broadcast : Progress : " + status);
                    break;
                case BluetoothService.MSG_GATT_DISCONNECTED:
                    Log.i(LOG_TAG, "Broadcast : Disconnected");
                    break;
                case BluetoothService.MSG_GATT_CONNECTED:
                    updateStatus("Testing connection");
                    Log.i(LOG_TAG, "Broadcast : Testing connection");
                    mState = STATE_TESTING_CONNECTION;
                    mBluetoothService.writeBLEData(Packet.getPacketBegin());
                    break;
                case BluetoothService.MSG_RECEIVED_DATA:
                    byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_BLE_DATA);
                    if (data == null) {
                        Log.w(LOG_TAG, "Broadcast called but no data passed");
                        return;
                    }

                    parseData(new String(data));
            }
        }
    };

    private void parseData(@NonNull String data) {
        if (Packet.isAcknowledge(data)) {
            gotAcknowledged();
        } else if (Packet.isValidPacket(data) && Packet.isButtonPressed(data)) {
            buttonPressed(Packet.getButtonNumber(data));
        } else {
            Log.w(LOG_TAG, "Could not parse data : " + Packet.formatForPrint(data));
        }
    }

    private void gotAcknowledged() {
        Log.i(LOG_TAG, "Received acknowledge");
        if (mState == STATE_TESTING_CONNECTION) {
            statusBox.setVisibility(View.GONE);
            startGame();
        }
    }

    private void buttonPressed(int buttonNumber) {
        Log.i(LOG_TAG, "Button number " + buttonNumber + " pressed");
        if (buttonNumber == mLedNumberToPress && mState == STATE_INGAME) {
            String dataToSend = Packet.getAcknowledge() + Packet.getPacketLedOff(mLedNumberToPress);

            mLedNumberToPress = getRandomLedNumber();

            dataToSend += Packet.getPacketLedOn(mLedNumberToPress) + Packet.getPacketShift();

            mBluetoothService.writeBLEData(dataToSend);
        }
    }

    private void startGame() {
        mState = STATE_INGAME;
        mGameTimer.start();
        mLedNumberToPress = getRandomLedNumber();
        mBluetoothService.writeBLEData(Packet.getPacketLedOn(mLedNumberToPress) + Packet.getPacketShift());
    }

    private final CountDownTimer mGameTimer = new CountDownTimer(30000, 10) {

        private final NumberFormat formatter = new DecimalFormat("#0.000");

        @Override
        public void onTick(long l) {
            countDownTimer.setText(formatter.format(l / 1000.0));
        }

        @Override
        public void onFinish() {
            countDownTimer.setText("0.000");
            mState = STATE_GAME_ENDED;

            mBluetoothService.writeBLEData(Packet.getPacketLedOff(mLedNumberToPress) + Packet.getPacketShift() + Packet.getPacketEnd());
        }
    };

    private static int getRandomLedNumber() {
        return (int) (Math.random() * 64);
    }


    private static IntentFilter getGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.MSG_PROGRESS);
        intentFilter.addAction(BluetoothService.MSG_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.MSG_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.MSG_RECEIVED_DATA);
        return intentFilter;
    }


    public void cancelConnect(View view) {
        finish();
    }

    private void updateStatus(String status) {
        statusTextView.setText(status);
    }

    private void makeFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            );
        }
    }
}
