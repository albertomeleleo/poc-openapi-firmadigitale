# OpenAPI Digital Signature Application

Applicazione full-stack per la gestione di firme elettroniche tramite le API di OpenAPI (firmadigitale.com).

## Stack Tecnologico

- **Frontend**: React 18 + TypeScript + Vite
- **Backend**: Spring Boot 3.2 + Java 17
- **Autenticazione**: OAuth 2.0
- **Database**: H2 (development)

## Prerequisiti

- Java 17+
- Node.js 18+
- Maven 3.8+
- Account OpenAPI Console per credenziali API

## Setup Iniziale

1. Clona il repository
2. Copia `.env.example` in `.env` nel backend e inserisci le credenziali OpenAPI
3. Installa le dipendenze

## Avvio Applicazione

### Backend
```bash
cd backend
mvn spring-boot:run
```

Il backend sarà disponibile su `http://localhost:8080`

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Il frontend sarà disponibile su `http://localhost:3000`

## Documentazione API

- Sandbox: https://test.ws.firmadigitale.com
- Produzione: https://ws.firmadigitale.com
- Documentazione: https://console.openapi.com/apis/firma/documentation
- OAS Spec: https://console.openapi.com/oas/en/firmadigitale.openapi.json
