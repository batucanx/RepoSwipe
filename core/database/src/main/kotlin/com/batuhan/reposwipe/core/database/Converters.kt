package com.batuhan.reposwipe.core.database

import androidx.room.TypeConverter

/** GitHub topic slugs are lowercase kebab-case identifiers — never contain a comma — so a plain
 * comma-join/split is safe and avoids pulling in a JSON serializer for one small list column. */
class Converters {
    @TypeConverter
    fun fromTopics(topics: List<String>): String = topics.joinToString(",")

    @TypeConverter
    fun toTopics(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
}
