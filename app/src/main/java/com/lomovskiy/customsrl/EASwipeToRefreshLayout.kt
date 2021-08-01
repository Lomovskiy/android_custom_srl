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
import android.widget.ListView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.widget.ListViewCompat
import kotlin.math.abs

class EASwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle),
    NestedScrollingParent,
    NestedScrollingChild {

    private var notify: Boolean = true

    var isRefreshing: Boolean = false
        set(refreshing) {
            if (isRefreshing != refreshing) {
                field = refreshing
                if (refreshing) {
                    if (currentState != State.LOADING) {
                        startRefreshing()
                    }
                } else {
                    notify = false
                    currentState = State.MOVING
                    stopRefreshing()
                }
            }
        }

    private val triggerOffSetTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64F, resources.displayMetrics).toInt()
    private val maxOffSetTop = triggerOffSetTop * 2

    private var downX = 0F
    private var downY = 0F

    private var offsetY = 0F

    private var currentState: State = State.IDLE

    private var mNestedScrollingParentHelper: NestedScrollingParentHelper = NestedScrollingParentHelper(this)
    private var mNestedScrollingChildHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private val mParentScrollConsumed = IntArray(2)
    private val mParentOffsetInWindow = IntArray(2)
    private var mNestedScrollInProgress = false

    private var onRefreshListener: OnRefreshListener? = null

    private val progressView: EAProgressBar = EAProgressBar(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        isImmediately = false
        visibility = View.GONE
    }

    private val arrowView: ImageView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ARROW_SIZE_DP, resources.displayMetrics).toInt(),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ARROW_SIZE_DP, resources.displayMetrics).toInt(),
            Gravity.CENTER
        )
        setBackgroundResource(R.drawable.ic_ea_swipe_to_refresh_layout_arrow)
    }

    private val arrowAnimator: ObjectAnimator = ObjectAnimator.ofFloat(arrowView, View.ROTATION, 0f, 180f).apply {
        duration = ARROW_SPEED
    }

    private val arrowAnimatorListener = object : AnimatorListenerAdapter() {

        override fun onAnimationStart(animation: Animator?) {
            progressView.visibility = View.GONE
            arrowView.visibility = View.VISIBLE
        }

        override fun onAnimationEnd(animation: Animator?) {
            arrowView.visibility = View.GONE
            progressView.isLoading = true
            progressView.visibility = View.VISIBLE
            arrowView.rotation = 0F
        }

    }

    init {
        addView(FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, triggerOffSetTop)
            addView(progressView)
            addView(arrowView)
        })
        isNestedScrollingEnabled = true
    }

    private lateinit var topView: View
    private lateinit var childView: View

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount != 2) {
            throw IllegalStateException("Only a topView and a contentView are allowed. Exactly 2 children are expected, but was $childCount")
        }

        topView = getChildAt(0)
        childView = getChildAt(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

//        fun setInitialValues() {
//            val topView = topChildView
//            val layoutParams = topView.layoutParams as MarginLayoutParams
//            val topViewHeight = topView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
//            topChildView = topChildView.copy(positionAttr = PositionAttr(height = topViewHeight))
//        }

        measureChild(topView, widthMeasureSpec, heightMeasureSpec)
        measureChild(childView, widthMeasureSpec, heightMeasureSpec)
//        setInitialValues()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutTopView()
        layoutContentView()
//        topView.layout(l, t, r, b)
//        childView.layout(l, t, r, b)
    }

    private fun layoutTopView() {
        val lp = topView.layoutParams as MarginLayoutParams
        val left: Int = paddingLeft + lp.leftMargin
        val top: Int = (paddingTop + lp.topMargin) - topView.measuredHeight
        val right: Int = left + topView.measuredWidth
        val bottom = 0
        topView.layout(left, top, right, bottom)
    }

    private fun layoutContentView() {
        val lp = childView.layoutParams as MarginLayoutParams
        val left: Int = paddingLeft + lp.leftMargin
        val top: Int = paddingTop + lp.topMargin
        val right: Int = left + childView.measuredWidth
        val bottom: Int = top + childView.measuredHeight
        childView.layout(left, top, right, bottom)
    }

//    private fun layoutTopView() {
//        val topView = topChildView.view
//        val topViewAttr = topChildView.positionAttr
//
//        val lp = topView.layoutParams as MarginLayoutParams
//        val left: Int = paddingLeft + lp.leftMargin
//        val top: Int = (paddingTop + lp.topMargin) - topViewAttr.height - ELEVATION
//        val right: Int = left + topView.measuredWidth
//        val bottom = - ELEVATION
//        topChildView = topChildView.copy(positionAttr = PositionAttr(left = left, top = top, right = right, bottom = bottom))
//        topView.layout(left, top, right, bottom)
//    }

