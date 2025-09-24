# MotorReporting

Generate an executive HTML dashboard for insurance quote attempts. The application
ingests either an Excel (`.xlsx`/`.xls`) or CSV file with the following columns:

```
QuoteRequestedOn, Status, ReferenceNumber, InsurancePurpose, ICName,
ShoryMakeEn, ShoryModelEn, OverrideIsGccSpec, Age, LicenseIssueDate,
BodyCategory, ChassisNumber, InsuranceType, ManufactureYear,
RegistrationDate, QuotationNo, EstimatedValue, InsuranceExpiryDate,
ErrorText
```

The report splits the data into Third Party Liability (TPL) and Comprehensive (Comp)
quotes, calculates KPIs, highlights failure reasons, and renders multiple charts before
exporting everything into a single self-contained HTML file named `quote_generation_report.html`.

## Prerequisites

* Java 11 or newer
* Maven 3.8+

## Build

```bash
mvn clean package
```

## Usage

```bash
java -jar target/motor-reporting-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/quotes.xlsx
```

### Limiting the reporting window

The generated reports can be restricted to a specific `QuoteRequestedOn` date range.
Set the inclusive boundaries via environment variables or JVM system properties using
the ISO `yyyy-MM-dd` format:

```bash
REPORT_START_DATE=2024-01-01 REPORT_END_DATE=2024-01-31 \
  java -jar target/motor-reporting-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/quotes.xlsx
```

Alternatively, use `-Dreport.startDate=...` and `-Dreport.endDate=...` when launching
the JVM. Records without a `QuoteRequestedOn` value are skipped whenever a filter is
active.

The command now executes three sequential steps:

1. Cleans the dataset and writes `quote_generation_cleaned.csv` beside the source file.
2. Builds the HTML dashboard `quote_generation_report.html` in the same directory.
3. Generates the PDF summary `quote_generation_report.pdf` alongside the other outputs.

For CSV inputs simply point the command at the `.csv` file and the same trio of artifacts will be
created next to it.

### Running from an IDE

If you prefer to run the application from your IDE, use the `com.example.motorreporting.MotorReportingMain`
class. Supply the input file name (for example `quotes_sample.csv`) as the program argument and the
application will resolve it relative to the bundled `source data` directory.

### Sample data

A sample CSV file is included at `source data/quotes_sample.csv` so you can generate a report without
providing your own data.

## Output

The HTML report includes:

* A navigation bar linking to the overview, TPL, Comprehensive, and Failure sections
* KPI cards with total quotes, pass/fail counts, fail percentage, and value lost
* Quote distribution, failure reason, and manufacture year charts powered by Chart.js
* Optional daily trend line when quote request dates are available
* Interactive DataTables for failed quotes and error catalogues with search, filter, and export options

> **Note:** The generated dashboard references Bootstrap 5, Chart.js, and DataTables assets via their CDNs.
