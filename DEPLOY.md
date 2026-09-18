# Wdrożenie produkcyjne

Instrukcja ponownego wdrożenia aplikacji, która **już działa** na serwerze.
Pierwsza instalacja jest opisana w komentarzach `docker-compose.prod.yml` i `.env.example`.

## Środowisko

| Co | Gdzie |
|---|---|
| Serwer | `51.83.154.99` (domena `weddingo.pl`) |
| Użytkownik SSH | `ubuntu` (logowanie na `root` jest zablokowane) |
| Repozytorium backendu | `~/weddingo/eventsAppBE`, gałąź `master` |
| Repozytorium frontendu | `~/weddingo/weddingoFE-`, gałąź `main` |
| Sekrety | `~/weddingo/eventsAppBE/.env` (nie ma go w repo) |

Połączenie:

```bash
ssh ubuntu@51.83.154.99
```

Jeśli `docker compose` zwraca „permission denied”, użytkownik nie jest w grupie `docker` —
dopisz `sudo` przed każdą komendą `docker`.

## Kolejność wdrożenia

**Backend i frontend wdrażaj razem.** API i aplikacja bywają zmieniane parami
(np. gdy zmienia się kształt endpointu), więc wdrożenie tylko jednej strony potrafi
zepsuć działającą funkcję.

## 1. Kopia bazy

Migracje zwykle tylko dodają kolumny i tabele, ale kopia jest tania:

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml exec -T db sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" events' > ~/backup-events-$(date +%F-%H%M).sql
```

Hasło root bierze się ze zmiennej wewnątrz kontenera, więc nie trzeba go nigdzie wpisywać.

## 2. Nowy kod w obu repozytoriach

```bash
cd ~/weddingo/eventsAppBE && git pull --ff-only origin master
```

```bash
cd ~/weddingo/weddingoFE- && git pull --ff-only origin main
```

## 3. Przebudowa i restart

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml up -d --build
```

Budowanie odbywa się na serwerze: Maven kompiluje backend, `npm ci` instaluje zależności
frontendu. Na maszynie z 4 GB RAM build konkuruje o pamięć z działającą bazą i API.
Jeśli wystąpią problemy, rozdziel to na dwa etapy:

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml build && docker compose -f docker-compose.prod.yml up -d
```

## 4. Weryfikacja

Migracje Liquibase wykonują się automatycznie przy starcie API:

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml logs --tail=200 api | grep -iE "liquibase|Started EventsAppApplication|ERROR"
```

Oczekiwany wynik: wykaz wykonanych zestawów zmian (albo „Database is up to date”)
i na końcu `Started EventsAppApplication`.

Stan kontenerów:

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml ps
```

Na koniec sprawdź w przeglądarce:

1. **Ustawienia → Ustawienia oferty** — logo, kolor tła, orientacja, wybór i kolejność pól, własny PDF.
2. **Oferta → Przygotuj ofertę PDF** — „Zapisz” zapisuje dane bez generowania pliku, „Generuj PDF” otwiera podgląd.
3. **Formularz dla klientów** (`/formularz/<token>`) — logo w prawym górnym rogu, pytanie o przekąski, limit 5 zdjęć.

## Wdrożenie tylko jednej części

`docker-compose.prod.yml` leży w repozytorium backendu, a kontekst budowania frontendu
wskazuje na katalog obok (`../weddingoFE-`). Komendy `docker` uruchamiasz więc zawsze
z `~/weddingo/eventsAppBE`, nawet gdy wdrażasz sam frontend.

### Tylko frontend

```bash
cd ~/weddingo/weddingoFE- && git pull --ff-only origin main
```

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml up -d --build web
```

Backend i baza nie są ruszane. Kontener `web` to jednak Caddy, który obsługuje HTTPS
i przekazuje ruch do API, więc jego wymiana przerywa na chwilę **całą** stronę, łącznie
z API — zwykle na sekundę lub dwie.

Kontrola po wdrożeniu:

```bash
cd ~/weddingo/eventsAppBE && docker compose -f docker-compose.prod.yml logs --tail=50 web
```

W przeglądarce odśwież stronę. Pliki Angulara mają nazwy ze skrótem treści, więc
przeglądarka sama pobierze nowe wersje.

### Tylko backend

```bash
cd ~/weddingo/eventsAppBE && git pull --ff-only origin master && docker compose -f docker-compose.prod.yml up -d --build api
```

Strona działa bez przerwy, a zapytania do API nie przechodzą przez kilka sekund startu.
Migracje bazy wykonają się tak samo jak przy pełnym wdrożeniu.

### Kiedy nie wystarczy jedna część

Jeśli zmiana dotyczy kształtu API (np. nowy albo zmieniony endpoint), wdrożenie tylko
jednej strony zepsuje działającą funkcję. Wtedy wdrażaj obie części — patrz sekcja
„Kolejność wdrożenia”.

### Porządki

Stare obrazy i warstwy budowania zajmują miejsce na dysku:

```bash
docker image prune -f
```

## Wycofanie zmian

Wróć na poprzedni commit i przebuduj — w obu repozytoriach na wersje z tej samej epoki:

```bash
cd ~/weddingo/eventsAppBE && git log --oneline -10
```

```bash
cd ~/weddingo/eventsAppBE && git checkout <commit> && docker compose -f docker-compose.prod.yml up -d --build
```

Migracje nie są cofane, ale poprzedni kod działa z nowszą bazą: dodawane kolumny mają
wartości domyślne albo dopuszczają `null`, a nowe tabele są dla niego niewidoczne.
Dane wprowadzone przez nowsze funkcje pozostaną w bazie, tylko nie będą używane.

## Uwagi eksploatacyjne

- **Przerwa w działaniu** to kilka sekund, gdy kontener API jest wymieniany.
- **Baza nie ma opublikowanego portu** — jest dostępna tylko z sieci Dockera.
- **Caddy nie ogranicza rozmiaru żądania**, więc wgrywanie plików (logo do 2 MB,
  własny PDF do 10 MB) przechodzi bez zmian w konfiguracji. Limity pilnuje aplikacja.
- **Logo i własny PDF tenanta trzymane są w bazie**, więc kopie zapasowe rosną razem z nimi.
  Wygenerowane oferty PDF leżą na wolumenie `pdf_data`, poza bazą.
- **Certyfikaty Let's Encrypt** siedzą na wolumenie `caddy_data` i przeżywają restart —
  nie usuwaj tego wolumenu, żeby nie trafić na limity wystawiania certyfikatów.
- **Nowe zmienne w `.env`** pojawiają się rzadko. Jeśli API nie wstaje po wdrożeniu,
  sprawdź w logu brakującą zmienną i porównaj `.env` z `.env.example`.

## Historia wdrożeń — na co uważać

### Wersja z ustawieniami oferty (migracje 017–019)

- **Zmienił się endpoint generowania PDF** (`GET` z parametrami → `POST` z listą pól),
  więc backend i frontend muszą pójść razem.
- **Frontend ma nową zależność** `@angular/cdk`; `npm ci` bierze ją z `package-lock.json`.
- **Migracja 017** dodaje kolejność elementów wyceny i ustawia ją według dotychczasowej
  kolejności istniejących wierszy.
- **Migracje 018 i 019** dodają ustawienia oferty oraz tabelę na własny PDF tenanta.
  Do czasu zapisania ustawień PDF wygląda jak wcześniej: białe tło, orientacja pozioma
  i pola ze starego szablonu.
- **Stary szablon `oferta-template.pdf` został usunięty** — PDF powstaje w całości z kodu.
