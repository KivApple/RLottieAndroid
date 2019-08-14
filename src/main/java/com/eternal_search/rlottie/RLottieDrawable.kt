package com.eternal_search.rlottie

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import java.lang.IllegalArgumentException

class RLottieDrawable private constructor(): Drawable(), Animatable {
	private var nativePtr: Long = 0
	private var millisecondsPerFrame = 0
	private var frameCount = 1
	private var isRunning = false
	var autoRepeat = true
	private var currentFrameIndex = 0
	private var lastUpdateTime: Long = System.currentTimeMillis()
	private val paint = Paint()
	private var currentBitmap: Bitmap? = null
	private var nextBitmap: Bitmap? = null
	private var nextBitmapRendering = false
	private var isScheduled = false
	private val updateRunnable = Runnable {
		isScheduled = false
		invalidateSelf()
	}
	var progress: Float
		get() = currentFrameIndex.toFloat() / frameCount
		set(value) {
			if (value < 0.0f || value > 1.0f) {
				throw IllegalArgumentException("Progress must be in range 0..1")
			}
			val newFrameIndex = (value * frameCount).toInt()
			if (newFrameIndex != currentFrameIndex) {
				currentFrameIndex = newFrameIndex
				forceFinishNextBitmapRendering()
				finishNextBitmapRendering()
				swapBitmaps()
				lastUpdateTime = System.currentTimeMillis()
				invalidateSelf()
			}
		}
	
	constructor(context: Context, rawRes: Int, name: String): this() {
		context.resources.openRawResource(rawRes).use { stream ->
			val size = stream.available()
			val buffer = ByteArray(size)
			stream.read(buffer)
			val json = String(buffer)
			val params = IntArray(2)
			nativePtr = RLottieMethods.createFromJson(name, json, params)
			if (nativePtr == 0L) {
				throw IllegalStateException("Failed to load RLottie animation $name")
			}
			frameCount = params[0]
			millisecondsPerFrame = 1000 / params[1]
		}
	}
	
	constructor(context: Context, rawRes: Int): this(context, rawRes, "@$rawRes")
	
	override fun getOpacity(): Int = PixelFormat.TRANSPARENT
	
	override fun getIntrinsicWidth(): Int = 256
	
	override fun getIntrinsicHeight(): Int = 256
	
	private fun startNextBitmapRendering() {
		forceFinishNextBitmapRendering()
		RLottieMethods.renderFrame(nativePtr, currentFrameIndex, nextBitmap!!,
			nextBitmap!!.width, nextBitmap!!.height, nextBitmap!!.rowBytes)
		nextBitmapRendering = true
	}
	
	private fun finishNextBitmapRendering() {
		if (!nextBitmapRendering) {
			startNextBitmapRendering()
		}
		RLottieMethods.getFrame(nativePtr, nextBitmap!!)
		nextBitmapRendering = false
	}
	
	private fun forceFinishNextBitmapRendering() {
		if (nextBitmapRendering) {
			finishNextBitmapRendering()
		}
	}
	
	private fun swapBitmaps() {
		val tmp = nextBitmap
		nextBitmap = currentBitmap
		currentBitmap = tmp
	}
	
	override fun draw(canvas: Canvas) {
		if (nativePtr == 0L) {
			throw IllegalStateException("RLottieDrawable is recycled")
		}
		if (nextBitmap == null || nextBitmap!!.width != bounds.width() || nextBitmap!!.height != bounds.height()) {
			forceFinishNextBitmapRendering()
			nextBitmap?.recycle()
			nextBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
			currentBitmap?.recycle()
			currentBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
			finishNextBitmapRendering()
			swapBitmaps()
		}
		if (isRunning) {
			val now = System.currentTimeMillis()
			val deltaTime = now - lastUpdateTime
			lastUpdateTime = now
			val deltaFrames = deltaTime / millisecondsPerFrame
			if (deltaFrames > 0) {
				currentFrameIndex += deltaFrames.toInt()
				if (currentFrameIndex >= frameCount && !autoRepeat) {
					currentFrameIndex = 0
					stop()
				} else {
					currentFrameIndex %= frameCount
				}
				finishNextBitmapRendering()
				swapBitmaps()
				startNextBitmapRendering()
			}
		}
		canvas.save()
		canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
		canvas.drawBitmap(currentBitmap!!, 0.0f, 0.0f, paint)
		canvas.restore()
		if (isRunning && !isScheduled) {
			isScheduled = true
			scheduleSelf(updateRunnable, lastUpdateTime + millisecondsPerFrame)
		}
	}
	
	override fun start() {
		if (isRunning) return
		isRunning = true
		invalidateSelf()
	}
	
	override fun stop() {
		if (!isRunning) return
		isRunning = false
	}
	
	override fun isRunning(): Boolean = isRunning
	
	fun recycle() {
		unscheduleSelf(updateRunnable)
		if (nativePtr == 0L) return
		stop()
		forceFinishNextBitmapRendering()
		RLottieMethods.destroy(nativePtr)
		nativePtr = 0L
		currentBitmap?.recycle()
		nextBitmap?.recycle()
		currentBitmap = null
		nextBitmap = null
	}
	
	protected fun finalize() {
		recycle()
	}
	
	override fun setAlpha(p0: Int) {
	}
	
	override fun setColorFilter(p0: ColorFilter?) {
	}
}
