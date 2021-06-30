package com.lomovskiy.customsrl

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

class EAProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defaultSpinTimeMs: Int = 500
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val frames: Array<Drawable> = arrayOf(
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_2)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_3)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_4)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_5)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_6)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_7)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_8)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_36_loader_9)!!
    )

    private val animation: AnimationDrawable

    init {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.EAProgressBar, defStyleAttr, 0)
        val spinTypeMs: Int = typedArray.getInt(R.styleable.EAProgressBar_spin_time_ms, defaultSpinTimeMs)
        val msPerFrame: Int = spinTypeMs / frames.size
        animation = AnimationDrawable().apply {
            frames.forEach { frame: Drawable ->
                addFrame(frame, msPerFrame)
            }
        }
        background = animation
        typedArray.recycle()
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
