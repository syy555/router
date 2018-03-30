package com.cat.router

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import fm.qingting.router.annotations.RouterPath

@RouterPath("/main")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
