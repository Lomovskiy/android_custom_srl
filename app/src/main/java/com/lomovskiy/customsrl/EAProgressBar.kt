package com.lomovskiy.customsrl

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ProgressBar
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

class EAProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ProgressBar(context, attrs, defStyleAttr) {

    private val animatedVectorDrawable: AnimatedVectorDrawableCompat =
        AnimatedVectorDrawableCompat.create(context, R.drawable.avd_loader)!!

    var isImmediately: Boolean = true

    init {

        isIndeterminate = true
        setupAnimation()

    }

    fun start() {
        animatedVectorDrawable.start()
    }

    fun stop() {
        animatedVectorDrawable.stop()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            calculateSize(widthMeasureSpec),
            calculateSize(heightMeasureSpec)
        )
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!isImmediately) {
            return
        }
        if (hasWindowFocus) {
            animatedVectorDrawable.start()
        } else {
            animatedVectorDrawable.stop()
        }
    }

    private fun calculateSize(measureSpec: Int): Int {
        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(measureSpec)
            else -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_SIZE_DP, resources.displayMetrics).toInt()
        }
    }

    private fun setupAnimation() {
        animatedVectorDrawable.registerAnimationCallback(object: Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                post { animatedVectorDrawable.start() }
            }
        })
        indeterminateDrawable = animatedVectorDrawable
    }

    companion object {

        const val DEFAULT_SIZE_DP = 36F

    }

}
