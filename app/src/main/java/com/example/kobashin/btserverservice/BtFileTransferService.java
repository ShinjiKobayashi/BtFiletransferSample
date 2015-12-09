package com.example.kobashin.btserverservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class BtFileTransferService extends Service {

    private final static int STATE_DISCONNECTED = 0;
    private final static int STATE_CONNECTED = 1;

    private int mState = STATE_DISCONNECTED;

    private HandlerThread mThread = null;
    private MyHandler mHandler = null;


    private BluetoothAdapter mBtAdapter = null;


    static class MyHandler extends Handler {

        private UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");
        private WeakReference<BtFileTransferService> mService = null;
        private BluetoothServerSocket btserver = null;
        private BluetoothSocket mSocket = null;

        private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });


        final static int CMD_CONNECT = 0;
        final static int CMD_SEND_FILE = 1;
        final static int CMD_DISCONNECT = 2;

        private Looper mLooper = null;

        MyHandler(BtFileTransferService service, Looper looper){
            super(looper);
            mLooper = looper;
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BtFileTransferService service = mService.get();
            switch(msg.what){
                case CMD_CONNECT:
                    if(service.mState != STATE_CONNECTED) {
                        service.mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                    }
                    try {
                        btserver = service.mBtAdapter.listenUsingRfcommWithServiceRecord("bt_file_transfer", uuid);
                        Future<BluetoothSocket> future = executor.submit(new Callable<BluetoothSocket>() {
                            @Override
                            public BluetoothSocket call() throws Exception {
                                try {
                                    BluetoothSocket socket = btserver.accept();
                                    return socket;
                                } catch (IOException e){
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        });

                        mSocket = future.get();
                        if(mSocket != null) {
                            mSocket.connect();
                            service.mState = STATE_CONNECTED;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    break;
                case CMD_SEND_FILE:
                    if(service.mState != STATE_CONNECTED && mSocket != null){
                        break;
                    }

                    // BitmapをFileから読み出して、Base64化してStringにする
                    // Smaple実装はText

                    final String sndText = (String)msg.obj;
                    executor.submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            byte[] byteString = (sndText + " ").getBytes();
                                            byteString[byteString.length - 1] = 0;

                                            try {
                                                OutputStream os = mSocket.getOutputStream();
                                                os.write(byteString);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });



                    break;
                case CMD_DISCONNECT:
                    try {
                        mSocket.close();
                        service.mState = STATE_DISCONNECTED;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }


    public BtFileTransferService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mThread = new HandlerThread("myThread");
        mThread.start();

        mHandler = new MyHandler(this, mThread.getLooper());
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if(action == null){
            return Service.START_STICKY;
        }

        switch (action){
            case "com.example.kobashin.btserverservice.send":
                if(mState == STATE_DISCONNECTED){
                    mHandler.sendEmptyMessage(MyHandler.CMD_CONNECT);
                }

                Message msg = mHandler.obtainMessage();
                msg.what = MyHandler.CMD_SEND_FILE;
                msg.obj = intent.getStringExtra("text");
                mHandler.sendMessage(msg);
                break;
            case "com.example.kobashin.btserverservice.close":
                mHandler.sendEmptyMessage(MyHandler.CMD_DISCONNECT);
                stopSelf();
                break;
            default:
                break;
        }



        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        mThread.quit();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
