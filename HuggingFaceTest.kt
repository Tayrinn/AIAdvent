import com.tayrinn.aiadvent.data.api.OpenAIApiImplInternal
import com.tayrinn.aiadvent.data.model.OpenAIRequest
import com.tayrinn.aiadvent.data.model.OpenAIMessage
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🧪 Тестирование Hugging Face API интеграции")

    try {
        val api = OpenAIApiImplInternal()

        val request = OpenAIRequest(
            model = "deepseek-ai/DeepSeek-V3-0324",
            messages = listOf(
                OpenAIMessage(
                    role = "user",
                    content = "How many G in huggingface?"
                )
            ),
            maxCompletionTokens = 100
        )

        println("📤 Отправляем запрос к Hugging Face API...")
        val response = api.chatCompletion(request)

        println("📥 Получен ответ:")
        println("   ID: ${response.id}")
        println("   Model: ${response.model}")
        println("   Content: ${response.choices.firstOrNull()?.message?.content}")

        if (response.choices.isNotEmpty()) {
            val content = response.choices.first().message.content
            if (content.isNotBlank()) {
                println("✅ Тест прошел успешно! Получен ответ от Hugging Face API")
            } else {
                println("⚠️ Получен пустой ответ")
            }
        } else {
            println("❌ Ответ не содержит choices")
        }

    } catch (e: Exception) {
        println("❌ Ошибка при тестировании: ${e.message}")
        e.printStackTrace()
    }
}
