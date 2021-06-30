package com.lomovskiy.customsrl

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout

const val TAG_LOG = "TAG_LOG"

inline fun View.dp(value: Int): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}

class Srl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SimplePullToRefreshLayout(context, attrs, defStyleAttr) {

    private val loaderView: LoaderView = LoaderView(context).apply {
        layoutParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
    }

    init {

        addView(FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, dp(64))
            addView(loaderView)
        })

        onTriggerListener {

            android.os.Handler(Looper.getMainLooper()).postDelayed({
                stopPullingDown()
            }, loaderView.getFullDuration().toLong())
        }

    }

    override fun onCouldStartAnimation() {
        loaderView.startLoading()
    }

    override fun onCouldEndAnimation() {
        loaderView.stopLoading()
    }

}
