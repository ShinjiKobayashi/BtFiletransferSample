package com.example.kobashin.btserverservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MainActivity extends AppCompatActivity {


    private UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");
    private BluetoothAdapter mAdapter = null;
    private BluetoothDevice targetDevice = null;
    private BluetoothSocket mSocket = null;



    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ((Button) findViewById(R.id.bt_start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((CheckBox) findViewById(R.id.checkbox)).isChecked()) {
                    // client mode
                    mAdapter = BluetoothAdapter.getDefaultAdapter();
                    Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
                    for(BluetoothDevice device : devices){
                        targetDevice = device;
                    }

                    try {
                        mSocket = targetDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                        mSocket.connect();

                        InputStream is = mSocket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead = -1;
                        String message = "";
                        while (true) {
                            message = "";
                            bytesRead = is.read(buffer);
                            if (bytesRead != -1) {
                                while ((bytesRead==1024)&&(buffer[1023] != 0)) {
                                    message = message + new String(buffer, 0, bytesRead);
                                    bytesRead = is.read(buffer);
                                }
                                message = message + new String(buffer, 0, bytesRead - 1);
                                Log.i("koba", "getMessage: " + message);
                                break;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return;
                }
                // server mode
                Intent intent = new Intent();
                intent.setAction("com.example.kobashin.btserverservice.send");
                intent.setClassName("com.example.kobashin.btserverservice", "com.example.kobashin.btserverservice.BtFileTransferService");
                startService(intent);
            }
        });

        ((Button)findViewById(R.id.bt_end)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox) findViewById(R.id.checkbox)).isChecked()) {
                    // client mode


                    return;
                }
                // server mode
                Intent intent = new Intent();
                intent.setAction("com.example.kobashin.btserverservice.close");
                intent.setClassName("com.example.kobashin.btserverservice", "com.example.kobashin.btserverservice.BtFileTransferService");
                startService(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
