package com.lomovskiy.customsrl

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

class OneShotAnimationDrawable(
    private val onEndListener: Runnable
) : AnimationDrawable() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    init {
        isOneShot = true
    }

    override fun start() {
        handler.postDelayed(onEndListener, getFullDuration().toLong())
        super.start()
    }

    private fun getFullDuration(): Int {
        var fullDuration = 0
        for (i in 0..numberOfFrames) {
            fullDuration += getDuration(i)
        }
        return fullDuration
    }

}

class LoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val arrowEndRunnable: Runnable = Runnable {
        setBackgroundDrawable(loadingAnimation)
        loadingAnimation.start()
    }

    private val arrowAnimation: OneShotAnimationDrawable = OneShotAnimationDrawable(arrowEndRunnable).apply {
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_1)!!, 10)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_2)!!, 10)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_3)!!, 10)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_4)!!, 10)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_5)!!, 10)
    }

    private val loadingAnimation: AnimationDrawable = AnimationDrawable().apply {
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_2)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_3)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_4)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_5)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_6)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_7)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_8)!!, 100)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_9)!!, 100)
    }

    init {
        setBackgroundDrawable(arrowAnimation)
    }

    fun startLoading() {
        setBackgroundDrawable(arrowAnimation)
        (background as AnimationDrawable).start()
    }

    fun stopLoading() {
        (background as AnimationDrawable).stop()
        setBackgroundResource(R.drawable.ic_36_strelka_1)
    }

}