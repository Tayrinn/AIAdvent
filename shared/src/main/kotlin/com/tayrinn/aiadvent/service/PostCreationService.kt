package com.tayrinn.aiadvent.service

import com.tayrinn.aiadvent.data.api.createOpenAIApiImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Модель сообщения для создания постов
 */
data class PostMessage(
    val content: String,
    val isUser: Boolean = false,
    val isAgent1: Boolean = false,
    val isError: Boolean = false
)

/**
 * Сервис для создания постов через 4 этапа ИИ-агентов
 */
class PostCreationService {
    
    private val openAIApi = createOpenAIApiImpl()
    
    /**
     * Этап 1: Создание концепции поста
     * Анализирует тему и ключевые мысли, определяет структуру и целевую аудиторию
     */
    suspend fun createConcept(
        topic: String,
        keyIdeas: String,
        onMessage: (PostMessage) -> Unit
    ): PostConcept = withContext(Dispatchers.IO) {
        val prompt = """
Ты - эксперт по контент-маркетингу. Твоя задача - создать концепцию поста на основе темы и ключевых идей.

ТЕМА: $topic

КЛЮЧЕВЫЕ ИДЕИ:
$keyIdeas

ЗАДАЧА:
1. Определи целевую аудиторию для этого поста
2. Выбери подходящий тон и стиль написания
3. Создай структуру поста (введение, основная часть, заключение)
4. Определи основное сообщение (call-to-action)
5. Предложи 3-5 хештегов

Ответь в следующем формате:
**🎯 Целевая аудитория:** [описание]
**📝 Тон и стиль:** [описание]
**📋 Структура поста:**
- Введение: [краткое описание]
- Основная часть: [краткое описание]
- Заключение: [краткое описание]
**💡 Основное сообщение:** [описание]
**#️⃣ Хештеги:** [список хештегов]
        """.trimIndent()

        try {
            val (response, _) = openAIApi.sendMessage(prompt, emptyList(), null)
            
            val message = PostMessage(
                content = "🎨 **ЭТАП 1: Создание концепции**\n\n$response",
                isUser = false,
                isAgent1 = true
            )
            onMessage(message)
            
            // Парсим ответ в структурированный объект
            PostConcept(
                topic = topic,
                keyIdeas = keyIdeas,
                targetAudience = extractSection(response, "Целевая аудитория"),
                toneAndStyle = extractSection(response, "Тон и стиль"),
                structure = extractSection(response, "Структура поста"),
                mainMessage = extractSection(response, "Основное сообщение"),
                hashtags = extractSection(response, "Хештеги"),
                fullResponse = response
            )
        } catch (e: Exception) {
            val errorMessage = PostMessage(
                content = "❌ Ошибка на этапе создания концепции: ${e.message}",
                isUser = false,
                isError = true
            )
            onMessage(errorMessage)
            throw e
        }
    }
    
    /**
     * Этап 2: Анализ и планирование контента
     * Детализирует структуру, планирует контент для каждой части
     */
    suspend fun analyzeAndPlan(
        concept: PostConcept,
        onMessage: (PostMessage) -> Unit
    ): PostPlan = withContext(Dispatchers.IO) {
        val prompt = """
Ты - стратег контента. На основе концепции поста создай детальный план написания.

КОНЦЕПЦИЯ ПОСТА:
${concept.fullResponse}

ЗАДАЧА:
1. Создай цепляющий заголовок (3-5 вариантов)
2. Напиши план введения с конкретными пунктами
3. Детализируй основную часть по пунктам
4. Спланируй заключение с призывом к действию
5. Определи ключевые слова для SEO
6. Предложи визуальные элементы (если нужны)

Ответь в следующем формате:
**📰 Варианты заголовков:**
1. [заголовок 1]
2. [заголовок 2]
3. [заголовок 3]

**🚀 План введения:**
- [пункт 1]
- [пункт 2]
- [пункт 3]

**📝 Детальная структура основной части:**
1. [раздел 1]: [описание содержания]
2. [раздел 2]: [описание содержания]
3. [раздел 3]: [описание содержания]

**🎯 План заключения:**
- [пункт 1]
- [пункт 2]
- [призыв к действию]

**🔍 Ключевые слова:** [список через запятую]
**🖼️ Визуальные элементы:** [предложения]
        """.trimIndent()

        try {
            val (response, _) = openAIApi.sendMessage(prompt, emptyList(), null)
            
            val message = PostMessage(
                content = "📊 **ЭТАП 2: Анализ и планирование**\n\n$response",
                isUser = false,
                isAgent1 = true
            )
            onMessage(message)
            
            PostPlan(
                concept = concept,
                headlines = extractSection(response, "Варианты заголовков"),
                introductionPlan = extractSection(response, "План введения"),
                mainContentPlan = extractSection(response, "Детальная структура основной части"),
                conclusionPlan = extractSection(response, "План заключения"),
                keywords = extractSection(response, "Ключевые слова"),
                visualElements = extractSection(response, "Визуальные элементы"),
                fullResponse = response
            )
        } catch (e: Exception) {
            val errorMessage = PostMessage(
                content = "❌ Ошибка на этапе анализа и планирования: ${e.message}",
                isUser = false,
                isError = true
            )
            onMessage(errorMessage)
            throw e
        }
    }
    
