package com.airline.loyalty

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Duration

@ApplicationScoped
class AirlineLoyaltyService {

    @ConfigProperty(name = "openai.api.key")
    lateinit var openaiApiKey: String

    private val logger = Logger.getLogger(AirlineLoyaltyService::class.java)
    
    // Application-wide conversation memory with 20 message window
    private val chatMemory: ChatMemory = MessageWindowChatMemory.withMaxMessages(20)
    
    // Lazy initialization of the chat model
    private val chatModel: ChatModel by lazy { createChatModel() }

    private val systemPrompt = """
        You are an expert airline loyalty program assistant with conversation memory. You help travelers understand:
        - How to earn and redeem airline miles and points
        - Elite status benefits and requirements
        - Credit card partnerships and bonuses
        - Award booking strategies and sweet spots
        - Program transfers and partnerships
        - Travel tips for maximizing loyalty benefits
        
        IMPORTANT: Remember user details throughout our conversation. If a user tells you their name, preferences, 
        travel patterns, or any personal information, remember it and reference it in future responses. 
        This creates a more personalized and helpful experience.
        
        Provide helpful, accurate, and concise answers. If you're not sure about specific program details, 
        recommend checking the airline's official website for the most current information.
        
        Keep responses under 300 words and focus on practical advice. When appropriate, reference previous 
        parts of our conversation to provide more contextual and personalized assistance.
    """.trimIndent()

    fun processQuery(query: String): String {
        logger.debug("Processing query with conversation memory: $query")
        
        return try {
            // Add system message to memory if it's the first interaction
            if (chatMemory.messages().isEmpty()) {
                chatMemory.add(SystemMessage.from(systemPrompt))
                logger.debug("Added system message to conversation memory")
            }
            
            // Add user message to memory
            val userMessage = UserMessage.from(query)
            chatMemory.add(userMessage)
            
            // Generate response using all messages in memory
            val response = chatModel.chat(chatMemory.messages())
            val answer = response.aiMessage().text()
            
            // Add AI response to memory
            chatMemory.add(response.aiMessage())
            
            logger.debug("Generated response with memory context: ${answer.take(100)}...")
            logger.debug("Current memory size: ${chatMemory.messages().size} messages")
            
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

    private fun createChatModel(): ChatModel {
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
    
    // Method to get conversation status for UI
    fun getConversationStatus(): String {
        val messageCount = chatMemory.messages().size
        return when {
            messageCount <= 1 -> "New conversation"
            messageCount <= 10 -> "Active conversation ($messageCount messages)"
            else -> "Long conversation ($messageCount messages)"
        }
    }
}
