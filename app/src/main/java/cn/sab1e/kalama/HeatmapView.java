package cn.sab1e.kalama;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class HeatmapView extends View {

    private float[][] thermographMatrix;
    private float[][] previousMatrix;
    private Paint paint;
    private float interpolationFactor = 0.1f; // 控制插帧过渡的平滑度
    private long lastUpdateTime = 0;
    private float refreshRate = 0;
    private OnFPSUpdateListener fpsUpdateListener;
    private Context context = getContext();
    private UserConfiguration userConfiguration = UserConfiguration.getInstance(context);
    private TextView tv_colorStripMinTemp;
    private TextView tv_colorStripMaxTemp;

    public HeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);  // 启用抗锯齿
        paint.setTextAlign(Paint.Align.CENTER);  // 文本居中对齐

    }
    public void setTextView(TextView tv_colorStripMinTemp,TextView tv_colorStripMaxTemp){
        this.tv_colorStripMinTemp = tv_colorStripMinTemp;
        this.tv_colorStripMaxTemp = tv_colorStripMaxTemp;
    }
    // 设置 FPS 更新监听器
    public void setOnFPSUpdateListener(OnFPSUpdateListener listener) {
        this.fpsUpdateListener = listener;
    }

    // 获取 FPS 更新回调接口
    public interface OnFPSUpdateListener {
        void onFPSUpdate(int fps);
    }

    // 更新热成像图数据
    public void updateThermograph(float[][] newThermographMatrix) {
        this.thermographMatrix = newThermographMatrix;
        // 触发视图重绘
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long currentTime = System.nanoTime();
        if (lastUpdateTime != 0) {
            // 计算每次绘制的时间间隔（以毫秒为单位）
            long timeInterval = (currentTime - lastUpdateTime) / 1000000;  // 纳秒转为毫秒
            if (timeInterval > 0) {  // 确保时间间隔大于 0
                refreshRate = 1000f / timeInterval;  // 刷新率为每秒更新的次数
                final int fpsInt = Math.round(refreshRate);  // 四舍五入为整数
                // 通知 MainActivity 更新 FPS
                if (fpsUpdateListener != null) {
                    fpsUpdateListener.onFPSUpdate(fpsInt);
                }
            }
        }
        lastUpdateTime = currentTime;

        if (thermographMatrix != null) {
            // 计算目标图像的宽度和高度
            int targetWidth = 72;
            int targetHeight = 72;

            // 使用双线性插值算法将温度矩阵插值到目标分辨率
            float[][] interpolatedMatrix = BilinearInterpolation.bilinearInterpolate(thermographMatrix, targetWidth, targetHeight);

            // 判断是否启用图像插帧模式
            if (userConfiguration.isInterpolateFramesMode() && previousMatrix != null) {
                // 插帧处理：如果有前一帧的数据，进行插帧操作
                interpolatedMatrix = interpolateFrames(previousMatrix, interpolatedMatrix);
            }

            // 保存当前帧为下一帧做插值使用
            previousMatrix = interpolatedMatrix;

            // 获取插值后的温度矩阵的最大值和最小值
            float[] minMax = new float[2];
            float minTemp = 0f;
            float maxTemp = 0f;
            if(userConfiguration.isRelativeTemperatureMode()){
                getMinMaxTemperature(interpolatedMatrix, minMax);
                minTemp = minMax[0];
                maxTemp = minMax[1];
            }else {
                minTemp = userConfiguration.getAbsMinimumTemperature();
                maxTemp = userConfiguration.getAbsMaximumTemperature();
            }
            if(tv_colorStripMinTemp != null){
                String minTempText = Math.round(minTemp) +"℃";
                String maxTempText = Math.round(maxTemp) +"℃";
                tv_colorStripMinTemp.setText(minTempText);
                tv_colorStripMaxTemp.setText(maxTempText);
            }

            // 遍历插值后的矩阵，绘制每个格子
            for (int i = 0; i < targetHeight; i++) {
                for (int j = 0; j < targetWidth; j++) {
                    // 获取插值后的温度值
                    float temp = interpolatedMatrix[i][j];
                    int color = ColorManager.getColor(context,temp, maxTemp, minTemp);
                    paint.setColor(color);
                    // 绘制每个格子的矩形
                    float left = j * (getWidth() / targetWidth);
                    float top = i * (getHeight() / targetHeight);
                    float right = left + (getWidth() / targetWidth);
                    float bottom = top + (getHeight() / targetHeight);
                    canvas.drawRect(left, top, right, bottom, paint);
                }
            }

            // 计算中心点的坐标
            int centerX = targetWidth / 2;
            int centerY = targetHeight / 2;

            // 获取中心点的温度值
            float centerTemp = interpolatedMatrix[centerY][centerX];
            int centerColor = ColorManager.getColor(context, centerTemp, maxTemp, minTemp);
            int invertedColor = invertColor(centerColor);

            // 设置绘制十字的颜色为反色
            paint.setColor(invertedColor);
            paint.setStrokeWidth(8);  // 设置十字线的宽度
            // 计算十字的尺寸，线条长度为目标图像的宽度的一半
            float lineLength = 45;

            // 绘制十字形（水平线和垂直线）
            canvas.drawLine(centerX * (getWidth() / targetWidth) - lineLength, centerY * (getHeight() / targetHeight),
                    centerX * (getWidth() / targetWidth) + lineLength, centerY * (getHeight() / targetHeight), paint);
            canvas.drawLine(centerX * (getWidth() / targetWidth), centerY * (getHeight() / targetHeight) - lineLength,
                    centerX * (getWidth() / targetWidth), centerY * (getHeight() / targetHeight) + lineLength, paint);

            // 获取十字右下位置的温度
            float tempAtPointer = interpolatedMatrix[centerY + 1][centerX + 1];
            int tempColor = ColorManager.getColor(context, tempAtPointer, maxTemp, minTemp);
            int invertedTempColor = invertColor(tempColor);

            // 设置文本颜色为反色
            paint.setColor(invertedTempColor);
            paint.setTextSize(60);  // 设置字体大小
            String tempText = String.valueOf(Math.round(tempAtPointer)) + "℃";

            // 绘制温度文本
            float textX = (centerX + 1) * (getWidth() / targetWidth) + 60;  // 十字指针右下位置的X坐标
            float textY = (centerY + 1) * (getHeight() / targetHeight) + 60; // 十字指针右下位置的Y坐标
            canvas.drawText(tempText, textX, textY, paint);  // 绘制文本
        }
    }


    // 按照插值因子，对前后两帧进行插值
    private float[][] interpolateFrames(float[][] previous, float[][] current) {
        int rows = current.length;
        int cols = current[0].length;
        float[][] interpolated = new float[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                interpolated[i][j] = previous[i][j] + interpolationFactor * (current[i][j] - previous[i][j]);
            }
        }

        return interpolated;
    }

    // 获取温度矩阵中的最小值和最大值
    private void getMinMaxTemperature(float[][] matrix, float[] minMax) {
        float minTemp = Float.MAX_VALUE;
        float maxTemp = Float.MIN_VALUE;

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                float temp = matrix[i][j];
                if (temp < minTemp) {
                    minTemp = temp;
                }
                if (temp > maxTemp) {
                    maxTemp = temp;
                }
            }
        }

        minMax[0] = minTemp;
        minMax[1] = maxTemp;
    }

    // 计算反色
    private int invertColor(int color) {
        // 反转 RGB 值
        return Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color));
    }
}