//    private fun layoutContentView() {
//        val lp = childView.view.layoutParams as MarginLayoutParams
//        val left: Int = paddingLeft + lp.leftMargin
//        val top: Int = paddingTop + lp.topMargin
//        val right: Int = left + childView.view.measuredWidth
//        val bottom: Int = top + childView.view.measuredHeight
//        childView.view.layout(left, top, right, bottom)
//    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || isRefreshing || currentState == State.MOVING || mNestedScrollInProgress || canChildScrollUp()) {
            return false
        }

        fun checkIfScrolledFurther(ev: MotionEvent, dy: Float, dx: Float) =
            if (!childView.canScrollVertically(-1)) {
                ev.y > downY && abs(dy) > abs(dx)
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
        if (!isEnabled || isRefreshing || currentState == State.MOVING || mNestedScrollInProgress || canChildScrollUp()) {
            return false
        }

        var handledTouchEvent = true

        if (currentState != State.IDLE) {
            handledTouchEvent = false
        }

        parent.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                offsetY = (event.y - downY) * (1 - STICKY_FACTOR * STICKY_MULTIPLIER)
                notify = true
                move()
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP,
            -> {
                currentState = State.MOVING
                stopRefreshing()
            }
        }

        return handledTouchEvent
    }

    private fun startRefreshing() {
        val triggerOffset: Float = if (offsetY > triggerOffSetTop) offsetY else triggerOffSetTop.toFloat()

        ValueAnimator.ofFloat(0F, 1F).apply {
            duration = ROLL_BACK_DURATION
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                positionChildren(triggerOffset * animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    offsetY = triggerOffset
                }
            })
            start()
        }
    }

    private fun move() {
        offsetY = offsetY.coerceIn(0f, maxOffSetTop.toFloat())
        positionChildren(offsetY)
        if (offsetY > triggerOffSetTop) {
            if (arrowAnimator.isRunning || progressView.visibility == View.VISIBLE) {
                return
            }
            arrowAnimator.addListener(arrowAnimatorListener)
            arrowAnimator.start()
        }
    }

    private fun stopRefreshing() {
        val rollBackOffset = if (offsetY > triggerOffSetTop) offsetY - triggerOffSetTop else offsetY
        val triggerOffset = if (rollBackOffset != offsetY) triggerOffSetTop else 0

        ValueAnimator.ofFloat(1F, 0F).apply {
            duration = ROLL_BACK_DURATION
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                positionChildren(triggerOffset + rollBackOffset * animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (notify && triggerOffset != 0 && currentState == State.MOVING) {
                        currentState = State.LOADING
                        isRefreshing = true
                        offsetY = triggerOffset.toFloat()
                        onRefreshListener?.onRefresh()
                    } else {
                        currentState = State.IDLE
                        offsetY = 0f
                        progressView.visibility = View.GONE
                        progressView.isLoading = false
                        arrowView.visibility = View.VISIBLE
                    }
                }
            })
            start()
        }
    }

    private fun positionChildren(offset: Float) {
        topView.bringToFront()
        topView.y = topView.top + offset
        childView.y = childView.top + offset
    }

    fun setOnRefreshListener(listener: OnRefreshListener) {
        onRefreshListener = listener
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

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return (isEnabled && currentState != State.MOVING && !isRefreshing
                && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        offsetY = 0f
        mNestedScrollInProgress = true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (dy > 0 && offsetY > 0) {
            if (dy > offsetY) {
                consumed[1] = dy - offsetY.toInt()
                offsetY = 0f
            } else {
                offsetY -= dy.toFloat()
                consumed[1] = dy
            }
            move()
        }
        val parentConsumed: IntArray = mParentScrollConsumed
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0]
            consumed[1] += parentConsumed[1]
        }
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onStopNestedScroll(target: View) {
        mNestedScrollingParentHelper.onStopNestedScroll(target)
        mNestedScrollInProgress = false
        if (offsetY > 0) {
            notify = true
            currentState = State.MOVING
            stopRefreshing()
            offsetY = 0f
        }
        stopNestedScroll()
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow)
        val dy: Int = dyUnconsumed + mParentOffsetInWindow[1]
        if (dy < 0 && !canChildScrollUp()) {
            offsetY += abs(dy).toFloat()
            move()
        }
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mNestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mNestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mNestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?,
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
            dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    private fun canChildScrollUp(): Boolean {
        if (childView is ListView) {
            return ListViewCompat.canScrollList((childView as ListView?)!!, -1)
        }
        return childView.canScrollVertically(-1)
    }

    private companion object {
        const val STICKY_FACTOR = 0.66F
        const val STICKY_MULTIPLIER = 0.75F
        const val ROLL_BACK_DURATION = 300L
        const val ARROW_SIZE_DP: Float = 36F
        const val ARROW_SPEED: Long = 150
    }

    enum class State {
        IDLE,
        MOVING,
        LOADING
    }

    fun interface OnRefreshListener {

        fun onRefresh()

    }

}
