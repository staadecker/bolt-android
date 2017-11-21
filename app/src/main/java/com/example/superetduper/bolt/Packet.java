package com.example.superetduper.bolt;


class Packet {

    private static final byte START_OF_PACKET = 2;
    private static final byte END_OF_PACKET = 3;

    static final byte ACKNOWLEDGE = 6;

    private static final byte BEGIN_PACKET = 66;
    private static final byte LED_ON = 79;
    private static final byte LED_OFF = 73;
    private static final byte SHIFT_OUT = 83;
    private static final byte END_GAME = 69;

    static final byte BUTTON_PRESSED = 80;

    private String packet;
    private byte command;
    private int buttonNumber;

    Packet(byte[] packet){
        this(new String(packet));
    }

    private Packet(String packet){
        this.packet = packet.substring(1, packet.length() - 1);
        this.command = (byte) this.packet.charAt(0);
        if (this.packet.length() > 1){
            this.buttonNumber = Integer.valueOf(this.packet.substring(1));
        }
    }

    int getButtonNumber(){
        return buttonNumber;
    }

    byte getCommandByte(){
        return command;
    }

    private byte[] getPacketBytes(){
        return enclosePacket(packet).getBytes();
    }

    private static String enclosePacket(String packet){
        return START_OF_PACKET + packet + END_OF_PACKET;
    }

    static byte[] getPacketBegin(){
        return new Packet(BEGIN_PACKET + "").getPacketBytes();
    }

    static byte[] getPacketEnd(){
        return new Packet(END_GAME + "").getPacketBytes();
    }

    static byte[] getPacketLedOn(int ledNumber){
        return new Packet(LED_ON + String.valueOf(ledNumber)).getPacketBytes();
    }

    static byte[] getPacketLedOff(int ledNumber){
        return new Packet(LED_OFF + String.valueOf(ledNumber)).getPacketBytes();
    }

    static byte[] getPacketShift(){
        return new Packet(SHIFT_OUT + "").getPacketBytes();
    }
}
