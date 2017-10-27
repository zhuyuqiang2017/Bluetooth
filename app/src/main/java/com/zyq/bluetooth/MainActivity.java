package com.zyq.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView remindInfo;
    private Button discovery,send;
    private Switch mSwitch;
    private EditText editText;
    private ListView listView;
    private BluetoothAdapter mBluetoothAdapter;
    private DeviceAdapter mDeviceAdapter;
    private final int LOCATION_PERMISSION_CODE = 2;
    private final int UPDATE_REMIND_INFO = 3;
    public static final int UPDATE_BLUETOOTH_MESSAGE = 4;
    private boolean registered = false;
    private List<DeviceItem> mDevices = new ArrayList<>();
    private BluetoothService mService;

    private Handler mMainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_REMIND_INFO:
                    int res = msg.arg1;
                    if(res>0){
                        remindInfo.setText(getString(res));
                    }
                    break;
                case UPDATE_BLUETOOTH_MESSAGE:
                    Bundle data = msg.getData();
                    String message = data.getString("bluetooth_message","");
                    remindInfo.setText(message);
                    break;
            }

        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())){
                Message m = Message.obtain();
                m.what = UPDATE_REMIND_INFO;
                m.arg1 = R.string.start_discovery;
                mMainHandler.sendMessage(m);
                mDevices.clear();
            }
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())){
                Message m = Message.obtain();
                m.what = UPDATE_REMIND_INFO;
                m.arg1 = R.string.finish_discovery;
                mMainHandler.sendMessage(m);
            }
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                DeviceItem item = new DeviceItem();
                item.setBluetoothDevice(device);
                item.setDeviceAddress(device.getAddress());
                item.setDeviceName(device.getName());
                mDevices.add(item);
                mDeviceAdapter.notifyDataSetChanged();
            }
        }
    };

    private BluetoothService.OnBlueServiceStateChange mStateListener = new BluetoothService.OnBlueServiceStateChange() {
        @Override
        public void onStateChanged(final int state) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (listView != null) {
                        listView.setEnabled(state != BluetoothService.STATE_NONE);
                    }
                    if (send != null) {

                        send.setEnabled(state == BluetoothService.STATE_CONNECTED);
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkBluetoothAndLocationPermission();
        setContentView(R.layout.activity_main);
        initBluetooth();
        initView();
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(intent);
    }

    private void initBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
        if(!registered){
            IntentFilter mFilter = new IntentFilter();
            mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mFilter.addAction(BluetoothDevice.ACTION_FOUND);
            Intent result = registerReceiver(mReceiver,mFilter);
            if(result != null){
                registered = true;
            }
        }
        mService = new BluetoothService(mMainHandler);
        mService.startListener();
    }

    private void initView(){
        mSwitch = (Switch) findViewById(R.id.bluetooth_switch);
        editText = (EditText)findViewById(R.id.bluetooth_edit);
        send = (Button)findViewById(R.id.bluetooth_send);
        discovery = (Button) findViewById(R.id.bluetooth_discovery);
        remindInfo = (TextView)findViewById(R.id.bluetooth_message);
        listView  = (ListView)findViewById(R.id.bluetooth_list);
        mDeviceAdapter = new DeviceAdapter(this,mDevices);
        listView.setAdapter(mDeviceAdapter);
        if(mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON || mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON){
            mSwitch.setChecked(true);
        }
        applyListener();
    }

    private void applyListener(){
        if(mService != null){
            mService.setOnBlueServiceStateChange(mStateListener);
        }
        if(mSwitch != null){
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    boolean result = false;
                    if(mBluetoothAdapter != null ){
                        if(isChecked){
                            result = mBluetoothAdapter.enable();
                        }else{
                            result = mBluetoothAdapter.disable();
                        }
                        if(!result){
                            buttonView.setChecked(!isChecked);
                        }
                    }
                }
            });
        }
        if(editText != null){
            editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editText.setFocusable(true);
                }
            });
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (send != null){
                        if(s.toString().getBytes().length>0){
                            send.setEnabled(true);
                        }else{
                            send.setEnabled(false);
                        }
                    }

                }
            });
        }
        if(send != null){
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(editText != null && editText.getText().toString().getBytes().length>0){
                        mService.sendMessage(editText.getText().toString());
                        editText.setText("");
                    }
                }
            });
        }

        if(discovery != null){
            discovery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF || mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF){
                        Toast.makeText(MainActivity.this,"Open Bluetooth device first !!!",Toast.LENGTH_SHORT).show();
                    }else{
                        do {
                            mBluetoothAdapter.startDiscovery();
                        }while (!mBluetoothAdapter.isDiscovering());
                    }
                }
            });
        }

        if(listView != null){
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(position>=0 && position <mDevices.size()){
                        BluetoothDevice device = mDevices.get(position).getBluetoothDevice();
                        mService.startConnect(device);
                        android.util.Log.e("zyq","client request connect server");
                    }
                }
            });
        }
    }

    private void cancelListener(){
        if(mService != null){
            mService.setOnBlueServiceStateChange(null);
        }
        if(mSwitch != null){
            mSwitch.setOnCheckedChangeListener(null);
        }
        if(editText != null){
            editText.addTextChangedListener(null);
        }
        if(send != null){
            send.setOnClickListener(null);
        }

        if(discovery != null){
            discovery.setOnClickListener(null);
        }

        if(listView != null){
            listView.setOnItemClickListener(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(registered){
            unregisterReceiver(mReceiver);
        }
        cancelListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean grantedLocation = true;

        if(requestCode == LOCATION_PERMISSION_CODE){
            for(int i : grantResults){
                if(i != PackageManager.PERMISSION_GRANTED){
                    grantedLocation = false;
                }
            }
        }

        if(!grantedLocation){
            Toast.makeText(this,"Permission error !!!",Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkBluetoothAndLocationPermission(){

        if((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION_CODE);
        }
    }
    private void hideKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if(inputMethodManager.isActive()){
            inputMethodManager.toggleSoftInput(0,InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }
}
