package fm.qingting.router

import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.net.Uri
import android.os.Bundle

/**
 * Created by lee on 2018/4/10.
 */
abstract class RouterIntercept(val path : String = "", host:String = "") {

    val url = host+path
    abstract fun launch(context: Context,
                        uri: Uri,
                        callBack: RouterTaskCallBack?,
                        options: Bundle?,lifecycle: Lifecycle?): Boolean

}

