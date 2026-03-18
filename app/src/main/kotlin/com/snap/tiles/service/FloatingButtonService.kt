package com.snap.tiles.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.snap.tiles.R
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.executor.DynamicActionExecutor
import com.snap.tiles.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingButtonService : Service() {

    companion object {
        private const val TAG = "FloatBtnService"
        private const val CHANNEL_ID = "float_btn_channel"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FloatingButtonService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingButtonService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var floatView: FrameLayout? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        startForeground(NOTIF_ID, buildNotification())
        showFloatingButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        floatView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "removeView: $e") }
        }
        floatView = null
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Floating Button", NotificationManager.IMPORTANCE_LOW)
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_icon)
            .setContentTitle(getString(R.string.float_notif_title))
            .setContentText(getString(R.string.float_notif_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val sizeDp = when (PrefsManager.getButtonSize()) {
            "SMALL" -> 48f
            "LARGE" -> 72f
            else -> 60f // MEDIUM
        }
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density + 0.5f).toInt()

        val layoutParams = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val (savedX, savedY) = PrefsManager.getFloatPosition()
            x = savedX
            y = savedY
        }

        val slot = PrefsManager.getControlledSlot()
        val iconRes = TileConfigRepo.customSlots.firstOrNull { it.slotIndex == slot }?.iconRes
            ?: R.drawable.ic_tile_icon

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(230, 26, 115, 232))
        }

        val container = FrameLayout(this).apply {
            background = bg
            elevation = 8f * density
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            val pad = sizePx / 4
            setPadding(pad, pad, pad, pad)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
        container.addView(icon, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var longPressHandled = false

        val longPressRunnable = Runnable {
            longPressHandled = true
            Log.d(TAG, "long press → open app")
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    longPressHandled = false
                    v.postDelayed(longPressRunnable, 500L)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isDragging && (abs(dx) > 8 || abs(dy) > 8)) {
                        isDragging = true
                        v.removeCallbacks(longPressRunnable)
                    }
                    if (isDragging) {
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(container, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(longPressRunnable)
                    if (isDragging) {
                        PrefsManager.saveFloatPosition(layoutParams.x, layoutParams.y)
                    } else if (!longPressHandled) {
                        toggleControlledSlot()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }

        floatView = container
        windowManager.addView(container, layoutParams)
        Log.d(TAG, "showFloatingButton: size=${sizeDp}dp, slot=$slot")
    }

    private fun toggleControlledSlot() {
        val slot = PrefsManager.getControlledSlot()
        val config = TileConfigRepo.get(slot)
        val actionIds = config.actionIds
        if (actionIds.isEmpty()) {
            Log.d(TAG, "toggleControlledSlot: slot $slot has no actions")
            return
        }
        Log.d(TAG, "toggleControlledSlot: slot=$slot, actions=$actionIds")
        scope.launch {
            val currentOn = actionIds.all { DynamicActionExecutor.getState(it, contentResolver) }
            DynamicActionExecutor.toggleAll(actionIds, contentResolver, applicationContext, !currentOn)
        }
    }
}
