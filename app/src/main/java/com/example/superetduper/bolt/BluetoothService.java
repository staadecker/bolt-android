package com.example.superetduper.bolt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

public class BluetoothService extends Service implements BluetoothAdapter.LeScanCallback {

    private static final String LOG_TAG = BluetoothService.class.getSimpleName();

    private final static String BOARD_ADDRESS = "00:15:83:31:67:97";
    private final static UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final static UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mGattCharacteristic;

    public static final String MSG_PROGRESS = "com.example.superetduper.bolt.PROGRESS";
    public final static String MSG_GATT_CONNECTED = "com.example.superetduper.bolt.CONNECTED";
    public final static String MSG_GATT_DISCONNECTED = "com.example.superetduper.bolt.DISCONNECTED";
    public static final String MSG_RECEIVED_DATA = "com.example.superetduper.bolt.DATA_AVAILABLE";

    public static final String EXTRA_BLE_DATA = "com.example.superetduper.bolt.EXTRA_BLE_DATA";
    public static final String EXTRA_PROGRESS_STRING = "com.example.superetduper.bolt.EXTRA_PROGRESS_STRING";

    public void connect(){
        if (mBluetoothGatt == null) {
            mBluetoothAdapter.startLeScan(this);
            broadcastConnectionProgress("Searching for devices");
        } else {
            mBluetoothGatt.connect();
            broadcastConnectionProgress("Connecting on resume");
        }
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
        if (bluetoothDevice.getAddress().equals(BOARD_ADDRESS)) {
            mBluetoothAdapter.stopLeScan(this);
            mBluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), true, mGattCallback);
            broadcastConnectionProgress("Connecting to board");
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (gatt.discoverServices()) {
                    broadcastConnectionProgress("Discovering services");
                } else {
                    Log.w(LOG_TAG, "Failed to start discovering services");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(MSG_GATT_DISCONNECTED);
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
                broadcastUpdate(MSG_GATT_CONNECTED);

            } else {
                Log.w(LOG_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastBLEData(characteristic.getValue());
            } else {
                Log.w(LOG_TAG, "Failed to read characteristic");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastBLEData(characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "Sent packet : " + Packet.formatForPrint(characteristic.getStringValue(0)));
            } else {
                Log.w(LOG_TAG, "Failed sending packet. Status : " + status);
            }
        }
    };

    private void broadcastBLEData(final byte[] transmission) {
        Intent intent = new Intent(MSG_RECEIVED_DATA);
        intent.putExtra(EXTRA_BLE_DATA, transmission);
        sendBroadcast(intent);
    }

    private void broadcastConnectionProgress(final String progressMessage){
        Intent intent = new Intent(MSG_PROGRESS);
        intent.putExtra(EXTRA_PROGRESS_STRING, progressMessage);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        sendBroadcast(new Intent(action));
    }

    public void writeBLEData(String packet) {
        Log.i(LOG_TAG, "Trying to send data");
        if (mBluetoothGatt != null) {
            mGattCharacteristic.setValue(packet.getBytes());
            mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
        } else {
            Log.w(LOG_TAG, "Failed to write characteristic, connection is null");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanUp();

        return super.onUnbind(intent);
    }

    private void cleanUp(){
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(this);
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

    class LocalBinder extends Binder{
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
}
