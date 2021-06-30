package com.lomovskiy.customsrl

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

const val TAG_LOG = "TAG_LOG"

inline fun View.dp(value: Int): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}

class Srl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SimplePullToRefreshLayout(context, attrs, defStyleAttr) {

    private val loaderView: LoaderView

    init {

        loaderView = LoaderView(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
        }

        addView(FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, dp(64))
            addView(loaderView)
        })

        onTriggerListener {
            loaderView.startLoading()

            android.os.Handler(Looper.getMainLooper()).postDelayed({
                loaderView.stopLoading()
                stopRefreshing()
            }, 2700)
        }

    }

    override fun onMoveUp() {
        loaderView.startLoading()
    }

}
