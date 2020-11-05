package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class AppMetadataTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(icon = "icon"),
            expectedWithJson(includeNulls = true),
        ).map {
            Json.decodeFromString<AppMetadata>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson(), expectedWithJson(icon = "icon"))
            .map { Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        senderId: String = "senderId",
        name: String = "name",
        icon: String? = null,
        includeNulls: Boolean = false
    ): Pair<AppMetadata, String> {
        val json = if (icon != null || includeNulls) {
            """
                {
                    "senderId": "$senderId",
                    "name": "$name",
                    "icon": ${Json.encodeToString(icon)}
                }
            """.trimIndent()
        } else {
            """
                {
                    "senderId": "$senderId",
                    "name": "$name"
                }
            """.trimIndent()
        }

        return AppMetadata(senderId, name, icon) to json
    }
}