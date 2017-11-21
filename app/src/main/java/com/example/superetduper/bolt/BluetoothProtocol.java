package com.example.superetduper.bolt;

import java.io.ByteArrayOutputStream;

@Deprecated
final class BluetoothProtocol {
    private static final byte START_OF_PACKET = 2;
    private static final byte END_OF_PACKET = 3;

    private static final byte ACKNOWLEDGE = 6;

    private static final byte BEGIN_PACKET = 66;
    private static final byte LED_ON = 79;
    private static final byte LED_OFF = 73;
    private static final byte SHIFT_OUT = 83;
    private static final byte END_GAME = 69;

    public static final byte BUTTON_PRESSED = 80;

    static byte[] getPacketBegin(){
        return getEnclosedPacket(BEGIN_PACKET);
    }

    static byte[] getPacketShiftOut(){
        return getEnclosedPacket(SHIFT_OUT);
    }

    static byte[] getPacketEndGame(){
        return getEnclosedPacket(END_GAME);
    }

    static byte[] getAcknoweldgePacket(){
        byte[] packet = {ACKNOWLEDGE};
        return packet;
    }


    static byte[] getTurnLedOnPacket(int buttonNumber){
        String buttonNumberString = String.format(String.valueOf(buttonNumber), 00.);
        byte[] packet = new byte[5];
        packet[1] = LED_ON;
        packet[2] = (byte) buttonNumberString.charAt(0);
        packet[3] = (byte) buttonNumberString.charAt(1);
        return getEnclosedPacket(packet);
    }

    static byte[] getTurnLedOffPacket(int buttonNumber){
        String buttonNumberString = String.format(String.valueOf(buttonNumber), 00.);
        byte[] packet = new byte[5];
        packet[1] = LED_OFF;
        packet[2] = (byte) buttonNumberString.charAt(0);
        packet[3] = (byte) buttonNumberString.charAt(1);
        return getEnclosedPacket(packet);
    }

    private static byte[] getEnclosedPacket(byte data){
        byte[] packet = {START_OF_PACKET, data, END_OF_PACKET};
        return packet;
    }

    private static byte[] getEnclosedPacket(byte[] data){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(START_OF_PACKET);
        output.write(data, 0, data.length);
        output.write(END_OF_PACKET);
        return output.toByteArray();
    }

    static String stripPacket(byte[] packet){
        return new String(packet).substring(1, packet.length  - 1);
    }


}
