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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class GameActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

    private final static String LOG_TAG = GameActivity.class.getSimpleName();

    private static final int BLUETOOTH_ENABLE_RESULT_CODE = 100;

    private final static String BOARD_ADDRESS = "00:15:83:31:67:97";
    private final static UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final static UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final int MSG_PROGRESS = 6;
    private final static int MSG_GATT_CONNECTED = 3;
    private final static int MSG_GATT_DISCONNECTED = 4;

    private static final int MSG_RECEIVED_DATA = 5;

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mGattCharacteristic;

    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        makeFullScreen();

        statusTextView = findViewById(R.id.status_text_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        if (!mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_RESULT_CODE);
        }


        mBluetoothAdapter.startLeScan(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Disconnect from any active tag connection
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
        if (bluetoothDevice.getAddress().equals(BOARD_ADDRESS)) {
            mBluetoothAdapter.stopLeScan(this);
            mBluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), false, mGattCallback);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to board"));
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (gatt.discoverServices()) {
                    mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering services"));
                } else {
                    Log.w(LOG_TAG, "Failed to start discovering services");
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.sendMessage(Message.obtain(null, MSG_GATT_DISCONNECTED));
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
                sendMessage(BluetoothProtocol.getPacketBegin());
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Testing connection"));

            } else {
                Log.w(LOG_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.sendMessage(Message.obtain(null, MSG_RECEIVED_DATA, characteristic.getValue()));
            } else {
                Log.w(LOG_TAG, "Failed to read characteristic");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            mHandler.sendMessage(Message.obtain(null, MSG_RECEIVED_DATA, characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(LOG_TAG, "onCharacteristicWrite. Status : " + status);
        }
    };

    private void sendMessage(byte[] value) {
        mGattCharacteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS:
                    String status = (String) msg.obj;
                    updateStatus(status);
                    Log.i(LOG_TAG, status);
                    break;
                case MSG_GATT_DISCONNECTED:
                    updateStatus("Disconnected");
                    Log.i(LOG_TAG, "Disconnected");
                    break;
                case MSG_GATT_CONNECTED:
                    updateStatus("Connected");
                    Log.i(LOG_TAG, "Connected");
                    break;
                case MSG_RECEIVED_DATA:
                    Log.i(LOG_TAG, "Received data : " + new String((byte[]) msg.obj));
            }
        }
    };

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