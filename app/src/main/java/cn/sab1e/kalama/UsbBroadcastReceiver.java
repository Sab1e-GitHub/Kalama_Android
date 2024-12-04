package cn.sab1e.kalama;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbBroadcastReceiver extends BroadcastReceiver {

    private KalamaDeviceManager kalamaDeviceManager;

    public UsbBroadcastReceiver(KalamaDeviceManager kalamaDeviceManager) {
        this.kalamaDeviceManager = kalamaDeviceManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("BroadcastReceiver","Get Broadcast: "+action);
        if ("com.android.USB_PERMISSION".equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d("USB", "Get Permission Successful! " + device.getDeviceName());
                    kalamaDeviceManager.openDevice();  // 授权成功后打开设备
                } else {
                    Log.d("USB", "Permission denied for device " + device.getDeviceName());
                }
            }
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d("USB", "USB 设备插入: " + device.getDeviceName());
                kalamaDeviceManager.openDevice();
            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d("USB", "USB 设备拔出: " + device.getDeviceName());
//                kalamaDeviceManager.closeDevice(); // 拔出设备时关闭设备
            }
        }
    }
}
