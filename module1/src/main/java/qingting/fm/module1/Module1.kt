package qingting.fm.module1

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import fm.qingting.router.Router
import fm.qingting.router.RouterLauncher
import fm.qingting.router.annotations.RouterField
import fm.qingting.router.annotations.RouterPath

import kotlinx.android.synthetic.main.activity_main.*



@RouterPath("/module1")
class Module1 : AppCompatActivity() {

    @field:RouterField("title")
    var title :String = "mldule1"

    @field:RouterField(Router.TASK_ID)
    var callbackid :String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.process(intent,this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setTitle(title)
        val bundle = Bundle()
        bundle.putString("123","456")
        Router.execute(callbackid,bundle)
        Router.registerLauncher(View::class.java,object:RouterLauncher{
            override fun launch(context: Context, uri: Uri, clazz: Class<*>, taskId: String?, options: Bundle?): Boolean {
                Toast.makeText(context, "abcdef", Toast.LENGTH_SHORT).show()
                return true
            }
        })
        fab.setOnClickListener { Router.launch(this, Uri.parse("//action.qingting.fm/abc"))
        }
    }

}
