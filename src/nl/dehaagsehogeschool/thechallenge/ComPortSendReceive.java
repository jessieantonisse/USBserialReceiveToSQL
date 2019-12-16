/**
 * 20190613 Gert den Neijsel
 * Dit programma leest data vanaf de USB seriële poort uit (vanaf bijv. Arduino of Microbit)
 * en insert dit in een MySQL database.
 *
 * Instructies om dit aan de praat te krijgen:
 * Voer na installatie de volgende commando's uit in de SQL server:
 * Maak een nieuwe database aan met de naam:
 *      vb1
 * Maak in die databse een tabel aan:
 *      create table tbl1(tijdstip TEXT, temperatuur FLOAT);
 *
 * Bron van de ComPortSendReceive klasse: https://www.codeproject.com/tips/801262/sending-and-receiving-strings-from-com-port-via-js
 *
 * Gebruik Arduino voorbeeld "07.Temperaturesensor-Bluetooth.ino" om data te genereren.
 */
package nl.dehaagsehogeschool.thechallenge;

import java.io.*; // IOException
import java.text.SimpleDateFormat;
import java.util.*; // Scanner
import jssc.*;

public class ComPortSendReceive {

    private static SerialPort serialPort;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String portName;
        String[] portNames = SerialPortList.getPortNames();

        if (portNames.length == 0) {
            System.out.println("Er zijn geen seriële poorten. Sluit je Micro:bit aan!");
            System.out.println("Druk Enter om te stoppen");
            try {
                System.in.read();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }

        if (portNames.length == 1) {
            portName = portNames[0];
            System.out.println(portName + " wordt nu gebruikt");
        } else {

            System.out.println("Meerdere seriële poorten gedetecteerd: ");
            for (int i = 0; i < portNames.length; i++) {
                System.out.println(portNames[i]);
            }
            System.out.println("Type poortnaam die je wilt gebruiken en druk Enter...");
            Scanner in = new Scanner(System.in);
            portName = in.next();
        }

        // Schrijven naar seriële poort
        serialPort = new SerialPort(portName);
        try {
            // seriële poort openen
            serialPort.openPort();

            serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);

            serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
            // schrijf string naar poort
            serialPort.writeString("Test-naar-serieel");

            System.out.println("String naar seriële poort geschreven, wacht op antwoord..");
        } catch (SerialPortException ex) {
            System.out.println("Fout bij schrijven naar seriële poort: " + ex);
        }
    }

    // Ontvangen van data vanaf seriële poort
    // Bron: https://arduino.stackexchange.com/questions/3755/how-to-use-readline-from-jssc
    private static class PortReader implements SerialPortEventListener {
        String vorigTijdstip = null;
        StringBuilder bericht = new StringBuilder();
        InsertIntoSQL database = new InsertIntoSQL();
        @Override
        public void serialEvent(SerialPortEvent event) {

            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    byte buffer[] = serialPort.readBytes();
                    for (byte b : buffer) {
                        // indien regeleinde gedetecteerd ('\r' of '\n')
                        if ((b == '\r' || b == '\n') && bericht.length() > 0) {

                            //StringBuilder naar String converteren
                            String berichtData = bericht.toString();

                            // tijdstip = nu
                            String tijdstip = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                            // regeleindes verwijderen
                            berichtData = berichtData.replace("\n", "").replace("\r", "");

                            // regeleindes verwijderen
                            tijdstip = tijdstip.replace("\n", "").replace("\r", "");

                            // String naar float omzetten
                            Float temperatuur = Float.parseFloat(berichtData);

                            // afronden op 1 cijfer achter de komma
                            temperatuur = (float)(Math.round(temperatuur * 10.0) / 10.0);

                            if (tijdstip.equals(vorigTijdstip)){
                                System.out.println("Regel uit buffer genegeerd:");
                            }
                            else {
                                database.insert(tijdstip, temperatuur);
                            }

                            System.out.print(tijdstip);
                            System.out.print("  ");
                            System.out.println(temperatuur);
                            vorigTijdstip = tijdstip;

                            bericht.setLength(0);
                        } else {
                            bericht.append((char) b);
                        }
                    }
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                    System.out.println("serialEvent");
                }
            }

        }
    }
}
