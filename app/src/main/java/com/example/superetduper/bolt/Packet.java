package com.example.superetduper.bolt;


class Packet {

    private static final byte START_OF_PACKET = 2;
    private static final byte END_OF_PACKET = 3;

    private static final byte BEGIN_PACKET = 66;
    private static final byte LED_ON = 79;
    private static final byte LED_OFF = 73;
    private static final byte SHIFT_OUT = 83;
    private static final byte END_GAME = 69;

    static final byte BUTTON_PRESSED = 80;

    private byte command;
    private int buttonNumber;

    Packet(byte[] packet){
        this(new String(packet));
    }

    private Packet(String packet){
        this.command = (byte) packet.charAt(1);
        if (packet.length() > 2){
            this.buttonNumber = Integer.valueOf(packet.substring(2));
        }
    }

    int getButtonNumber(){
        return buttonNumber;
    }

    byte getCommandByte(){
        return command;
    }

    static String getPacketBegin(){
        return encloseData(BEGIN_PACKET);
    }

    static String getPacketEnd(){
        return encloseData(END_GAME);
    }

    static String getPacketLedOn(int ledNumber){
        return encloseData(LED_ON + String.valueOf(ledNumber));
    }

    static String getPacketLedOff(int ledNumber){
        return encloseData(LED_OFF + String.valueOf(ledNumber));
    }

    static String getPacketShift(){
        return encloseData(SHIFT_OUT);
    }

    private static String encloseData(byte data){
        return encloseData(data + "");
    }

    private static String encloseData(String data){
        return START_OF_PACKET + data + END_OF_PACKET;
    }


}
