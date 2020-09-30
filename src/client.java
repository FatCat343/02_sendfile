import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class client {
    static String addr;
    static Integer port;
    static String path;
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(addr, port);
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(path));

            byte [] byteArray = new byte[8192];
            String[] bits = path.split("/");
            output.write(bits[bits.length - 1].getBytes()); //send filename to server

            File file = new File(path);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(file.length());
            output.write(buffer.array());//send size of file

            int in;
            while ((in = input.read(byteArray)) != -1){ //send file by portions
                output.write(byteArray,0,in);
            }

            input.close();
            output.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
