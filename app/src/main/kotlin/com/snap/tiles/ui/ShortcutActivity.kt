package com.snap.tiles.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.executor.DynamicActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slotIndex = intent.getIntExtra("slot_index", -1)
        if (slotIndex == -1) {
            finish()
            return
        }

        lifecycleScope.launch {
            val config = TileConfigRepo.get(slotIndex)
            val actionIds = config.actionIds

            if (actionIds.isEmpty()) {
                finish()
                return@launch
            }

            val label = config.label.takeIf { it.isNotBlank() }
                ?: TileConfigRepo.customSlots.firstOrNull { it.slotIndex == slotIndex }
                    ?.let { getString(it.labelRes) }
                ?: "Slot $slotIndex"

            val targetOn = withContext(Dispatchers.IO) {
                val isOn = actionIds.all { DynamicActionExecutor.getState(it, contentResolver) }
                val target = !isOn
                DynamicActionExecutor.toggleAll(actionIds, contentResolver, applicationContext, target)
                target
            }

            val stateStr = if (targetOn) "ON" else "OFF"
            Toast.makeText(applicationContext, "$label: $stateStr", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
