package fm.qingting.router

import android.content.Context
import android.net.Uri
import android.os.Bundle

/**
 * Created by lee on 2018/4/11.
 */
interface RouterLauncher {
    fun launch(context: Context, uri: Uri, clazz: Class<*>, options: Bundle?): Boolean
}
