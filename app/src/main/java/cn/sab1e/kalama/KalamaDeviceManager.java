package cn.sab1e.kalama;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;


import java.io.InputStream;
import java.util.Map;

import com.hoho.android.usbserial.driver.*;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class KalamaDeviceManager
{
    public final int VID = 0x1234;
    public final int PID = 0x0066;
    private final int BAUD_RATE = 576000;
    private UsbManager usbManager;
    public UsbDeviceConnection connection;
    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager ioManager; // 用于读取数据
    private float[][] thermographMatrix;
    private Context context;
    private MainActivity mainActivity;
    private HeatmapView heatmapView;
    public KalamaDeviceManager(MainActivity mainActivity,HeatmapView heatmapView){
        this.mainActivity = mainActivity;
        this.context = mainActivity.getApplicationContext();
        this.heatmapView = heatmapView;
    }
    public boolean openDevice(){
        try{
            connectUSBDevice();
            return true;
        }catch (Exception e){
            Toast.makeText(context,"Error："+e.toString(),Toast.LENGTH_LONG).show();
            return false;
        }
    }
    private void connectUSBDevice(){
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        // 创建 PendingIntent 来处理权限请求
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent("com.android.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);

        boolean isFindDevice = false;

        for (UsbDevice device : deviceList.values()) {
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();

            if (deviceVID == VID && devicePID == PID) {
                isFindDevice=true;
                // 如果设备符合条件，检查权限
                if (usbManager.hasPermission(device)) {
                    // 如果已经有权限，可以直接操作设备
                    Log.d("USB", "Permission granted, proceeding with device.");
                    connection = usbManager.openDevice(device);
                    // 查找并连接 USB 串口
                    UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
                    if (driver != null) {
                        usbSerialPort = driver.getPorts().get(0); // 获取串口端口
                        try {

                            usbSerialPort.open(connection);
                            usbSerialPort.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE); // 设置串口参数
                            startReadingData();  // 开始读取数据
                        } catch (IOException e) {
                            Log.e("USB", "Error opening serial port", e);
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    // 如果没有权限，请求权限
                    usbManager.requestPermission(device, permissionIntent);
                }
            }
        }
        if(!isFindDevice){
            Toast.makeText(context,"没有找到设备", Toast.LENGTH_LONG).show();
        }

    }
    private float[][] thermographParser(byte[] data) {
        // 初始化温度矩阵
        final int ROWS = 8;
        final int COLS = 8;
        float[][] temperatureMatrix = new float[ROWS][COLS];

        // 解析每个像素的数据
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                // 每个温度传感器的温度数据占2个字节
                int highByte = data[(row * COLS + col) * 2 + 1]; // 高字节
                int lowByte = data[(row * COLS + col) * 2]; // 低字节

                // 解析温度值
                temperatureMatrix[row][col] = parseThermography(highByte, lowByte);
            }
        }

        return temperatureMatrix;
    }

    private float parseThermography(int highByte, int lowByte) {
        // 提取符号位，符号位在高字节的第 3 位
        int sign = ((highByte >> 3) & 0x01) == 1 ? -1 : 1;

        // 计算整数部分（高字节的低3位和低字节的高6位）
        int integerPart = ((highByte & 0x07) << 6) | ((lowByte >> 2) & 0x3F);

        // 计算小数部分（低字节的低2位）
        int fractionalPart = lowByte & 0x03;

        // 计算温度值并返回
        return sign * (integerPart + (fractionalPart * 0.25f));
    }

    private void startReadingData() {
        // 使用 SerialInputOutputManager 读取数据

        ioManager = new SerialInputOutputManager(usbSerialPort, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
//                Log.d("USB", "Received data: " + Arrays.toString(data));
                // 处理接收到的数据
                thermographMatrix = thermographParser(data);
//                Log.d("USB", "Thermograph data: " + Arrays.deepToString(thermographMatrix));
                // 更新热成像图
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        heatmapView.updateThermograph(thermographMatrix);
                    }
                });
            }

            @Override
            public void onRunError(Exception e) {
                Log.e("USB", "Error in IO Manager", e);
            }
        });
        ioManager.setReadBufferSize(128);   // 调整缓冲区大小为128字节，以便接收byte类型8*16的二维矩阵
//        Log.d("USB", "Buffer Size: " + ioManager.getReadBufferSize());
        // 开始读取数据
        new Thread(ioManager).start();
    }



}
