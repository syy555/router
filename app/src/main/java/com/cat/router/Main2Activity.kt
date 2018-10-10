package com.cat.router

import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import fm.qingting.router.Router
import fm.qingting.router.RouterTaskCallBack
import fm.qingting.router.annotations.RouterPath

import kotlinx.android.synthetic.main.activity_main2.*

@RouterPath("/main2")
class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Router.process(intent,this)
        setContentView(R.layout.activity_main2)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            Router.launch(this, Uri.parse("//app.qingting.fm/module1?title=123"),object:RouterTaskCallBack{
                override fun done(result: Bundle?) {
                    Toast.makeText(applicationContext,result?.getString("123"),Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

}
