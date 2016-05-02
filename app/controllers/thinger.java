/*
package controllers;

import akka.util.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

*/
/**
 * Created by andrewlynch on 2/05/2016.
 *//*

public class thinger {
    public static  void doIt(Socket clientSocket) throws IOException {
        InputStream is = clientSocket.getInputStream();
        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        PrintWriter pw2 = new PrintWriter(out);

        pw.println("GET / HTTP/1.0");
        pw.println();
        pw.flush();

        pw2.println("GET / HTTP/1.0");
        pw2.println();
        pw2.flush();

        byte[] array = ByteBuffer.wrap(out.toByteArray(), 0, 16).array();
//        new ByteString(array)

        byte[] buffer = new byte[1024];

        int read;
        while((read = is.read(buffer)) != -1) {
            String output = new String(buffer, 0, read);
            System.out.print(output);
            System.out.flush();
        };
        clientSocket.close();
    }
}
*/
