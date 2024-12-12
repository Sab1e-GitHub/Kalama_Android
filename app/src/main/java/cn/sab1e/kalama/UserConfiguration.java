package cn.sab1e.kalama;

import android.content.Context;
import android.util.Log;

public class UserConfiguration {
    private static UserConfiguration instance;

    public boolean isRelativeTemperatureMode() {
        return isRelativeTemperatureMode;
    }

    public boolean isInterpolateFramesMode() {
        return isInterpolateFramesMode;
    }

    public float getAbsMaximumTemperature() {
        return absMaximumTemperature;
    }

    public float getAbsMinimumTemperature() {
        return absMinimumTemperature;
    }

    public String getColorMode() {
        return colorMode;
    }

    public void setInterpolateFramesMode(boolean interpolateFramesMode) {
        isInterpolateFramesMode = interpolateFramesMode;
        storageManager.setBoolean("isInterpolateFramesMode", isInterpolateFramesMode);
    }

    public void setRelativeTemperatureMode(boolean relativeTemperatureMode) {
        isRelativeTemperatureMode = relativeTemperatureMode;
        storageManager.setBoolean("isRelativeTemperatureMode", isRelativeTemperatureMode);
    }

    public void setAbsMaximumTemperature(float absMaximumTemperature) {
        this.absMaximumTemperature = absMaximumTemperature;
        storageManager.setFloat("absMaximumTemperature", absMaximumTemperature);
    }

    public void setAbsMinimumTemperature(float absMinimumTemperature) {
        this.absMinimumTemperature = absMinimumTemperature;
        storageManager.setFloat("absMinimumTemperature", absMinimumTemperature);
    }

    public void setColorMode(String colorMode) {
        this.colorMode = colorMode;
        storageManager.setString("colorMode", colorMode);
    }

    private boolean isInterpolateFramesMode = false;
    private boolean isRelativeTemperatureMode = true;
    private boolean isCameraAssistMode = false;
    private float absMaximumTemperature = 80f;
    private float absMinimumTemperature = 0f;
    private float thermographAlpha = 1f;
    private String colorMode = "ROYGB";
    private Context context;
    private final StorageManager storageManager;
    private int thermographRotation = 0;

    private UserConfiguration(Context context) {
        try {
            storageManager = StorageManager.getInstance(context);
            isRelativeTemperatureMode = storageManager.getBoolean("isRelativeTemperatureMode", false);
            isInterpolateFramesMode = storageManager.getBoolean("isInterpolateFramesMode", false);
            isCameraAssistMode = storageManager.getBoolean("isCameraAssistMode",true);

            absMaximumTemperature = storageManager.getFloat("absMaximumTemperature", 0f);
            absMinimumTemperature = storageManager.getFloat("absMinimumTemperature", 0f);
            thermographAlpha = storageManager.getFloat("thermographAlpha",0.8f);
            thermographRotation = storageManager.getInt("thermographRotation",0);

            colorMode = storageManager.getString("colorMode", "ROYGB");
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public static UserConfiguration getInstance(Context context) {
        if (instance == null) {
            instance = new UserConfiguration(context);
        }
        return instance;
    }

    public boolean isCameraAssistMode() {
        return isCameraAssistMode;
    }

    public void setCameraAssistMode(boolean cameraAssistMode) {
        isCameraAssistMode = cameraAssistMode;
        storageManager.setBoolean("isCameraAssistMode", isCameraAssistMode);
    }

    public float getThermographAlpha() {
        return thermographAlpha;
    }

    public void setThermographAlpha(float thermographAlpha) {
        this.thermographAlpha = thermographAlpha;
        storageManager.setFloat("thermographAlpha", thermographAlpha);
    }

    public int getThermographRotation() {
        return thermographRotation;
    }

    public void setThermographRotation(int thermographRotation) {
        this.thermographRotation = thermographRotation;
        storageManager.setInt("thermographRotation", thermographRotation);
    }
}
