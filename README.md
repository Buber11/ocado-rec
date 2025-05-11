Jakub_Balcerzak_Java_Wrocław 
# Optymalizator Płatności / Payment Optimizer

##  README (English)

###  System Requirements

- Java 17 or newer  
- Maven (for building the project)

###  Installation & Build

1. Clone the repository:
   ```bash
   git clone https://github.com/Buber11/ocado-rec
   ```

2. Build the project with dependencies (Fat JAR):
   ```bash
   mvn clean package
   ```

    Output: `target/app-with-dependencies.jar`

###  Run the Program

```bash
  java -jar target/app-with-dependencies.jar path/to/orders.json path/to/paymentmethods.json
```

###  How the Algorithm Works

1. **Load data**:
   - Orders from `orders.json`: ID, value, available promotions.
   - Payment methods from `paymentmethods.json`: ID, discount %, limit.

2. **Sort orders**:
   - Priority 1: Highest available discount.
   - Priority 2: Highest order value.

3. **Generate payment options**:
   - Full points payment (15% discount).
   - Full card payment with promotion (e.g., mZysk: 10% discount).
   - Partial points + card (10% discount).
   - Fallback card payment without discount.

4. **Select the best option**:
   - Maximize savings.
   - Prefer points if discounts are equal.

5. **Update payment limits**:
   - Deduct used amounts from method limits.

###  Sample Output

```
PUNKTY      100.00  
mZysk       165.00  
BosBankrut  190.00  
```

 **Note**: The Fat JAR (`app-with-dependencies.jar`) includes all necessary libraries for standalone execution.

---

## README (Polski)

### Wymagania systemowe

- Java 17 lub nowsza  
- Maven (do budowy projektu)

### Instalacja i budowa

1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/Buber11/ocado-rec
   ```

2. Zbuduj projekt jako Fat JAR (z zależnościami):
   ```bash
   mvn clean package
   ```

   Wynikowy plik: `target/app-with-dependencies.jar`

### Uruchomienie

```bash
  java -jar target/app-with-dependencies.jar ścieżka/do/orders.json ścieżka/do/paymentmethods.json
```

###  Jak działa algorytm?

1. **Wczytywanie danych**:
   - Zamówienia (`orders.json`) – ID, wartość, dostępne promocje.
   - Metody płatności (`paymentmethods.json`) – ID, rabat %, limit.

2. **Sortowanie zamówień**:
   - Priorytet 1: Najwyższy dostępny rabat.
   - Priorytet 2: Najwyższa wartość zamówienia.

3. **Generowanie opcji płatności**:
   - Pełna płatność punktami (rabat 15%).
   - Pełna płatność kartą z promocją (np. mZysk: 10% rabatu).
   - Częściowa płatność punktami (min. 10% + karta, rabat 10%).
   - Płatność awaryjna – karta bez rabatu.

4. **Wybór najlepszej opcji**:
   - Maksymalizacja oszczędności.
   - Preferencja dla punktów przy równych rabatach.

5. **Aktualizacja limitów**:
   - Odjęcie zużytych kwot z limitów płatności.

###  Przykładowy wynik

```
PUNKTY      100.00  
mZysk       165.00  
BosBankrut  190.00  
```
---
