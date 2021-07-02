package com.lomovskiy.customsrl

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

inline fun View.dp(value: Int): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}

class EASwipeToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    companion object {

        private const val STICKY_FACTOR = 0.66F
        private const val STICKY_MULTIPLIER = 0.75F
        private const val ROLL_BACK_DURATION = 500L

        const val DECELERATION_DEGREE: Long = 2
        const val ARROW_SPEED: Long = 75 * DECELERATION_DEGREE
        const val REFRESH_DURATION: Long = ARROW_SPEED * 2

    }

    private val progressBarIOS: ProgressBarIOS = ProgressBarIOS(context).apply {
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        isImmediately = false
        visibility = View.GONE
    }


    private val arrowView: ImageView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
        setBackgroundResource(R.drawable.ic_36_strelka_1)
    }

    private val arrowAnimator: ValueAnimator
  
    private var triggerOffset = 0
    private var maxOffSet = 0

    private var downX = 0F
    private var downY = 0F

    private var offsetY = 0F

    private var currentState: State = State.IDLE

    private val onTriggerListeners: MutableCollection<() -> Unit> = mutableListOf()

    private lateinit var topChildView: View
    private lateinit var contentChildView: View

    init {

        addView(
            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(64))
                addView(progressBarIOS)
                addView(arrowView)
            }
        )
        arrowAnimator = ObjectAnimator.ofFloat(arrowView, View.ROTATION, 0f, 180f).apply {
            duration = ARROW_SPEED
            doOnStart { arrowView.visibility = View.VISIBLE }
            doOnEnd {
                arrowView.visibility = View.GONE
                progressImage.visibility = View.VISIBLE
                animatedVectorDrawable.start()
                reverseArrowAnimator()
            }
        }

        onTriggerListener {

            android.os.Handler(Looper.getMainLooper()).postDelayed({
                stopPullingDown()
            }, REFRESH_DURATION)
        }

    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount != 2) {
            throw IllegalStateException("Only a topView and a contentView are allowed. Exactly 2 children are expected, but was $childCount")
        }

        topChildView = getChildAt(0)
        contentChildView = getChildAt(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(topChildView, widthMeasureSpec, heightMeasureSpec)
        measureChild(contentChildView, widthMeasureSpec, heightMeasureSpec)
        triggerOffset = topChildView.measuredHeight
        maxOffSet = triggerOffset * 3
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutTopView()
        layoutContentView()
    }

    private fun layoutTopView() {
        val lp = topChildView.layoutParams as MarginLayoutParams
        val left: Int = paddingLeft + lp.leftMargin
        val top: Int = (paddingTop + lp.topMargin) - topChildView.measuredHeight
        val right: Int = left + topChildView.measuredWidth
        val bottom = 0
        topChildView.layout(left, top, right, bottom)
    }

    private fun layoutContentView() {
        val lp = contentChildView.layoutParams as MarginLayoutParams
        val left: Int = paddingLeft + lp.leftMargin
        val top: Int = paddingTop + lp.topMargin
        val right: Int = left + contentChildView.measuredWidth
        val bottom: Int = top + contentChildView.measuredHeight
        contentChildView.layout(left, top, right, bottom)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        fun checkIfScrolledFurther(ev: MotionEvent, dy: Float, dx: Float) =
                if (!contentChildView.canScrollVertically(-1)) {
                    ev.y > downY && Math.abs(dy) > Math.abs(dx)
                } else {
                    false
                }

        var shouldStealTouchEvents = false

        if (currentState != State.IDLE) {
            shouldStealTouchEvents = false
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                shouldStealTouchEvents = checkIfScrolledFurther(ev, dy, dx)
            }
        }

        return shouldStealTouchEvents
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handledTouchEvent = true

        if (currentState != State.IDLE) {
            handledTouchEvent = false
        }

        parent.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                offsetY = (event.y - downY) * (1 - STICKY_FACTOR * STICKY_MULTIPLIER)
                move()
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                currentState = State.ROLLING
                stopPullingDown()
            }
        }

        return handledTouchEvent
    }

    private fun move() {
        val pullFraction: Float = when {
            offsetY == 0F -> {
                0F
            }
            triggerOffset > offsetY -> {
                offsetY / triggerOffset
            }
            else -> {
                1F
            }
        }
        offsetY = when {
            offsetY < 0 -> {
                0f
            }
            offsetY > maxOffSet -> {
                maxOffSet.toFloat()
            }
            else -> {
                offsetY
            }
        }

        topChildView.y = topChildView.top + offsetY
        contentChildView.y = contentChildView.top + offsetY
        if (offsetY > triggerOffset) {
            onCouldStartAnimation()
        }
    }

    private fun stopPullingDown() {
        val rollBackOffset = if (offsetY > triggerOffset) offsetY - triggerOffset else offsetY
        val triggerOffset = if (rollBackOffset != offsetY) triggerOffset else 0

        ValueAnimator.ofFloat(1F, 0F).apply {
            duration = ROLL_BACK_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                topChildView.y = topChildView.top + triggerOffset + rollBackOffset * animatedValue as Float
                contentChildView.y = contentChildView.top + triggerOffset + rollBackOffset * animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (triggerOffset != 0 && currentState == State.ROLLING) {
                        // можно запускать refresh
                        currentState = State.TRIGGERING
                        offsetY = triggerOffset.toFloat()
                        onTriggerListeners.forEach { it() }
                    } else {
                        // просто вернулись назад (недостаточно потянули)
                        currentState = State.IDLE
                        offsetY = 0f
                        onCouldEndAnimation()
                    }
                }
            })
            start()
        }
    }

    private fun onCouldStartAnimation() {
        if (arrowAnimator.isRunning || progressBarIOS.visibility == View.VISIBLE) {
            return
        }
        arrowAnimator.start()
    }

    private fun onCouldEndAnimation() {
        progressBarIOS.visibility = View.GONE
        progressBarIOS.stop()
        arrowView.visibility = View.VISIBLE
    }

    fun onTriggerListener(onTriggerListener: () -> Unit) {
        onTriggerListeners.add(onTriggerListener)
    }

    fun removeOnTriggerListener(onTriggerListener: () -> Unit) {
        onTriggerListeners.remove(onTriggerListener)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?) = null != p && p is MarginLayoutParams

    override fun generateDefaultLayoutParams() = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?) = MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?) = MarginLayoutParams(p)

    enum class State {
        IDLE,
        ROLLING,
        TRIGGERING
    }

    private fun reverseArrowAnimator() {
        arrowView.rotation = 0F
    }

}
