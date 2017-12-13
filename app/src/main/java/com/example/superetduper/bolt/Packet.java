package com.example.superetduper.bolt;

final class Packet {

    private static final char START_OF_PACKET = 2;
    private static final char END_OF_PACKET = 3;

    private static final char BEGIN_PACKET = 66;
    private static final char LED_ON = 79;
    private static final char LED_OFF = 73;
    private static final char SHIFT_OUT = 83;
    private static final char END_GAME = 69;

    private static final char[] commandBytes = new char[]{BEGIN_PACKET, LED_OFF, LED_ON, SHIFT_OUT, END_GAME};

    private static final byte BUTTON_PRESSED = 80;
    private static final char ACKNOWLEDGE = 6;

    static boolean isValidPacket(String packet){
        if (packet.charAt(0) != START_OF_PACKET || packet.charAt(packet.length() - 1) != END_OF_PACKET){
            return false;
        } else if (!isValidCommandChar(getCommandByte(packet))){
            return false;
        }
        return 3 <= packet.length() && packet.length() <=5;
    }

    static boolean isButtonPressed(String packet){
        return getCommandByte(packet) == BUTTON_PRESSED;
    }

    static boolean isAcknowledge(String packet) {
        return packet.length() == 1 && packet.charAt(0) == ACKNOWLEDGE;
    }

    static char getAcknowledge() {
        return ACKNOWLEDGE;
    }

    static int getButtonNumber(String packet){
        return Integer.valueOf(packet.substring(2, packet.length() -1));
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

    static String formatForPrint(String packet){
        packet = packet.replace(String.valueOf(START_OF_PACKET), String.valueOf((int) START_OF_PACKET));
        packet = packet.replace(String.valueOf(END_OF_PACKET), String.valueOf((int) END_OF_PACKET));
        return packet;
    }



    private static boolean isValidCommandChar(char commandByte){
        for (char command : commandBytes){
            if (command == commandByte){
                return true;
            }
        }
        return false;
    }

    private static char getCommandByte(String packet){
        return packet.charAt(1);
    }

    private static String encloseData(char data){
        return encloseData(data + "");
    }

    private static String encloseData(String data){
        return START_OF_PACKET + data + END_OF_PACKET;
    }
}
