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

    public void setTextView(TextView tv_colorStripMinTemp, TextView tv_colorStripMaxTemp) {
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
            long timeInterval = (currentTime - lastUpdateTime) / 1000000;
            if (timeInterval > 0) {
                refreshRate = 1000f / timeInterval;
                final int fpsInt = Math.round(refreshRate);
                if (fpsUpdateListener != null) {
                    fpsUpdateListener.onFPSUpdate(fpsInt);
                }
            }
        }
        lastUpdateTime = currentTime;

        if (thermographMatrix != null) {
            int targetWidth = 72;
            int targetHeight = 72;

            float[][] interpolatedMatrix = BilinearInterpolation.bilinearInterpolate(thermographMatrix, targetWidth, targetHeight);

            if (userConfiguration.isInterpolateFramesMode() && previousMatrix != null) {
                interpolatedMatrix = interpolateFrames(previousMatrix, interpolatedMatrix);
            }
            previousMatrix = interpolatedMatrix;

            // 根据用户配置获取旋转角度
            int rotation = getThermographRotation();
            interpolatedMatrix = rotateMatrix(interpolatedMatrix, rotation);

            float[] minMax = new float[2];
            float minTemp, maxTemp;
            if (userConfiguration.isRelativeTemperatureMode()) {
                getMinMaxTemperature(interpolatedMatrix, minMax);
                minTemp = minMax[0];
                maxTemp = minMax[1];
            } else {
                minTemp = userConfiguration.getAbsMinimumTemperature();
                maxTemp = userConfiguration.getAbsMaximumTemperature();
            }
            if (tv_colorStripMinTemp != null) {
                tv_colorStripMinTemp.setText(Math.round(minTemp) + "℃");
                tv_colorStripMaxTemp.setText(Math.round(maxTemp) + "℃");
            }

            for (int i = 0; i < targetHeight; i++) {
                for (int j = 0; j < targetWidth; j++) {
                    float temp = interpolatedMatrix[i][j];
                    int color = ColorManager.getColor(context, temp, maxTemp, minTemp);
                    paint.setColor(color);

                    float left = j * (getWidth() / targetWidth);
                    float top = i * (getHeight() / targetHeight);
                    float right = left + (getWidth() / targetWidth);
                    float bottom = top + (getHeight() / targetHeight);
                    canvas.drawRect(left, top, right, bottom, paint);
                }
            }

            int centerX = targetWidth / 2;
            int centerY = targetHeight / 2;

            float centerTemp = interpolatedMatrix[centerY][centerX];
            int centerColor = ColorManager.getColor(context, centerTemp, maxTemp, minTemp);
            int invertedColor = invertColor(centerColor);

            paint.setColor(invertedColor);
            paint.setStrokeWidth(8);
            float lineLength = 45;
            canvas.drawLine(centerX * (getWidth() / targetWidth) - lineLength, centerY * (getHeight() / targetHeight),
                    centerX * (getWidth() / targetWidth) + lineLength, centerY * (getHeight() / targetHeight), paint);
            canvas.drawLine(centerX * (getWidth() / targetWidth), centerY * (getHeight() / targetHeight) - lineLength,
                    centerX * (getWidth() / targetWidth), centerY * (getHeight() / targetHeight) + lineLength, paint);

            float tempAtPointer = interpolatedMatrix[centerY + 1][centerX + 1];
            int tempColor = ColorManager.getColor(context, tempAtPointer, maxTemp, minTemp);
            int invertedTempColor = invertColor(tempColor);

            paint.setColor(invertedTempColor);
            paint.setTextSize(60);
            String tempText = Math.round(tempAtPointer) + "℃";
            float textX = (centerX + 1) * (getWidth() / targetWidth) + 60;
            float textY = (centerY + 1) * (getHeight() / targetHeight) + 60;
            canvas.drawText(tempText, textX, textY, paint);
        }
    }

    private float[][] rotateMatrix(float[][] matrix, int rotation) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        float[][] rotated = new float[cols][rows];

        switch (rotation) {
            case 0:
                return matrix;
            case 1:
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        rotated[j][rows - 1 - i] = matrix[i][j];
                    }
                }
                break;
            case 2:
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        rotated[rows - 1 - i][cols - 1 - j] = matrix[i][j];
                    }
                }
                break;
            case 3:
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        rotated[cols - 1 - j][i] = matrix[i][j];
                    }
                }
                break;
        }
        return rotated;
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

    // 获取用户配置的旋转角度
    private int getThermographRotation() {
        return userConfiguration.getThermographRotation();
    }
}
