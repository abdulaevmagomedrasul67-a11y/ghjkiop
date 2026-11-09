package kotlinx.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Serializable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class SerialName(val value: String)

interface KSerializer<T>

inline fun <reified T> kotlinx.serialization.json.Json.decodeFromString(string: String): T {
    return decodeFromString(T::class.java, string)
}

inline fun <reified T> kotlinx.serialization.json.Json.encodeToString(value: T): String {
    return encodeToString(T::class.java, value)
}
