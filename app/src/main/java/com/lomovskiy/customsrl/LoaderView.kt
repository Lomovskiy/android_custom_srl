package com.lomovskiy.customsrl

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class LoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
    }

    fun startLoading() {
        setBackgroundResource(R.drawable.loader)
        (background as AnimationDrawable).start()
    }

    fun stopLoading() {
        (background as AnimationDrawable).stop()
        setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
    }

}