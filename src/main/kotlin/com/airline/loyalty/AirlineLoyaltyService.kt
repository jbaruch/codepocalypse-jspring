package com.airline.loyalty

import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
class AirlineLoyaltyService {

    @Inject
    @ConfigProperty(name = "openai.api.key")
    lateinit var openaiApiKey: String

    @Inject
    lateinit var ragService: DocumentRAGService

    private val logger = Logger.getLogger(AirlineLoyaltyService::class.java)
    private val chatMemory = MessageWindowChatMemory.withMaxMessages(20)
    
    private val chatModel: ChatModel by lazy {
        OpenAiChatModel.builder()
            .apiKey(openaiApiKey)
            .modelName("gpt-3.5-turbo")
            .build()
    }

    fun processQuery(userQuery: String): String {
        return try {
            logger.info("Processing query: $userQuery")
            logger.info("RAG Service Status: ${ragService.getStatus()}")
            
            // Build enhanced query with RAG context
            val enhancedQuery = buildEnhancedQuery(userQuery)
            
            // Add system message only if this is the first interaction
            if (chatMemory.messages().isEmpty()) {
                val systemMessage = SystemMessage.from("""
                    You are an expert airline loyalty program assistant. You help users understand and maximize their benefits from airline loyalty programs, particularly Delta SkyMiles and United MileagePlus.
                    
                    Remember user information throughout our conversation and provide personalized recommendations. If a user mentions their status, travel patterns, or preferences, reference this information in future responses.
                    
                    Be helpful, accurate, and conversational. Provide specific actionable advice when possible.
                """.trimIndent())
                chatMemory.add(systemMessage)
            }
            
            // Add user message to memory
            val userMessage = UserMessage.from(enhancedQuery)
            chatMemory.add(userMessage)
            
            // Generate response
            val response = chatModel.chat(chatMemory.messages())
            
            // Add AI response to memory
            chatMemory.add(response.aiMessage())
            
            response.aiMessage().text()
            
        } catch (e: Exception) {
            logger.error("Error processing query: ${e.message}")
            "I apologize, but I'm having trouble processing your request right now. Please try again in a moment."
        }
    }
    
    private fun buildEnhancedQuery(userQuery: String): String {
        return if (ragService.isReady()) {
            val relevantContent = ragService.searchRelevantContent(userQuery)
            
            if (relevantContent.isNotEmpty()) {
                val contextSection = relevantContent.joinToString("\n\n") { "Context: $it" }
                """
                Based on the following airline loyalty program information:
                
                $contextSection
                
                User Question: $userQuery
                
                Please provide a helpful answer using the context information above.
                """.trimIndent()
            } else {
                "User Question: $userQuery\n\nNote: No specific program information was found for this query. Please provide general guidance."
            }
        } else {
            "User Question: $userQuery\n\nNote: Loyalty program database is currently loading. Please provide general guidance based on your knowledge."
        }
    }
    
    fun getConversationStatus(): String {
        val messageCount = chatMemory.messages().size
        return when {
            messageCount == 0 -> "New conversation"
            messageCount <= 10 -> "Active conversation ($messageCount messages)"
            else -> "Long conversation ($messageCount messages)"
        }
    }
}
