import com.tayrinn.aiadvent.data.model.HuggingFaceRequest
import com.tayrinn.aiadvent.data.model.HuggingFaceMessage
import kotlinx.serialization.json.Json

fun main() {
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    val request = HuggingFaceRequest(
        model = "deepseek-ai/DeepSeek-V3-0324",
        messages = listOf(
            HuggingFaceMessage(
                role = "system",
                content = "Ты - полезный AI помощник."
            ),
            HuggingFaceMessage(
                role = "user",
                content = "Привет!"
            )
        )
    )

    val jsonString = json.encodeToString(request)
    println("📋 Упрощенный JSON запрос к Hugging Face API:")
    println(jsonString)
}

