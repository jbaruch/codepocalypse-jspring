package com.airline.loyalty

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.loader.UrlDocumentLoader
import dev.langchain4j.data.document.parser.TextDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingMatch
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.EmbeddingSearchResult
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

@ApplicationScoped
class DocumentRAGService {

    // Using local Mistral model - no API key needed

    private val logger = Logger.getLogger(DocumentRAGService::class.java)
    private val embeddingStore: EmbeddingStore<TextSegment> = InMemoryEmbeddingStore()
    private val isReady = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)
    
    private val embeddingModel = MistralAiEmbeddingModel.builder()
        .baseUrl("http://localhost:12434/engines/llama.cpp/v1")
        .apiKey("not-needed") // Required by the API but not used with local models
        .modelName("mistral")
        .timeout(Duration.ofSeconds(60))
        .build()

    @PostConstruct
    fun initialize() {
        logger.info("Initializing RAG service with local Mistral model")
        loadDocumentsAsync()
    }

    private fun loadDocumentsAsync() {
        if (isLoading.compareAndSet(false, true)) {
            // Create fallback documents immediately to ensure service is ready
            createFallbackDocuments()
            isReady.set(true)
            
            CompletableFuture.runAsync {
                try {
                    val documents = loadDocuments()
                    if (documents.isNotEmpty()) {
                        // Clear existing fallback and add real documents
                        embeddingStore.removeAll()
                        ingestDocuments(documents)
                    }
                } catch (e: Exception) {
                    logger.error("Error during document loading: ${e.message}")
                } finally {
                    isLoading.set(false)
                }
            }
        }
    }

    private fun loadDocuments(): List<Document> {
        val documents = mutableListOf<Document>()
        val urls = listOf(
            "https://www.delta.com/us/en/skymiles/medallion-program/how-to-qualify",
            "https://www.united.com/en/us/fly/mileageplus/whats-new.html"
        )

        urls.forEach { url ->
            try {
                val document = UrlDocumentLoader.load(url, TextDocumentParser())
                documents.add(document)
            } catch (e: Exception) {
                logger.warn("Failed to load document from $url: ${e.message}")
            }
        }

        return documents
    }

    private fun ingestDocuments(documents: List<Document>) {
        try {
            val splitter = DocumentSplitters.recursive(300, 30)
            val ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()

            ingestor.ingest(documents)
            
        } catch (e: Exception) {
            logger.error("Error during document ingestion: ${e.message}")
            throw e
        }
    }

    private fun createFallbackDocuments() {
        try {
            val fallbackContent = """
            Delta SkyMiles Program 2025:
            - Medallion Status levels: Silver, Gold, Platinum, Diamond
            - Silver Medallion: Earn through flights and spending with Delta
            - Gold Medallion: Higher tier with enhanced benefits
            - Platinum Medallion: Premium tier with priority services
            - Diamond Medallion: Top tier with maximum benefits
            - Earn miles through flights, credit cards, and partners
            - Miles don't expire for active members
            - Elite benefits include priority boarding, free checked bags, upgrades, bonus miles
            
            United MileagePlus Program 2025:
            - Premier Status levels: Silver, Gold, Platinum, 1K
            - Premier Silver 2025 Requirements: 5,000 PQP + 15 PQF OR 6,000 PQP only
            - Premier Gold 2025 Requirements: 8,000 PQP + 24 PQF OR 10,000 PQP only
            - Premier Platinum 2025 Requirements: 15,000 PQP + 36 PQF OR 18,000 PQP only
            - Premier 1K 2025 Requirements: 22,000 PQP + 54 PQF OR 28,000 PQP only
            - PQP = Premier Qualifying Points, PQF = Premier Qualifying Flights
            - Qualification period: January 1 - December 31, 2025 for 2026 status
            - Miles don't expire for active members
            - Elite benefits include priority services, upgrades, bonus miles, free checked bags
            - Must take minimum 4 United or United Express flights to qualify
            """.trimIndent()

            val fallbackDocument = Document.from(fallbackContent)
            ingestDocuments(listOf(fallbackDocument))
            
        } catch (e: Exception) {
            logger.error("Error creating fallback documents: ${e.message}")
        }
    }

    fun searchRelevantContent(query: String): List<String> {
        return if (isReady.get()) {
            try {
                val queryEmbedding = embeddingModel.embed(query).content()
                val searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(3)
                    .minScore(0.7)
                    .build()

                val searchResult = embeddingStore.search(searchRequest)
                searchResult.matches().map { it.embedded().text() }
                
            } catch (e: Exception) {
                logger.error("Error searching relevant content: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun isReady(): Boolean = isReady.get()

    fun getStatus(): String {
        return when {
            !isReady.get() && isLoading.get() -> "Loading..."
            isReady.get() -> "Ready"
            else -> "Disabled"
        }
    }
}
