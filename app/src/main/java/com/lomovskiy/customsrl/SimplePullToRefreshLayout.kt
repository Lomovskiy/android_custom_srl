package com.lomovskiy.customsrl

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

open class SimplePullToRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : ViewGroup(context, attrs, defStyle), RefreshView {

    var triggerOffSetTop = 0
        private set
    var maxOffSetTop = 0
        private set

    private var downX = 0F
    private var downY = 0F

    private var offsetY = 0F
    private var lastPullFraction = 0F

    private var currentState: State = State.IDLE

    private val onProgressListeners: MutableCollection<(Float) -> Unit> = mutableListOf()
    private val onTriggerListeners: MutableCollection<() -> Unit> = mutableListOf()

    companion object {
        private const val STICKY_FACTOR = 0.66F
        private const val STICKY_MULTIPLIER = 0.75F
        private const val ROLL_BACK_DURATION = 500L
    }

    private lateinit var topChildView: ChildView
    private lateinit var contentChildView: ChildView

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount != 2) {
            throw IllegalStateException("Only a topView and a contentView are allowed. Exactly 2 children are expected, but was $childCount")
        }

        topChildView = ChildView(getChildAt(0))
        contentChildView = ChildView(getChildAt(1))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        fun setInitialValues() {
            val topView = topChildView.view
//            val layoutParams = topView.layoutParams as MarginLayoutParams
//            val topViewHeight = topView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
            topChildView = topChildView.copy(positionAttr = PositionAttr(height = topView.measuredHeight))
            triggerOffSetTop = topView.measuredHeight
            maxOffSetTop = triggerOffSetTop * 3
        }

        measureChild(topChildView.view, widthMeasureSpec, heightMeasureSpec)
        measureChild(contentChildView.view, widthMeasureSpec, heightMeasureSpec)

        setInitialValues()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        fun layoutTopView() {
            val topView = topChildView.view
            val topViewAttr = topChildView.positionAttr

            val lp = topView.layoutParams as MarginLayoutParams
            val left: Int = paddingLeft + lp.leftMargin
            val top: Int = (paddingTop + lp.topMargin) - topViewAttr.height
            val right: Int = left + topView.measuredWidth
            val bottom = 0

            topChildView = topChildView.copy(positionAttr = PositionAttr(left = left, top = top, right = right, bottom = bottom))
            topView.layout(left, top, right, bottom)
        }

        fun layoutContentView() {
            val contentView = contentChildView.view

            val lp = contentView.layoutParams as MarginLayoutParams
            val left: Int = paddingLeft + lp.leftMargin
            val top: Int = paddingTop + lp.topMargin
            val right: Int = left + contentView.measuredWidth
            val bottom: Int = top + contentView.measuredHeight

            contentChildView = contentChildView.copy(positionAttr = PositionAttr(left = left, top = top, right = right, bottom = bottom))
            contentView.layout(left, top, right, bottom)
        }

        layoutTopView()
        layoutContentView()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        fun checkIfScrolledFurther(ev: MotionEvent, dy: Float, dx: Float) =
                if (!contentChildView.view.canScrollVertically(-1)) {
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
            triggerOffSetTop > offsetY -> {
                offsetY / triggerOffSetTop
            }
            else -> {
                1F
            }
        }
        offsetY = when {
            offsetY < 0 -> {
                0f
            }
            offsetY > maxOffSetTop -> {
                maxOffSetTop.toFloat()
            }
            else -> {
                offsetY
            }
        }

        onProgressListeners.forEach { it(pullFraction) }
        lastPullFraction = pullFraction

        topChildView.view.y = topChildView.positionAttr.top + offsetY
        contentChildView.view.y = contentChildView.positionAttr.top + offsetY
        if (offsetY > triggerOffSetTop) {
            onCouldStartAnimation()
        }
    }

    override fun stopPullingDown() {
        val rollBackOffset = if (offsetY > triggerOffSetTop) offsetY - triggerOffSetTop else offsetY
        val triggerOffset = if (rollBackOffset != offsetY) triggerOffSetTop else 0

        ValueAnimator.ofFloat(1F, 0F).apply {
            duration = ROLL_BACK_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                topChildView.view.y = topChildView.positionAttr.top + triggerOffset + rollBackOffset * animatedValue as Float
                contentChildView.view.y = contentChildView.positionAttr.top + triggerOffset + rollBackOffset * animatedValue as Float
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

    //<editor-fold desc="Helpers">
    fun onProgressListener(onProgressListener: (Float) -> Unit) {
        onProgressListeners.add(onProgressListener)
    }

    open fun onCouldStartAnimation() {

    }

    open fun onCouldEndAnimation() {

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

    enum class ViewType(val value: Int) {
        UNKNOWN(-1),
        TOP_VIEW(0),
        CONTENT(1);

        companion object {
            fun fromValue(value: Int) = values().first { it.value == value }
        }
    }

    enum class State {
        IDLE,
        ROLLING,
        TRIGGERING
    }

    data class ChildView(val view: View, val positionAttr: PositionAttr = PositionAttr())
    data class PositionAttr(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0, val height: Int = 0)
    //</editor-fold>
}
