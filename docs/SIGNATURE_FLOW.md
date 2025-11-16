# Flusso di Firma Digitale - Documentazione Tecnica

Questa documentazione descrive i flussi di interazione tra Frontend, Backend e OpenAPI per la gestione delle firme digitali.

## Indice

1. [Architettura Generale](#architettura-generale)
2. [Flusso di Autenticazione OAuth](#flusso-di-autenticazione-oauth)
3. [Creazione Richiesta di Firma](#creazione-richiesta-di-firma)
4. [Recupero Lista Richieste](#recupero-lista-richieste)
5. [Dettagli Richiesta Singola](#dettagli-richiesta-singola)
6. [Download Documento Firmato](#download-documento-firmato)
7. [Recupero Audit Trail](#recupero-audit-trail)
8. [Gestione Errori](#gestione-errori)

---

## Architettura Generale

```mermaid
graph TB
    subgraph "Client Layer"
        UI[React UI]
        Store[Zustand Store]
        API[API Service]
    end

    subgraph "Backend Layer"
        Controller[Firma Controller]
        Service[Firma Service]
        Client[OpenAPI Client]
        Config[OAuth Config]
    end

    subgraph "External Services"
        OpenAPI[OpenAPI Firma API]
        OAuth[OAuth Server]
    end

    UI --> Store
    Store --> API
    API --> Controller
    Controller --> Service
    Service --> Client
    Client --> Config
    Client --> OpenAPI
    Client --> OAuth

    style UI fill:#e1f5ff
    style Store fill:#e1f5ff
    style API fill:#e1f5ff
    style Controller fill:#fff4e1
    style Service fill:#fff4e1
    style Client fill:#fff4e1
    style OpenAPI fill:#e8f5e9
    style OAuth fill:#e8f5e9
```

### Componenti Principali

#### Frontend (React)
- **UI Components**: Form e lista per gestire le richieste
- **Zustand Store**: State management centralizzato
- **API Service**: Client HTTP con Axios

#### Backend (Spring Boot)
- **Controller**: REST endpoints esposti al frontend
- **Service**: Business logic e validazione
- **Client**: Integrazione con OpenAPI
- **Config**: Gestione OAuth e configurazioni

#### OpenAPI
- **Firma API**: Servizi di firma elettronica
- **OAuth Server**: Autenticazione client credentials

---

## Flusso di Autenticazione OAuth

```mermaid
sequenceDiagram
    participant Backend as Backend<br/>(OpenAPI Client)
    participant OAuth as OAuth Server<br/>(OpenAPI)

    Note over Backend: Prima chiamata API<br/>Token non presente

    Backend->>OAuth: POST /oauth/token
    Note right of Backend: grant_type: client_credentials<br/>client_id: xxx<br/>client_secret: yyy

    OAuth-->>Backend: 200 OK
    Note left of OAuth: access_token: "abc123..."<br/>expires_in: 3600<br/>token_type: "Bearer"

    Note over Backend: Salva token in memoria<br/>con timestamp di scadenza

    rect rgb(200, 230, 255)
        Note over Backend: Token valido per 1 ora
        Backend->>Backend: Usa token per API calls
    end

    Note over Backend: Token scaduto o in scadenza

    Backend->>OAuth: POST /oauth/token<br/>(Refresh)
    OAuth-->>Backend: 200 OK (Nuovo token)

    Note over Backend: Aggiorna token in memoria
```

### Dettagli Implementazione

**File**: [OpenApiFirmaClient.java](../backend/src/main/java/com/openapi/firma/client/OpenApiFirmaClient.java)

```java
private void authenticate() {
    // Controlla se il token è ancora valido
    if (accessToken != null && tokenExpiry != null
        && Instant.now().isBefore(tokenExpiry)) {
        return; // Token ancora valido
    }

    // Richiedi nuovo token
    // grant_type: client_credentials
    // client_id e client_secret da configurazione
}
```

**Configurazione**: [application.yml](../backend/src/main/resources/application.yml)

```yaml
openapi:
  firma:
    oauth:
      token-url: https://test.ws.firmadigitale.com/oauth/token
      client-id: ${OPENAPI_CLIENT_ID}
      client-secret: ${OPENAPI_CLIENT_SECRET}
```

---

## Creazione Richiesta di Firma

```mermaid
sequenceDiagram
    actor User
    participant UI as React UI<br/>(FirmaRequestForm)
    participant Store as Zustand Store
    participant API as API Service
    participant Controller as Backend Controller
    participant Service as Firma Service
    participant Client as OpenAPI Client
    participant OpenAPI as OpenAPI Firma API

    User->>UI: Compila form<br/>- Titolo<br/>- PDF file<br/>- Firmatari
    User->>UI: Click "Crea Richiesta"

    UI->>UI: Valida form localmente
    UI->>API: fileToBase64(pdfFile)
    API-->>UI: Base64 string

    UI->>Store: createRequest(firmaRequest)
    activate Store
    Store->>Store: set loading = true

    Store->>API: POST /api/firma
    activate API
    Note right of API: FirmaRequest {<br/>  title, description,<br/>  filename, content (Base64),<br/>  members: [{<br/>    firstname, lastname,<br/>    email, phone,<br/>    signs: [{page, position}]<br/>  }]<br/>}

    API->>Controller: createSignatureRequest()
    activate Controller
    Controller->>Controller: Validazione Jakarta<br/>(@Valid @RequestBody)

    Controller->>Service: createSignatureRequest()
    activate Service
    Service->>Service: Validazione business logic<br/>- Content non vuoto<br/>- Almeno 1 member<br/>- Ogni member ha signs

    Service->>Client: createFirmaRequest()
    activate Client
    Client->>Client: authenticate()<br/>(Se token scaduto)
    Client->>OpenAPI: POST /firma_elettronica/base
    Note right of Client: Headers:<br/>  Authorization: Bearer {token}<br/>  Content-Type: application/json

    OpenAPI-->>Client: 200 OK
    Note left of OpenAPI: FirmaResponse {<br/>  id, filename,<br/>  status: "created",<br/>  members: [{<br/>    ..., signLink,<br/>    status: "pending"<br/>  }]<br/>}

    Client-->>Service: ApiResponse<FirmaResponse>
    deactivate Client
    Service-->>Controller: ApiResponse<FirmaResponse>
    deactivate Service
    Controller-->>API: 201 CREATED
    deactivate Controller
    API-->>Store: ApiResponse
    deactivate API

    Store->>Store: Aggiorna stato:<br/>- requests = [new, ...old]<br/>- currentRequest = new<br/>- loading = false

    Store-->>UI: FirmaResponse
    deactivate Store
    UI->>User: Mostra conferma<br/>"Richiesta creata!"
    UI->>UI: Reset form
```

### Request/Response Schema

**Frontend Request**:
```typescript
interface FirmaRequest {
  title?: string;
  description?: string;
  filename: string;
  content: string; // Base64 PDF
  members: Member[];
}

interface Member {
  firstname: string;
  lastname: string;
  email: string;
  phone: string; // +393331234567
  signs: SignPosition[];
}

interface SignPosition {
  page: number; // starts at 1
  position?: string; // "x1,y1,x2,y2"
}
```

**Backend Response**:
```json
{
  "data": {
    "id": "uuid-123-456",
    "filename": "contratto.pdf",
    "title": "Contratto di lavoro",
    "status": "created",
    "members": [
      {
        "firstname": "Mario",
        "lastname": "Rossi",
        "email": "mario@example.com",
        "phone": "+393331234567",
        "status": "pending",
        "signLink": "https://firma.openapi.com/sign/xxx",
        "createdAt": "2025-01-15T10:00:00",
        "updatedAt": "2025-01-15T10:00:00"
      }
    ]
  },
  "success": true,
  "message": "Richiesta creata con successo"
}
```

---

## Recupero Lista Richieste

```mermaid
sequenceDiagram
    actor User
    participant UI as React UI<br/>(FirmaRequestList)
    participant Store as Zustand Store
    participant API as API Service
    participant Controller as Backend Controller
    participant Service as Firma Service
    participant Client as OpenAPI Client
    participant OpenAPI as OpenAPI Firma API

    User->>UI: Apre tab "Le Mie Richieste"

    UI->>Store: useEffect(() => fetchAllRequests())
    activate Store
    Store->>Store: set loading = true

    Store->>API: GET /api/firma
    activate API
    API->>Controller: getAllSignatureRequests()
    activate Controller
    Controller->>Service: getAllSignatureRequests()
    activate Service
    Service->>Client: getAllFirmaRequests()
    activate Client

    Client->>Client: authenticate()
    Client->>OpenAPI: GET /firma_elettronica
    Note right of Client: Headers:<br/>  Authorization: Bearer {token}

    OpenAPI-->>Client: 200 OK
    Note left of OpenAPI: Array di FirmaResponse

    Client-->>Service: ApiResponse<List<FirmaResponse>>
    deactivate Client
    Service-->>Controller: ApiResponse
    deactivate Service
    Controller-->>API: 200 OK
    deactivate Controller
    API-->>Store: ApiResponse
    deactivate API

    Store->>Store: Aggiorna stato:<br/>- requests = data<br/>- loading = false

    Store-->>UI: requests[]
    deactivate Store

    UI->>UI: Renderizza lista<br/>con badge di stato
    UI->>User: Mostra richieste

    Note over UI: Ogni card mostra:<br/>- Titolo/Filename<br/>- Status badge<br/>- Lista firmatari<br/>- Link firma<br/>- Pulsante download
```

### Stati della Richiesta

```mermaid
stateDiagram-v2
    [*] --> created: Richiesta creata
    created --> started: Primo firmatario accede
    started --> started: Altri firmatari accedono
    started --> finished: Tutti hanno firmato
    started --> refused: Qualcuno rifiuta
    started --> expired: Timeout scaduto
    created --> expired: Nessuno accede entro timeout

    finished --> [*]: Download disponibile
    refused --> [*]
    expired --> [*]

    created --> error: Errore validazione
    started --> error: Errore processo
    error --> [*]

    note right of created
        Status: "created"
        Download: Non disponibile
        Sign links: Attivi
    end note

    note right of started
        Status: "started"
        Download: Non disponibile
        Alcuni firmatari completati
    end note

    note right of finished
        Status: "finished"
        Download: Disponibile
        Tutti hanno firmato
    end note
```

---

## Dettagli Richiesta Singola

```mermaid
sequenceDiagram
    actor User
    participant UI as React UI
    participant Store as Zustand Store
    participant API as API Service
    participant Controller as Backend Controller
    participant Service as Firma Service
    participant Client as OpenAPI Client
    participant OpenAPI as OpenAPI Firma API

    User->>UI: Click su "Dettagli"<br/>di una richiesta

    UI->>Store: fetchRequestById(id)
    activate Store
    Store->>Store: set loading = true

    Store->>API: GET /api/firma/{id}
    activate API
    API->>Controller: getSignatureRequestById(id)
    activate Controller
    Controller->>Service: getSignatureRequestById(id)
    activate Service

    Service->>Service: Valida ID non vuoto

    Service->>Client: getFirmaRequestById(id)
    activate Client
    Client->>Client: authenticate()
    Client->>OpenAPI: GET /firma_elettronica/{id}

    OpenAPI-->>Client: 200 OK
    Note left of OpenAPI: FirmaResponse aggiornata<br/>con stati recenti

    Client-->>Service: ApiResponse<FirmaResponse>
    deactivate Client
    Service-->>Controller: ApiResponse
    deactivate Service
    Controller-->>API: 200 OK
    deactivate Controller
    API-->>Store: ApiResponse
    deactivate API

    Store->>Store: Aggiorna stato:<br/>- currentRequest = data<br/>- Aggiorna in requests[]<br/>- loading = false

    Store-->>UI: FirmaResponse
    deactivate Store

    UI->>User: Mostra dettagli completi:<br/>- Stato aggiornato<br/>- Progressione firme<br/>- Link attivi
```

---

## Download Documento Firmato

```mermaid
sequenceDiagram
    actor User
    participant UI as React UI
    participant Store as Zustand Store
    participant API as API Service
    participant Controller as Backend Controller
    participant Service as Firma Service
    participant Client as OpenAPI Client
    participant OpenAPI as OpenAPI Firma API
    participant Browser as Browser

    User->>UI: Click "Scarica PDF Firmato"
    Note over UI: Pulsante visibile solo<br/>se status = "finished"

    UI->>Store: downloadDocument(id, filename)
    activate Store
    Store->>Store: set loading = true

    Store->>API: GET /api/firma/{id}/download
    activate API
    API->>Controller: downloadSignedDocument(id)
    activate Controller
    Controller->>Service: downloadSignedDocument(id)
    activate Service

    Service->>Client: downloadSignedDocument(id)
    activate Client
    Client->>Client: authenticate()
    Client->>OpenAPI: GET /firma_elettronica/{id}/download

    OpenAPI-->>Client: 200 OK
    Note left of OpenAPI: DownloadResponse {<br/>  content: "Base64...",<br/>  success: true<br/>}

    Client-->>Service: DownloadResponse
    deactivate Client
    Service-->>Controller: DownloadResponse
    deactivate Service
    Controller-->>API: 200 OK
    deactivate Controller
    API-->>Store: DownloadResponse
    deactivate API

    Store->>API: downloadBase64AsPdf(content, filename)
    activate API
    API->>API: Crea data URL:<br/>"data:application/pdf;base64,{content}"
    API->>Browser: Trigger download<br/>(createElement('a').click())
    Browser->>User: Salva file PDF
    deactivate API

    Store->>Store: set loading = false
    deactivate Store

    User->>User: Apre PDF firmato
```

### Formato Download Response

```json
{
  "content": "JVBERi0xLjQKJeLjz9MK...(Base64 PDF)",
  "success": true,
  "message": "Document downloaded successfully"
}
```

### Conversione Base64 → PDF

```typescript
// API Service Helper
downloadBase64AsPdf(base64Content: string, filename: string) {
  const linkSource = `data:application/pdf;base64,${base64Content}`;
  const downloadLink = document.createElement('a');
  downloadLink.href = linkSource;
  downloadLink.download = filename;
  downloadLink.click();
}
```

---

## Recupero Audit Trail

```mermaid
sequenceDiagram
    actor User
    participant UI as React UI
    participant API as API Service
    participant Controller as Backend Controller
    participant Service as Firma Service
    participant Client as OpenAPI Client
    participant OpenAPI as OpenAPI Firma API

    User->>UI: Click "Audit Trail"

    UI->>API: GET /api/firma/{id}/audit
    activate API
    API->>Controller: getAuditTrail(id)
    activate Controller
    Controller->>Service: getAuditTrail(id)
    activate Service
    Service->>Client: getAuditTrail(id)
    activate Client

    Client->>Client: authenticate()
    Client->>OpenAPI: GET /firma_elettronica/{id}/audit

    OpenAPI-->>Client: 200 OK
    Note left of OpenAPI: Audit Trail completo:<br/>- Timestamp eventi<br/>- Metodi autenticazione<br/>- Certificati digitali<br/>- SHA256 hashes<br/>- Revisioni documento

    Client-->>Service: ApiResponse<Object>
    deactivate Client
    Service-->>Controller: ApiResponse
    deactivate Service
    Controller-->>API: 200 OK
    deactivate Controller
    API-->>UI: Audit data
    deactivate API

    UI->>User: Mostra timeline audit
```

### Struttura Audit Trail

```json
{
  "data": {
    "requestId": "uuid-123",
    "events": [
      {
        "timestamp": "2025-01-15T10:00:00Z",
        "event": "Request created",
        "actor": "system"
      },
      {
        "timestamp": "2025-01-15T10:05:00Z",
        "event": "Email sent to signers",
        "details": "2 recipients"
      },
      {
        "timestamp": "2025-01-15T11:30:00Z",
        "event": "Document signed",
        "actor": "Mario Rossi",
        "authMethod": "OTP SMS",
        "certificateId": "cert-456"
      }
    ],
    "documentRevisions": [
      {
        "version": 1,
        "sha256": "abc123...",
        "timestamp": "2025-01-15T10:00:00Z"
      }
    ]
  },
  "success": true
}
```

---

## Gestione Errori

```mermaid
sequenceDiagram
    participant UI as Frontend
    participant API as API Service
    participant Backend as Backend
    participant OpenAPI as OpenAPI

    rect rgb(255, 220, 220)
        Note over UI,OpenAPI: Scenario 1: Errore di Validazione

        UI->>API: POST /api/firma<br/>(email non valida)
        API->>Backend: Request con dati invalidi
        Backend->>Backend: Validazione Jakarta<br/>(@Valid)
        Backend-->>API: 400 Bad Request
        Note left of Backend: {<br/>  success: false,<br/>  error: "Validation failed",<br/>  data: {<br/>    "email": "must be valid"<br/>  }<br/>}
        API-->>UI: ApiResponse (error)
        UI->>UI: Mostra errori su form
    end

    rect rgb(255, 240, 220)
        Note over UI,OpenAPI: Scenario 2: Errore OAuth

        UI->>API: POST /api/firma
        API->>Backend: Request valida
        Backend->>OpenAPI: POST /oauth/token<br/>(credenziali errate)
        OpenAPI-->>Backend: 401 Unauthorized
        Backend-->>API: 500 Internal Error
        Note left of Backend: {<br/>  success: false,<br/>  error: "Failed to authenticate"<br/>}
        API-->>UI: ApiResponse (error)
        UI->>UI: Mostra errore generico
    end

    rect rgb(255, 230, 230)
        Note over UI,OpenAPI: Scenario 3: Documento Non Pronto

        UI->>API: GET /api/firma/{id}/download
        API->>Backend: Request download
        Backend->>OpenAPI: GET /firma_elettronica/{id}/download
        OpenAPI-->>Backend: 404 Not Found
        Note left of OpenAPI: Documento non ancora firmato
        Backend-->>API: 404 Not Found
        API-->>UI: DownloadResponse (error)
        UI->>UI: Mostra messaggio:<br/>"Documento non ancora pronto"
    end

    rect rgb(240, 240, 255)
        Note over UI,OpenAPI: Scenario 4: Network Error

        UI->>API: GET /api/firma
        API->>Backend: Request timeout
        Backend-XBackend: Connection timeout
        Backend-->>API: 500 / Network Error
        API-->>UI: ApiResponse (error)
        UI->>UI: Mostra errore di connessione<br/>+ pulsante "Riprova"
    end
```

### Codici di Errore HTTP

| Codice | Significato | Gestione Frontend |
|--------|-------------|-------------------|
| 400 | Bad Request - Validazione fallita | Mostra errori specifici sui campi del form |
| 401 | Unauthorized - Token OAuth invalido | Log dell'errore, suggerisci verifica credenziali |
| 404 | Not Found - Risorsa non trovata | Messaggio "Richiesta non trovata" |
| 500 | Internal Server Error | Messaggio errore generico + retry |
| 503 | Service Unavailable | Messaggio "Servizio temporaneamente non disponibile" |

### Error Handling nel Frontend

```typescript
// Zustand Store
createRequest: async (request: FirmaRequest) => {
  set({ loading: true, error: null });
  try {
    const response = await firmaApi.createSignatureRequest(request);

    if (response.success && response.data) {
      // Success path
      set({
        requests: [response.data, ...state.requests],
        currentRequest: response.data,
        loading: false
      });
      return response.data;
    } else {
      // API returned error
      set({
        error: response.error || 'Failed to create request',
        loading: false
      });
      return null;
    }
  } catch (error: any) {
    // Network or unexpected error
    set({
      error: error.message || 'An unexpected error occurred',
      loading: false
    });
    return null;
  }
}
```

### Error Handling nel Backend

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(
        MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        return ResponseEntity.badRequest()
            .body(ApiResponse.builder()
                .success(false)
                .error("Validation failed")
                .data(errors)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity.status(500)
            .body(ApiResponse.builder()
                .success(false)
                .error("An unexpected error occurred")
                .build());
    }
}
```

---

## Riepilogo Endpoint

### Frontend → Backend

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/api/firma` | Crea nuova richiesta di firma |
| GET | `/api/firma` | Lista tutte le richieste |
| GET | `/api/firma/{id}` | Dettagli richiesta specifica |
| GET | `/api/firma/{id}/download` | Scarica documento firmato |
| GET | `/api/firma/{id}/audit` | Recupera audit trail |
| GET | `/api/firma/health` | Health check |

### Backend → OpenAPI

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/oauth/token` | Ottieni token OAuth |
| POST | `/firma_elettronica/base` | Crea richiesta firma |
| GET | `/firma_elettronica` | Lista richieste |
| GET | `/firma_elettronica/{id}` | Dettagli richiesta |
| GET | `/firma_elettronica/{id}/download` | Scarica PDF firmato |
| GET | `/firma_elettronica/{id}/audit` | Audit trail |

---

## Performance e Ottimizzazioni

### Caching del Token OAuth

```mermaid
graph LR
    A[Prima Richiesta] --> B{Token<br/>Presente?}
    B -->|No| C[Richiedi Token]
    C --> D[Salva in Memoria]
    D --> E[Usa Token]
    B -->|Sì| F{Token<br/>Valido?}
    F -->|Sì| E
    F -->|No| C
    E --> G[API Call]

    style C fill:#ffcccc
    style E fill:#ccffcc
    style F fill:#ffffcc
```

**Implementazione**: Il token viene salvato in memoria con timestamp di scadenza. Viene richiesto solo quando scade.

### Rate Limiting

OpenAPI ha un limite di **10,000 richieste/minuto**.

**Raccomandazioni**:
- Implementare debouncing sui pulsanti di refresh
- Cache client-side per liste recenti
- Polling intelligente con backoff esponenziale

### Ottimizzazioni Frontend

1. **Lazy Loading**: Carica lista richieste solo quando necessario
2. **Debouncing**: Sui pulsanti di refresh (min 1 secondo)
3. **Virtualization**: Per liste molto lunghe
4. **Memoization**: React.memo per componenti pesanti

---

## Note di Sicurezza

### Separazione delle Credenziali

```mermaid
graph TD
    A[Frontend] -->|NO credentials| B[Backend]
    B -->|OAuth credentials| C[OpenAPI]

    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C fill:#e8f5e9

    Note1[Le credenziali OAuth<br/>NON sono mai esposte<br/>al frontend]
    Note2[Il backend agisce<br/>come proxy sicuro]
```

### Best Practices

1. **Mai esporre credenziali OAuth nel frontend**
2. **Usare HTTPS in produzione**
3. **Validare sempre input lato server**
4. **Sanitizzare file PDF prima dell'upload**
5. **Implementare CORS restrittivo**
6. **Loggare tentativi di accesso non autorizzato**

---

## Riferimenti

- [OpenAPI Documentation](https://console.openapi.com/apis/firma/documentation)
- [CLAUDE.md](../CLAUDE.md) - Guida per sviluppatori
- [SETUP.md](../SETUP.md) - Istruzioni di setup
- [TROUBLESHOOTING.md](../TROUBLESHOOTING.md) - Risoluzione problemi
