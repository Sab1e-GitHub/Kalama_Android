package cn.sab1e.kalama;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class SquareView extends ViewGroup {
    public SquareView(Context context) {
        super(context);
    }

    public SquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        // 如果宽度指定特定值，并且高度未指定特定值（让高度等于宽度就保证了宽高相等）
        if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = width;
            if (heightMode == MeasureSpec.AT_MOST) { // 这里还考虑了高度受上限的情况
                height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
            }
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        // 测量所有子视图
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 在此方法中进行子视图的布局
        // 由于我们要让 SquareView 保持正方形，这里我们将子视图放置在中间
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int width = getWidth();
            int height = getHeight();
            int left = (width - child.getMeasuredWidth()) / 2;
            int top = (height - child.getMeasuredHeight()) / 2;
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }
}
