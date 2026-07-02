package com.tjlabs.tjjupitervm_demo_android

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMView

class VMHostActivity : Activity() {
    lateinit var vmView: TJJupiterVMView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        vmView = TJJupiterVMView(this)
        container.addView(vmView)
        setContentView(container)
    }
}
