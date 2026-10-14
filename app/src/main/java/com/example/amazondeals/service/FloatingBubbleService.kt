package com.example.amazondeals.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var dealUrl: String = "https://www.amazon.de/coupons"
    private var buyPrice: String = "18.60 €"
    private var resalePrice: String = "25.99 €"
    private var profitMargin: String = "+7.39 €"

    companion object {
        const val EXTRA_DEAL_URL = "extra_deal_url"
        const val EXTRA_BUY_PRICE = "extra_buy_price"
        const val EXTRA_RESALE_PRICE = "extra_resale_price"
        const val EXTRA_PROFIT_MARGIN = "extra_profit_margin"

        fun showBubble(
            context: Context,
            url: String,
            buyPrice: String,
            resalePrice: String,
            profitMargin: String
        ) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_DEAL_URL, url)
                putExtra(EXTRA_BUY_PRICE, buyPrice)
                putExtra(EXTRA_RESALE_PRICE, resalePrice)
                putExtra(EXTRA_PROFIT_MARGIN, profitMargin)
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            dealUrl = it.getStringExtra(EXTRA_DEAL_URL) ?: "https://www.amazon.de/coupons"
            buyPrice = it.getStringExtra(EXTRA_BUY_PRICE) ?: "18.60 €"
            resalePrice = it.getStringExtra(EXTRA_RESALE_PRICE) ?: "25.99 €"
            profitMargin = it.getStringExtra(EXTRA_PROFIT_MARGIN) ?: "+7.39 €"
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
            text = "🛒 Kauf: $buyPrice | 🏷️ Kleinanzeigen: $resalePrice | 💰 Marge: $profitMargin"
            textSize = 11f
            setBackgroundColor(0xFF1E293B.toInt())
            setTextColor(0xFF10B981.toInt())
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
                tv?.text = "🛒 Kauf: $buyPrice | 🏷️ Kleinanzeigen: $resalePrice | 💰 Marge: $profitMargin"
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
