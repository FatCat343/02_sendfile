import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class client {
    static String addr = "192.168.0.35";
    static Integer port = 1234;
    static String path = "C:\\study\\assignments\\seti\\02_sendfile\\test\\MTP1.doc";
    public static void main(String[] args) {
        try (            Socket socket = new Socket(addr, port);
                         DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                         BufferedInputStream input = new BufferedInputStream(new FileInputStream(path));) {
             byte [] byteArray = new byte[8192];
            File file = new File(path);
            output.writeInt(file.getName().length()); //send size of filename
            output.flush();
            output.writeChars(file.getName()); //send size of filename
            output.flush();
            System.out.println("sent name");

            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(file.length()); //|long| >= 10^12 bytes
            System.out.println("file length = "+ file.length());
            byte[] arr = new byte[2000];
            output.write(buffer.array());//send size of file
            output.flush();
            System.out.println("sent size");
            int in;
            while ((in = input.read(byteArray)) != -1) { //send file by portions
                output.write(byteArray,0,in);
                output.flush();
            }
            System.out.println("Sent file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
