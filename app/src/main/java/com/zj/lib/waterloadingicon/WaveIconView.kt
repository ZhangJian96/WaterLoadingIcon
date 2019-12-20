package com.zj.lib.waterloadingicon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View


/**
 * Created by zj on 2019-12-20 in project WaterLoadingIcon.
 */
class WaveIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePath: Path = Path()
    private val mPaint: Paint = Paint()
    private var waveLength: Float = 0.toFloat() //水波宽度
    private var waveHeight: Float = 0.toFloat()   //一个波峰的宽度，正弦的半个周期
    private var waveY: Float = 0.toFloat()
    private var waveBitmap: Bitmap? = null

    private val iconDrawable: Drawable?

    private val TAG = "WaveIconView"

    private var distance = 0f
    private var distanceTemp = 0f


    private var width: Float = 0.toFloat()
    private var height: Float = 0.toFloat()

    private var waveCanvas: Canvas? =
        null   //新建一个Canvas用于把资源文件的waveBitmap和贝塞尔曲线的path组合成新的waveBitmap


    private var icon: Bitmap? = null

    private fun getBitmapFromDrawable(): Bitmap {
        if (icon == null) {
            val config = if (iconDrawable!!.opacity != PixelFormat.OPAQUE)
                Bitmap.Config.ARGB_8888
            else
                Bitmap.Config.RGB_565
            icon = Bitmap.createBitmap(width.toInt(), height.toInt(), config)
            //注意，下面三行代码要用到，否在在View或者surfaceView里的canvas.drawBitmap会看不到图
            val canvas = Canvas(icon!!)
            iconDrawable.setBounds(0, 0, width.toInt(), height.toInt())
            iconDrawable.draw(canvas)
        }

        return icon!!
    }

    private var handler: IncreaseHandler? = null

    init {
        mPaint.isAntiAlias = true

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveIconView)
        iconDrawable = typedArray.getDrawable(R.styleable.WaveIconView_iconDrawable)
        typedArray.recycle()
        if (iconDrawable == null) {
            try {
                throw NullPointerException("iconDrawable must be set")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        wavePath.reset()
        wavePath.moveTo(-distance, waveY)
        for (i in 0..2) {
            wavePath.rQuadTo(
                waveLength / 2,
                waveHeight * Math.pow(-1.0, i.toDouble()).toFloat(),
                waveLength,
                0f
            )
        }
        wavePath.lineTo(width, height)
        wavePath.lineTo(0f, height)
        wavePath.close()
        mPaint.reset()
        mPaint.isAntiAlias = true
        waveCanvas!!.drawPath(wavePath, mPaint)
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        //裁剪图片
        waveCanvas!!.drawBitmap(getBitmapFromDrawable(), 0f, 0f, mPaint)
        mPaint.reset()
        mPaint.isAntiAlias = true
        canvas.drawBitmap(waveBitmap!!, 0f, 0f, mPaint)

        distanceTemp += waveLength / 50f
        val residual = distanceTemp % waveLength
        distance =
            if ((distanceTemp / waveLength).toInt() and 1 == 1) waveLength - residual else residual
        waveY -= height / 100f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = getWidth().toFloat()
        height = getHeight().toFloat()
        waveY = height
        waveHeight = height * 0.12f
        waveLength = width * 2 / 3f
        waveBitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        waveCanvas = Canvas(waveBitmap!!)
        icon = null
        if (handler != null) {
            handler!!.sendEmptyMessageDelayed(INVALIDATE, 25)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (handler == null) {
            handler = IncreaseHandler()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler!!.removeCallbacksAndMessages(null)
        handler = null
    }

    internal inner class IncreaseHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                INVALIDATE -> {
                    Log.d(TAG, "waveY：$waveY")
                    if (waveY < -waveHeight) {
                        waveY = height
                        waveBitmap = Bitmap.createBitmap(
                            width.toInt(),
                            height.toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        waveCanvas = Canvas(waveBitmap!!)
                        Log.d(TAG, "reset waveBitmap")
                    }
                    invalidate()
                    sendEmptyMessageDelayed(INVALIDATE, 20)
                }
            }
        }
    }

    companion object {

        private const val INVALIDATE = 0X777
    }
}
