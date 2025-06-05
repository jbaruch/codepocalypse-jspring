# Phase 03: RAG Enhancement - Airline Loyalty Assistant

## Overview

Phase 03 enhances the Airline Loyalty Assistant with **Retrieval Augmented Generation (RAG)** capabilities using LangChain4j 1.0.1. The system now retrieves and processes real-time information from Delta SkyMiles and United MileagePlus loyalty program websites to provide more accurate, grounded responses.

## Key Features

### 🔍 RAG Implementation
- **Document Loading**: Asynchronously loads content from airline loyalty program websites
- **In-Memory Vector Store**: Uses `InMemoryEmbeddingStore` for fast document retrieval
- **Parallel Processing**: Loads multiple documents concurrently for improved performance
- **Semantic Search**: Finds relevant context using OpenAI embeddings (`text-embedding-ada-002`)

### 🧠 Enhanced AI Responses
- **Context-Aware Answers**: AI responses are grounded in official airline program information
- **Source Attribution**: Responses reference official airline sources when using retrieved content
- **Conversation Memory**: Maintains 20-message conversation window with personalized context
- **Relevance Scoring**: Only uses high-confidence retrieved content (minimum score: 0.7)

### 📊 Real-Time Status
- **RAG Status Indicator**: UI shows current RAG system status (Loading/Ready/Disabled)
- **Document Processing**: Loads content from Delta and United loyalty program pages
- **Error Handling**: Graceful fallback when documents cannot be loaded

## Architecture

### Core Components

1. **DocumentRAGService** (`DocumentRAGService.kt`)
   - Manages document loading and embedding storage
   - Handles asynchronous document processing
   - Provides semantic search functionality

2. **Enhanced AirlineLoyaltyService** (`AirlineLoyaltyService.kt`)
   - Integrates RAG context into AI responses
   - Maintains conversation memory with RAG-enhanced prompts
   - Provides status information for UI

3. **Data Sources**
   - Delta SkyMiles: `https://www.delta.com/us/en/skymiles/overview`
   - United MileagePlus: `https://www.united.com/ual/en/us/fly/mileageplus/overview`

### Technical Stack

- **LangChain4j 1.0.1**: Core AI and RAG functionality
- **Quarkus 3.6.0**: Application framework
- **OpenAI API**: Chat model (`gpt-3.5-turbo`) and embeddings (`text-embedding-ada-002`)
- **Kotlin**: Primary programming language
- **Maven**: Build tool

## Setup and Configuration

### Prerequisites

1. **Java 17+** installed
2. **OpenAI API Key** with access to:
   - Chat completions (GPT-3.5-turbo)
   - Embeddings (text-embedding-ada-002)

### Environment Variables

```bash
export OPENAI_API_KEY="your-openai-api-key-here"
```

### Running the Application

```bash
# Build and run in development mode
./mvnw quarkus:dev

# The application will be available at http://localhost:8080
```

## RAG System Behavior

### Document Loading Process

1. **Initialization**: RAG service starts loading documents on application startup
2. **Parallel Loading**: Documents are fetched concurrently from airline websites
3. **Text Processing**: Documents are split into 300-character segments with 30-character overlap
4. **Embedding Generation**: Each segment is converted to vector embeddings
5. **Storage**: Embeddings are stored in in-memory vector database

### Query Enhancement Process

1. **User Query**: User submits a question about airline loyalty programs
2. **Semantic Search**: Query is embedded and matched against stored document segments
3. **Context Retrieval**: Top 3 most relevant segments are retrieved (score ≥ 0.7)
4. **Prompt Enhancement**: User query is enhanced with retrieved context
5. **AI Response**: Enhanced prompt is sent to ChatGPT for grounded response

### Status Indicators

The UI displays RAG status in the conversation indicator:

- **"RAG: Loading documents..."** - System is fetching and processing airline websites
- **"RAG: Ready"** - Documents loaded successfully, RAG fully operational
- **"RAG: Disabled (no API key)"** - OpenAI API key not configured

## Example Usage

