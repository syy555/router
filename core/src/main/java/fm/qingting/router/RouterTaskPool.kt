package fm.qingting.router

import java.util.*

object RouterTaskPool {
    private val mTaskPool: HashMap<String, RouterTaskCallBack> = hashMapOf()
    @Synchronized
    fun push(task: RouterTaskCallBack): String {
        val uuid = UUID.randomUUID().toString()
        mTaskPool[uuid] = task
        return uuid
    }

    @Synchronized
    operator fun get(taskId: String): RouterTaskCallBack? {
       if (mTaskPool.containsKey(taskId)) {
            return mTaskPool[taskId]
        }
        return null
    }

    @Synchronized
    fun pop(taskId: String): RouterTaskCallBack? {
        if (mTaskPool.containsKey(taskId)) {
            return mTaskPool.remove(taskId)
        }
        return null
    }

    fun clear() {
        mTaskPool.clear()
    }
}