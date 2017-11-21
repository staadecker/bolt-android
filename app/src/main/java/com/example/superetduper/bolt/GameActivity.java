package com.example.superetduper.bolt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.UUID;

public class GameActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

    private final static String LOG_TAG = GameActivity.class.getSimpleName();

    private static final int BLUETOOTH_ENABLE_RESULT_CODE = 100;

    private final static String BOARD_ADDRESS = "00:15:83:31:67:97";
    private final static UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final static UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final int MSG_PROGRESS = 1;
    private final static int MSG_GATT_CONNECTED = 2;
    private final static int MSG_GATT_DISCONNECTED = 3;

    private static final int MSG_RECEIVED_DATA = 4;

    private static final int STATE_CONNECTING = 0;
    private static final int STATE_TESTING_CONNECTION = 1;
    private static final int STATE_INGAME = 2;
    private static final int STATE_GAME_ENDED = 3;

    private static final byte ACKNOWLEDGE = 6;

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mGattCharacteristic;

    private TextView statusTextView;
    private LinearLayout statusBox;
    private TextView countDownTimer;

    private int mState = STATE_CONNECTING;

    private int mLedNumberToPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate");
        setContentView(R.layout.activity_game);

        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "No Bluetooth Support", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_RESULT_CODE);
            return;
        }

        if (mBluetoothGatt != null){
            mBluetoothGatt.connect();
            sendMessageToHandler(MSG_PROGRESS, "Connecting on resume");
        } else {
            mBluetoothAdapter.startLeScan(this);
            sendMessageToHandler(MSG_PROGRESS, "Searching for devices");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "onPause");
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");

        //Disconnect from any active tag connection
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
        if (bluetoothDevice.getAddress().equals(BOARD_ADDRESS)) {
            mBluetoothAdapter.stopLeScan(this);
            mBluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), true, mGattCallback);
            sendMessageToHandler(MSG_PROGRESS,"Connecting to board");
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (gatt.discoverServices()) {
                    sendMessageToHandler(MSG_PROGRESS, "Discovering services");
                } else {
                    Log.w(LOG_TAG, "Failed to start discovering services");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                sendMessageToHandler(MSG_GATT_DISCONNECTED);
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService gattService = gatt.getService(SERVICE_UUID);

                if (gattService == null) {
                    Log.w(LOG_TAG, "Service does not exist");
                    return;
                }

                mGattCharacteristic = gattService.getCharacteristic(CHARACTERISTIC_UUID);

                if (mGattCharacteristic == null) {
                    Log.w(LOG_TAG, "Characteristic does not exist");
                    return;
                }

                gatt.setCharacteristicNotification(mGattCharacteristic, true);
                sendMessageToHandler(MSG_GATT_CONNECTED);

            } else {
                Log.w(LOG_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendMessageToHandler(MSG_RECEIVED_DATA, characteristic.getValue());
            } else {
                Log.w(LOG_TAG, "Failed to read characteristic");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            sendMessageToHandler(MSG_RECEIVED_DATA, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.i(LOG_TAG, "Sent packet : " + characteristic.getStringValue(0));
            } else {
                Log.w(LOG_TAG, "Failed sending packet. Status : " + status);
            }
        }
    };

    private void sendPacket(String value) {
        if (mBluetoothGatt != null) {
            mGattCharacteristic.setValue(value.getBytes());
            mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
        } else {
            Log.w(LOG_TAG, "Failed to write characteristic, connection is null");
        }
    }

    private void sendMessageToHandler(int what, Object obj){
        mHandler.sendMessage(Message.obtain(null, what, obj));
    }

    private void sendMessageToHandler(int what){
        mHandler.sendMessage(Message.obtain(null, what));
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS:
                    String status = (String) msg.obj;
                    updateStatus(status);
                    Log.i(LOG_TAG, "Handler : "  + status);
                    break;
                case MSG_GATT_DISCONNECTED:
                    Log.i(LOG_TAG, "Handler : Disconnected");
                    break;
                case MSG_GATT_CONNECTED:
                    updateStatus("Testing connection");
                    Log.i(LOG_TAG, "Handler : Testing connection");
                    mState = STATE_TESTING_CONNECTION;
                    sendPacket(Packet.getPacketBegin());
                    break;
                case MSG_RECEIVED_DATA:
                    if (msg.obj == null) {
                        Log.w(LOG_TAG, "Handler called but no data passed");
                        return;
                    }

                    parseData((byte[]) msg.obj);
                    Log.i(LOG_TAG, "Received packet : " + new String((byte[]) msg.obj));
            }
        }
    };

    private void parseData(@NonNull byte[] data){
        if (isAcknowledge(data)){
            gotAcknowledged();
        } else {
            Packet packet = new Packet(data);

            if (Packet.BUTTON_PRESSED == packet.getCommandByte()) {
                buttonPressed(packet.getButtonNumber());
            } else {
                Log.w(LOG_TAG, "Could not parse data");
            }
        }
    }

    private void buttonPressed(int buttonNumeber){
        Log.i(LOG_TAG, "Button number " + buttonNumeber + " pressed");
        if (buttonNumeber == mLedNumberToPress && mState == STATE_INGAME){
            String dataToSend = String.valueOf(ACKNOWLEDGE);
            dataToSend += Packet.getPacketLedOff(mLedNumberToPress);
            mLedNumberToPress = getRandomLedNumber();
            dataToSend += Packet.getPacketLedOn(mLedNumberToPress);
            dataToSend += Packet.getPacketShift();

            sendPacket(dataToSend);
        }
    }

    private static boolean isAcknowledge(byte[] packet){
        return packet.length == 1 && packet[0] == ACKNOWLEDGE;
    }

    private void gotAcknowledged(){
        Log.i(LOG_TAG, "Received acknowledge");
        if (mState == STATE_TESTING_CONNECTION){
            statusBox.setVisibility(View.GONE);
            startGame();
        }
    }

    private CountDownTimer mGameTimer = new CountDownTimer(30000, 10) {

        NumberFormat formatter = new DecimalFormat("#0.000");

        @Override
        public void onTick(long l) {
            countDownTimer.setText(formatter.format(l/1000.0));
        }

        @Override
        public void onFinish() {
            countDownTimer.setText("0.000");
            mState = STATE_GAME_ENDED;
            String dataToSend = Packet.getPacketLedOff(mLedNumberToPress);
            dataToSend += Packet.getPacketEnd();

            sendPacket(dataToSend);
        }
    };

    private void startGame(){
        mState = STATE_INGAME;
        mGameTimer.start();
        mLedNumberToPress = getRandomLedNumber();
        sendPacket(Packet.getPacketLedOn(mLedNumberToPress) + Packet.getPacketShift());
    }

    private static int getRandomLedNumber(){
        return (int) (Math.random() * 64);
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