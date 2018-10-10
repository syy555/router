package fm.qingting.router

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import fm.qingting.router.annotations.RouterField
import java.io.Serializable
import java.lang.reflect.Field

object RouterHelper {
    private const val ROUTER = "RouterHelp"

    fun processIntent(intent: Intent?, declaring: Any?) {
        if (intent == null || declaring == null) {
            return
        }
        val clazz = declaring.javaClass
        val fields = clazz.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(RouterField::class.java)) {
                field.isAccessible = true
                val param = field.getAnnotation(RouterField::class.java)
                if (param != null) {
                    bundleBinding(param, intent.extras, field, declaring)
                    uriBinding(param, intent.data, field, declaring)
                }
            }
        }
    }

    fun processUri(uri: Uri?, declaring: Any?) {
        if (uri == null || declaring == null) {
            return
        }
        val clazz = declaring.javaClass
        val fields = clazz.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(RouterField::class.java)) {
                field.isAccessible = true
                val param = field.getAnnotation(RouterField::class.java)
                if (param != null)
                    uriBinding(param, uri, field, declaring)
            }
        }
    }

    fun processBundle(bundle: Bundle?, declaring: Any?) {
        if (bundle == null || declaring == null) {
            return
        }
        val clazz = declaring.javaClass
        val fields = clazz.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(RouterField::class.java)) {
                field.isAccessible = true
                val param = field.getAnnotation(RouterField::class.java)
                bundleBinding(param, bundle, field, declaring)
            }
        }
    }

    fun onSaveInstanceState(bundle: Bundle?, declaring: Any?) {
        if (bundle == null || declaring == null) {
            return
        }
        val clazz = declaring.javaClass
        val fields = clazz.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(RouterField::class.java)) {
                field.isAccessible = true
                val param = field.getAnnotation(RouterField::class.java)
                bundleSaving(param, bundle, field, declaring)
            }
        }
    }

    private fun uriBinding(parameter: RouterField, uri: Uri,
                           field: Field, declaring: Any) {

        try {
            val value = findParameter(parameter, uri, field)
            if (value != null) {
                //                value = TextUtils.isEmpty(value) ? "" : value;
                bindFieldValueWithString(value, field, declaring)
            }
        } catch (e: IllegalArgumentException) {
            if (BuildConfig.DEBUG)
                Log.d(ROUTER, ROUTER, e)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG)
                Log.d(ROUTER, ROUTER, e)
        }

    }

    private fun findParameter(parameter: RouterField, uri: Uri, field: Field): String? {
        val key = if (TextUtils.isEmpty(parameter.value)) field.name else parameter.value
        return uri.getQueryParameter(key)
    }

    private fun findParameters(parameter: RouterField, uri: Uri, field: Field): List<String> {
        val key = if (TextUtils.isEmpty(parameter.value)) field.name else parameter.value
        return uri.getQueryParameters(key)
    }


    @Throws(IllegalAccessException::class)
    private fun bindFieldValueWithString(value: String, field: Field, declaring: Any) {
        val type = field.type
        when (type) {
            RouterTaskCallBack::class.java -> field.set(declaring, Router.pop(value))
            Boolean::class.javaPrimitiveType -> field.setBoolean(declaring, java.lang.Boolean.valueOf(value))
            Boolean::class.java -> field.set(declaring, java.lang.Boolean.valueOf(value))
            Byte::class.javaPrimitiveType -> field.setByte(declaring, java.lang.Byte.valueOf(value)!!)
            Byte::class.java -> field.set(declaring, java.lang.Byte.valueOf(value))
            Short::class.javaPrimitiveType -> field.setShort(declaring, java.lang.Short.valueOf(value))
            Short::class.java -> field.set(declaring, java.lang.Short.valueOf(value))
            Int::class.javaPrimitiveType -> field.setInt(declaring, Integer.valueOf(value))
            Int::class.java -> field.set(declaring, Integer.valueOf(value))
            Long::class.javaPrimitiveType -> field.setLong(declaring, java.lang.Long.valueOf(value))
            Long::class.java -> field.set(declaring, java.lang.Long.valueOf(value))
            Float::class.javaPrimitiveType -> field.setFloat(declaring, java.lang.Float.valueOf(value)!!)
            Float::class.java -> field.set(declaring, java.lang.Float.valueOf(value))
            Double::class.javaPrimitiveType -> field.setDouble(declaring, java.lang.Double.valueOf(value)!!)
            Double::class.java -> field.set(declaring, java.lang.Double.valueOf(value))
            else -> field.set(declaring, value)
        }
    }


    private fun bundleBinding(parameter: RouterField, bundle: Bundle?,
                              field: Field, declaring: Any) {
        if (bundle == null) {
            return
        }
        val key = if (TextUtils.isEmpty(parameter.value)) field.name else parameter.value
        try {
            if (bundle.containsKey(key)) {
                if (field.type.isArray) {
                    bindFieldArrayValue(key, bundle, field, declaring)
                } else {
                    bindFieldSingleValue(key, bundle, field, declaring)
                }
            }
        } catch (e: IllegalAccessException) {
            Log.d(ROUTER, ROUTER, e)
        } catch (e: Exception) {
            Log.d(ROUTER, ROUTER, e)
        }

    }

    private fun bundleSaving(parameter: RouterField, bundle: Bundle,
                             field: Field, declaring: Any) {

        val key = if (TextUtils.isEmpty(parameter.value)) field.name else parameter.value
        try {

            if (field.type.isArray) {
                saveFieldArrayValue(key, bundle, field, declaring)
            } else {
                saveFieldSingleValue(key, bundle, field, declaring)
            }
        } catch (e: IllegalAccessException) {
            Log.d(ROUTER, ROUTER, e)
        }

    }

    /**
     * Binding value from [Bundle] with single type.
     *
     * @param key       field name.
     * @param bundle    save Object.
     * @param field     java [Field].
     * @param declaring the class declaring.
     * @throws IllegalAccessException throw Exception.
     */
    @Throws(IllegalAccessException::class)
    private fun bindFieldSingleValue(key: String, bundle: Bundle,
                                     field: Field, declaring: Any) {
        val type = field.type
        when {
            Boolean::class.javaPrimitiveType == type -> field.setBoolean(declaring, bundle.getBoolean(key))
            Boolean::class.java == type -> field.set(declaring, bundle.getBoolean(key))
            Byte::class.javaPrimitiveType == type -> field.setByte(declaring, bundle.getByte(key))
            Byte::class.java == type -> field.set(declaring, bundle.getByte(key))
            Char::class.javaPrimitiveType == type -> field.setChar(declaring, bundle.getChar(key))
            Char::class.java == type -> field.set(declaring, bundle.getChar(key))
            Short::class.javaPrimitiveType == type -> field.setShort(declaring, bundle.getShort(key))
            Short::class.java == type -> field.set(declaring, bundle.getShort(key))
            Int::class.javaPrimitiveType == type -> field.setInt(declaring, bundle.getInt(key))
            Int::class.java == type -> field.set(declaring, bundle.getInt(key))
            Long::class.javaPrimitiveType == type -> field.setLong(declaring, bundle.getLong(key))
            Long::class.java == type -> field.set(declaring, bundle.getLong(key))
            Float::class.javaPrimitiveType == type -> field.setFloat(declaring, bundle.getFloat(key))
            Float::class.java == type -> field.set(declaring, bundle.getFloat(key))
            Double::class.javaPrimitiveType == type -> field.setDouble(declaring, bundle.getDouble(key))
            Double::class.java == type -> field.set(declaring, bundle.getDouble(key))
            String::class.java == type -> field.set(declaring, bundle.getString(key))
            Bundle::class.java == type -> field.set(declaring, bundle.getBundle(key))
            CharSequence::class.java.isAssignableFrom(type) -> field.set(declaring, bundle.getCharSequence(key))
            IBinder::class.java.isAssignableFrom(type) && Build.VERSION.SDK_INT >= 18 -> field.set(declaring, bundle.getBinder(key))
            Parcelable::class.java.isAssignableFrom(type) -> field.set(declaring, bundle.getParcelable(key))
            Serializable::class.java.isAssignableFrom(type) -> field.set(declaring, bundle.getSerializable(key))
        }
    }

    /**
     * Binding value from [Bundle] with array type.
     *
     * @param key       field name.
     * @param bundle    save Object.
     * @param field     java [Field].
     * @param declaring the class declaring.
     * @throws IllegalAccessException throw Exception.
     */
    @Throws(IllegalAccessException::class)
    private fun bindFieldArrayValue(key: String, bundle: Bundle,
                                    field: Field, declaring: Any) {

        val type = field.type
        when {
            Boolean::class.javaPrimitiveType == type -> field.set(declaring, bundle.getBooleanArray(key))
            Byte::class.javaPrimitiveType == type -> field.set(declaring, bundle.getByteArray(key))
            Char::class.javaPrimitiveType == type -> field.set(declaring, bundle.getCharArray(key))
            Short::class.javaPrimitiveType == type -> field.set(declaring, bundle.getShortArray(key))
            Int::class.javaPrimitiveType == type -> field.set(declaring, bundle.getIntArray(key))
            Float::class.javaPrimitiveType == type -> field.set(declaring, bundle.getFloatArray(key))
            Double::class.javaPrimitiveType == type -> field.set(declaring, bundle.getDoubleArray(key))
            String::class.java == type -> field.set(declaring, bundle.getStringArray(key))
            CharSequence::class.java.isAssignableFrom(type) -> field.set(declaring, bundle.getCharSequenceArray(key))
            Parcelable::class.java.isAssignableFrom(type) -> field.set(declaring, bundle.getParcelableArray(key))
            Serializable::class.java.isAssignableFrom(type) -> field.set(declaring, bundle.getSerializable(key))
        }

    }


    /**
     * Save value to [Bundle] with single type.
     *
     * @param key       field name.
     * @param bundle    save Object.
     * @param field     java [Field].
     * @param declaring the class declaring.
     * @throws IllegalAccessException throw Exception.
     */
    @Throws(IllegalAccessException::class)
    private fun saveFieldSingleValue(key: String, bundle: Bundle,
                                     field: Field, declaring: Any) {

        val type = field.type
        when {
            Boolean::class.javaPrimitiveType == type -> bundle.putBoolean(key, field.getBoolean(declaring))
            Boolean::class.java == type -> bundle.putBoolean(key, field.get(declaring) as Boolean)
            Byte::class.javaPrimitiveType == type -> bundle.putByte(key, field.getByte(declaring))
            Byte::class.java == type -> bundle.putByte(key, field.get(declaring) as Byte)
            Char::class.javaPrimitiveType == type -> bundle.putChar(key, field.getChar(declaring))
            Char::class.java == type -> bundle.putChar(key, field.get(declaring) as Char)
            Short::class.javaPrimitiveType == type -> bundle.putShort(key, field.getShort(declaring))
            Short::class.java == type -> bundle.putShort(key, field.get(declaring) as Short)
            Int::class.javaPrimitiveType == type -> bundle.putInt(key, field.getInt(declaring))
            Int::class.java == type -> bundle.putInt(key, field.get(declaring) as Int)
            Long::class.javaPrimitiveType == type -> bundle.putLong(key, field.getLong(declaring))
            Long::class.java == type -> bundle.putLong(key, field.get(declaring) as Long)
            Float::class.javaPrimitiveType == type -> bundle.putFloat(key, field.getFloat(declaring))
            Float::class.java == type -> bundle.putFloat(key, field.get(declaring) as Float)
            Double::class.javaPrimitiveType == type -> bundle.putDouble(key, field.getDouble(declaring))
            Double::class.java == type -> bundle.putDouble(key, field.get(declaring) as Double)
            String::class.java == type -> bundle.putString(key, field.get(declaring) as String)
            Bundle::class.java == type -> bundle.putBundle(key, field.get(declaring) as Bundle)
            CharSequence::class.java.isAssignableFrom(type) -> bundle.putCharSequence(key, field.get(declaring) as CharSequence)
            IBinder::class.java.isAssignableFrom(type) && Build.VERSION.SDK_INT >= 18 -> bundle.putBinder(key, field.get(declaring) as IBinder)
            Parcelable::class.java.isAssignableFrom(type) -> bundle.putParcelable(key, field.get(declaring) as Parcelable)
            Serializable::class.java.isAssignableFrom(type) -> bundle.putSerializable(key, field.get(declaring) as Serializable)
        }
    }


    /**
     * Save value to [Bundle] with array type.
     *
     * @param key       field name.
     * @param bundle    save Object.
     * @param field     java [Field].
     * @param declaring the class declaring.
     * @throws IllegalAccessException throw Exception.
     */
    @Throws(IllegalAccessException::class)
    @Suppress("UNCHECKED_CAST")
    private fun saveFieldArrayValue(key: String, bundle: Bundle,
                                    field: Field, declaring: Any) {

        val type = field.type.componentType
        when {
            Boolean::class.javaPrimitiveType == type -> bundle.putBooleanArray(key, field.get(declaring) as BooleanArray)
            Byte::class.javaPrimitiveType == type -> bundle.putByteArray(key, field.get(declaring) as ByteArray)
            Char::class.javaPrimitiveType == type -> bundle.putCharArray(key, field.get(declaring) as CharArray)
            Short::class.javaPrimitiveType == type -> bundle.putShortArray(key, field.get(declaring) as ShortArray)
            Int::class.javaPrimitiveType == type -> bundle.putIntArray(key, field.get(declaring) as IntArray)
            Float::class.javaPrimitiveType == type -> bundle.putFloatArray(key, field.get(declaring) as FloatArray)
            Double::class.javaPrimitiveType == type -> bundle.putDoubleArray(key, field.get(declaring) as DoubleArray)
            String::class.java == type -> bundle.putStringArray(key, field.get(declaring) as Array<String>)
            CharSequence::class.java.isAssignableFrom(type) -> bundle.putCharSequenceArray(key, field.get(declaring) as Array<CharSequence>)
            Parcelable::class.java.isAssignableFrom(type) -> bundle.putParcelableArray(key, field.get(declaring) as Array<Parcelable>)
            Serializable::class.java.isAssignableFrom(type) -> bundle.putSerializable(key, field.get(declaring) as Array<Serializable>)
        }
    }

}