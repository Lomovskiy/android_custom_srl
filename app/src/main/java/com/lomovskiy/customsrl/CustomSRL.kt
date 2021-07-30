package com.lomovskiy.customsrl

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import kotlin.math.abs

const val TAG_LOG = "TAG_LOG"

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
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
    }

    private lateinit var childView: View

    private var downY: Float = 0F

    init {

        addView(progressView)

    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        var handled = false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.y - downY
                if (!childView.canScrollVertically(-1)) {
                    handled = ev.y > downY
                }
            }
        }
        return handled
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            downY = event.y
        }
        if (event.action == MotionEvent.ACTION_MOVE) {
            val offset = event.y - downY
//            Log.d(TAG_LOG, "offset %f".format(offset))
//            Log.d(TAG_LOG, "progressView.y %f, progressView.top %f".format(offset, progressView.y, progressView.top))
//            Log.d(TAG_LOG, "childView.y %f, childView.top %f".format(offset, childView.y, childView.top))
            progressView.y = progressView.top + offset
            childView.y = childView.top + offset
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(progressView, widthMeasureSpec, heightMeasureSpec)
        measureChild(childView, widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        progressView.layout(l, t, r, 0)
        childView.layout(l, t, r, b)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        childView = getChildAt(1)
    }

}
