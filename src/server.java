import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;
import static java.time.temporal.ChronoUnit.SECONDS;

class DownloadProgress{
    public LocalTime time; //time client is on server
    public double speed; //current speed
    public int total; //bytes downloaded
    public int valid = 1;

}
public class server {
    static ConcurrentHashMap<InetAddress, DownloadProgress> speeds = new ConcurrentHashMap<>();
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
    ConcurrentHashMap<InetAddress, DownloadProgress> speeds;
    public void run(){
        DownloadProgress dp = new DownloadProgress();
        dp.valid = 1;
        try(DataInputStream input = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
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
            String msg;
            int namelen = 0;
            while (namelen < len) {
                int tmp = input.read(byteArray, 0, len);
                if (tmp >= 0) namelen+=tmp;
                else break;
            }
            String name = new String(byteArray, 0, namelen, StandardCharsets.UTF_8);
            System.out.println("name = " + name);
            File file = new File("C:\\study\\assignments\\seti\\02_sendfile\\uploads\\" + name.substring(0, len));
            try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
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
                int totallen = len;
                while ((len != -1) && (totallen != size)) {
                    output.write(byteArray, 0, len);
                    output.flush();
                    speeds.get(inetAddress).total += len;
                    speeds.get(inetAddress).time = LocalTime.from(start);
                    if (LocalTime.from(looptime).getSecond() != 0)
                        speeds.get(inetAddress).speed = (double) len / LocalTime.from(looptime).getNano();
                    looptime = LocalTime.now();
                    len = input.read(byteArray);
                    totallen += len;
                }
                System.out.println("received file");
                if (totallen == size) {
                    msg = "success";
                } else {
                    msg = "failure";
                }
            }
            out.writeInt(msg.getBytes(StandardCharsets.UTF_8).length); //send size of message
            out.flush();
            out.write(msg.getBytes(StandardCharsets.UTF_8)); //send message
            out.flush();
            speeds.get(inetAddress).valid = 0;
            System.out.println("finished with client");
        } catch (IOException e) {
            dp.valid = 0;
            e.printStackTrace();
        }
    }

    ClientHandler(Socket s, ConcurrentHashMap<InetAddress, DownloadProgress> ht) {
        client = s;
        speeds = ht;
    }

    public static void StartClientHandler(Socket s, ConcurrentHashMap<InetAddress, DownloadProgress> ht){
        Thread t = new Thread(new ClientHandler(s, ht));
        t.start();
    }

}

class TimeHandler implements Runnable{
    ConcurrentHashMap<InetAddress, DownloadProgress> speeds;
    public void run(){
        ShowSpeed();
    }
    TimeHandler(ConcurrentHashMap<InetAddress, DownloadProgress> ht) {
        speeds = ht;
    }
    public static void StartTimeHandler(ConcurrentHashMap<InetAddress, DownloadProgress> ht){
        Thread t = new Thread(new TimeHandler(ht));
        t.start();
    }
    private void ShowSpeed(){
        while (true) {
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Iterator<Map.Entry<InetAddress, DownloadProgress>> it = speeds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<InetAddress, DownloadProgress> pair = it.next();
                DownloadProgress dp = pair.getValue();
                System.out.println("Client : "  + pair.getKey() + "\t Speed : "  + dp.speed * 1000000000 + "\t Average Speed : "  + (double)dp.total * 1000000000/dp.time.getNano());
                if (dp.valid == 0) { //timeout
                    it.remove();
                }
            }
        }
    }
}