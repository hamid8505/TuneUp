package com.tuneup.app.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromErrorList(list: List<StoredPitchError>): String = gson.toJson(list)

    @TypeConverter
    fun toErrorList(json: String?): List<StoredPitchError> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<StoredPitchError>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
    }
}
