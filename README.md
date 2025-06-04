# ✈️ Airline Loyalty Assistant

A minimal web application that helps users with questions about airline loyalty programs, miles, rewards, and travel benefits using AI-powered responses.

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- OpenAI API Key

### Setup

1. **Set your OpenAI API Key**:
   ```bash
   export OPENAI_API_KEY=your_openai_api_key_here
   ```

2. **Run the application**:
   ```bash
   ./mvnw quarkus:dev
   ```

3. **Open your browser** and navigate to:
   ```
   http://localhost:8080
   ```

That's it! The application is now running and ready to answer your airline loyalty questions.

## 🏗️ Architecture

This application follows a simple, demo-focused architecture:

- **Framework**: Quarkus 3.23.1 with Kotlin
- **AI Integration**: LangChain4j 1.0.1 with OpenAI
- **Templating**: Quarkus Qute for server-side rendering
- **Build Tool**: Maven

### Project Structure
```
src/
├── main/
│   ├── kotlin/com/airline/loyalty/
│   │   ├── AirlineLoyaltyController.kt    # Main web controller
│   │   └── AirlineLoyaltyService.kt       # AI service layer
│   └── resources/
│       ├── application.properties         # Configuration
│       └── templates/
│           └── AirlineLoyaltyController/
│               └── index.html             # Main UI template
```

## 🎯 Features

- **Simple Q&A Interface**: Clean web form for asking questions
- **AI-Powered Responses**: Uses OpenAI GPT-3.5-turbo for intelligent answers
- **Airline Loyalty Expertise**: Specialized prompts for airline miles and rewards
- **Error Handling**: Graceful handling of API errors and validation
- **Responsive Design**: Modern, mobile-friendly UI

## 💡 Example Questions

- "How do I earn elite status with American Airlines?"
- "What's the best way to redeem Chase Ultimate Rewards for flights?"
- "Which airline credit card offers the best benefits?"
- "How can I maximize my miles earning on international flights?"
- "What are the best airline transfer partners for hotel points?"

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `OPENAI_API_KEY` | Your OpenAI API key | Yes |

### Application Properties

Key configuration options in `application.properties`:

```properties
# Server port (default: 8080)
quarkus.http.port=8080

# OpenAI API key (set via environment variable)
openai.api.key=${OPENAI_API_KEY:}

# Logging level
quarkus.log.level=INFO
```

## 🛠️ Development

### Running in Development Mode
```bash
./mvnw quarkus:dev
```

This starts the application with:
- Hot reload enabled
- Development UI available at http://localhost:8080/q/dev/
- Automatic template reloading

### Building for Production
```bash
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## 🔧 Troubleshooting

### Common Issues

1. **"Please set the OPENAI_API_KEY environment variable"**
   - Make sure you've exported your OpenAI API key
   - Verify the key is valid and has sufficient credits

2. **"Rate limit exceeded"**
   - You've hit OpenAI's rate limits
   - Wait a moment and try again

3. **Application won't start**
   - Ensure Java 17+ is installed: `java -version`
   - Check Maven is available: `mvn -version`
   - Verify port 8080 is not in use

### Logs
The application logs all requests and errors to the console for easy debugging.

## 🎨 Customization

### Modifying the AI Prompt
Edit the `systemPrompt` in `AirlineLoyaltyService.kt` to customize the AI's behavior and expertise.

### Styling Changes
Update the CSS in `src/main/resources/templates/AirlineLoyaltyController/index.html` to modify the appearance.

### Adding Features
The architecture is designed for easy extension:
- Add new endpoints in `AirlineLoyaltyController.kt`
- Extend the service layer in `AirlineLoyaltyService.kt`
- Create new templates in the `templates/` directory

## 📝 Technical Notes

- Built with simplicity as the primary goal
- Uses server-side rendering to avoid client-side complexity
- Implements proper error handling and user feedback
- Follows Quarkus best practices for Kotlin development
- Designed to be demo-ready with minimal setup

## 🤝 Contributing

This is a demonstration application focused on simplicity and functionality. When making changes:

1. Maintain the simple architecture
2. Ensure zero-debugging deployment
3. Follow the existing patterns
4. Test with `./mvnw quarkus:dev`

## 📄 License

This project is for demonstration purposes.
