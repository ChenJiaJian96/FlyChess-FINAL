//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BugMain {
    public BugMain() {
    }

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(8083);
        } catch (IOException var6) {
            var6.printStackTrace();
        }

        Socket client = null;

        while(true) {
            while(true) {
                try {
                    client = serverSocket.accept();
                    test C = test.A;
                    C.setIndex(1000);
                    System.out.println(C);
                    OutputStream outputStream = client.getOutputStream();
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                    objectOutputStream.writeObject(C);
                    objectOutputStream.flush();
                } catch (IOException var7) {
                    var7.printStackTrace();
                }
            }
        }
    }
}
