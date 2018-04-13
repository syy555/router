package fm.qingting.router

import android.content.Context
import android.net.Uri
import android.os.Bundle
import java.util.*

/**
 * Created by lee on 2018/4/10.
 */
object Router {
    private var pathMap = HashMap<String, Class<*>>()
    private var mIntercepts = HashMap<String, RouterIntercept>()
    private val defaultRouterLauncher = DefaultRouterLauncher()
    var routerLauncher: RouterLauncher? = null

    fun init(map: Map<String, Class<*>>) {
        map.keys.forEach { it ->
            if (pathMap.containsKey(it)) {
                throw RuntimeException(String.format(Locale.getDefault(), "key:%s already exist",
                        it))
            }
        }
        pathMap.putAll(map)
    }

    fun register(routerIntercept: RouterIntercept) {
        if (mIntercepts.containsKey(routerIntercept.path)) {
            throw RuntimeException(String.format(Locale.getDefault(), "key:%s already exist",
                    routerIntercept.path))
        }
        mIntercepts.put(routerIntercept.path, routerIntercept)
    }

    fun launch(context: Context, uri: Uri, callBack: RouterTaskCallBack?, options: Bundle?): Boolean {
        if (pathMap == null) {
            throw IllegalArgumentException("init() method not invoke.")
        }
        val host = uri.host
        return if (host.equals(RouterHost.ACTION.toString(), true) || host.equals(RouterHost.APP.toString(), true)) {
            val clazz = pathMap[uri.path]
            if (clazz != null) {
                if (defaultRouterLauncher.launch(context, uri, clazz, callBack, options)) {
                    return true
                }
                val routerLauncher = routerLauncher
                if (routerLauncher != null && routerLauncher.launch(context, uri, clazz, callBack, options)) {
                    return true
                }
            }
            parseActionPath(context, uri, callBack, options)
        } else {
            false
        }
    }

    private fun parseActionPath(context: Context, uri: Uri, callBack: RouterTaskCallBack?, options: Bundle?): Boolean {
        val routerIntercept = mIntercepts[uri.path] ?: return false
        return routerIntercept.launch(context, uri, callBack, options)
    }

}


private enum class RouterHost constructor(private val value: String) {
    APP("app.qingting.fm"),
    ACTION("action.qingting.fm");

    override fun toString(): String {
        return value
    }
}


