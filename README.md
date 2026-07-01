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

## Instrukcja uruchomienia

### Wymagania wstępne
Do uruchomienia projektu wymagane jest wyłącznie zainstalowane środowisko uruchomieniowe Java (JDK 17). Pobieraniem zależności, bazy danych H2 oraz konfiguracją frameworka zarządza narzędzie Maven Wrapper.

### Uruchomienie aplikacji
Należy otworzyć terminal w głównym katalogu projektu i wykonać poniższe polecenie:

```
./mvnw spring-boot:run
```

Dla systemów z rodziny Windows należy użyć polecenia:

```
mvnw.cmd spring-boot:run
```

Baza danych H2 działa w pamięci i jest uruchamiana automatycznie wraz z aplikacją. 

## Dokumentacja API (Swagger)
Po poprawnym uruchomieniu aplikacji, interaktywna dokumentacja oraz interfejs do ręcznego testowania endpointów dostępne są w przeglądarce pod adresem:
http://localhost:8080/swagger-ui.html

Plik z definicją API w formacie OpenAPI (YAML) można pobrać pod adresem:
http://localhost:8080/v3/api-docs.yaml

## Testy
Projekt zawiera zestaw testów jednostkowych oraz integracyjnych. Aby je wykonać, należy uruchomić polecenie:

```
./mvnw test
```

## Licencja
Projekt udostępniony na licencji MIT. Szczegóły znajdują się w pliku LICENSE.