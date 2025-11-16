# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack application for digital signature management using OpenAPI firma API (firmadigitale.com). The application integrates with OpenAPI's OAuth-authenticated API to provide digital, electronic, and massive signature capabilities with SPID support.

## Architecture

### Monorepo Structure
```
/backend     - Spring Boot 3.2 REST API (Java 17)
/frontend    - React 18 + TypeScript + Vite SPA
```

### Backend Architecture (Spring Boot)

**Layer Structure:**
- `controller/` - REST endpoints exposing firma operations to frontend
- `service/` - Business logic and OpenAPI client integration
- `client/` - HTTP client for OpenAPI firma API calls
- `config/` - OAuth2, CORS, and OpenAPI configuration
- `dto/` - Data transfer objects for API requests/responses
- `entity/` - JPA entities for signature requests tracking
- `exception/` - Custom exceptions and global error handling

**Key Integration Points:**
- OAuth 2.0 token management for OpenAPI authentication
- Rate limiting: 10,000 requests/minute to OpenAPI
- Environment switching: sandbox (`test.ws.firmadigitale.com`) vs production (`ws.firmadigitale.com`)
- Proxy pattern: Backend acts as secure proxy to hide API credentials from frontend

### Frontend Architecture (React)

**Directory Structure:**
- `src/components/` - Reusable React components
- `src/pages/` - Route-level page components
- `src/services/` - API client (axios) for backend communication
- `src/store/` - Zustand state management
- `src/types/` - TypeScript type definitions
- `src/utils/` - Helper functions

**State Management:**
- Zustand for global state (auth, signature requests)
- React Router v6 for navigation

## Development Commands

### Backend (from `/backend`)
```bash
# Run application (port 8080)
mvn spring-boot:run

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Build JAR
mvn clean package

# Skip tests during build
mvn clean package -DskipTests
```

### Frontend (from `/frontend`)
```bash
# Install dependencies
npm install

# Run dev server (port 3000)
npm run dev

# Run tests
npm test

# Lint code
npm run lint

# Build for production
npm run build

# Preview production build
npm run preview
```

### Running Full Stack
Start backend first, then frontend. The Vite dev server proxies `/api/*` requests to `localhost:8080`.

## OpenAPI Integration

**Authentication Flow:**
1. Backend obtains OAuth token using client credentials
2. Token stored and refreshed automatically
3. All firma API requests include Bearer token

**Available Endpoints (via backend proxy):**
- `GET /firma_elettronica` - Retrieve signature status
- `POST /firma_elettronica` - Create signature request
- `GET /firma_elettronica_ui` - UI-based signature flow
- `POST /firma_elettronica_ui` - Create UI signature session
- `DELETE /firma_elettronica_ui` - Cancel UI session
- `GET /spid_personale` - SPID identity verification
- `GET /prodotti` - Available signature products
- `POST /richiesta` - Create signature request
- `PATCH /richiesta` - Update signature request

**Environment Configuration:**
- Credentials stored in backend `.env` file (never committed)
- Use `.env.example` as template
- Switch environments via `OPENAPI_ENVIRONMENT` variable

## Configuration

**Backend (application.yml):**
- Spring profile support for dev/prod
- OAuth credentials via environment variables
- CORS configured for localhost:3000 and localhost:5173
- H2 in-memory database for development
- OpenAPI base URLs configurable per environment

**Frontend (vite.config.ts):**
- Proxy configuration for `/api` routes
- Port 3000 default

## Database

H2 in-memory database for signature request tracking. Access console at:
`http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:mem:firmadb`
- Username: `sa`
- Password: (empty)

For production, replace H2 with PostgreSQL or MySQL in `pom.xml` and `application.yml`.

## Security Considerations

- OAuth credentials must remain in backend only
- Frontend communicates only with backend, never directly with OpenAPI
- CORS restricted to development origins
- Rate limiting should be implemented in backend service layer
- Token refresh logic required for long-running sessions

## API Documentation

- OpenAPI Firma Docs: https://console.openapi.com/apis/firma/documentation
- OAS Specification: https://console.openapi.com/oas/en/firmadigitale.openapi.json
- Use Swagger UI in docs for endpoint testing

## Common Development Workflows

**Adding New Signature Endpoint:**
1. Create DTO in `backend/dto/` matching OpenAPI spec
2. Add method in `backend/client/OpenApiFirmaClient.java`
3. Implement business logic in `backend/service/FirmaService.java`
4. Expose via controller in `backend/controller/FirmaController.java`
5. Create TypeScript types in `frontend/src/types/`
6. Add API call in `frontend/src/services/api.ts`
7. Implement UI component/page

**Switching Environments:**
Update `.env`:
```
OPENAPI_ENVIRONMENT=production  # or sandbox
```
Restart backend.
