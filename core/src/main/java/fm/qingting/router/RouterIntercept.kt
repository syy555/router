package fm.qingting.router

import android.content.Context
import android.net.Uri
import android.os.Bundle

/**
 * Created by lee on 2018/4/10.
 */
abstract class RouterIntercept(val path: String) {

    abstract fun launch(context: Context,
                        uri: Uri,
                        taskId: String?,
                        options: Bundle?): Boolean

}

