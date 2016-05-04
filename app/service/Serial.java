package service;

import gnu.io.*;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Created by alynch on 4/26/2016 AD.
 */
public class Serial {

    public static void main(String[] args) throws IOException, UnsupportedCommOperationException, NoSuchPortException, PortInUseException {
//        System.out.println(Arrays.toString(listSerialPorts()));

//        String[] portNames = SerialPortList.getPortNames();
//        System.out.println("portNames = " + Arrays.toString(portNames));

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

        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM4");
        if ( portIdentifier.isCurrentlyOwned() )
        {
            System.out.println("Error: Port is currently in use");
        }
        else
        {
            System.out.println("Connect 1/2");
            CommPort commPort = portIdentifier.open("aaaa",6000);

            if ( commPort instanceof SerialPort )
            {
                System.out.println("Connect 2/2");
                SerialPort serialPort = (SerialPort) commPort;
                System.out.println("BaudRate: " + serialPort.getBaudRate());
                System.out.println("DataBIts: " + serialPort.getDataBits());
                System.out.println("StopBits: " + serialPort.getStopBits());
                System.out.println("Parity: " + serialPort.getParity());
                System.out.println("FlowControl: " + serialPort.getFlowControlMode());
//                serialPort.setSerialPortParams(4800,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_ODD);
//                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
                System.out.println("BaudRate: " + serialPort.getBaudRate());
                System.out.println("DataBIts: " + serialPort.getDataBits());
                System.out.println("StopBits: " + serialPort.getStopBits());
                System.out.println("Parity: " + serialPort.getParity());
                System.out.println("FlowControl: " + serialPort.getFlowControlMode());
                InputStream in = serialPort.getInputStream();
                OutputStream out = serialPort.getOutputStream();

                byte[] bytes = "TXN~AUTH~1234567890123456~100~MERCHANT REFERENCE 12345678~~~\r".getBytes();
                out.write(bytes);
                out.flush();
//                org.apache.commons.io.IOUtils.readFully(in,);
                String theString = IOUtils.toString(in, "UTF-8");
                System.out.println(theString);
            }
            else
            {
                System.out.println("Error: Only serial ports are handled by this example.");
            }
        }
    }
/*
    private static String[] listSerialPorts() {

        Enumeration ports = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> portList = new ArrayList<>();
        String portArray[];
        while (ports.hasMoreElements()) {
            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portList.add(port.getName());
                if(port.getName().contains("4")) {
                    if ( commPort instanceof SerialPort )
                    {
                        SerialPort serialPort = (SerialPort) commPort;
                        serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
                        InputStream in = serialPort.getInputStream();

                    }
                }
            }
        }
        portArray = portList.toArray(new String[0]);
        return portArray;
    }*/
}
