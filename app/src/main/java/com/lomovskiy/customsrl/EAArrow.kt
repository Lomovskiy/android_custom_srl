package com.lomovskiy.customsrl

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class EAArrow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val animation: ValueAnimator

    init {
        setBackgroundResource(R.drawable.ic_36_strelka_1)
        val valueAnimator: ValueAnimator = ValueAnimator.ofInt(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180)
        valueAnimator.addUpdateListener {
            rotation = (it.animatedValue as Int).toFloat()
        }
        valueAnimator.duration = 100
        animation = valueAnimator
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            animation.start()
        }
    }

}
