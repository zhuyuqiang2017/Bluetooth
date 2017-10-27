package com.zyq.bluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Administrator on 2017/10/20 0020.
 */

public class DeviceItem {
    private String deviceName;
    private String deviceAddress;
    private BluetoothDevice bluetoothDevice;
    public DeviceItem(){}

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        if(deviceName == null || "".equals(deviceName)){
            this.deviceName = "unknown";
        }else{
            this.deviceName = deviceName;
        }
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }
}
