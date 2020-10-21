import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class client {
    static String addr = "10.9.44.247";
    static Integer port = 1234;
    static String path = "C:\\study\\assignments\\seti\\02_sendfile\\test\\MTP1.doc";
    public static void main(String[] args) {
        try (            Socket socket = new Socket(addr, port);
                         DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                         BufferedInputStream input = new BufferedInputStream(new FileInputStream(path));
                         DataInputStream in = new DataInputStream(socket.getInputStream())) {
            byte [] byteArray = new byte[8192];
            File file = new File(path);
            output.writeInt(file.getName().getBytes(StandardCharsets.UTF_8).length); //send size of filename
            output.flush();
            output.write(file.getName().getBytes(StandardCharsets.UTF_8)); //send filename
            output.flush();
            System.out.println("sent name");
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(file.length()); //|long| >= 10^12 bytes
            output.write(buffer.array());//send size of file
            output.flush();
            System.out.println("sent size");
            int inlen;
            while ((inlen = input.read(byteArray)) != -1) { //send file by portions
                output.write(byteArray,0,inlen);
                output.flush();
            }
            System.out.println("Sent file");
            inlen = in.readInt();
            int namelen = 0;
            while (namelen < inlen) {
                int tmp = in.read(byteArray);
                if (tmp >= 0) namelen+= tmp;
                else break;
            }
            String retval = new String(byteArray, 0, namelen, StandardCharsets.UTF_8);
            System.out.println(retval);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
