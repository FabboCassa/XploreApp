# 🗺️ XploreApp (Frontend): Struttura del Progetto e Architettura

Questa guida spiega in modo semplice e chiaro a cosa serve ogni singola cartella all'interno del codice sorgente dell'app mobile (sviluppata in Kotlin Multiplatform).
Così come il backend, l'applicazione mobile segue i principi di **Clean Architecture**. L'obiettivo è tenere separato ciò che l'utente *vede* (UI) da ciò che l'app *fa* (Logica di Business) e da *dove prende i dati* (Database/Network).

Tutto il codice principale e condiviso tra Android e iOS si trova all'interno della cartella `composeApp/src/commonMain/kotlin/org/xplore/project/`.

---

## 🧠 1. Il Cuore dell'App (`domain/`)
Questo è il centro nevralgico dell'applicazione, indipendente da qualsiasi libreria esterna, interfaccia grafica o database.

### `domain/model/` (Le Entità)
- **Cosa fa:** Definisce "che cos'è" ogni cosa all'interno della nostra app. È la pura rappresentazione dei dati.
- **Cosa ci trovi:** Le classi Kotlin come `MapPin`, `SavedRoute`, `Community`, `Museum`, `Friend`, `Notification`.
- **Spiegazione per esterni:** Sono i concetti base della nostra agenzia di viaggi. Un "Percorso Salvato" esiste indipendentemente da come viene disegnato sullo schermo o da come viene salvato nel server.

### `domain/repository/` (I Contratti / Interfacce)
- **Cosa fa:** Definisce quali azioni si possono compiere sui dati, ma *non spiega come farlo*.
- **Cosa ci trovi:** Interfacce come `SavedRouteRepository` (es. "ottieni i percorsi di un utente"), `AuthRepository`, `CommunityRepository`.
- **Spiegazione per esterni:** È una lista delle mansioni: "Qualcuno deve sapermi dare la lista degli amici". A questo livello non ci interessa *chi* lo farà (se il database locale o un server su internet), ci basta sapere che l'app offre questa funzionalità.

---

## 🚚 2. I Fattorini e il Magazzino (`data/` e `network/`)
Questi moduli sanno come e dove reperire le informazioni nel mondo reale. Sono gli strati "sporchi" dell'app che chiamano le API esterne e implementano i contratti definiti nel `domain`.

### `network/` (Il Telefono)
- **Cosa fa:** Contiene le configurazioni "fisiche" per connettersi a internet e parlare con il nostro Backend (API).
- **Cosa ci trovi:** Il client HTTP (`HttpClient.kt` usando Ktor) e la gestione dei token di sicurezza (es. per mantenere l'accesso dell'utente e non chiedergli la password ogni volta).
- **Spiegazione per esterni:** È il telefono fisso che i nostri fattorini usano per chiamare il magazzino centrale (il backend .NET) e farsi inviare i dati JSON richiesti.

### `data/remote/` (Le Chiamate API al Server)
- **Cosa fa:** Effettua fisicamente le chiamate di rete al backend, gestendo le risposte e convertendole per la nostra app.

### `data/local/` (L'Archivio nel Telefono)
- **Cosa fa:** Salva piccoli frammenti di dati direttamente nella memoria locale del telefono, se serve un funzionamento offline o di base (es. Preferenze utente, lingua, stato di login).

### `data/repository/` (I Fattorini a Lavoro)
- **Cosa fa:** Contiene l'implementazione vera e propria (il codice concreto) delle interfacce `Repository` definite nel `domain`.
- **Cosa ci trovi:** Classi (`RepositoryImpl`) che dicono: "Quando l'app mi chiede la lista degli amici, faccio aprire una chiamata di rete nel modulo network, se il server risponde restituisco l'elenco, se non c'è internet mando un messaggio d'errore".

---

## 🎨 3. L'Interfaccia Utente e le Schermate (`ui/`)
Qui è racchiuso tutto ciò che l'utente vede e con cui interagisce, sviluppato in **Compose Multiplatform** (la UI dichiarativa moderna). Questa cartella è ordinatamente divisa in base alle singole *funzionalità* (Features) dell'app.

