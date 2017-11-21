package com.example.superetduper.bolt;


class Packet {

    private static final char START_OF_PACKET = 2;
    private static final char END_OF_PACKET = 3;

    private static final char BEGIN_PACKET = 66;
    private static final char LED_ON = 79;
    private static final char LED_OFF = 73;
    private static final char SHIFT_OUT = 83;
    private static final char END_GAME = 69;

    static final byte BUTTON_PRESSED = 80;

    static int getButtonNumber(String packet){
        return Integer.valueOf(packet.substring(2, packet.length() -1));
    }

    static char getCommandByte(String packet){
        return packet.charAt(1);
    }

    static boolean isValid(String packet){
        return 3 <= packet.length() && packet.length() <=5 && packet.charAt(0) == START_OF_PACKET && packet.charAt(packet.length() - 1) == END_OF_PACKET;
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

    private static String encloseData(char data){
        return encloseData(data + "");
    }

    private static String encloseData(String data){
        return START_OF_PACKET + data + END_OF_PACKET;
    }
}
