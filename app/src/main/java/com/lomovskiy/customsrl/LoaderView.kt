package com.lomovskiy.customsrl

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

class ListenableAnimationDrawable(
    private val onStartTask: Runnable? = null,
    private val onEndTask: Runnable? = null
) : AnimationDrawable() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun start() {
        onStartTask?.run()
        onEndTask?.let {
            handler.postDelayed(it, getFullDuration().toLong())
        }
        super.start()
    }

    fun getFullDuration(): Int {
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

    private val arrowAnimation: ListenableAnimationDrawable = ListenableAnimationDrawable(
        onEndTask = {
            setBackgroundDrawable(loadingAnimation)
            loadingAnimation.start()
        }
    ).apply {
        isOneShot = true
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_1)!!, 25)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_2)!!, 25)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_3)!!, 25)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_4)!!, 25)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_5)!!, 25)
    }

    private val loadingAnimation: ListenableAnimationDrawable = ListenableAnimationDrawable().apply {
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_2)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_3)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_4)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_5)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_6)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_7)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_8)!!, 70)
        addFrame(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_9)!!, 70)
    }

    private lateinit var currentState: State

    init {
        updateState(State.START)
    }

    fun startLoading() {
        if (currentState == State.LOADING) {
            return
        }
        updateState(State.LOADING)
    }

    fun stopLoading() {
        updateState(State.START)
    }

    private fun updateState(state: State) {
        currentState = state
        when (state) {
            State.START -> {
                arrowAnimation.stop()
                loadingAnimation.stop()
                setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ic_36_strelka_1))
            }
            State.LOADING -> {
                setBackgroundDrawable(arrowAnimation)
                arrowAnimation.start()
            }
            State.END -> {
                setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ic_36_loader_1))
            }
        }
    }

    fun getFullDuration(): Int {
        return arrowAnimation.getFullDuration() + loadingAnimation.getFullDuration()
    }

    enum class Mode {
        FULL,
        LIGHT
    }

    enum class State {
        START,
        LOADING,
        END
    }

}