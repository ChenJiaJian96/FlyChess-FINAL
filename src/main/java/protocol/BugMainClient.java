//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public class BugMainClient {
    public BugMainClient() {
    }

    public static void main(String[] args) {
        Socket server = null;

        try {
            server = new Socket("127.0.0.1", 8083);
        } catch (IOException var7) {
            var7.printStackTrace();
        }

        InputStream inputStream = null;

        try {
            inputStream = server.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            test recvMsg = (test)objectInputStream.readObject();
            if (recvMsg != null) {
                System.out.println(recvMsg);
            } else {
                System.out.println("recv null");
            }
        } catch (IOException var5) {
            var5.printStackTrace();
        } catch (ClassNotFoundException var6) {
            var6.printStackTrace();
        }

    }
}
