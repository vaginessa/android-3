package dam.com.netbeast;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.StrictMode;
import android.util.Log;

/**
 * Created by Cayetano Rodríguez Medina on 17/5/16.
 */

public class UDPMessenger {
    protected static String DEBUG_TAG = "UDPMessenger";
    protected static final Integer BUFFER_SIZE = 4096;

    protected String TAG;
    protected static final int MULTICAST_PORT = 16180;
    protected static final String MULTICAST_IP = "239.0.16.18";

    private boolean receiveMessages = false;

    protected Context context;
    private DatagramSocket socket;
    private DatagramSocket rsocket;

    private Thread receiverThread;


    public UDPMessenger(Context context) throws IllegalArgumentException {
        if(context == null)
            throw new IllegalArgumentException();

        this.context = context.getApplicationContext();

        // This lines enable network access in main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }


    public boolean sendMessage(String message) throws IllegalArgumentException {
        if(message == null || message.length() == 0)
            throw new IllegalArgumentException();

        // Check for WiFi connectivity
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = null;
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) // connected to wifi
                mWifi  = activeNetwork;
        }

        if(mWifi == null || !mWifi.isConnected())
        {
            Log.d(DEBUG_TAG, "Sorry! You need to be in a WiFi network in order to send UDP multicast packets. Aborting.");
            return false;
        }

        // Create the send socket
        if(socket == null) {
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                Log.d(DEBUG_TAG, "There was a problem creating the sending socket. Aborting.");
                e.printStackTrace();
                return false;
            }
        }

        // Build the packet
        DatagramPacket packet;
        byte data[] = message.getBytes();

        try {
            packet = new DatagramPacket(data, data.length, InetAddress.getByName(MULTICAST_IP), MULTICAST_PORT);
        } catch (UnknownHostException e) {
            Log.d(DEBUG_TAG, "It seems that " + MULTICAST_IP + " is not a valid ip! Aborting.");
            e.printStackTrace();
            return false;
        }

        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "There was an error sending the UDP packet. Aborted.");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void startMessageReceiver() {

        //Clear the dashboard list before receive new dashboards data.
        Global.getInstance().clearDashboards();

        // Check for WiFi connectivity
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = null;
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) // connected to wifi
                mWifi = activeNetwork;
        }

        if (mWifi == null || !mWifi.isConnected()) {
            Log.d(DEBUG_TAG, "Sorry! You need to be in a WiFi network in order to send UDP multicast packets. Aborting.");
        } else {
            Runnable receiver = new Runnable() {
                @Override
                public void run() {
                    // Enable multicast
                    WifiManager wim = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    if (wim != null) {
                        MulticastLock mcLock = wim.createMulticastLock(TAG);
                        mcLock.acquire();
                    }

                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);

                    // Create the receiver socket
                    if (rsocket == null) {
                        try {
                            rsocket = new DatagramSocket(MULTICAST_PORT);
                        } catch (IOException e) {
                            Log.d(DEBUG_TAG, "Impossible to create a new socket on port " + MULTICAST_PORT);
                            e.printStackTrace();
                            return;
                        }
                    }

                    while (receiveMessages) {
                        try {
                            socket.receive(rPacket);
                        } catch (IOException e1) {
                            Log.d(DEBUG_TAG, "There was a problem receiving the incoming message.");
                            e1.printStackTrace();
                            continue;
                        }

                        // The port is stored inside the packet that is sent by the dashboard
                        String port = new String(rPacket.getData(), 0, rPacket.getLength());
                        // We can also get the ip from the packet
                        String ip = rPacket.getAddress().getHostAddress();
                        Log.d("DASHBOARD", ip + ":" + port);
                        // Set values of IP and port in Global class
                        Global.getInstance().addDashboard(ip, port);

                        // If we call stopMessageReceiver() method, we must go out from the while loop
                        if (!receiveMessages)
                            break;

                    }
                }
            };

            receiveMessages = true;

            // Create thread if it's not created
            if (receiverThread == null)
                receiverThread = new Thread(receiver);

            // Start thread if it's not alive
            if (!receiverThread.isAlive()) {
                receiverThread.start();
            }
        }
    }

    public void stopMessageReceiver() {
        receiveMessages = false;
    }

}