### Sample Queries That Benefit from RAG

1. **"What are the elite status tiers for Delta SkyMiles?"**
   - RAG retrieves current tier information from Delta's website
   - Response includes accurate, up-to-date tier names and requirements

2. **"How do I earn United miles?"**
   - System finds relevant earning methods from United's program page
   - Provides specific earning rates and bonus opportunities

3. **"What's the difference between Delta and United loyalty programs?"**
   - Retrieves information from both airline websites
   - Enables comprehensive comparison with current program details

### Conversation Flow Example

```
User: "What are the benefits of Delta Gold status?"
System: [Retrieves Delta SkyMiles content] → Enhanced response with current Gold benefits

User: "How does that compare to United Silver?"
System: [Uses conversation memory + retrieves United content] → Comparative analysis
```

## Technical Implementation Details

### Document Processing Pipeline

```kotlin
// 1. Load documents asynchronously
val futures = documentUrls.map { url ->
    CompletableFuture.supplyAsync({ loadDocumentFromUrl(url) }, executor)
}

// 2. Split into segments
val splitter = DocumentSplitters.recursive(300, 30)
val segments = splitter.split(document)

// 3. Generate embeddings
val embeddings = embeddingModel.embedAll(segments).content()

// 4. Store in vector database
embeddingStore.addAll(embeddings, segments)
```

### Semantic Search Implementation

```kotlin
// 1. Embed user query
val queryEmbedding = embeddingModel.embed(query).content()

// 2. Search for similar content
val searchRequest = EmbeddingSearchRequest.builder()
    .queryEmbedding(queryEmbedding)
    .maxResults(3)
    .minScore(0.7)
    .build()

// 3. Retrieve relevant segments
val searchResult = embeddingStore.search(searchRequest)
```

## Performance Characteristics

- **Startup Time**: ~10-30 seconds for document loading (depends on network)
- **Query Response**: ~2-5 seconds (embedding + LLM processing)
- **Memory Usage**: ~50-100MB for in-memory vector store
- **Concurrent Users**: Supports multiple users sharing the same document store

## Error Handling

### Graceful Degradation

1. **No API Key**: RAG disabled, basic AI responses still available
2. **Document Load Failure**: System continues with available documents
3. **Network Issues**: Falls back to general AI knowledge without RAG context
4. **Rate Limiting**: Proper retry logic with exponential backoff

### Logging

- Document loading progress and errors
- RAG search performance metrics
- API call success/failure rates
- User query processing status

## Future Enhancements

### Potential Improvements

1. **Document Refresh**: Periodic updates of airline program information
2. **Additional Sources**: More airline loyalty programs (American, Southwest, etc.)
3. **Caching**: Persistent vector store for faster startup
4. **Filtering**: User-specific document filtering by preferred airlines
5. **Analytics**: Query success rates and document relevance metrics

## Troubleshooting

### Common Issues

1. **"RAG: Loading documents..." persists**
   - Check internet connectivity
   - Verify airline websites are accessible
   - Review application logs for specific errors

2. **Responses lack specific details**
   - Ensure OpenAI API key has embedding access
   - Check if documents loaded successfully
   - Verify minimum relevance score settings

3. **Slow response times**
   - Monitor OpenAI API rate limits
   - Check network latency to OpenAI services
   - Consider reducing document segment count

## Development Notes

### LangChain4j 1.0.1 Compatibility

- Uses core LangChain4j modules only (specialized RAG modules not yet published)
- Implements RAG manually using available embedding and document APIs
- Compatible with Quarkus 3.6.0 dependency injection

### Architecture Decisions

- **In-Memory Storage**: Chosen for simplicity and demo purposes
- **Async Loading**: Prevents blocking application startup
- **Minimal Dependencies**: Uses only published LangChain4j 1.0.1 artifacts
- **Error Resilience**: System remains functional even if RAG fails

---

**Phase 03 successfully enhances the Airline Loyalty Assistant with production-ready RAG capabilities while maintaining the simple, demo-focused architecture.**
