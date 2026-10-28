package com.codingwitharul.myupialert.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Keep
@Entity(tableName = "transactions")
data class PaymentTransaction(
    @PrimaryKey
    var id: String = "",
    var amount: String? = null,
    var type: String? = null, // "credit" or "debit"
    var sender: String? = null,
    var body: String? = null,
    var timestamp: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["id"] = id
        map["amount"] = amount ?: ""
        map["type"] = type ?: ""
        map["sender"] = sender ?: ""
        map["body"] = body ?: ""
        map["timestamp"] = timestamp
        return map
    }

    @Throws(JSONException::class)
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("amount", amount)
        json.put("type", type)
        json.put("sender", sender)
        json.put("body", body)
        json.put("timestamp", timestamp)
        return json
    }

    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>?): PaymentTransaction? {
            if (map == null) return null
            val id = (map["id"] ?: map["a"]) as? String
            val amount = (map["amount"] ?: map["b"]) as? String
            val type = (map["type"] ?: map["c"]) as? String
            val sender = (map["sender"] ?: map["d"]) as? String
            val body = (map["body"] ?: map["e"]) as? String
            val tsObj = map["timestamp"] ?: map["f"] ?: map["e"]
            val timestamp = when (tsObj) {
                is Long -> tsObj
                is Number -> tsObj.toLong()
                else -> 0L
            }
            if (id == null && sender == null && amount == null && body == null) {
                return null
            }
            return PaymentTransaction(
                id = id ?: "",
                amount = amount ?: "",
                type = type ?: "",
                sender = sender ?: "",
                body = body ?: "",
                timestamp = timestamp
            )
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(json: JSONObject): PaymentTransaction {
            return PaymentTransaction(
                id = json.optString("id", ""),
                amount = json.optString("amount", ""),
                type = json.optString("type", ""),
                sender = json.optString("sender", ""),
                body = json.optString("body", ""),
                timestamp = json.optLong("timestamp", 0L)
            )
        }

        @JvmStatic
        fun toJsonArray(transactions: List<PaymentTransaction?>): String {
            val array = JSONArray()
            for (t in transactions) {
                try {
                    if (t != null) array.put(t.toJson())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            return array.toString()
        }

        @JvmStatic
        fun fromJsonArray(jsonString: String?): List<PaymentTransaction> {
            val list = ArrayList<PaymentTransaction>()
            if (jsonString.isNullOrEmpty()) return list
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val t = fromJson(array.getJSONObject(i))
                    list.add(t)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return list
        }
    }
}
