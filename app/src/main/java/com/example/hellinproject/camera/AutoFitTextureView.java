package com.example.hellinproject.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

// 이미지의 가로세로 비율과 TextureView의 가로세로 비율이 맞지 않으면 카메라를 통해 들어오는 이미지가 왜곡될 수 있으므로 이를 맞추기 위해 사용
// AutoFitTextureView는 가로세로 비율을 setAspectRatio() 함수에 전달받은 가로세로 비율로 유지
public class AutoFitTextureView extends TextureView {
    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public AutoFitTextureView(final Context context) {
        this(context, null);
    }

    public AutoFitTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    // 이미지의 가로, 세로 값을 전달하면 멤버 변수 ratioWidth와 ratioHeight에 이를 저장하고,
    // requestLayout() 함수를 호출하여 View를 다시 그림
    // 이때 onMeasure() 함수가 호출되며 AutoFitTextureView의 가로, 세로 크기를 계산함
    public void setAspectRatio(final int width, final int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }
}