    /**
     * Этап 3: Создание черновика поста
     * Пишет полный черновик на основе плана
     */
    suspend fun createDraft(
        plan: PostPlan,
        onMessage: (PostMessage) -> Unit
    ): PostDraft = withContext(Dispatchers.IO) {
        val prompt = """
Ты - талантливый копирайтер. Напиши полный черновик поста на основе детального плана.

ПЛАН ПОСТА:
${plan.fullResponse}

ТРЕБОВАНИЯ:
1. Используй один из предложенных заголовков (выбери лучший)
2. Напиши увлекательное введение
3. Создай содержательную основную часть
4. Добавь убедительное заключение с призывом к действию
5. Сделай текст живым и интересным для читателя
6. Включи ключевые слова естественным образом
7. Добавь эмоциональность и примеры где уместно

ФОРМАТ ОТВЕТА:
**📰 ЗАГОЛОВОК:**
[выбранный заголовок]

**🚀 ВВЕДЕНИЕ:**
[текст введения]

**📝 ОСНОВНАЯ ЧАСТЬ:**
[полный текст основной части]

**🎯 ЗАКЛЮЧЕНИЕ:**
[текст заключения с призывом к действию]

**#️⃣ ХЕШТЕГИ:**
[финальные хештеги]
        """.trimIndent()

        try {
            val (response, _) = openAIApi.sendMessage(prompt, emptyList(), null)
            
            val message = PostMessage(
                content = "✍️ **ЭТАП 3: Создание черновика**\n\n$response",
                isUser = false,
                isAgent1 = true
            )
            onMessage(message)
            
            PostDraft(
                plan = plan,
                headline = extractSection(response, "ЗАГОЛОВОК"),
                introduction = extractSection(response, "ВВЕДЕНИЕ"),
                mainContent = extractSection(response, "ОСНОВНАЯ ЧАСТЬ"),
                conclusion = extractSection(response, "ЗАКЛЮЧЕНИЕ"),
                hashtags = extractSection(response, "ХЕШТЕГИ"),
                fullResponse = response
            )
        } catch (e: Exception) {
            val errorMessage = PostMessage(
                content = "❌ Ошибка на этапе создания черновика: ${e.message}",
                isUser = false,
                isError = true
            )
            onMessage(errorMessage)
            throw e
        }
    }
    
