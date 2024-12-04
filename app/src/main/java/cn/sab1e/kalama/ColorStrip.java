package cn.sab1e.kalama;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ColorStrip extends View {
    private UserConfiguration userConfiguration = UserConfiguration.getInstance(getContext());
    private Paint paint;
    private int stripWidth; // 色条的宽度
    private int stripHeight = 100; // 色条的高度
    private float minValue = 0; // 色条的最小值
    private float maxValue = 100; // 色条的最大值
    private Paint borderPaint;
    private Context context = getContext();

    public ColorStrip(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
        borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK); // 设置边框颜色
        borderPaint.setStyle(Paint.Style.STROKE); // 只绘制边框，不填充
        borderPaint.setStrokeWidth(10); // 设置边框宽度
    }

    // 设置色条的最小值和最大值
    public void setMinMaxValue(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        invalidate(); // 更新视图
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        stripWidth = getWidth(); // 获取视图的宽度
    }
    public void updateColorStrip(){
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateBorderColor();
        // 计算色条上的每个位置的颜色，并绘制每一列
        for (int i = 0; i < stripWidth; i++) {
            // 当前的位置 (根据当前位置 i 映射到最小值和最大值之间)
            float position = minValue + (i / (float) stripWidth) * (maxValue - minValue);

            // 获取当前位置对应的颜色
            int color = ColorManager.getColor(context,position, maxValue, minValue);
            paint.setColor(color);

            // 绘制每一列的矩形
            canvas.drawRect(i, 0, i + 1, stripHeight, paint);
        }
        // 绘制边框
        float left = 0;
        float top = 0;
        float right = getWidth();
        float bottom = getHeight();
        canvas.drawRect(left, top, right, bottom, borderPaint);
    }
    // 根据当前的UI模式（夜间或日间模式）设置边框颜色
    private void updateBorderColor() {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // 夜间模式下，设置边框为白色
            borderPaint.setColor(Color.WHITE);

        } else {
            // 日间模式下，设置边框为黑色
            borderPaint.setColor(Color.BLACK);
        }
    }

    // 如果在运行时切换主题，调用此方法更新边框颜色
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateBorderColor();
        invalidate(); // 重新绘制
    }
}