In ogni cartella delle funzionalità trovi essenzialmente due componenti strettamente uniti:
1.  **I Componenti Compose (Le Schermate):** Il codice che disegna i pulsanti, i testi, i popup e le mappe (es. `HomeScreen.kt`).
2.  **Il ViewModel (L'Intermediario/Rappresentante):** Una classe che gestisce lo "Stato" della schermata (es. `Caricamento`, `Errore`, `Mostra lista amici`) e invia comandi ai Repository. Le schermate non chiamano mai le API direttamente, parlano unicamente col ViewModel.

### Sottocartelle principali delle Funzionalità:
- **`ui/auth/`**: Gestisce tutto ciò che riguarda la schermata di Login, Registrazione e Recupero Password.
- **`ui/home/`**: La vista principale con la Mappa, il rendering dei PIN e la barra di ricerca base per esplorare la città.
- **`ui/itinerary/`**: Contiene la logica più delicata per i percorsi: la modalità di navigazione (Turn-by-turn navigation), il calcolo multi-tappa, il ricalcolo e la visualizzazione della *Floating Bar* (il riquadro scuro che ti indica la prossima destinazione).
- **`ui/profile/`**: L'Hub personale dell'utente dove si visionano la Lista Amici, le Notifiche, le Richieste in Sospeso e le "Route Salvate/Completate".
- **`ui/community/`**: Gestione di feature sociali, i Gruppi (Creazione, Esplorazione) e inviti tra utenti.
- **`ui/poi/`**: I Dettagli di un Punto di Interesse (Musei, Parchi), con eventuali immagini e info del backend.
- **`ui/components/`**: Pezzetti di interfaccia riutilizzabili e ripetibili (es. Il bottone generico `XploreButton`, `TopBar`, icone di caricamento) per evitare di riscrivere sempre la stessa grafica.
- **`ui/navigation/`**: Il "Vigile urbano" dell'app (`XploreNavHost`) che sa esattamente come gestire le transizioni, cioè cosa caricare a schermo quando clicchi e navighi da una pagina all'altra.
- **`ui/theme/`**: La tavolozza dei colori, il font di testo (Typography) e il design system generale (Material 3).

---

## ⚙️ 4. Organizzazione e Ingranaggi (`di/` e `util/`)

### `di/` (Gestione Dipendenze - Il Direttore Orchestrale)
- **Cosa fa:** Configura e connette in automatico tutti i pezzi dell'app all'avvio (framework: **Koin**).
- **Cosa ci trovi:** Moduli che dichiarano le relazioni padre-figlio strutturali. "Ogni volta che si richiede la Schermata Auth, forniscile il suo `AuthViewModel`; a quest'ultimo allega l'istanza corretta di `AuthRepository` e il client `HttpClient`".
- **Spiegazione per esterni:** È il capo del personale. Quando assumi un venditore da mettere allo sportello (la Schermata UI), il Direttore (DI) gli assegna prima d'iniziare un telefono aziendale preimpostato (Repository) così che sappia già usare l'interfono senza doverlo cercare e cablare da solo!

### `util/` (Gli Attrezzi del Mestiere)
- **Cosa fa:** Contiene tutte quelle funzioni generiche d'appoggio utilizzate ripetutamente ad ogni livello.
- **Cosa ci trovi:** Formattatori di stringhe, logiche di troncamento date/ora, helper per il caricamento immagini, funzioni matematiche per le coordinate.

---

## 🔄 Un Esempio Pratico (Il flusso dei dati)
Cosa accade nel motore dell'app quando ci stai giocando "da fuori"?

1.  **Azione:** L'Utente clicca su "Salva Percorso" dentro l'Applet in basso (`ui/itinerary/ItineraryFloatingBar.kt`).
2.  **Richiesta UI:** La Schermata grafica non salva le cose materialmente sull'hard disk; alza semplicemente la mano al suo supervisore, il **ViewModel** (`ItineraryViewModel`), dicendo tramite un evento: *"L'Utente vuole salvare il percorso correntemente attivato!"*
3.  **Il Commesso lavora:** Il **ViewModel** riceve l'informazione, controlla che non ci siano errori superficiali (es. hai disattivato il wifi?) e delega l'incarico pesante chiamando il **Contratto** generale: il `SavedRouteRepository` (del pacchetto `domain/`).
4.  **L'Operaio sgobba:** L'implementazione vera nel livello **Data** (`data/repository/SavedRouteRepositoryImpl`) esegue fisicamente il lavoro. Cripta i token, prende il client HTTPS (`network/`) e invia un pacchetto JSON formattato alla REST API del nostro backend su internet.
5.  **Risposta:** Il server Cloud ci risponde confermando `HTTP 200 OK`. Il **Repository** impacchetta questo successo e lo rigira all'indietro al **ViewModel**.
6.  **Stato Rinfrescato:** Il **ViewModel** aggiorna il suo "Stato Esistenziale", trasformandolo emotivamente ed internamente in *'Salvataggio appena Riuscito'*.
7.  **Aggiornamento UI:** La **Schermata visiva**, che osserva ("ascolta") costantemente lo stato d'animo del ViewModel, percepisce il cambiamento fulmineo in tempo reale e mostra uno snackbar (l'avvisino colorato toast) a fondo schermo all'**Utente** confermando l'azione!