    /**
     * Этап 4: Финальная редактура и оптимизация
     * Полирует текст, проверяет на ошибки, оптимизирует для публикации
     */
    suspend fun finalizePost(
        draft: PostDraft,
        onMessage: (PostMessage) -> Unit
    ): FinalPost = withContext(Dispatchers.IO) {
        val prompt = """
Ты - главный редактор. Твоя задача - довести черновик поста до идеального состояния.

ЧЕРНОВИК ПОСТА:
${draft.fullResponse}

ЗАДАЧА:
1. Проверь и исправь грамматические ошибки
2. Улучши стилистику и читаемость
3. Оптимизируй длину предложений и абзацев
4. Убедись, что призыв к действию четкий и мотивирующий
5. Проверь, что хештеги актуальны и релевантны
6. Добавь финальные штрихи для максимального воздействия
7. Создай краткую версию поста (для соцсетей с ограничениями)

ФОРМАТ ОТВЕТА:
**🎯 ФИНАЛЬНАЯ ВЕРСИЯ ПОСТА:**

[полная отредактированная версия поста]

**📱 КРАТКАЯ ВЕРСИЯ (для соцсетей):**

[сокращенная версия]

**📈 РЕКОМЕНДАЦИИ ПО ПУБЛИКАЦИИ:**
- Лучшее время для публикации: [рекомендация]
- Подходящие платформы: [список]
- Дополнительные советы: [советы]

**✨ ИТОГОВАЯ ОЦЕНКА:**
Сильные стороны: [перечисление]
Ожидаемый эффект: [прогноз]
        """.trimIndent()

        try {
            val (response, _) = openAIApi.sendMessage(prompt, emptyList(), null)
            
            val message = PostMessage(
                content = "✨ **ЭТАП 4: Финальная редактура**\n\n$response",
                isUser = false,
                isAgent1 = true
            )
            onMessage(message)
            
            FinalPost(
                draft = draft,
                finalVersion = extractSection(response, "ФИНАЛЬНАЯ ВЕРСИЯ ПОСТА"),
                shortVersion = extractSection(response, "КРАТКАЯ ВЕРСИЯ"),
                publicationTips = extractSection(response, "РЕКОМЕНДАЦИИ ПО ПУБЛИКАЦИИ"),
                assessment = extractSection(response, "ИТОГОВАЯ ОЦЕНКА"),
                fullResponse = response
            )
        } catch (e: Exception) {
            val errorMessage = PostMessage(
                content = "❌ Ошибка на этапе финальной редактуры: ${e.message}",
                isUser = false,
                isError = true
            )
            onMessage(errorMessage)
            throw e
        }
    }
    
    /**
     * Полный процесс создания поста через все 4 этапа
     */
    suspend fun createFullPost(
        topic: String,
        keyIdeas: String,
        onMessage: (PostMessage) -> Unit
    ): FinalPost {
        onMessage(PostMessage(
            content = "🚀 **ЗАПУСК СОЗДАНИЯ ПОСТА**\n\nТема: $topic\nКлючевые идеи: $keyIdeas\n\nНачинаем 4-этапный процесс создания поста...",
            isUser = false,
            isAgent1 = true
        ))
        
        val concept = createConcept(topic, keyIdeas, onMessage)
        val plan = analyzeAndPlan(concept, onMessage)
        val draft = createDraft(plan, onMessage)
        val finalPost = finalizePost(draft, onMessage)
        
        onMessage(PostMessage(
            content = "🎉 **СОЗДАНИЕ ПОСТА ЗАВЕРШЕНО!**\n\nПост успешно создан и готов к публикации. Все этапы пройдены!",
            isUser = false,
            isAgent1 = true
        ))
        
        return finalPost
    }
    
    /**
     * Извлекает секцию из ответа ИИ по ключевому слову
     */
    private fun extractSection(response: String, sectionKey: String): String {
        val lines = response.lines()
        val startIndex = lines.indexOfFirst { it.contains(sectionKey, ignoreCase = true) }
        if (startIndex == -1) return ""
        
        val nextSectionIndex = lines.drop(startIndex + 1).indexOfFirst { 
            it.startsWith("**") && it.contains(":")
        }
        
        val endIndex = if (nextSectionIndex == -1) lines.size else startIndex + 1 + nextSectionIndex
        
        return lines.subList(startIndex + 1, endIndex)
            .joinToString("\n")
            .trim()
    }
}

/**
 * Модели данных для этапов создания поста
 */
@Serializable
data class PostConcept(
    val topic: String,
    val keyIdeas: String,
    val targetAudience: String,
    val toneAndStyle: String,
    val structure: String,
    val mainMessage: String,
    val hashtags: String,
    val fullResponse: String
)

@Serializable
data class PostPlan(
    val concept: PostConcept,
    val headlines: String,
    val introductionPlan: String,
    val mainContentPlan: String,
    val conclusionPlan: String,
    val keywords: String,
    val visualElements: String,
    val fullResponse: String
)

@Serializable
data class PostDraft(
    val plan: PostPlan,
    val headline: String,
    val introduction: String,
    val mainContent: String,
    val conclusion: String,
    val hashtags: String,
    val fullResponse: String
)

@Serializable
data class FinalPost(
    val draft: PostDraft,
    val finalVersion: String,
    val shortVersion: String,
    val publicationTips: String,
    val assessment: String,
    val fullResponse: String
)
