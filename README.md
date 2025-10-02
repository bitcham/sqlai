# SQL AI - Natural Language to Database Query

## Demo
![Image](https://github.com/user-attachments/assets/10d74c6d-6bce-45d2-a991-564525ce868d)
![Image](https://github.com/user-attachments/assets/9afc7be4-332d-43ba-add5-e340c8f98ff2)
![Image](https://github.com/user-attachments/assets/76271900-b824-4547-8e5a-eb90c3aab2de)

> Transform natural language questions into SQL queries and get instant results - built for non-technical users who need data but don't know SQL.

## 🎯 Key Features

- **Auto-execution UX**: Queries are automatically executed after generation - users see data first, SQL is available in collapsible section
- **Multi-AI Provider Support**: Claude API, OpenAI API, Gemini API - choose the best model for your use case
- **Smart Schema Integration**: Automatic database metadata extraction for accurate query generation
- **High Accuracy**: Prompt template + schema metadata increased accuracy from <1% to >95%
- **Performance Optimized**: Optimized prompt engineering to keep generation time at 4.22s (vs 6.5s without optimization and 3.0s baseline without prompting) while maintaining 95%+ accuracy
- **Query History**: Track and revisit past queries with execution status
- **Safety First**: SELECT-only validation, query timeout, row limits, dangerous keyword blocking

## 🚀 Performance Improvements

### Query Generation: Accuracy vs Speed Trade-off
**Complex query test** (100 sample records): "List the top 3 most popular products in each category based on total quantity sold, showing product name, category, total units sold, and what percentage of category sales each product represents"

<!-- Performance comparison images -->
**Without Prompting (Baseline):**
<img width="636" height="165" alt="Image" src="https://github.com/user-attachments/assets/629d3175-fd9a-43e5-94fc-df0fab41a98b" />
- **3.0s** - Fast but <1% accuracy

**With Prompting (Accuracy Focus):**
<img width="635" height="165" alt="Image" src="https://github.com/user-attachments/assets/ef350ddb-3b66-4750-bc0d-b317607105b4" />
- **6.5s** - Slower but 95%+ accuracy

**With Prompting + Optimization (Best Balance):**
<img width="640" height="176" alt="Image" src="https://github.com/user-attachments/assets/216d0206-b3a4-4faf-bd4c-30ffe727a6e5" />
- **4.22s** - 35% faster than prompting alone, maintains 95%+ accuracy

### Accuracy: <1% → 95%+
Prompt template engineering + schema metadata integration dramatically improved SQL generation accuracy.

## 🛠️ Tech Stack

**Backend**
- Kotlin 2.2.20 + Spring Boot 3.5.6
- Hexagonal Architecture
- Spring Data JPA (H2/MySQL/PostgreSQL)
- Apache HttpClient 5.2.1
- Kotest + MockK (187 tests, 100% passing)

**Frontend**
- React 19 + TypeScript
- Vite 7.1.7
- Tailwind CSS v3
- TanStack Query (React Query)
- Radix UI

## 📐 Architecture

```
backend/src/main/kotlin/com/sqlai/
├── domain/              # Pure business logic (9 JPA entities)
├── service/             # Application services (7 services)
│   ├── datasource/     # Database metadata sync
│   ├── ai/             # Query generation, prompt engineering
│   └── query/          # Execution & history
├── repository/         # Spring Data JPA (6 repositories)
├── provider/           # AI providers & database introspectors
├── controller/         # REST API
├── dto/               # Request/Response objects
├── config/            # Spring Boot configuration
└── exception/         # Custom exceptions & global handler
```

**Key Design Decisions:**
- Single entity approach (persistence + domain logic)
- Prompt templates in `.txt` files (version controlled, not DB)
- Schema format: Structured plain text for all AI models
- Configuration-based execution policies (application.yml)
- Environment variables for API keys (no hardcoded secrets)

## 🔌 API Endpoints

```
POST   /api/query/generate    # Generate SQL and auto-execute
GET    /api/query/history     # Retrieve recent queries (default: 20)
GET    /api/query/health      # Service health check
```

## 🚦 Getting Started

### Prerequisites
- Java 21
- Node.js 18+
- Database (H2/MySQL/PostgreSQL)
- AI API Keys: `CLAUDE_API_KEY`, `OPENAI_API_KEY`, `GEMINI_API_KEY`

### Backend Setup
```bash
cd backend
./gradlew bootRun
# Runs on http://localhost:8080
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:3000
```

### Configuration
Set environment variables in your shell or `.env`:
```bash
export CLAUDE_API_KEY=your_key_here
export OPENAI_API_KEY=your_key_here
export GEMINI_API_KEY=your_key_here
```

Backend configuration: `backend/sqlai/src/main/resources/application.yml`

## 📊 Project Statistics

- **51** Kotlin source files
- **18** test files (187 tests, 100% passing ✅)
- **15** TypeScript files
- **10** React components
- **Bundle size**: 97KB JS (gzipped), 3.5KB CSS (gzipped)

## 🎓 What I Learned

- **Prompt Engineering**: Template-based prompting with dynamic schema injection - achieved 95%+ accuracy and optimized latency to 4.22s(vs 6.5s without optimization)
- **Performance Optimization**: Measuring and improving query generation latency
- **Hexagonal Architecture**: Clean separation of domain logic from infrastructure
- **Spring Boot 3.5**: Modern Kotlin + Spring patterns with constructor injection
- **React 19**: React features with TypeScript and TanStack Query


## 📝 License

MIT License

