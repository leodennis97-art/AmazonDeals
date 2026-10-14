package com.example.amazondeals.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.amazondeals.R

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var dealUrl: String = "https://www.amazon.de/coupons"
    private var dealPrice: String = "0.01 €"

    companion object {
        const val EXTRA_DEAL_URL = "extra_deal_url"
        const val EXTRA_DEAL_PRICE = "extra_deal_price"

        fun showBubble(context: Context, url: String, price: String) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_DEAL_URL, url)
                putExtra(EXTRA_DEAL_PRICE, price)
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            dealUrl = it.getStringExtra(EXTRA_DEAL_URL) ?: "https://www.amazon.de/coupons"
            dealPrice = it.getStringExtra(EXTRA_DEAL_PRICE) ?: "0.01 €"
        }

        if (bubbleView == null) {
            createFloatingBubble()
        } else {
            updateBubbleContent()
        }

        return START_STICKY
    }

    private fun createFloatingBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val container = FrameLayout(this)
        val textView = TextView(this).apply {
            text = "💰 $dealPrice (Günstiger!)"
            textSize = 12f
            setBackgroundColor(0xFF10B981.toInt())
            setTextColor(0xFF000000.toInt())
            setPadding(24, 16, 24, 16)
        }
        container.addView(textView)

        bubbleView = container

        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(bubbleView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            openDealInBrowser()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(bubbleView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBubbleContent() {
        bubbleView?.let { container ->
            if (container is FrameLayout && container.childCount > 0) {
                val tv = container.getChildAt(0) as? TextView
                tv?.text = "💰 $dealPrice (Günstiger!)"
            }
        }
    }

    private fun openDealInBrowser() {
        try {
            val uri = Uri.parse(dealUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            try {
                intent.setPackage("com.android.chrome")
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent.createChooser(intent, "Günstigeren Deal öffnen"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Link öffnen fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let {
            windowManager?.removeView(it)
        }
    }
}
