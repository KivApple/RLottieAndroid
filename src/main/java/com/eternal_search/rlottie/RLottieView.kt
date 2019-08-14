package com.eternal_search.rlottie

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

class RLottieView(context: Context, attrs: AttributeSet?, defStyleAttr: Int): View(context, attrs, defStyleAttr),
	Animatable {
	
	var rawResId: Int = -1
		set(value) {
			field = value
			loadDrawable()
		}
	var autoPlay: Boolean = true
		set(value) {
			field = value
			if (field) {
				drawable?.start()
				invalidate()
			}
		}
	var autoRepeat: Boolean = true
		set(value) {
			field = value
			drawable?.autoRepeat = true
			if (value) {
				drawable?.start()
				invalidate()
			}
		}
	private var drawable: RLottieDrawable? = null
	private var startAfterLoad: Boolean = false
	var progress: Float
		get() = drawable?.progress ?: 0.0f
		set(value) {
			drawable?.progress = value
		}
	
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
	
	constructor(context: Context) : this(context, null)
	
	init {
		val values = context.obtainStyledAttributes(attrs, R.styleable.RLottieView)
		rawResId = values.getResourceId(R.styleable.RLottieView_rawRes, -1)
		autoRepeat = values.getBoolean(R.styleable.RLottieView_autoRepeat, true)
		autoPlay = values.getBoolean(R.styleable.RLottieView_autoPlay, true)
		values.recycle()
	}
	
	private fun loadDrawable() {
		drawable?.recycle()
		drawable = null
		if (rawResId < 0) return
		drawable = RLottieDrawable(context, rawResId)
		drawable?.autoRepeat = autoRepeat
		drawable?.callback = object : Drawable.Callback {
			override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
				postDelayed(what, `when` - System.currentTimeMillis())
			}
			
			override fun unscheduleDrawable(who: Drawable, what: Runnable) {
				removeCallbacks(what)
			}
			
			override fun invalidateDrawable(who: Drawable) {
				invalidate()
			}
		}
		if (autoPlay || startAfterLoad) {
			drawable?.start()
		}
		invalidate()
	}
	
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		drawable?.setBounds(0, 0, width, height)
		drawable?.draw(canvas)
		if (drawable?.isRunning == true) {
			invalidate()
		}
	}
	
	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		if (drawable == null) {
			loadDrawable()
		}
	}
	
	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		drawable?.recycle()
		drawable = null
	}
	
	override fun start() {
		startAfterLoad = true
		drawable?.start()
	}
	
	override fun stop() {
		startAfterLoad = false
		autoPlay = false
		drawable?.stop()
	}
	
	override fun isRunning(): Boolean = drawable?.isRunning == true || startAfterLoad
}
