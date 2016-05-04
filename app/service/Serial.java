package service;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Created by alynch on 4/26/2016 AD.
 */
public class Serial {

    public static void main(String[] args) {
        System.out.println(Arrays.toString(listSerialPorts()));

        String[] portNames = SerialPortList.getPortNames();
        System.out.println("portNames = " + Arrays.toString(portNames));

        /*SerialPort serialPort = new SerialPort("COM1");
        try {
            serialPort.openPort();//Open serial port
            serialPort.setParams(9600, 8, 1, 0);//Set params.
            byte[] buffer = serialPort.readBytes(10);//Read 10 bytes from serial port
            serialPort.closePort();//Close serial port
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }*/

    }

    private static String[] listSerialPorts() {

        Enumeration ports = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> portList = new ArrayList<>();
        String portArray[];
        while (ports.hasMoreElements()) {
            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portList.add(port.getName());
            }
        }
        portArray = portList.toArray(new String[0]);
        return portArray;
    }
}
