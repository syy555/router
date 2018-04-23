package fm.qingting.router

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.MainThread
import android.util.Log
import java.util.*

/**
 * Created by lee on 2018/4/10.
 */
object Router {
    const val TASK_ID = "router_task_callback_id"

    private var pathMap = HashMap<String, Class<*>>()
    private var mIntercepts = HashMap<String, RouterIntercept>()
    private val defaultRouterLauncher = DefaultRouterLauncher()
    private var launcherMap = HashMap<Class<*>, RouterLauncher>()
    fun init(map: Map<String, Class<*>>) {
        map.keys.forEach { it ->
            Log.d("123", it)
            if (pathMap.containsKey(it)) {
                throw RuntimeException(String.format(Locale.getDefault(), "key:%s already exist",
                        it))
            }
        }
        pathMap.putAll(map)
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
        launcherMap[type] = routerLauncher
    }


    fun registerIntercept(routerIntercept: RouterIntercept) {
        if (mIntercepts.containsKey(routerIntercept.path)) {
            throw RuntimeException(String.format(Locale.getDefault(), "key:%s already exist",
                    routerIntercept.path))
        }
        mIntercepts[routerIntercept.path] = routerIntercept
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
    fun launch(context: Context, uri: Uri, callBack: RouterTaskCallBack? = null, options: Bundle? = null): Boolean {
        var finalUri = uri
        val host = finalUri.host
        var taskId: String? = null
        if (callBack != null) {
            val pair = packTask(finalUri, callBack)
            taskId = pair.first
            finalUri = pair.second
        }
        return if (host.equals(RouterHost.ACTION.toString(), true) || host.equals(RouterHost.APP.toString(), true)) {
            val clazz = pathMap[finalUri.path]

            if (clazz != null) {
                launcherMap.keys.forEach {
                    if (it.isAssignableFrom(clazz)) {
                        val launcher = launcherMap[it]
                        if (launcher != null && launcher.launch(context, finalUri, clazz, taskId, options)) {
                            return true
                        }
                    }
                }
                if (defaultRouterLauncher.launch(context, finalUri, clazz, taskId, options)) {
                    return true
                }
            }
            parseActionPath(context, finalUri, taskId, options)
        } else {
            false
        }
    }

    private fun parseActionPath(context: Context, uri: Uri, taskId: String?, options: Bundle?): Boolean {
        val routerIntercept = mIntercepts[uri.path] ?: return false
        return routerIntercept.launch(context, uri, taskId, options)
    }

    private fun packTask(uri: Uri, task: RouterTaskCallBack?): Pair<String?, Uri> {
        val taskId = if (task != null) RouterTaskPool.push(task) else null

        val swap = StringBuilder(uri.toString())
        val taskQuery = (if (swap.indexOf("?") == -1) "?" else "&") + ("$TASK_ID=$taskId")
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
    fun execute(taskId: String?, data: Bundle) {
        val id = taskId
        if (id != null) {
            val task = RouterTaskPool.pop(taskId)
            task?.done(data)
        }
    }

}


private enum class RouterHost constructor(private val value: String) {
    APP("app.qingting.fm"),
    ACTION("action.qingting.fm");

    override fun toString(): String {
        return value
    }
}


