@file:Suppress("unused")

package fm.qingting.router

import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.MainThread
import java.util.*
import kotlin.collections.HashSet

/**
 * Created by lee on 2018/4/10.
 */
object Router {
    const val TASK_NAME = "router_task_callback_id"
    private var methodUrlSet = HashSet<String>()
    private var urlMap = HashMap<String, Class<*>>()
    private var mIntercepts = HashMap<String, RouterIntercept>()
    private val defaultRouterLauncher = DefaultRouterLauncher()
    private var defaultRouteIntercept: RouterIntercept? = null
    private val methodRouteInterceptList: MutableList<RouterIntercept> = arrayListOf()
    private var launcherMap = HashMap<Class<*>, RouterLauncher>()
    var defaultHost = ""
    fun registerUrl(path: String, target: Class<*>) {
        if (urlMap.containsKey(path)) {
            throw RuntimeException(String.format(Locale.getDefault(), "key:%s already exist",
                    path))
        }
        urlMap[path] = target
    }

    /**
     * Analytical parameters. from [Uri] or from [Bundle]
     *
     * @param intent    [Intent]
     * @param declaring Corresponding object
     */
    fun process(intent: Intent, declaring: Any) {
        if (intent.data != null && Uri.EMPTY != intent.data) {
            RouterHelper.processUri(intent.data, declaring)
        }

        if (intent.extras != null) {
            RouterHelper.processBundle(intent.extras, declaring)
        }
    }

    fun registerLauncher(type: Class<*>, routerLauncher: RouterLauncher) {
        this.launcherMap[type] = routerLauncher
    }

    @JvmOverloads
    fun registerMethodIntercept(routerIntercept: RouterIntercept, urlKeys: Set<String> = HashSet()) {
        if (!routerIntercept.url.isEmpty()) {
            throw RuntimeException(String.format(Locale.getDefault(), "the url of frontIntercept must be empty, found:%s ",
                    routerIntercept.url))
        }
        if (!urlKeys.intersect(methodUrlSet).isEmpty()) {
            if (!urlKeys.isEmpty())
                throw RuntimeException(String.format(Locale.getDefault(), "the url of frontIntercept must be empty, found:%s ",
                        urlKeys.intersect(methodUrlSet).first().toString()))
        }
        methodUrlSet.addAll(urlKeys)
        methodRouteInterceptList.add(routerIntercept)
    }


    fun registerDefaultIntercept(routerIntercept: RouterIntercept) {
        if (defaultRouteIntercept != null) {
            throw RuntimeException("defaultRouteIntercept can only be set once")
        }
        if (!routerIntercept.url.isEmpty()) {
            throw RuntimeException(String.format(Locale.getDefault(), "the url of defaultIntercept must be empty, found:%s ",
                    routerIntercept.url))
        }
        defaultRouteIntercept = routerIntercept
    }


    fun registerIntercept(routerIntercept: RouterIntercept) {
        if (mIntercepts.containsKey(routerIntercept.url) || methodUrlSet.contains(routerIntercept.url)) {
            throw RuntimeException(String.format(Locale.getDefault(), "key:%s already exist",
                    routerIntercept.url))
        }
        mIntercepts[routerIntercept.url] = routerIntercept
    }

    fun unRegisterIntercept(routerIntercept: RouterIntercept) {
        if (mIntercepts.containsKey(routerIntercept.url) && mIntercepts[routerIntercept.url] == routerIntercept) {
            mIntercepts.remove(routerIntercept.url)

        } else {
            throw RuntimeException(String.format(Locale.getDefault(), "routerIntercept:%s already exist",
                    routerIntercept.url))
        }
    }


    /**
     * execute Router callback with taskId.
     * this method must be invoking in android MathThread.
     *
     * @param context context
     * @param uri   the uri that point to the target
     * @param callBack   callback which notify the sender
     * @param options   extra data need to pass to the target.
     */
    @MainThread
    @JvmOverloads
    fun launch(context: Context, uri: Uri, callBack: RouterTaskCallBack? = null, options: Bundle? = null, lifecycle: Lifecycle? = null): Boolean {


        if (parseActionPath(context, uri, callBack, options, lifecycle)) {
            return true
        }
        methodRouteInterceptList.forEach {
            if (it.launch(context, uri, callBack, options, lifecycle)) {
                return true
            }
        }
        val clazz = urlMap[uri.host + uri.path]
        if (clazz != null) {
            var finalUri = uri
            var taskId: String? = null
            if (callBack != null) {
                val pair = packTask(finalUri, callBack)
                taskId = pair.first
                finalUri = pair.second
            }
            launcherMap.keys.forEach {
                if (it.isAssignableFrom(clazz)) {
                    val launcher = launcherMap[it]
                    if (launcher != null && launcher.launch(context, finalUri, clazz, options)) {
                        return true
                    }
                }
            }
            if (defaultRouterLauncher.launch(context, finalUri, clazz, options)) {
                return true
            }
            val tempTaskId = taskId
            if (tempTaskId != null) {
                RouterTaskPool.pop(tempTaskId)
            }
        }
        val tempDefaultRouteIntercept = defaultRouteIntercept
        if (tempDefaultRouteIntercept != null && tempDefaultRouteIntercept.launch(context, uri, callBack, options, lifecycle)) {
            return true
        }

        return false
    }

    private fun parseActionPath(context: Context, uri: Uri, callBack: RouterTaskCallBack?, options: Bundle?, lifecycle: Lifecycle?): Boolean {
        val routerIntercept = mIntercepts[uri.host + uri.path] ?: return false
        return routerIntercept.launch(context, uri, callBack, options, lifecycle)
    }

    private fun packTask(uri: Uri, task: RouterTaskCallBack?): Pair<String?, Uri> {
        val taskId = if (task != null) RouterTaskPool.push(task) else null

        val swap = StringBuilder(uri.toString())
        val taskQuery = (if (swap.indexOf("?") == -1) "?" else "&") + ("$TASK_NAME=$taskId")
        val pos = swap.indexOf("#")
        if (pos == -1) {
            swap.append(taskQuery)
        } else {
            swap.insert(pos, taskQuery)
        }
        return Pair(taskId, Uri.parse(swap.toString()))
    }


    /**
     * execute Router callback with taskId.
     * this method must be invoking in android MathThread.
     *
     * @param taskId the callback id
     * @param data   demand return data.
     */
    @MainThread
    fun execute(taskId: String?, data: Bundle?) {
        if (taskId != null) {
            val task = RouterTaskPool.pop(taskId)
            task?.done(data)
        }
    }

    /**
     * direct pop the router callback with specific task id
     *
     * @param taskId the callback id
     */
    fun pop(taskId: String?):RouterTaskCallBack? {
        if (taskId != null) {
            return RouterTaskPool.pop(taskId)
        }
        return null
    }
}


private enum class RouterHost constructor(private val value: String) {
    APP("app.qingting.fm"),
    ACTION("action.qingting.fm");

    override fun toString(): String {
        return value
    }
}


