package cn.sab1e.kalama;

import android.content.Context;
import android.graphics.Color;

public class ColorManager {

    public static int getColor(Context context,float temperature, float maxTemp, float minTemp){
        UserConfiguration userConfiguration = UserConfiguration.getInstance(context);
        if(userConfiguration.getColorMode().equals("ROYGB")){
            return getColorFromTemperature_ROYGB(temperature, maxTemp, minTemp);
        } else if (userConfiguration.getColorMode().equals("YOPB")) {
            return getColorFromTemperature_YOPB(temperature, maxTemp, minTemp);
        }else if (userConfiguration.getColorMode().equals("BGW")) {
            return getColorFromTemperature_BGW(temperature, maxTemp, minTemp);
        }else {
            return getColorFromTemperature_ROYGB(temperature, maxTemp, minTemp);
        }
    }
    private static int getColorFromTemperature_ROYGB(float temperature, float maxTemp, float minTemp) {
        // 计算温度在最小值和最大值之间的比例
        float ratio = (temperature - maxTemp) / (minTemp - maxTemp);
        ratio = Math.min(Math.max(ratio, 0f), 1f);  // 保证比例在0到1之间

        // 计算颜色渐变
        if (ratio <= 0.2f) {
            // 从白色到红色
            return interpolateColor(Color.WHITE, Color.RED, ratio * 5); // 0~0.2 过渡到红色
        } else if (ratio <= 0.4f) {
            // 从红色到黄色（过渡区间为0.2~0.4）
            return interpolateColor(Color.RED, Color.YELLOW, (ratio - 0.2f) * 5); // 红到黄
        } else if (ratio <= 0.6f) {
            // 从黄色到绿色（过渡区间为0.4~0.6）
            return interpolateColor(Color.YELLOW, Color.GREEN, (ratio - 0.4f) * 5); // 黄到绿
        } else if (ratio <= 0.8f) {
            // 从绿色到蓝色（过渡区间为0.6~0.8）
            return interpolateColor(Color.GREEN, Color.BLUE, (ratio - 0.6f) * 5); // 绿到蓝
        } else {
            // 从蓝色到黑色（过渡区间为0.8~1）
            return interpolateColor(Color.BLUE, Color.BLACK, (ratio - 0.8f) * 5); // 蓝到黑
        }
    }

    private static int getColorFromTemperature_YOPB(float temperature, float maxTemp, float minTemp) {
        // 计算温度在最小值和最大值之间的比例
        float ratio = (temperature - maxTemp) / (minTemp - maxTemp);
        ratio = Math.min(Math.max(ratio, 0f), 1f);  // 保证比例在0到1之间

        // 计算颜色渐变
        if (ratio <= 0.2f) {
            // 从白色到黄色
            return interpolateColor(Color.WHITE, Color.YELLOW, ratio * 5); // 白到黄
        } else if (ratio <= 0.4f) {
            // 从黄色到橙色
            return interpolateColor(Color.YELLOW, Color.parseColor("#FFA500"), (ratio - 0.2f) * 5); // 黄到橙
        } else if (ratio <= 0.6f) {
            // 从橙色到紫色
            return interpolateColor(Color.parseColor("#FFA500"), Color.parseColor("#800080"), (ratio - 0.4f) * 5); // 橙到紫
        } else if (ratio <= 0.8f) {
            // 从紫色到深蓝色
            return interpolateColor(Color.parseColor("#800080"), Color.parseColor("#00008B"), (ratio - 0.6f) * 5); // 紫到深蓝
        } else if (ratio <= 1.0f) {
            // 从深蓝色到黑色
            return interpolateColor(Color.parseColor("#00008B"), Color.BLACK, (ratio - 0.8f) * 5); // 深蓝到黑
        } else {
            return Color.BLACK; // 保证最高温度时为黑色
        }
    }

    private static int getColorFromTemperature_BGW(float temperature, float maxTemp, float minTemp) {
        // 计算温度在最小值和最大值之间的比例
        float ratio = (temperature - maxTemp) / (minTemp - maxTemp);
        ratio = Math.min(Math.max(ratio, 0f), 1f);  // 保证比例在0到1之间

        // 计算颜色渐变
        if (ratio <= 0.5f) {
            // 从白色到灰色
            return interpolateColor(Color.WHITE, Color.GRAY, ratio * 2); // 白到灰
        } else {
            // 从灰色到黑色
            return interpolateColor(Color.GRAY, Color.BLACK, (ratio - 0.5f) * 2); // 灰到黑
        }
    }

    // 线性插值函数，用于两种颜色之间的过渡
    private static int interpolateColor(int colorStart, int colorEnd, float ratio) {
        int rStart = Color.red(colorStart);
        int gStart = Color.green(colorStart);
        int bStart = Color.blue(colorStart);

        int rEnd = Color.red(colorEnd);
        int gEnd = Color.green(colorEnd);
        int bEnd = Color.blue(colorEnd);

        int r = (int) (rStart + ratio * (rEnd - rStart));
        int g = (int) (gStart + ratio * (gEnd - gStart));
        int b = (int) (bStart + ratio * (bEnd - bStart));

        return Color.rgb(r, g, b);
    }
    // 平滑曲线插值函数
    private static float smoothStep(float t) {
        return t * t * (3f - 2f * t); // 使用 smoothstep 插值方法
    }
}

