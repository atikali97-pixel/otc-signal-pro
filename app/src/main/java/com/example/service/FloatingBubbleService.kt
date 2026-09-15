package com.example.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.MainActivity
import com.example.R

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Settings.canDrawOverlays(this)) {
            setupFloatingView()
        } else {
            stopSelf()
        }
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // Simple programmatically constructed view for reliable inflation without custom xml layout issues
        val bubbleLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#E6090D16"))
            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E6090D16"))
                cornerRadius = 40f
                setStroke(3, android.graphics.Color.parseColor("#00E676"))
            }
            background = shape

            val icon = ImageView(this@FloatingBubbleService).apply {
                setImageResource(R.mipmap.ic_launcher)
                layoutParams = android.widget.LinearLayout.LayoutParams(80, 80)
            }
            val text = TextView(this@FloatingBubbleService).apply {
                text = " Lucifer 🎙️"
                setTextColor(android.graphics.Color.parseColor("#00E676"))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(12, 10, 0, 0)
            }
            addView(icon)
            addView(text)

            setOnClickListener {
                val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }

        // Drag listener
        bubbleLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(bubbleLayout, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingView = bubbleLayout
        try {
            windowManager?.addView(floatingView, params)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
