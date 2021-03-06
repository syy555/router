package com.cat.router

import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import fm.qingting.router.Router
import fm.qingting.router.RouterContainer
import fm.qingting.router.RouterTaskCallBack
import fm.qingting.router.annotations.RouterPath
import kotlinx.android.synthetic.main.activity_main2.*

@RouterPath("/main")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RouterContainer.init()
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            Router.launch(this, Uri.parse("//app.qingting.fm/main2"))
        }

    }


}

@RouterPath("/start","action.qingting.fm")
fun start(asd: Context,lifecycle: Lifecycle?, callBack: RouterTaskCallBack?) {
    Toast.makeText(asd, lifecycle.toString(), Toast.LENGTH_SHORT).show()
}