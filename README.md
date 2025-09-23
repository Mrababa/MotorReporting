# MotorReporting

Generate a PDF summary for insurance quote attempts. The application ingests either an
Excel (`.xlsx`/`.xls`) or CSV file with the following columns:

```
QuoteRequestedOn, Status, ReferenceNumber, InsurancePurpose, ICName,
ShoryMakeEn, ShoryModelEn, OverrideIsGccSpec, Age, LicenseIssueDate,
BodyCategory, ChassisNumber, InsuranceType, ManufactureYear,
RegistrationDate, QuotationNo, EstimatedValue, InsuranceExpiryDate,
ErrorText
```

The report splits the data into Third Party Liability (TPL) and Comprehensive (Comp)
quotes, calculates KPIs, highlights failure reasons, and renders multiple charts before
exporting everything into a single PDF named `quote_generation_report.pdf`.

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

The PDF is created in the same directory as the input file. For CSV inputs simply point
the command at the `.csv` file.

### Running from an IDE

If you prefer to run the application from your IDE, use the `com.example.motorreporting.MotorReportingMain`
class. Supply the input file name (for example `quotes_sample.csv`) as the program argument and the
application will resolve it relative to the bundled `source data` directory.

### Sample data

A sample CSV file is included at `source data/quotes_sample.csv` so you can generate a report without
providing your own data.

## Output

The PDF report includes:

* Overall totals and failure percentages
* Grouped statistics for TPL and Comprehensive quotes
* Failure reason pie charts per group
* Bar chart comparing failures by manufacture year
* KPI summary chart
* Recommendations based on the top failure reasons
