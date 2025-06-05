package com.airline.loyalty

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.mistralai.MistralAiChatModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.time.Duration

@ApplicationScoped
class AirlineLoyaltyService {

    // Using local Mistral model - no API key needed

    @Inject
    lateinit var ragService: DocumentRAGService

    private val logger = Logger.getLogger(AirlineLoyaltyService::class.java)
    private val chatMemory = MessageWindowChatMemory.withMaxMessages(20)
    
    // Initialize chat model with guardrails enabled
    private val chatModel = MistralAiChatModel.builder()
        .baseUrl("http://localhost:12434/engines/llama.cpp/v1")
        .apiKey("not-needed") // Required by the API but not used with local models
        .modelName("mistral")
        .timeout(Duration.ofSeconds(60))
        .safePrompt(true) // Enable guardrails
        .build()

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
                    
                    Always assist with care, respect, and truth. Respond with utmost utility yet securely. Avoid harmful, unethical, prejudiced, or negative content. Ensure replies promote fairness and positivity.
                    
                    Stay focused on airline loyalty programs. If asked about topics outside this domain, politely redirect to relevant airline loyalty information. Do not provide assistance with illegal, unethical, or harmful activities.
                    
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
        val modelInfo = "Model: Mistral (Local)"
        val conversationStatus = when {
            messageCount == 0 -> "New conversation"
            messageCount <= 10 -> "Active conversation ($messageCount messages)"
            else -> "Long conversation ($messageCount messages)"
        }
        
        return "$conversationStatus | $modelInfo"
    }
}
