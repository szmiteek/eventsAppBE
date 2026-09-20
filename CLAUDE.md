# Weddingo (eventsApp) — notatki dla Claude Code

Aplikacja dla firm dekoracyjnych/eventowych: klient wypełnia publiczny formularz → powstaje
**oferta** → oferta jest wyceniana i generowana do PDF → po podpisaniu zamienia się w **event**.

## Repozytoria

| Część | Katalog | Repo | Gałąź |
|---|---|---|---|
| Backend | `nfApp/` | `szmiteek/eventsAppBE` | `master` |
| Frontend | `nf-frontend/` | `szmiteek/weddingoFE-` | `main` |

`nfFront/` to stara wersja frontendu — nie ruszamy jej.
Backend i frontend zmieniają się parami (kształt endpointów), więc wdrażaj je razem — patrz `DEPLOY.md`.

## Stack

- **Backend**: Java 17, Spring Boot 3.5, Spring Security + JWT (jjwt), Spring Data JPA, MySQL 8.4,
  Liquibase, PDFBox 3, Lombok, Spring Mail.
- **Frontend**: Angular 22 (standalone components, sygnały, `@if`/`@for`), TypeScript, SCSS,
  Prettier (`printWidth: 100`, pojedyncze cudzysłowy), vitest.
- **Baza lokalnie**: kontener `events_db` z `docker-compose.yml` (`events` / user `user`, hasło `user`).

## Uruchomienie lokalne

```bash
docker compose -f /Users/adrianszmit/Desktop/nfapp/nfApp/docker-compose.yml up -d
```

```bash
cd /Users/adrianszmit/Desktop/nfapp/nfApp && ./run-local.sh
```

```bash
cd /Users/adrianszmit/Desktop/nfapp/nf-frontend && npm start
```

- `run-local.sh` czyta sekrety z `local.env` (wzór: `local.env.example`) i włącza profil `dev`,
  który jako jedyny dodaje CORS dla `http://localhost:4200`.
- Backend: `http://localhost:8080`, prefiks API `event-api` (frontend dev woła
  `http://localhost:8080/event-api`, produkcyjnie `/event-api` za reverse proxy).
- Frontend: `http://localhost:4200`. Formularz klienta: `/formularz/:token`.
- Seed z migracji 008/009: `super@admin` / `admin123` (SUPER_ADMIN), `default@tenant` / `tenant123` (TENANT).
- `mvn` nie jest w PATH — używaj `./run-local.sh` albo
  `~/.m2/wrapper/dists/apache-maven-3.9.11-bin/*/apache-maven-3.9.11/bin/mvn`.
- `preview_start` z `.claude/launch.json` nie potrafi odpalić backendu (sandbox nie czyta plików
  z Desktopu) — backend uruchamiaj skryptem w tle.

## Weryfikacja zmian

```bash
cd /Users/adrianszmit/Desktop/nfapp/nf-frontend && npx ng build --configuration development
```

Backend nie ma testów (katalog `src/test` w ogóle nie istnieje), więc zmiany sprawdzamy kompilacją
backendu, buildem frontendu (on jako jedyny sprawdza typy w szablonach) i ręcznym przejściem ścieżki
w działającej aplikacji.

## Wielodostępność (multi-tenant)

- Każdy rekord ma `tenantId`; serwisy filtrują po `CurrentTenantProvider.requireTenantId()`
  i rzucają 404 (`getOwnedOffer` / `getOwnedEvent`), gdy rekord należy do innego tenanta.
- Role: `TENANT` (cała aplikacja), `SUPER_ADMIN` (dodatkowo `/event-api/tenants/**`).
- Bez logowania: `/event-api/auth/login`, `/event-api/public/**` (formularz klienta po tokenie
  `tenant.public_form_token`), callback Google OAuth.

## Model domenowy

- `Offer` — dane z formularza klienta + wycena (`price`), status (`NOT_READY`, `READY`, `SENT`,
  `SIGNED`), zdjęcia (`OfferImage`, maks. 5) i pozycje wyceny (`EventElement`).
- `Event` — powstaje przy zmianie statusu oferty na `SIGNED` (`OfferService.updateStatus` →
  `EventService.createFromOffer`). `EventMapper.fromOffer` przepisuje dane oferty, a pozycje wyceny
  są kopiowane, żeby oferta i event mogły się dalej rozjeżdżać niezależnie.
