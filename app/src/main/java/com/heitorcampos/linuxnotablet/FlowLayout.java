package com.heitorcampos.linuxnotablet;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Linha de botões que quebra para a linha de baixo quando não cabe (celular na vertical). */
public class FlowLayout extends ViewGroup {
    final int gap;

    public FlowLayout(Context c, int gapPx) { super(c); gap = gapPx; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int maxW = MeasureSpec.getSize(wSpec) - getPaddingLeft() - getPaddingRight();
        if (MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED) maxW = Integer.MAX_VALUE;
        int x = 0, y = 0, rowH = 0, usedW = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            v.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
            if (x > 0 && x + w > maxW) { y += rowH + gap; x = 0; rowH = 0; }
            x += w + gap; rowH = Math.max(rowH, h); usedW = Math.max(usedW, x - gap);
        }
        int H = y + rowH + getPaddingTop() + getPaddingBottom();
        int W = MeasureSpec.getMode(wSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(wSpec) : usedW + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(W, resolveSize(H, hSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
            if (x > 0 && x + w > maxW) { y += rowH + gap; x = 0; rowH = 0; }
            v.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + gap; rowH = Math.max(rowH, h);
        }
    }
}
