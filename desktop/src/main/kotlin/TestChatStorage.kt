import com.tayrinn.aiadvent.data.local.ChatStorage
import com.tayrinn.aiadvent.data.model.ChatMessage
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🧪 Testing ChatStorage...")

    val chatStorage = ChatStorage("test_chat_data")

    // Создаем тестовые сообщения
    val message1 = ChatMessage(content = "Привет!", isUser = true)
    val message2 = ChatMessage(content = "Как дела?", isUser = false, isAgent1 = true)
    val message3 = ChatMessage(content = "Отлично!", isUser = true)

    println("📝 Saving messages...")
    chatStorage.saveMessage(message1)
    chatStorage.saveMessage(message2)
    chatStorage.saveMessage(message3)

    println("📖 Loading messages...")
    val loadedMessages = chatStorage.loadMessages()

    println("📊 Loaded ${loadedMessages.size} messages:")
    loadedMessages.forEach { msg ->
        println("  ${if (msg.isUser) "👤" else "🤖"} ${msg.content}")
    }

    // Тестируем последние сообщения
    val lastMessages = chatStorage.getLastMessages(3)
    println("📋 Last 3 messages:")
    lastMessages.forEach { msg ->
        println("  ${if (msg.isUser) "👤" else "🤖"} ${msg.content}")
    }

    println("✅ Test completed!")
}
