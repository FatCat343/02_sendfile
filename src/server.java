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
    public int speed; //current speed
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
            while (true) {
                Socket client = in.accept();
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

            InetAddress inetAddress;
            SocketAddress socketAddress = client.getRemoteSocketAddress(); //get client's IP
            inetAddress = ((InetSocketAddress)socketAddress).getAddress();
            if (inetAddress instanceof Inet4Address)
                System.out.println("IPv4: " + inetAddress);
            else if (inetAddress instanceof Inet6Address)
                    System.out.println("IPv6: " + inetAddress);
                else
                    System.err.println("Not an IP address.");

            speeds.put(inetAddress, dp);
            byte [] byteArray = new byte[8192];
            LocalTime start = LocalTime.now();     //?
            int len = input.read(byteArray);

            String name = new String(byteArray, StandardCharsets.UTF_8); //gets name of file
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("uploads/" + name));

            len += input.read(byteArray);
            int size = ByteBuffer.wrap(byteArray).getInt(); //gets size of file
            speeds.get(inetAddress).total = len;
            speeds.get(inetAddress).time = LocalTime.from(start);
            speeds.get(inetAddress).speed = len/LocalTime.from(start).getSecond();

            LocalTime looptime = LocalTime.now();
            while ((len = input.read(byteArray)) != -1){
                output.write(byteArray,0,len);
                //write method uplod to DownloadProgress???
                speeds.get(inetAddress).total += len;
                speeds.get(inetAddress).time = LocalTime.from(start);
                speeds.get(inetAddress).speed = len/LocalTime.from(looptime).getSecond();
                looptime = LocalTime.now();
            }
            speeds.get(inetAddress).valid = 0;
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
            }
            Iterator it = speeds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                DownloadProgress dp = (DownloadProgress)pair.getValue();
                System.out.println("Client : "  + pair.getKey() + "\t Speed : "  + dp.speed + "\t Average Speed : "  + dp.total/dp.time.getSecond());
                if (dp.valid == 0) { //timeout
                    it.remove();
                }
            }
        }
    }
}