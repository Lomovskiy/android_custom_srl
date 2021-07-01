package com.lomovskiy.customsrl

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class EAProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val animation: AnimationDrawable

    init {
        setBackgroundResource(R.drawable.eaptogressbar)
        animation = background as AnimationDrawable
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            animation.start()
        } else {
            animation.stop()
        }
    }

}
