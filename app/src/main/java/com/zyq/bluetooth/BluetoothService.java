package com.zyq.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.apache.http.conn.scheme.HostNameResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Administrator on 2017/10/20 0020.
 */

public class BluetoothService {
    private Handler mHandler;
    private AcceptListener mListenerThread;
    private ConnectThread mConnectTread;
    private DataThread mDataThread;
    private static final UUID UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    public static final int STATE_NONE = -1;
    public static final int STATE_LISTEN = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    private int mState;
    private OnBlueServiceStateChange mOnBlueServiceStateChange;
    public interface OnBlueServiceStateChange{
        void onStateChanged(int state);
    }

    public BluetoothService(Handler handler) {
        this.mHandler = handler;
        mState = STATE_NONE;
    }

    public void startListener() {
        mListenerThread = new AcceptListener();
        mListenerThread.start();
    }

    public synchronized void stop() {
        if (mListenerThread != null) {
            mListenerThread = null;
        }
        if (mConnectTread != null) {
            mConnectTread = null;
        }
        if (mDataThread != null) {
            mDataThread.close();
            mDataThread = null;
        }
        setState(STATE_NONE);
    }

    public synchronized void setState(int state) {
        this.mState = state;
        if(mOnBlueServiceStateChange != null){
            mOnBlueServiceStateChange.onStateChanged(mState);
        }
    }

    private void connected(BluetoothSocket socket){
        if (mListenerThread != null) {
            mListenerThread = null;
        }
        if(mConnectTread != null){
            mConnectTread = null;
        }
        if (mDataThread != null) {
            mDataThread.close();
            mDataThread = null;
        }
        mDataThread = new DataThread(socket);
        mDataThread.start();
        setState(STATE_CONNECTED);
    }

    public void setOnBlueServiceStateChange(OnBlueServiceStateChange listener){
        this.mOnBlueServiceStateChange = listener;
    }

    public void startConnect(BluetoothDevice device){
        if(device != null){
            if (mListenerThread != null) {
                mListenerThread = null;
            }
            if (mConnectTread != null) {
                mConnectTread = null;
            }
            if (mDataThread != null) {
                mDataThread.close();
                mDataThread = null;
            }
            mConnectTread = new ConnectThread(device);
            mConnectTread.start();
            setState(STATE_CONNECTING);
        }

    }

    private class AcceptListener extends Thread {
        private BluetoothServerSocket mServerSocket;
        private BluetoothAdapter mAdapter;
        private BluetoothSocket mSocket;
        public AcceptListener() {
            mAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        @Override
        public void run() {
            try {
                setState(STATE_LISTEN);
                mServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord("bluetooth",UUID_INSECURE);
                while (mState != STATE_CONNECTED && mState != STATE_CONNECTING){
                    mSocket = mServerSocket.accept();
                    if(mSocket != null){
                        setState(STATE_CONNECTING);
                        synchronized (BluetoothService.this){
                           connected(mSocket);
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mClientSocket;
        private BluetoothDevice mDevice;
        public ConnectThread(BluetoothDevice device) {
            this.mDevice = device;
            try {
                mClientSocket = mDevice.createInsecureRfcommSocketToServiceRecord(UUID_INSECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean connected = false;
            int i = 0;
            do{
                try {
                    mClientSocket.connect();
                } catch (IOException e) {
                    connected = true;
                }
                i++;
            }while(connected && i<10);
            connected(mClientSocket);
        }
    }

    private class DataThread extends Thread {

        private BluetoothSocket mDataSocket;
        private OutputStream mOut;
        private InputStream mIn;
        public DataThread(BluetoothSocket socket) {
            this.mDataSocket = socket;
            try {
                mIn = mDataSocket.getInputStream();
                mOut = mDataSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setState(STATE_CONNECTED);
        }

        @Override
        public void run() {
            super.run();
            android.util.Log.e("zyq","read message ....");
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mIn.read(buffer);
                    Bundle data = new Bundle();
                    data.putString("bluetooth_message",new String(buffer,0,bytes));
                    Message m = Message.obtain();
                    m.setData(data);
                    m.what = MainActivity.UPDATE_BLUETOOTH_MESSAGE;
                    mHandler.sendMessage(m);
                } catch (IOException e) {
                    android.util.Log.e("zyq","DataThread : e = "+e.getMessage());
                    setState(STATE_NONE);
                    BluetoothService.this.startListener();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mOut.write(buffer);
                mOut.flush();
            } catch (IOException e) {
            }
        }

        public void close(){
            try {
                if (mDataSocket != null) {
                    mDataSocket.close();
                }
                if(mOut != null){
                    mOut.close();
                }
                if(mIn != null){
                    mIn.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendMessage(String message){
        if(mState == STATE_CONNECTED && mDataThread != null){
            mDataThread.write(message.getBytes());
        }
    }
}
