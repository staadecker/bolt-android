package com.example.superetduper.bolt;

final class BluetoothProtocol {
    private static final byte START_OF_PACKET = 2;
    private static final byte END_OF_PACKET = 3;

    private static final byte BEGIN_PACKET = 66;

    static byte[] getPacketBegin(){
        byte[] packet = {START_OF_PACKET, BEGIN_PACKET, END_OF_PACKET};
        return packet;
    }
}
