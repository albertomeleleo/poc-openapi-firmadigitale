# Backend - Firma Digitale

## Avvio in Modalità Development

### 1. Configurazione

Il file `.env` nella root del backend contiene tutte le configurazioni necessarie per lo sviluppo.

```bash
# Copia il file .env.example (già fatto automaticamente)
cp .env.example .env
```

### 2. Profili Disponibili

#### Dev + Mock (Predefinito)
Usa dati mock senza chiamate reali alle API OpenAPI:

```bash
SPRING_PROFILES_ACTIVE=dev,mock
```

#### Dev + Real API
Usa le API reali di OpenAPI (richiede credenziali valide):

```bash
SPRING_PROFILES_ACTIVE=dev
OPENAPI_CLIENT_ID=your_real_client_id
OPENAPI_CLIENT_SECRET=your_real_client_secret
```

### 3. Avvio dell'applicazione

```bash
# Compila e avvia
mvn clean install
mvn spring-boot:run

# Oppure usa il JAR
mvn clean package
java -jar target/firma-backend-0.0.1-SNAPSHOT.jar
```

### 4. Verifica

L'applicazione sarà disponibile su:
- API: http://localhost:8080/api
- H2 Console: http://localhost:8080/api/h2-console

Test health check:
```bash
curl http://localhost:8080/api/firma/health
```

### 5. Variabili d'Ambiente

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev,mock` | Profili Spring Boot attivi |
| `SERVER_PORT` | `8080` | Porta del server |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:firmadb` | URL database H2 |
| `OPENAPI_CLIENT_ID` | - | Client ID OpenAPI (opzionale con mock) |
| `OPENAPI_CLIENT_SECRET` | - | Client Secret OpenAPI (opzionale con mock) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | Origins CORS permessi |

### 6. Database H2

Accedi alla console H2 per visualizzare i dati:

1. Vai su: http://localhost:8080/api/h2-console
2. JDBC URL: `jdbc:h2:mem:firmadb`
3. Username: `sa`
4. Password: (vuoto)

### 7. Logging

I log sono configurati per mostrare:
- SQL queries (JPA)
- HTTP requests (Spring Web)
- Debug custom (com.openapi.firma)

Modifica i livelli in `.env`:
```bash
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_OPENAPI_FIRMA=DEBUG
```

## Modalità Mock vs Real

### Mock Mode (Default)
- ✅ Nessuna chiamata API esterna
- ✅ Risposte immediate
- ✅ Dati simulati persistiti in memoria
- ✅ Ideale per sviluppo frontend
- ⚠️ Non richiede credenziali OpenAPI

### Real Mode
- ✅ Chiamate reali alle API OpenAPI
- ✅ OAuth automatico
- ✅ Firme elettroniche reali
- ⚠️ Richiede credenziali valide
- ⚠️ Potenziali costi associati

## Struttura Profili

```
application.yml          → Configurazione base
application-dev.yml      → Configurazione sviluppo
application-mock.yml     → Configurazione mock
application-prod.yml     → Configurazione produzione (TODO)
```

## Troubleshooting

### Porta 8080 già in uso
Cambia porta in `.env`:
```bash
SERVER_PORT=8081
```

### H2 Console non accessibile
Verifica in `.env`:
```bash
SPRING_H2_CONSOLE_ENABLED=true
```

### CORS errors dal frontend
Aggiungi l'origin in `.env`:
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:TUA_PORTA
```

### Mock client non attivo
Verifica il profilo in `.env`:
```bash
SPRING_PROFILES_ACTIVE=dev,mock
```

## Prossimi Passi

- [ ] Implementare persistenza su PostgreSQL per produzione
- [ ] Aggiungere autenticazione JWT per gli utenti
- [ ] Implementare webhook callbacks
- [ ] Aggiungere metrics e monitoring
- [ ] Containerizzare con Docker
