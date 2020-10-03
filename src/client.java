import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class client {
    static String addr = "192.168.0.35";
    static Integer port = 1234;
    static String path = "C:\\study\\assignments\\seti\\02_sendfile\\test\\test.txt";
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(addr, port);
            //File file = new File(path);
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream sock = new BufferedInputStream(socket.getInputStream());
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(path));

            byte [] byteArray = new byte[8192];
            //String[] bits = path.split("/");
            File file = new File(path);
            output.write(file.getName().getBytes()); //send filename to server
            output.flush();
            System.out.println("sent name");

            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(file.length());
            System.out.println("file length = "+ file.length());
            byte[] arr = new byte[2000];
            sock.read(arr);
            //System.out.println(arr.toString());
            output.write(buffer.array());//send size of file
            //output.write((int)file.length());
            output.flush();
            System.out.println("sent size");

            int in;
            sock.read(arr);
            //System.out.println(arr.toString());
            while ((in = input.read(byteArray)) != -1) { //send file by portions
                output.write(byteArray,0,in);
                output.flush();
            }
            System.out.println("Sent file");
            //int fin = sock.read(arr);
            //System.out.println(arr.toString());
            input.close();
            output.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
