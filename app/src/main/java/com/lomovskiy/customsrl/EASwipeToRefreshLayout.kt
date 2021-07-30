package com.lomovskiy.customsrl

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView

class EASwipeToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    private val progressView: EAProgressBar = EAProgressBar(context).apply {
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        isImmediately = false
        visibility = View.GONE
    }

    private val arrowView: ImageView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ARROW_SIZE_DP, resources.displayMetrics).toInt(),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ARROW_SIZE_DP, resources.displayMetrics).toInt(),
            Gravity.CENTER
        )
        setBackgroundResource(R.drawable.ic_36_strelka_1)
    }

    private val arrowAnimator: ObjectAnimator = ObjectAnimator.ofFloat(arrowView, View.ROTATION, 0f, 180f).apply {
        duration = ARROW_SPEED
    }

    private val arrowAnimatorListener = object : AnimatorListenerAdapter() {

        override fun onAnimationStart(animation: Animator?) {
            arrowView.visibility = View.VISIBLE
        }

        override fun onAnimationEnd(animation: Animator?) {
            arrowView.visibility = View.GONE
            progressView.start()
            progressView.visibility = View.VISIBLE
            arrowView.rotation = 0F
        }

    }

    private lateinit var topChildView: View
    private lateinit var contentChildView: View

    private var triggerOffset = 0
    private var maxOffSet = 0

    private var downX = 0F
    private var downY = 0F

    private var offsetY = 0F

    private var currentState: State = State.IDLE

    private var onRefreshListener: OnRefreshListener? = null

    init {

        addView(
            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, HEIGHT_DP, resources.displayMetrics).toInt()
                )
                addView(progressView)
                addView(arrowView)
            }
        )

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
        maxOffSet = triggerOffset * MAX_OFFSET_MULTIPLIER
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutTopView()
        layoutContentView()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
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
                if (!contentChildView.canScrollVertically(-1)) {
                    shouldStealTouchEvents = ev.y > downY && Math.abs(dy) > Math.abs(dx)
                } else {
                    shouldStealTouchEvents = false
                }
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
                topChildView.y = topChildView.top + offsetY
                contentChildView.y = contentChildView.top + offsetY
                if (offsetY > triggerOffset) {
                    onCouldStartAnimation()
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                currentState = State.ROLLING
                stopRefreshing()
            }
        }

        return handledTouchEvent
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p != null && p is MarginLayoutParams
    }

    override fun generateDefaultLayoutParams(): MarginLayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): MarginLayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): MarginLayoutParams {
        return MarginLayoutParams(p)
    }

    fun setOnRefreshListener(listener: OnRefreshListener) {
        onRefreshListener = listener
    }

    fun stopRefreshing() {
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
                        onRefreshListener?.onRefresh()
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

    private fun onCouldStartAnimation() {
        if (arrowAnimator.isRunning || progressView.visibility == View.VISIBLE) {
            return
        }
        arrowAnimator.addListener(arrowAnimatorListener)
        arrowAnimator.start()
    }

    private fun onCouldEndAnimation() {
        progressView.visibility = View.GONE
        progressView.stop()
        arrowView.visibility = View.VISIBLE
    }

    enum class State {
        IDLE,
        ROLLING,
        TRIGGERING
    }

    interface OnRefreshListener {

        fun onRefresh()

    }

    private companion object {

        const val STICKY_FACTOR: Float = 0.66F
        const val STICKY_MULTIPLIER: Float = 0.75F
        const val ROLL_BACK_DURATION: Long = 500L
        const val ARROW_SPEED: Long = 150
        const val HEIGHT_DP: Float = 64F
        const val ARROW_SIZE_DP: Float = 36F
        const val MAX_OFFSET_MULTIPLIER: Int = 3

    }

}
