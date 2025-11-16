# Troubleshooting

## Errori di Compilazione Backend

### Errore: "cannot find symbol - variable log"

**Causa**: L'annotazione `@Slf4j` di Lombok non viene processata correttamente durante la compilazione.

**Soluzione**:

1. **Assicurati che Lombok sia installato nell'IDE**:

   **IntelliJ IDEA**:
   - Installa il plugin Lombok: `Settings > Plugins > Marketplace > cerca "Lombok" > Install`
   - Abilita annotation processing: `Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable annotation processing`

   **Eclipse**:
   - Scarica `lombok.jar` da https://projectlombok.org/download
   - Esegui: `java -jar lombok.jar`
   - Segui le istruzioni per installare Lombok nell'IDE

   **VS Code**:
   - Installa l'estensione "Language Support for Java(TM) by Red Hat"
   - Lombok dovrebbe essere supportato automaticamente

2. **Pulisci e ricompila il progetto**:
   ```bash
   cd backend
   mvn clean install
   ```

3. **Se il problema persiste**, riavvia l'IDE dopo aver installato Lombok.

### Errore: "cannot find symbol - method isSuccess()"

**Causa**: Lombok non ha generato i getter/setter per le classi DTO.

**Soluzione**: Stesso procedimento di sopra. L'annotazione `@Data` di Lombok genera automaticamente i metodi `isSuccess()`, `getData()`, etc.

## Configurazione Maven

Se Maven non è installato sul sistema, puoi:

1. **Usare Maven Wrapper** (se disponibile):
   ```bash
   cd backend
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

2. **Installare Maven**:

   **macOS**:
   ```bash
   brew install maven
   ```

   **Linux (Ubuntu/Debian)**:
   ```bash
   sudo apt-get install maven
   ```

   **Windows**:
   - Scarica da https://maven.apache.org/download.cgi
   - Estrai e aggiungi al PATH

3. **Verificare l'installazione**:
   ```bash
   mvn -version
   ```

## Errori di Runtime

### Errore: "Failed to authenticate with OpenAPI"

**Causa**: Credenziali OAuth non valide o non configurate.

**Soluzione**:
1. Verifica che il file `.env` esista nella cartella `backend/`
2. Controlla che `OPENAPI_CLIENT_ID` e `OPENAPI_CLIENT_SECRET` siano corretti
3. Verifica su https://console.openapi.com che le credenziali siano attive
4. Assicurati che l'ambiente sia impostato correttamente (`OPENAPI_ENVIRONMENT=sandbox`)

### Errore: "Connection refused" o "Port 8080 already in use"

**Causa**: Il backend non è in esecuzione o la porta è già occupata.

**Soluzione**:
1. Verifica che il backend sia in esecuzione: `curl http://localhost:8080/api/firma/health`
2. Se la porta è occupata, cambia la porta in `application.yml`:
   ```yaml
   server:
     port: 8081
   ```
3. Aggiorna anche il proxy nel frontend (`vite.config.ts`)

### Errore: "CORS policy"

**Causa**: Il frontend sta chiamando il backend da un'origine non autorizzata.

**Soluzione**: Aggiungi l'origine in `application.yml`:
```yaml
cors:
  allowed-origins: http://localhost:3000,http://localhost:5173,http://localhost:TUAPORTA
```

## Errori Frontend

### Errore: "npm install" fallisce

**Soluzione**:
1. Cancella `node_modules` e `package-lock.json`:
   ```bash
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

2. Se usi Node.js vecchio, aggiorna a Node 18+:
   ```bash
   node --version  # Verifica versione
   ```

### Errore: File non trovato durante upload PDF

**Causa**: Il browser non può accedere al file selezionato.

**Soluzione**: Assicurati di selezionare un file PDF valido tramite l'input file. Non specificare il percorso manualmente.

### TypeScript errors

**Soluzione**:
```bash
cd frontend
npm run build  # Controlla gli errori TypeScript
```

## Errori API OpenAPI

### Error 400: "Invalid request format"

**Causa**: Il payload della richiesta non è conforme allo schema OpenAPI.

**Soluzione**:
1. Verifica che il PDF sia codificato in Base64 correttamente
2. Controlla che tutti i campi obbligatori siano presenti:
   - `content` (Base64)
   - `members` (array non vuoto)
   - Per ogni member: `firstname`, `lastname`, `email`, `phone`, `signs`
3. Assicurati che il telefono abbia il prefisso internazionale: `+393331234567`

### Error 404: "File not ready or wrong ID"

**Causa**: Il documento firmato non è ancora pronto o l'ID non è valido.

**Soluzione**:
1. Verifica che lo stato della richiesta sia "finished" prima di scaricare
2. Controlla l'ID della richiesta
3. Attendi che tutti i firmatari completino la firma

### Rate Limit Exceeded

**Causa**: Superate le 10,000 richieste al minuto.

**Soluzione**:
1. Implementa un sistema di rate limiting lato client
2. Usa un backoff esponenziale per i retry
3. Considera di cachare le risposte quando possibile

## Debug

### Abilitare log dettagliati nel backend

In `application.yml`:
```yaml
logging:
  level:
    com.openapi.firma: DEBUG
    org.springframework.web: DEBUG
```

### Ispezionare richieste HTTP

**Frontend** (Browser DevTools):
1. Apri DevTools (F12)
2. Tab "Network"
3. Filtra per "XHR" o "Fetch"
4. Ispeziona le richieste verso `/api/firma`

**Backend** (Logs):
I log mostrano tutte le richieste HTTP in arrivo e le risposte dell'API OpenAPI.

### Testare API direttamente

**Con curl**:
```bash
# Health check
curl http://localhost:8080/api/firma/health

# Get all requests
curl http://localhost:8080/api/firma

# Get specific request
curl http://localhost:8080/api/firma/{id}
```

**Con Postman o Insomnia**:
Importa la collezione API e testa gli endpoint direttamente.

## Database H2 Console

Per ispezionare il database durante lo sviluppo:

1. Accedi a: http://localhost:8080/api/h2-console
2. JDBC URL: `jdbc:h2:mem:firmadb`
3. Username: `sa`
4. Password: (vuoto)

## Supporto

Se il problema persiste:

1. Controlla i log del backend per errori dettagliati
2. Controlla la console del browser per errori JavaScript
3. Verifica la documentazione ufficiale OpenAPI: https://console.openapi.com/apis/firma/documentation
4. Assicurati di usare l'ambiente sandbox per i test
