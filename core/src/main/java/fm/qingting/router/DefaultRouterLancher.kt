package fm.qingting.router

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat

/**
 * Created by lee on 2018/4/11.
 */
class DefaultRouterLauncher : RouterLauncher {
    override fun launch(context: Context, uri: Uri, clazz: Class<*>, taskId: String?, options: Bundle?): Boolean {
        if (Activity::class.java.isAssignableFrom(clazz)) {
            val intent = Intent(context, clazz)
            if (context is Application) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intent.data = uri
            ActivityCompat.startActivity(context, intent, options)
            return true
        }
        return false
    }
}