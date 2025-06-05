package com.airline.loyalty

import io.quarkus.qute.Template
import io.quarkus.qute.Location
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.jboss.logging.Logger

@Path("/")
@Produces(MediaType.TEXT_HTML)
class AirlineLoyaltyController {

    @Inject
    @Location("AirlineLoyaltyController.html")
    lateinit var template: Template

    @Inject
    lateinit var loyaltyService: AirlineLoyaltyService

    private val logger = Logger.getLogger(AirlineLoyaltyController::class.java)

    @GET
    fun index(): String {
        logger.info("Serving main page")
        return template.data("query", "")
            .data("answer", "")
            .data("error", "")
            .data("conversationStatus", loyaltyService.getConversationStatus())
            .render()
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun processQuery(@FormParam("query") query: String?): String {
        logger.info("Processing query: $query")
        
        return try {
            when {
                query.isNullOrBlank() -> {
                    logger.warn("Empty query received")
                    template.data("query", "")
                        .data("answer", "")
                        .data("error", "Please enter a question about airline loyalty programs.")
                        .data("conversationStatus", loyaltyService.getConversationStatus())
                        .render()
                }
                query.length > 500 -> {
                    logger.warn("Query too long: ${query.length} characters")
                    template.data("query", query)
                        .data("answer", "")
                        .data("error", "Question is too long. Please keep it under 500 characters.")
                        .data("conversationStatus", loyaltyService.getConversationStatus())
                        .render()
                }
                else -> {
                    val answer = loyaltyService.processQuery(query)
                    logger.info("Successfully processed query")
                    template.data("query", query)
                        .data("answer", answer)
                        .data("error", "")
                        .data("conversationStatus", loyaltyService.getConversationStatus())
                        .render()
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing query", e)
            template.data("query", query ?: "")
                .data("answer", "")
                .data("error", "Sorry, I encountered an error processing your question. Please try again.")
                .data("conversationStatus", loyaltyService.getConversationStatus())
                .render()
        }
    }
}