- Usunięcie eventu kasuje też ofertę, z której powstał — to jedyny sposób na usunięcie podpisanej oferty.
- `TenantOfferSettings` — wygląd PDF-a tenanta: kolor tła, orientacja, logo, własny PDF wstępu
  (`TenantOfferCoverPdf`) i **wybrane pola** informacyjne (maks. `MAX_INFO_FIELDS` = 8).

## ZASADA: nowe pole w encji `Offer`

Każde nowe pole dodawane do encji `Offer` musi być **do wyboru w Ustawieniach oferty jako pole do
generowania PDF-a**. Pełna ścieżka dodania pola:

1. `Offer` (encja) + migracja Liquibase w `src/main/resources/db/changelog/` (kolejny numer) wpięta
   w `changelog-master.xml`.
2. `OfferDTO`, `OfferCreateCommand`, `OfferUpdateCommand` (w update typ owijany, np. `Boolean`,
   bo `Optional.ofNullable(...).ifPresent(...)` pomija pola nieprzysłane) i `OfferMapper`
   (create + DTO + update).
3. **`OfferInfoField`** — nowa stała z etykietą i ekstraktorem wartości jako tekst. Kolejność
   deklaracji idzie za kolejnością pól w formularzu klienta. To ona zasila listę
   `availableFields` w ustawieniach oferty.
4. `pdf-prepare-modal.ts` → `FIELD_DEFINITIONS` (klucz = nazwa stałej enuma) + obsługa `kind`
   w `pdf-prepare-modal.html`. **Bez wpisu w `FIELD_DEFINITIONS` pole wybrane w ustawieniach
   zniknie po cichu z modala** (`flatMap` je odfiltruje).
5. Jeśli pole pochodzi z formularza klienta: `PublicOfferCommand` (+ walidacja),
   `PublicOfferService`, `public-offer.model.ts`, `public-offer.service.ts` (FormData!),
   `public-offer-form.ts` (sygnał, `canSubmit`, `submit`) i `public-offer-form.html`.
6. `offer.model.ts` (`Offer` + `OfferUpdateCommand`), podgląd w `offer-detail.html`,
   etykieta w `error.interceptor.ts` (`FIELD_LABELS`).
7. Jeśli pole ma trafić też na event: `Event`, `EventDTO`, `EventMapper.fromOffer` + `mapToDTO`,
   `event.model.ts` i ta sama migracja.

## PDF oferty

- `OfferPdfService` / `OfferPdfRenderer` (PDFBox) generują strony oferty („Informacje ogólne”
  z pól wybranych w ustawieniach, wycena, zdjęcia) i doklejają je na końcu własnego PDF-a tenanta;
  rozmiar strony bierze się wtedy z pierwszej strony tego PDF-a.
- Gotowe pliki lądują w `app.storage.offer-pdf-dir` (domyślnie `./storage/offer-pdfs`).
- Podpisanej oferty (`SIGNED`) nie da się już edytować — modal PDF przechodzi w tryb tylko do odczytu.

## Konwencje

- Kod, nazwy pól i komentarze po **angielsku**; wszystko, co widzi użytkownik (etykiety, komunikaty
  błędów, opisy) po **polsku**. Komunikaty commitów po polsku, bez polskich znaków.
- Komentarze piszemy oszczędnie i tylko o tym, *dlaczego* coś jest zrobione tak, a nie inaczej —
  nie opisujemy oczywistości.
- Migracje Liquibase są **tylko przyrostowe** — nie edytujemy raz wykonanego changeSetu; nowe kolumny
  typu BOOLEAN dodajemy z `defaultValueBoolean` i `nullable="false"`, żeby istniejące rekordy miały wartość.
- Walidacja: `@NotNull`/`@NotBlank` z kodami (`EMPTY_VALUE`, `DATE_IN_PAST`, `NOT_EMAIL`), które
  frontend tłumaczy w `error.interceptor.ts`.
- W commandach publicznego formularza pola tak/nie są typu `Boolean` (nie `boolean`), żeby brak
  odpowiedzi był błędem walidacji, a nie cichym „Nie”.
- Frontend: komponenty standalone, sygnały zamiast `BehaviorSubject`, pliki bez sufiksu
  `.component`, szablony w osobnych `.html`.
