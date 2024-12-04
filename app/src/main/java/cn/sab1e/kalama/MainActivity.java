package cn.sab1e.kalama;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.usb.UsbDevice;

import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;

import android.view.inputmethod.EditorInfo;

import com.google.common.util.concurrent.ListenableFuture;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import android.Manifest;

public class MainActivity extends AppCompatActivity {
    private UsbBroadcastReceiver usbBroadcastReceiver;
    private KalamaDeviceManager kalamaDeviceManager;
    private TextView tv_showFPS;
    public String[] colorModeList = new String[]{"ROYGB", "YOPB", "BGW"};

    private UserConfiguration userConfiguration;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private PreviewView previewView;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private SwitchCompat sw_cameraAssistMode;
    private HeatmapView heatmapView;
    private LinearLayout ll_cameraSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // find view by id
        Spinner sp_colorMode = findViewById(R.id.sp_colorMode);
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorModeList);
        // 设置下拉样式
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 设置适配器给 Spinner
        sp_colorMode.setAdapter(adapter);
        Button btn_connect = findViewById(R.id.btn_connect);

        heatmapView = findViewById(R.id.heatmapView);
        tv_showFPS = findViewById(R.id.tv_showFPS);  // 获取 TextView

        EditText et_minTemperature = findViewById(R.id.et_minTemperature);
        EditText et_maxTemperature = findViewById(R.id.et_maxTemperature);

        SwitchCompat sw_interpolateFramesMode = findViewById(R.id.sw_interpolateFramesMode);
        SwitchCompat sw_relativeTemperatureMode = findViewById(R.id.sw_relativeTemperatureMode);
        sw_cameraAssistMode = findViewById(R.id.sw_cameraAssistMode);

        ColorStrip cs_colorStrip = findViewById(R.id.cs_colorStrip);

        LinearLayout ll_absTempSettings = findViewById(R.id.ll_absTempSettings);
        ll_cameraSettings = findViewById(R.id.ll_cameraSettings);

        TextView tv_colorStripMinTemp = findViewById(R.id.tv_colorStripMinTemp);
        TextView tv_colorStripMaxTemp = findViewById(R.id.tv_colorStripMaxTemp);
        heatmapView.setTextView(tv_colorStripMinTemp, tv_colorStripMaxTemp);

        previewView = findViewById(R.id.previewView);

        SeekBar sb_thermographAlpha = findViewById(R.id.sb_thermographAlpha);

        sb_thermographAlpha.setMax(100);
        sb_thermographAlpha.setMin(0);


        userConfiguration = UserConfiguration.getInstance(this);

        sp_colorMode.setSelection(getIndexOfString(colorModeList, userConfiguration.getColorMode()));
        sw_interpolateFramesMode.setChecked(userConfiguration.isInterpolateFramesMode());
        sw_relativeTemperatureMode.setChecked(userConfiguration.isRelativeTemperatureMode());
        et_minTemperature.setText(String.valueOf(userConfiguration.getAbsMinimumTemperature()));
        et_maxTemperature.setText(String.valueOf(userConfiguration.getAbsMaximumTemperature()));
        if (userConfiguration.isRelativeTemperatureMode()) {
            ll_absTempSettings.setVisibility(View.GONE);
        } else {
            ll_absTempSettings.setVisibility(View.VISIBLE);
        }
        if (userConfiguration.isCameraAssistMode()) {
            ll_cameraSettings.setVisibility(View.VISIBLE);
            sw_cameraAssistMode.setChecked(true);
        } else {
            ll_cameraSettings.setVisibility(View.GONE);
            sw_cameraAssistMode.setChecked(false);
        }
        sb_thermographAlpha.setProgress((int) (userConfiguration.getThermographAlpha() * 100));


        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // 如果权限已授予，设置监听器
            setUpCamera(sw_cameraAssistMode);
        }


        sb_thermographAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alpha = (float) progress / 100;
                heatmapView.setAlpha(alpha);
                userConfiguration.setThermographAlpha(alpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        et_minTemperature.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // 处理回车事件
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String inputText = et_minTemperature.getText().toString();
                    Toast.makeText(MainActivity.this, "输入内容: " + inputText, Toast.LENGTH_SHORT).show();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et_minTemperature.getWindowToken(), 0);
                    et_minTemperature.clearFocus();
                    userConfiguration.setAbsMinimumTemperature(Float.parseFloat((inputText)));
                    return true;
                }
                return false;
            }
        });

        et_maxTemperature.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // 处理回车事件
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String inputText = et_maxTemperature.getText().toString();
                    Toast.makeText(MainActivity.this, "输入内容: " + inputText, Toast.LENGTH_SHORT).show();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et_maxTemperature.getWindowToken(), 0);
                    et_maxTemperature.clearFocus();
                    userConfiguration.setAbsMaximumTemperature(Float.parseFloat(inputText));
                    return true;
                }
                return false;
            }
        });

        // 设置监听器
        sp_colorMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedItem = parentView.getItemAtPosition(position).toString();
                Toast.makeText(MainActivity.this, "选中了: " + selectedItem, Toast.LENGTH_SHORT).show();
                userConfiguration.setColorMode(selectedItem);
                cs_colorStrip.updateColorStrip();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                Toast.makeText(MainActivity.this, "没有选中任何项", Toast.LENGTH_SHORT).show();
            }
        });


        sw_interpolateFramesMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    userConfiguration.setInterpolateFramesMode(true);
                } else {
                    userConfiguration.setInterpolateFramesMode(false);
                }
            }
        });

        sw_relativeTemperatureMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    userConfiguration.setRelativeTemperatureMode(true);
                    ll_absTempSettings.setVisibility(View.GONE);

                } else {
                    userConfiguration.setRelativeTemperatureMode(false);
                    ll_absTempSettings.setVisibility(View.VISIBLE);
                }
            }
        });

        sw_cameraAssistMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        // 如果没有权限，请求权限
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                    } else {
                        startCamera();
                        heatmapView.setAlpha(userConfiguration.getThermographAlpha());
                        ll_cameraSettings.setVisibility(View.VISIBLE);
                        userConfiguration.setCameraAssistMode(true);
                    }
                } else {
                    stopCamera();
                    heatmapView.setAlpha(1.0f);
                    ll_cameraSettings.setVisibility(View.GONE);
                    userConfiguration.setCameraAssistMode(false);
                }

            }
        });

        kalamaDeviceManager = new KalamaDeviceManager(this, heatmapView);
        usbBroadcastReceiver = new UsbBroadcastReceiver(kalamaDeviceManager);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kalamaDeviceManager.openDevice();
            }
        });
        // 设置 FPS 更新监听器
        heatmapView.setOnFPSUpdateListener(new HeatmapView.OnFPSUpdateListener() {
            @Override
            public void onFPSUpdate(int fps) {
                // 在主线程中更新 TextView 显示 FPS
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_showFPS.setText(String.format("FPS: %d", fps));
                    }
                });
            }
        });
    }


    public static int getIndexOfString(String[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i;  // 返回索引
            }
        }
        return -1;  // 如果没有找到，返回 -1
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        filter.addAction("com.android.USB_PERMISSION");
        registerReceiver(usbBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        kalamaDeviceManager.openDevice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(usbBroadcastReceiver);
    }

    // 以下是相机部分
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予后，设置监听器
                setUpCamera(findViewById(R.id.sw_cameraAssistMode));
                sw_cameraAssistMode.setChecked(true);
                heatmapView.setAlpha(userConfiguration.getThermographAlpha());
                ll_cameraSettings.setVisibility(View.VISIBLE);
            } else {
                // 权限被拒绝
                Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show();
                sw_cameraAssistMode.setChecked(false);
            }
        }
    }

    // 设置相机
    private void setUpCamera(SwitchCompat switchCamera) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                // 如果开关已经打开，启动相机预览
                if (switchCamera.isChecked()) {
                    startCamera();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 启动相机预览
    private void startCamera() {
        if (cameraProvider != null) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求相机权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                // 如果权限已授予
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);
            }

        }
    }

    // 关闭相机预览
    private void stopCamera() {
        if (camera != null) {
            cameraProvider.unbindAll();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            cameraProvider.unbindAll();
        }
    }
}