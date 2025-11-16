# Setup e Test dell'Applicazione

## Prerequisiti

1. **Java 17+** installato
2. **Maven 3.8+** installato
3. **Node.js 18+** e npm installati
4. **Credenziali OpenAPI**: Registrati su https://console.openapi.com per ottenere `client_id` e `client_secret`

## Configurazione Backend

### 1. Configura le credenziali

```bash
cd backend
cp .env.example .env
```

Modifica il file `.env` con le tue credenziali:

```env
OPENAPI_CLIENT_ID=il_tuo_client_id
OPENAPI_CLIENT_SECRET=il_tuo_client_secret
OPENAPI_ENVIRONMENT=sandbox
```

### 2. Avvia il backend

```bash
cd backend
mvn spring-boot:run
```

Il backend sarà disponibile su: `http://localhost:8080`

### 3. Verifica il backend

Testa l'endpoint di health:
```bash
curl http://localhost:8080/api/firma/health
```

Dovresti ricevere:
```json
{
  "data": "Firma service is running",
  "success": true,
  "message": "OK"
}
```

## Configurazione Frontend

### 1. Installa le dipendenze

```bash
cd frontend
npm install
```

### 2. Avvia il frontend

```bash
npm run dev
```

Il frontend sarà disponibile su: `http://localhost:3000`

## Test dell'Applicazione

### 1. Accedi all'applicazione

Apri il browser su `http://localhost:3000`

### 2. Crea una richiesta di firma

1. Clicca sulla tab "Nuova Richiesta"
2. Compila i campi:
   - **Titolo**: (opzionale) es. "Contratto di lavoro"
   - **Descrizione**: (opzionale) es. "Firma del contratto"
   - **Nome File**: es. "contratto.pdf"
   - **Documento PDF**: Carica un file PDF di test
3. Aggiungi i firmatari:
   - **Nome** e **Cognome**
   - **Email**: email valida del firmatario
   - **Telefono**: numero con prefisso internazionale (es. +393331234567)
   - **Posizioni Firma**:
     - Pagina: numero della pagina (inizia da 1)
     - Posizione: (opzionale) coordinate in pixel "x1,y1,x2,y2"
4. Puoi aggiungere più firmatari cliccando su "+ Aggiungi Firmatario"
5. Clicca su "Crea Richiesta di Firma"

### 3. Visualizza le richieste

1. Clicca sulla tab "Le Mie Richieste"
2. Vedrai tutte le richieste di firma create
3. Ogni richiesta mostra:
   - Stato (created, started, finished, etc.)
   - Informazioni sui firmatari
   - Link di firma per ogni firmatario

### 4. Scarica il documento firmato

Quando lo stato diventa "finished", puoi cliccare su "Scarica PDF Firmato" per scaricare il documento con le firme applicate.

## API Endpoints Disponibili

### Backend REST API

- `POST /api/firma` - Crea nuova richiesta di firma
- `GET /api/firma` - Lista tutte le richieste
- `GET /api/firma/{id}` - Dettagli richiesta specifica
- `GET /api/firma/{id}/download` - Scarica documento firmato
- `GET /api/firma/{id}/audit` - Ottieni audit trail
- `GET /api/firma/health` - Health check

## Esempio Payload

### Creare una richiesta di firma

```json
{
  "title": "Contratto di Lavoro",
  "description": "Firma del contratto di assunzione",
  "filename": "contratto_lavoro.pdf",
  "content": "JVBERi0xLjQKJeLjz9MK...", // Base64 PDF
  "members": [
    {
      "firstname": "Mario",
      "lastname": "Rossi",
      "email": "mario.rossi@example.com",
      "phone": "+393331234567",
      "signs": [
        {
          "page": 1,
          "position": "100,500,300,550"
        }
      ]
    }
  ]
}
```

## Troubleshooting

### Backend non si avvia

1. Verifica che Java 17+ sia installato: `java -version`
2. Verifica che Maven sia installato: `mvn -version`
3. Controlla che le credenziali in `.env` siano corrette
4. Verifica i log per errori di autenticazione OAuth

### Frontend non si connette al backend

1. Verifica che il backend sia in esecuzione su `localhost:8080`
2. Controlla la console del browser per errori
3. Verifica che il proxy in `vite.config.ts` sia configurato correttamente

### Errore durante la creazione della richiesta

1. Verifica che il file sia un PDF valido
2. Controlla che tutti i campi obbligatori siano compilati
3. Assicurati che il numero di telefono abbia il prefisso internazionale (+39...)
4. Verifica i log del backend per dettagli sull'errore

### Errore OAuth

1. Verifica che `OPENAPI_CLIENT_ID` e `OPENAPI_CLIENT_SECRET` siano corretti
2. Controlla che l'ambiente sia impostato su "sandbox" per i test
3. Verifica che le credenziali siano attive su https://console.openapi.com

## Database H2 Console

Durante lo sviluppo, puoi accedere alla console H2:

- URL: http://localhost:8080/api/h2-console
- JDBC URL: `jdbc:h2:mem:firmadb`
- Username: `sa`
- Password: (vuoto)

## Ambiente di Produzione

Per passare all'ambiente di produzione:

1. Modifica il file `.env`:
   ```env
   OPENAPI_ENVIRONMENT=production
   ```

2. Riavvia il backend

⚠️ **ATTENZIONE**: In produzione verranno create firme reali e potrebbero esserci costi associati.

## Prossimi Passi

- Implementa autenticazione utente
- Aggiungi persistenza su database PostgreSQL
- Implementa gestione UI personalizzate (`/firma_elettronica_ui`)
- Aggiungi supporto SPID (`/spid_personale`)
- Implementa webhook per callback asincroni
