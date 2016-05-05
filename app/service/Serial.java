package service;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by alynch on 4/26/2016 AD.
 */
public class Serial {

    public static void main(String[] args) throws IOException, UnsupportedCommOperationException, NoSuchPortException, PortInUseException, InterruptedException {
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
        if (portIdentifier.isCurrentlyOwned()) {
            System.out.println("Error: Port is currently in use");
        } else {
            System.out.println("Connect 1/2");
            CommPort commPort = portIdentifier.open("aaaa", 6000);

            if (commPort instanceof SerialPort) {
                System.out.println("Connect 2/2");
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
                System.out.println("BaudRate: " + serialPort.getBaudRate());
                System.out.println("DataBits: " + serialPort.getDataBits());
                System.out.println("StopBits: " + serialPort.getStopBits());
                System.out.println("Parity: " + serialPort.getParity());
                System.out.println("FlowControl: " + serialPort.getFlowControlMode());
                serialPort.setEndOfInputChar((byte) 13);
                System.out.println("END of thing: " + serialPort.getEndOfInputChar());
                OutputStream out = serialPort.getOutputStream();

                byte[] bytes = "CFG~SETD~123~Device1234~AUD~0007~ABCCORP_PARKING_002~\r".getBytes();
                out.write(bytes);
                out.flush();

                Thread.sleep(200);
                InputStream in = serialPort.getInputStream();
                System.out.println("Trying to read in stream");
                String s = readStream(in);
                System.out.println("Command response ->" + s);
                Thread.sleep(200);
                System.out.println("Writing auth");
                byte[] bytes2 = "TXN~AUTH~1234567890123456~100~MERCHANT REFERENCE 12345678~~~\r".getBytes();
                out.write(bytes2);
                out.flush();
                Thread.sleep(200);
                System.out.println("Trying to read second input stream");
                s = readStream(in);
                System.out.println("Command ->" + s);
            } else {
                System.out.println("Error: Only serial ports are handled by this example.");
            }
        }
    }

    private static String readStream(InputStream in) throws IOException, InterruptedException {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            int b = in.read();
            if (b == 13) {
                return s.toString();
            }
            String tmp;
            if (b >= 48 && b <= 127) { //ASCII readable
                tmp = "" + (char) b;
            } else {
                tmp = "(" + b + "/" + (Integer.valueOf(String.valueOf(b), 16)) + (")");
            }
            if(b == -1) {
                System.out.println("Read -1, sleeping");
                Thread.sleep(3000);
            } else {
                System.out.println(":Read " + tmp + ", blocking");
                s.append(tmp);
            }
        }
        return null;
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

