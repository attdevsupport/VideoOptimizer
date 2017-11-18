package com.att.arocollector.client;

import android.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

import java.net.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class ClientEcho implements Runnable {
	private static final String ADDRESS = "192.168.0.97";
	private static final int MAX_VALUE = 1024 * 10;
    private static Context context;
    private static long total = 0;
    private static long start = 0;
    private static long iter = 0;
    private static long prev = 0;

	public static void runUDPClient(Context context) {
        new Thread(new ClientEcho(context)).start();
	}

	public ClientEcho(Context context) {
        this.context = context;
    }

	public void run() {
        int count = 10;
        String[] res = new String[count];
        for(int i =0 ; i < count; i++) {
            try {
                res[i] = runTest(i);
                Thread.sleep(5*1000);//5s
            } catch (Exception e) {
                Log.e("UDP_TEST", e.getLocalizedMessage(), e);
            }
        }
        Log.e("UDP_TEST", Arrays.toString(res));
    }
    private String runTest(int testNum) {
        int i = 0;
        Log.i("UDP_TEST", "Starting Test " + testNum);
        try {
            Thread.sleep(5*1000);//5s
        } catch (InterruptedException e) {
            Log.e("UDP_TEST", e.getLocalizedMessage(), e);
        }
        start = System.currentTimeMillis();
        long total = 0;
        iter = 0;
        prev = 0;
        try(DatagramSocket dsock = new DatagramSocket()) {
            InetAddress add = InetAddress.getByName(ADDRESS);
            String message1 = "This is client calling";
            byte arr[] = message1.getBytes();
            DatagramPacket dpack = new DatagramPacket(arr, arr.length, add, 6789);
            dsock.send(dpack);
            dsock.setSoTimeout(5*1000);
            long prev = 0;
            while (true) {
                dsock.receive(dpack); // receive the packet
                prev = System.nanoTime();
                i++;
            }
        } catch (Exception ex) {
            Log.e("UDP_TEST", ex.getLocalizedMessage(), ex);
        }

        String msg =  "Loss: " + ((MAX_VALUE - i) * 100 / MAX_VALUE) + "%   ##############################";
        Log.e("UDP_TEST", msg);
        Log.e("UDP_TEST", "" + total + "/" + iter);
        return  "Loss: " + ((MAX_VALUE - i) * 100 / MAX_VALUE);
    }

    public static void printElapsed(String text) {
        Log.e("UDP_TEST", String.valueOf(System.currentTimeMillis() - start) + " ### " + text);
    }

    public static void reportTime(long current) {
        if(prev != 0) {
            total += (current - prev);
        }
        prev = current;
        iter++;
    }
}