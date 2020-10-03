import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Thread.sleep;
import static java.time.temporal.ChronoUnit.SECONDS;

class DownloadProgress{
    public LocalTime time; //time client is on server
    public double speed; //current speed
    public int total; //bytes downloaded
    public int valid = 1;

}
public class server {
    static Hashtable<InetAddress, DownloadProgress> speeds = new Hashtable<>();
    static int port = 1234;
    public static void main(String[] args) {
        try {
            ServerSocket in = new ServerSocket(port);
            TimeHandler.StartTimeHandler(speeds);
            System.out.println(in.getInetAddress().getHostAddress());
            System.out.println("ready to wait");
            while (true) {
                Socket client = in.accept();
                System.out.println("accepted new client");
                ClientHandler.StartClientHandler(client, speeds);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable{
    Socket client;
    Hashtable<InetAddress, DownloadProgress> speeds;
    public void run(){
        //get name + size + file itself
        DownloadProgress dp = new DownloadProgress();
        dp.valid = 1;

        try {
            BufferedInputStream input = new BufferedInputStream(client.getInputStream());
            BufferedOutputStream sock = new BufferedOutputStream(client.getOutputStream());
            InetAddress inetAddress;
            SocketAddress socketAddress = client.getRemoteSocketAddress(); //get client's IP
            inetAddress = ((InetSocketAddress)socketAddress).getAddress();
            if (inetAddress instanceof Inet4Address)
                System.out.println("IPv4: " + inetAddress);
            else if (inetAddress instanceof Inet6Address)
                    System.out.println("IPv6: " + inetAddress);
                else
                    System.err.println("Not an IP address.");

            //System.out.println("1");
            byte [] byteArray = new byte[8192];
            LocalTime start = LocalTime.now();     //?
            int len = input.read(byteArray);
            System.out.println("got byte array");
            String name = new String(byteArray, StandardCharsets.UTF_8); //gets name of file
            System.out.println("name length = " + name.length() + "len = " + len);
            System.out.println( name.split("\\.(?=[^\\.]+$)")[0] + "." + name.split("\\.(?=[^\\.]+$)")[1] + "\ndfhdsfh");

            File file = new File("C:\\study\\assignments\\seti\\02_sendfile\\uploads\\" + name.substring(0, len));
            sock.write("send size".getBytes());
            sock.flush();
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));

            len += input.read(byteArray);
            long size = ByteBuffer.wrap(byteArray).getLong(); //gets size of file
            System.out.println("size = " + size);
            sock.write("send file".getBytes());
            sock.flush();
            dp.total = len;
            dp.time = LocalTime.from(start);
            if (LocalTime.from(start).getSecond() != 0) dp.speed = len/LocalTime.from(start).getSecond();
                else dp.speed = 0;
            speeds.put(inetAddress, dp);

            LocalTime looptime = LocalTime.now();
            len = input.read(byteArray);
            while (len != -1){
                output.write(byteArray,0,len);
                output.flush();
                //write method uplod to DownloadProgress???
                speeds.get(inetAddress).total += len;
                speeds.get(inetAddress).time = LocalTime.from(start);
                if (LocalTime.from(looptime).getSecond() != 0) speeds.get(inetAddress).speed = len/LocalTime.from(looptime).getSecond();
                looptime = LocalTime.now();
                System.out.println("gor some piece of file");
                len = input.read(byteArray);
            }
            sock.write("operation completed".getBytes());
            sock.flush();
            speeds.get(inetAddress).valid = 0;
            System.out.println("finished with client");
            input.close();
            output.close();
        } catch (IOException e) {
            dp.valid = 0;
            e.printStackTrace();
        }
    }

    ClientHandler(Socket s, Hashtable ht) {
        client = s;
        speeds = ht;
    }

    public static void StartClientHandler(Socket s, Hashtable ht){
        Thread t = new Thread(new ClientHandler(s, ht));
        t.start();
    }

}

class TimeHandler implements Runnable{
    Hashtable<InetAddress, DownloadProgress> speeds;
    public void run(){
        while (true) {
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ShowSpeed();
        }
    }
    TimeHandler(Hashtable ht) {
        speeds = ht;
    }
    public static void StartTimeHandler(Hashtable ht){
        Thread t = new Thread(new TimeHandler(ht));
        t.start();
    }
    private void ShowSpeed(){
        //Enumeration<InetAddress> enumeration = speeds.keys();
        while (true) {
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("sleep was interrupted");
            }
            Iterator it = speeds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                DownloadProgress dp = (DownloadProgress)pair.getValue();
                System.out.println("Client : "  + pair.getKey() + "\t Speed : "  + dp.speed + "\t Average Speed : "  + (double)dp.total/dp.time.getSecond());
                if (dp.valid == 0) { //timeout
                    it.remove();
                }
            }
        }
    }
}