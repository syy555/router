package fm.qingting.router.annotations


/**
 *  Created by lee on 2018/3/12.
 */

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RouterPath(val value: String = "")


@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RouterField(val value: String = "")