package fm.qingting.router.annotations


/**
 *  Created by lee on 2018/3/12.
 */

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RouterPath(val value: String = "")


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RouterParameter(val value: String = "")