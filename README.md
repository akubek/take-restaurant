# Projekt zaliczeniowy na przedmiot TAKE: System Zarządzania Restauracją

Backendowa aplikacja typu REST API służąca do zarządzania procesami w restauracji, w tym obsługą klientów, menu oraz cyklem życia zamówień. Wersja demonstracyjna (Proof of Concept).

## Autorzy

- Artur Kubek
- Rafał Sularczyk
- Robert Dulik

## Technologie i Narzędzia

- Java 17
- Spring Boot 4.1.0
- H2 Database (baza in-memory)
- JUnit 5 / TestRestTemplate
- SpringDoc OpenAPI (Swagger)

## Instalacja i uruchomienie

### Wymagania wstępne

Do uruchomienia projektu wymagane jest wyłącznie zainstalowane środowisko uruchomieniowe Java (JDK 17).
Pobieraniem zależności, budowaniem aplikacji oraz uruchomieniem bazy danych zarządza dołączone narzędzie Maven Wrapper (mvnw). Nie ma potrzeby instalowania Mavena jako zewnętrznego narzędzia w systemie

Należy najpierw pobrać projekt, np. poprzez

```cmd or bash
git clone https://github.com/akubek/take-restaurant.git
```

### Kroki budowania projektu oraz uruchomienia

#### Linux / macOS

1. Otworzyć terminal w głównym katalogu projektu.

2. Zbudowanie projektu:

    ```bash
    chmod +x mvnw
    ./mvnw clean package
    ```

3. Uruchomienie aplikacji:

    ```bash
    ./mvnw spring-boot:run
    ```

#### Windows

1. Otworzyć terminal w głównym katalogu projektu.

2. Zbudowanie projektu:

    ```cmd
    mvnw.cmd clean package
    ```

3. Uruchomienie aplikacji:

    ```cmd
    mvnw.cmd spring-boot:run
    ```

Baza danych H2 działa w pamięci i jest uruchamiana automatycznie wraz z aplikacją. Dane nie są przechowywane.

## Dokumentacja API (Swagger)

Po poprawnym uruchomieniu aplikacji, interaktywna dokumentacja oraz interfejs do ręcznego testowania endpointów dostępne są w przeglądarce pod adresem:
<http://localhost:8080/swagger-ui/index.html>

Plik z definicją API w formacie OpenAPI (YAML) można pobrać pod adresem:
<http://localhost:8080/v3/api-docs.yaml>

## Testy

Projekt zawiera zestaw testów jednostkowych oraz integracyjnych. Aby je wykonać, należy uruchomić polecenie:

```cmd or bash
./mvnw test
```

## Licencja

Projekt udostępniony na licencji MIT. Szczegóły znajdują się w pliku LICENSE.
