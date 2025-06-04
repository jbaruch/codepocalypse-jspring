package com.airline.loyalty

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Duration

@ApplicationScoped
class AirlineLoyaltyService {

    @ConfigProperty(name = "openai.api.key")
    lateinit var openaiApiKey: String

    private val logger = Logger.getLogger(AirlineLoyaltyService::class.java)

    private val systemPrompt = """
        You are an expert airline loyalty program assistant. You help travelers understand:
        - How to earn and redeem airline miles and points
        - Elite status benefits and requirements
        - Credit card partnerships and bonuses
        - Award booking strategies and sweet spots
        - Program transfers and partnerships
        - Travel tips for maximizing loyalty benefits
        
        Provide helpful, accurate, and concise answers. If you're not sure about specific program details, 
        recommend checking the airline's official website for the most current information.
        
        Keep responses under 300 words and focus on practical advice.
    """.trimIndent()

    fun processQuery(query: String): String {
        logger.debug("Processing query with LangChain4j: $query")
        
        return try {
            val model = createChatModel()
            
            val systemMessage = SystemMessage.from(systemPrompt)
            val userMessage = UserMessage.from(query)
            
            val response = model.generate(listOf(systemMessage, userMessage))
            val answer = response.content().text()
            
            logger.debug("Generated response: ${answer.take(100)}...")
            answer
            
        } catch (e: Exception) {
            logger.error("Error generating response with LangChain4j", e)
            when {
                openaiApiKey.isBlank() -> {
                    "Please set the OPENAI_API_KEY environment variable to use the AI assistant."
                }
                e.message?.contains("api key", ignoreCase = true) == true -> {
                    "Invalid API key. Please check your OPENAI_API_KEY environment variable."
                }
                e.message?.contains("rate limit", ignoreCase = true) == true -> {
                    "Rate limit exceeded. Please try again in a moment."
                }
                else -> {
                    "I'm having trouble connecting to the AI service. Please try again later."
                }
            }
        }
    }

    private fun createChatModel(): ChatLanguageModel {
        if (openaiApiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key is not configured")
        }
        
        return OpenAiChatModel.builder()
            .apiKey(openaiApiKey)
            .modelName("gpt-3.5-turbo")
            .temperature(0.7)
            .timeout(Duration.ofSeconds(30))
            .maxRetries(2)
            .build()
    }
}
