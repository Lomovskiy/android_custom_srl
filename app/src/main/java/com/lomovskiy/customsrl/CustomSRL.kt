package com.lomovskiy.customsrl

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout

class CustomSRL @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val progressView: EAProgressBar = EAProgressBar(context).apply {
        layoutParams = SimpleSwipeRefreshLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    init {

        addView(progressView)

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(progressView, widthMeasureSpec, heightMeasureSpec)
        measureChild(getChildAt(1), widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        progressView.layout(l, t, r, b)
        getChildAt(1).layout(l, t, r, b)
    }

}