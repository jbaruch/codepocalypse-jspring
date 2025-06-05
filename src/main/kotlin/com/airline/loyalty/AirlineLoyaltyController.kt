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

    @Inject
    lateinit var ragService: DocumentRAGService

    private val logger = Logger.getLogger(AirlineLoyaltyController::class.java)

    @GET
    fun index(): String {
        return template.data("query", "")
            .data("answer", "")
            .data("error", "")
            .data("conversationStatus", loyaltyService.getConversationStatus())
            .data("ragStatus", ragService.getStatus())
            .render()
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun processQuery(@FormParam("query") query: String?): String {
        return try {
            when {
                query.isNullOrBlank() -> {
                    template.data("query", "")
                        .data("answer", "")
                        .data("error", "Please enter a question about airline loyalty programs.")
                        .data("conversationStatus", loyaltyService.getConversationStatus())
                        .data("ragStatus", ragService.getStatus())
                        .render()
                }
                query.length > 500 -> {
                    template.data("query", query)
                        .data("answer", "")
                        .data("error", "Question is too long. Please keep it under 500 characters.")
                        .data("conversationStatus", loyaltyService.getConversationStatus())
                        .data("ragStatus", ragService.getStatus())
                        .render()
                }
                else -> {
                    val answer = loyaltyService.processQuery(query)
                    template.data("query", query)
                        .data("answer", answer)
                        .data("error", "")
                        .data("conversationStatus", loyaltyService.getConversationStatus())
                        .data("ragStatus", ragService.getStatus())
                        .render()
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing query: ${e.message}")
            template.data("query", query ?: "")
                .data("answer", "")
                .data("error", "An error occurred while processing your request.")
                .data("conversationStatus", loyaltyService.getConversationStatus())
                .data("ragStatus", ragService.getStatus())
                .render()
        }
    }
}
