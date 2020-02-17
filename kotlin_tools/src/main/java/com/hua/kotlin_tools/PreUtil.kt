package com.hua.kotlin_tools

import android.content.Context
import android.preference.PreferenceManager
import java.security.AccessController.getContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019/6/16 4:31 PM
 */
object PrefUtil {

    @JvmStatic
    fun put(key: String, value: String, context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        sp.edit().putString(key, value).apply()
    }

    @JvmStatic
    fun put(key: String, value: Boolean, context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        sp.edit().putBoolean(key, value).apply()
    }

    @JvmStatic
    fun put(key: String, value: Long, context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        sp.edit().putLong(key, value).apply()
    }

    @JvmStatic
    fun put(key: String, value: Int, context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        sp.edit().putInt(key, value).apply()
    }

    @JvmStatic
    fun getString(key: String, defaultValue: String? = null, context: Context): String {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        return sp.getString(key, defaultValue) ?: ""
    }

    @JvmStatic
    fun getLong(key: String, context: Context): Long {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        return sp.getLong(key, -1L)
    }

    @JvmStatic
    fun getInt(key: String, defaultValue: Int = -1, context: Context): Int {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        return sp.getInt(key, defaultValue)
    }

    @JvmStatic
    fun getBoolean(key: String, defaultValue: Boolean = false, context: Context): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        return sp.getBoolean(key, defaultValue)
    }
}
