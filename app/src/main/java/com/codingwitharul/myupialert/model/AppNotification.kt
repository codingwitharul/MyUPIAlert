package com.codingwitharul.myupialert.model

import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject

@Keep
data class AppNotification(
    var id: String? = null,
    var packageName: String? = null,
    var title: String? = null,
    var text: String? = null,
    var timestamp: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["id"] = id ?: ""
        map["packageName"] = packageName ?: ""
        map["title"] = title ?: ""
        map["text"] = text ?: ""
        map["timestamp"] = timestamp
        return map
    }

    fun toJson(): JSONObject {
        return try {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("packageName", packageName)
            obj.put("title", title)
            obj.put("text", text)
            obj.put("timestamp", timestamp)
            obj
        } catch (e: Exception) {
            JSONObject()
        }
    }

    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>?): AppNotification? {
            if (map == null) return null
            val id = (map["id"] ?: map["a"]) as? String
            val pkg = (map["packageName"] ?: map["b"]) as? String
            val title = (map["title"] ?: map["c"]) as? String
            val text = (map["text"] ?: map["d"]) as? String
            val tsObj = map["timestamp"] ?: map["e"]
            val timestamp = when (tsObj) {
                is Long -> tsObj
                is Number -> tsObj.toLong()
                else -> 0L
            }
            if (id == null && pkg == null && title == null && text == null) {
                return null
            }
            return AppNotification(
                id = id ?: "",
                packageName = pkg ?: "",
                title = title ?: "",
                text = text ?: "",
                timestamp = timestamp
            )
        }

        @JvmStatic
        fun fromJson(obj: JSONObject): AppNotification? {
            return try {
                AppNotification(
                    id = obj.optString("id", ""),
                    packageName = obj.optString("packageName", ""),
                    title = obj.optString("title", ""),
                    text = obj.optString("text", ""),
                    timestamp = obj.optLong("timestamp", 0L)
                )
            } catch (e: Exception) {
                null
            }
        }

        @JvmStatic
        fun toJsonArray(list: List<AppNotification?>): String {
            val arr = JSONArray()
            for (n in list) {
                if (n != null) arr.put(n.toJson())
            }
            return arr.toString()
        }

        @JvmStatic
        fun fromJsonArray(jsonStr: String?): List<AppNotification> {
            val list = ArrayList<AppNotification>()
            if (jsonStr.isNullOrEmpty()) return list
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val n = fromJson(arr.getJSONObject(i))
                    if (n != null) list.add(n)
                }
            } catch (ignored: Exception) {}
            return list
        }
    }
}
