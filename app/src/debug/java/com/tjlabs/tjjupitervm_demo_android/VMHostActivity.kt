package com.tjlabs.tjjupitervm_demo_android

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMView

class VMHostActivity : Activity() {
    lateinit var container: FrameLayout
        private set

    lateinit var vmView: TJJupiterVMView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        vmView = TJJupiterVMView(this)
        setContentView(container)
    }

    override fun onDestroy() {
        if (::vmView.isInitialized) {
            vmView.stopService()
            vmView.closeFrame()
            vmView.release()
        }
        super.onDestroy()
    }
}
