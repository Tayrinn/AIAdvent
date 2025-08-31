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
        ),
        maxTokens = 100,
        temperature = 0.7,
        task = "text-generation"
    )

    val jsonString = json.encodeToString(request)
    println("📋 Пример JSON запроса к Hugging Face API:")
    println(jsonString)
}
