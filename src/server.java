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
        DownloadProgress dp = new DownloadProgress();
        dp.valid = 1;
        try(DataInputStream input = new DataInputStream(client.getInputStream());) {
            InetAddress inetAddress;
            SocketAddress socketAddress = client.getRemoteSocketAddress(); //get client's IP
            inetAddress = ((InetSocketAddress)socketAddress).getAddress();
            if (inetAddress instanceof Inet4Address)
                System.out.println("IPv4: " + inetAddress);
            else if (inetAddress instanceof Inet6Address)
                    System.out.println("IPv6: " + inetAddress);
                else
                    System.err.println("Not an IP address.");


            LocalTime start = LocalTime.now();     //?
            int len = input.readInt();
            byte [] byteArray = new byte[len];
//            for (int i = 0; i < len; i++){ //
//                char a = input.readChar();
//                System.out.println(a);
//                name = name.concat(Character.toString(a));
//            }
            int namelen = 0;
            while (namelen < len) {
                namelen+= input.read(byteArray, 0, len);
            }
            String name = new String(byteArray, StandardCharsets.UTF_8);
            System.out.println("name = " + name);
            File file = new File("C:\\study\\assignments\\seti\\02_sendfile\\uploads\\" + name.substring(0, len));
            try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));) {
                len += input.read(byteArray);
                long size = ByteBuffer.wrap(byteArray).getLong(); //gets size of file
                System.out.println("size = " + size);
                dp.total = len;
                dp.time = LocalTime.from(start);
                if (LocalTime.from(start).getSecond() != 0) dp.speed = (double) len / LocalTime.from(start).getNano();
                else dp.speed = 0;
                speeds.put(inetAddress, dp);

                LocalTime looptime = LocalTime.now();
                len = input.read(byteArray);
                while (len != -1) {
                    output.write(byteArray, 0, len);
                    output.flush();
                    speeds.get(inetAddress).total += len;
                    speeds.get(inetAddress).time = LocalTime.from(start);
                    if (LocalTime.from(looptime).getSecond() != 0)
                        speeds.get(inetAddress).speed = (double) len / LocalTime.from(looptime).getNano();
                    looptime = LocalTime.now();
                    len = input.read(byteArray);
                }
                speeds.get(inetAddress).valid = 0;
                System.out.println("finished with client");
            }
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
                System.out.println("Client : "  + pair.getKey() + "\t Speed : "  + dp.speed * 1000000000 + "\t Average Speed : "  + (double)dp.total * 1000000000/dp.time.getNano());
                if (dp.valid == 0) { //timeout
                    it.remove();
                }
            }
        }
    }
}