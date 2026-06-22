package com.indiewalkabout.moneymagic.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToIsoString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoStringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun stringListToJson(value: List<String>?): String? = value?.let(Json::encodeToString)

    @TypeConverter
    fun jsonToStringList(value: String?): List<String>? = value?.let(Json::decodeFromString)
}
