/*
 * Copyright (C) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mingming.seekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * Created by 00000_zhumingming on 17/2/13.
 */

public class RangeSeekBar extends View {

    private static final int DEFAULT_DURATION = 100;

    private int mDuration;

    private Scroller mScroller;

    private Drawable mCursorBG;

    private int[] mPressedEnableState = new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled};
    private int [] mUnPressedEnableState = new int[]{-android.R.attr.state_pressed,android.R.attr.state_enabled};

    private int mTextColorNormal;
    private int mTextColorSelected;
    private int mSeekBarColor;

    private int mSeekBarHeight;
    private int mTextSize;
    private int mMarginBetween;
    private int mPartLength;


    private RectF mSeekBarRect;
    private RectF mSeekBarRectSelected;

    private float[] mTextWidthArray;
    private Rect[] mClickRectArray;

    private CharSequence[] mTextArray;

    private Rect mPaddingRect;
    private Rect mCursorRect;

    private Paint mPaint;

    private int mRightBoundary;


    private float mCursorIndex = 0;
    private int mCursorNextIndex = 0;

    private int mLastX;
    private boolean mHited;
    private int mClickIndex = -1;


    private int mPointerID = -1;

    public RangeSeekBar(Context context) {
        this(context,null,0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
      this(context,attrs,0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        applyConfig(context, attrs);

        if (mPaddingRect == null){
            mPaddingRect = new Rect();
        }
        mPaddingRect.left = getPaddingLeft();
        mPaddingRect.top = getPaddingTop();
        mPaddingRect.right = getPaddingRight();
        mPaddingRect.bottom = getPaddingBottom();

        mCursorRect = new Rect();
        mSeekBarRect = new RectF();
        mSeekBarRectSelected = new RectF();

        if (mTextArray != null){
            mTextWidthArray = new float[mTextArray.length];
            mClickRectArray = new Rect[mTextArray.length];
        }

        mScroller = new Scroller(context,new DecelerateInterpolator());

        initPaint();
        initTextWidthArray();

        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);

    }

    private void applyConfig(Context context, AttributeSet attrs) {

        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RangeSeekBar);

        mDuration = a.getInteger(R.styleable.RangeSeekBar_autoMoveDuration,
                DEFAULT_DURATION);

        mCursorBG = a
                .getDrawable(R.styleable.RangeSeekBar_cursorBackground);

        mTextColorNormal = a.getColor(R.styleable.RangeSeekBar_textColorNormal,
                Color.BLACK);
        mTextColorSelected = a.getColor(
                R.styleable.RangeSeekBar_textColorSelected,
                Color.rgb(242, 79, 115));

        mSeekBarColor = a.getColor(
                R.styleable.RangeSeekBar_seekbarColor,
                Color.rgb(218, 215, 215));

        mSeekBarHeight = (int) a.getDimension(
                R.styleable.RangeSeekBar_seekbarHeight, 10);
        mTextSize = (int) a.getDimension(R.styleable.RangeSeekBar_textSize, 15);
        mMarginBetween = (int) a.getDimension(
                R.styleable.RangeSeekBar_spaceBetween, 15);

        mTextArray = a.getTextArray(R.styleable.RangeSeekBar_markTextArray);
        if (mTextArray != null && mTextArray.length > 0) {
            mCursorIndex = 0;
        }

        a.recycle();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mTextSize);
    }

    private void initTextWidthArray() {
        if (mTextArray != null && mTextArray.length > 0) {
            final int length = mTextArray.length;
            for (int i = 0; i < length; i++) {
                mTextWidthArray[i] = mPaint.measureText(mTextArray[i]
                        .toString());
            }
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        if (mPaddingRect == null){
            mPaddingRect = new Rect();
        }
        mPaddingRect.left = left;
        mPaddingRect.top = top;
        mPaddingRect.right = right;
        mPaddingRect.bottom = bottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int pointH = mCursorBG.getIntrinsicHeight();

        int heightNeeded = pointH + mMarginBetween +mTextSize+mPaddingRect.top+mPaddingRect.bottom;

        if (heightMode == MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    heightSize < heightNeeded ? heightSize : heightNeeded, MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    heightNeeded, MeasureSpec.EXACTLY);
        }
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        mSeekBarRect.left = mPaddingRect.left
                + mCursorBG.getIntrinsicWidth() / 2;
        mSeekBarRect.right = widthSize - mPaddingRect.right
                - mCursorBG.getIntrinsicWidth() / 2;
        mSeekBarRect.top = mPaddingRect.top + mTextSize ;
        mSeekBarRect.bottom = mSeekBarRect.top + mSeekBarHeight;

        mSeekBarRectSelected.top = mSeekBarRect.top;
        mSeekBarRectSelected.bottom = mSeekBarRect.bottom;

        mPartLength = ((int) (mSeekBarRect.right - mSeekBarRect.left))
                / (mTextArray.length - 1);

        mRightBoundary = (int) (mSeekBarRect.right + mCursorBG
                .getIntrinsicWidth() / 2);


        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /*** Draw text marks ***/
        final int length = mTextArray.length;
        mPaint.setTextSize(mTextSize);
        for (int i = 0; i < length; i++) {
            if (i == mCursorIndex) {
                mPaint.setColor(mTextColorSelected);
            } else {
                mPaint.setColor(mTextColorNormal);
            }

            final String text2draw = mTextArray[i].toString();
            final float textWidth = mTextWidthArray[i];

            float textDrawLeft = 0;
            // The last text mark's draw location should be adjust.
            textDrawLeft = mSeekBarRect.left + i * mPartLength - textWidth / 2;

            canvas.drawText(text2draw, textDrawLeft, mSeekBarRect.bottom + mMarginBetween
                    + mTextSize, mPaint);

            Rect rect = mClickRectArray[i];
            if (rect == null) {
                rect = new Rect();
                rect.top = mPaddingRect.top;
                rect.bottom = rect.top + mTextSize + mMarginBetween
                        + mSeekBarHeight;
                rect.left = (int) textDrawLeft;
                rect.right = (int) (rect.left + textWidth);

                mClickRectArray[i] = rect;
            }
        }


        /*** Draw seekbar ***/
        final float radius = (float) mSeekBarHeight / 2;
        mSeekBarRectSelected.left = mSeekBarRect.left + mPartLength
                * mCursorIndex;

        mPaint.setColor(mSeekBarColor);
        canvas.drawRoundRect(mSeekBarRect, radius, radius, mPaint);



        /*** Draw cursors ***/
        // left cursor first
        final int leftWidth = mCursorBG.getIntrinsicWidth();
        final int leftHeight = mCursorBG.getIntrinsicHeight();
        final int leftLeft = (int) (mSeekBarRectSelected.left - (float) leftWidth / 2);
        final int leftTop = (int) ((mSeekBarRect.top + mSeekBarHeight / 2) - (leftHeight / 2));
        mCursorRect.left = leftLeft;
        mCursorRect.top = leftTop;
        mCursorRect.right = leftLeft + leftWidth;
        mCursorRect.bottom = leftTop + leftHeight;
        mCursorBG.setBounds(mCursorRect);
        mCursorBG.draw(canvas);

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (getParent() != null){
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleTouchUp(event);
                break;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(MotionEvent event){
        final  int downX = (int) event.getX();
        final  int downY = (int)event.getY();
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        if (mCursorRect.contains(downX,downY)){
            if (mHited){
                return;
            }
            mLastX = downX;
            mPointerID = event.getPointerId(actionIndex);
            mCursorBG.setState(mPressedEnableState);
            mHited = true;
            invalidate();

        }else {
            // If touch x-y not be contained in cursor,
            // then we check if it in click areas
            final int clickBoundaryTop = mClickRectArray[0].top;
            final int clickBoundaryBottom = mClickRectArray[0].bottom;
            // Step one : if in boundary of total Y.
            if (downY < clickBoundaryTop || downY > clickBoundaryBottom) {
                mClickIndex = -1;
                return;
            }

            // Step two: find nearest mark in x-axis
            final int partIndex = (int) ((downX - mSeekBarRect.left) / mPartLength);
            final int partDelta = (int) ((downX - mSeekBarRect.left) % mPartLength);
            if (partDelta < mPartLength / 2) {
                mClickIndex = partIndex;
            } else if (partDelta > mPartLength / 2) {
                mClickIndex = partIndex + 1;
            }

            if (mClickIndex == mCursorIndex) {
                mClickIndex = -1;
                return;
            }

            // Step three: check contain
            if (!mClickRectArray[mClickIndex].contains(downX, downY)) {
                mClickIndex = -1;
            }
        }
    }


    private void handleTouchMove(MotionEvent event){
        if (mHited){
            final int x = (int)event.getX();
            float deltaX = x - mLastX;
            mLastX = (int) x;
            if (mCursorRect.left+ deltaX < mPaddingRect.left){
                mCursorIndex = 0;
                invalidate();
                return;
            }
            if (mCursorRect.right + deltaX > mRightBoundary){
                mCursorIndex = mTextArray.length - 1;
                invalidate();
                return;
            }

            if (deltaX == 0) {
                return;
            }

            // Calculate the movement.
            final float moveX = deltaX / mPartLength;
            mCursorIndex += moveX;

            invalidate();
        }


    }

    private void handleTouchUp(MotionEvent event){
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int actionID = event.getPointerId(actionIndex);

        if (actionID == mPointerID){
            if (!mHited){
                return;
            }
            final int lower = (int)Math.floor(mCursorIndex);
            final int higher =(int)Math.ceil(mCursorIndex);
            final float offset = mCursorIndex - lower;

            if (offset < 0.5f){
                mCursorNextIndex = lower;
            }else if (offset >0.5f){
                mCursorNextIndex = higher;
            }

            if (!mScroller.computeScrollOffset()) {
                final int fromX = (int) (mCursorIndex * mPartLength);

                mScroller.startScroll(fromX, 0, mCursorNextIndex
                        * mPartLength - fromX, 0, mDuration);
            }

            // Reset values of parameters
            mCursorBG.setState(mUnPressedEnableState);
            mHited = false;
            mPointerID = -1;
            invalidate();
        }else {
            final int pointerIndex = event.findPointerIndex(actionID);
            final int upX = (int) event.getX(pointerIndex);
            final int upY = (int) event.getY(pointerIndex);

            if (mClickIndex != -1
                    && mClickRectArray[mClickIndex].contains(upX, upY)) {
                int fromX = 0;
                if (!mScroller.computeScrollOffset()) {
                    mCursorNextIndex = mClickIndex;
                    fromX = (int) (mCursorIndex * mPartLength);
                    mScroller.startScroll(fromX, 0,
                            mCursorNextIndex * mPartLength - fromX, 0,
                            mDuration);
                    invalidate();
                }
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            final int deltaX = mScroller.getCurrX();

            mCursorIndex = (float) deltaX / mPartLength;

            invalidate();
        }

    }


    public void setSelection(int partIndex) {

        if (partIndex > mTextArray.length - 1 || partIndex < 0) {
            throw new IllegalArgumentException(
                    "Index should from 0 to size of text array minus 2!");
        }
        // if not initialized, just record the location
        if (mPartLength == 0) {
            mCursorIndex = partIndex;

            return;
        }

        if (partIndex != mCursorIndex) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            mCursorNextIndex = partIndex;
            final int leftFromX = (int) (mCursorIndex * mPartLength);
            mScroller.startScroll(leftFromX, 0, mCursorNextIndex
                    * mPartLength - leftFromX, 0, mDuration);
            invalidate();
        }
    }


    public int getSelection(){
        return (int)mCursorIndex;
    }

    public void setCursorBackground(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException(
                    "Do you want to make left cursor invisible?");
        }

        mCursorBG = drawable;

        requestLayout();
        invalidate();
    }

    public void setCursorBackground(int resID) {
        if (resID < 0) {
            throw new IllegalArgumentException(
                    "Do you want to make left cursor invisible?");
        }

        mCursorBG = getResources().getDrawable(resID);

        requestLayout();
        invalidate();
    }


    public void setTextMarkColorNormal(int color) {
        if (color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make text mark invisible?");
        }

        mTextColorNormal = color;

        invalidate();
    }

    public void setTextMarkColorSelected(int color) {
        if (color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make text mark invisible?");
        }

        mTextColorSelected = color;

        invalidate();
    }

    public void setSeekbarColor(int color) {
        if (color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make seekbar invisible?");
        }

        mSeekBarColor = color;

        invalidate();
    }

    /**
     * In pixels. Users should call this method before view is added to parent.
     *
     * @param height
     */
    public void setSeekbarHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException(
                    "Height of seekbar can not less than 0!");
        }

        mSeekBarHeight = height;
    }

    /**
     * To set space between text mark and seekbar.
     *
     * @param space
     */
    public void setSpaceBetween(int space) {
        if (space < 0) {
            throw new IllegalArgumentException(
                    "Space between text mark and seekbar can not less than 0!");
        }

        mMarginBetween = space;

        requestLayout();
        invalidate();
    }

    /**
     * This method should be called after {@link #setTextMarkSize(int)}, because
     * view will measure size of text mark by paint.
     *
     */
    public void setTextMarks(CharSequence... marks) {
        if (marks == null || marks.length == 0) {
            throw new IllegalArgumentException(
                    "Text array is null, how can i do...");
        }

        mTextArray = marks;
        mCursorIndex = 0;
        mTextWidthArray = new float[marks.length];
        mClickRectArray = new Rect[mTextArray.length];
        initTextWidthArray();

        requestLayout();
        invalidate();
    }

    /**
     * Users should call this method before view is added to parent.
     *
     * @param size
     *            in pixels
     */
    public void setTextMarkSize(int size) {
        if (size < 0) {
            return;
        }

        mTextSize = size;
        mPaint.setTextSize(size);
    }
}
