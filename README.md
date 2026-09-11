# Ethio Bahmni Core (ethbahmnicore)

OpenMRS / Bahmni backend module for Ethiopia-specific features. Currently provides a **CBHI location hierarchy** (Region → Zone → Woreda) that is **separate from** patient residential Address Hierarchy / `person_address`.

Target platform: **Bahmni 0.93 / OpenMRS 2.1.7 / Java 8**.

## Build & deploy

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
mvn clean package
```

Install the omod:

- `omod/target/ethbahmnicore-1.0.0-SNAPSHOT.omod` → OpenMRS *Administration → Manage Modules*, or  
- copy into the OpenMRS modules directory and restart.

Requires module: `webservices.rest`.

## Person attributes (registration contract)

When PaymentMethod = Credit and Credit Information = CBHI, registration should capture CBHI geography as **person attributes** (string names must match exactly):

| Attribute name   | Format           | Created by                          |
|------------------|------------------|-------------------------------------|
| CBHI ID          | String           | Already in DB                       |
| CBHIExpiryDate   | Date             | Already in DB                       |
| CBHI Region      | String           | Liquibase in this module (if missing) |
| CBHI Zone        | String           | Liquibase in this module (if missing) |
| CBHI Woreda      | String           | Liquibase in this module (if missing) |

Frontend should write selected display names onto the patient object, e.g. `patient["CBHI Region"]`, `patient["CBHI Zone"]`, `patient["CBHI Woreda"]` (same pattern as other Bahmni person attributes). Do **not** write these into Address Hierarchy / `person_address`.

Suggested `default_config` placement: `CBHIInformation` section (shown via existing `attributesConditions` for Credit + CBHI).

## REST contract

Base path: `/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation`

### Search / cascade

`GET /openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation`

| Param        | Required | Description |
|--------------|----------|-------------|
| `parentUuid` | no       | Parent location uuid. Omit / empty for root **regions**. |
| `level`      | no       | `REGION` \| `ZONE` \| `WOREDA` |
| `q`          | no       | Case-insensitive name contains search |

Response:

```json
{
  "results": [
    {
      "uuid": "…",
      "name": "AFAR",
      "display": "AFAR",
      "level": "REGION",
      "parentUuid": null
    }
  ]
}
```

Cascading UI flow (same idea as Bahmni top-down address fields):

1. Regions: `GET .../cbhiLocation` (or `?level=REGION`)
2. Zones: `GET .../cbhiLocation?parentUuid=<regionUuid>`
3. Woredas: `GET .../cbhiLocation?parentUuid=<zoneUuid>`
4. Optional filter: add `&q=aba`

Examples:

```bash
# All CBHI regions
curl -u admin:password \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation'

# Zones under a region
curl -u admin:password \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation?parentUuid=<region-uuid>'

# Search woredas by name within a zone
curl -u admin:password \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation?parentUuid=<zone-uuid>&q=ada'
```

### Import hierarchy

Packaged CSV: `classpath:cbhi/cbhi_locations.csv` (~12 regions, ~83 zones, ~826 woredas).

**Do not rely on module startup to seed data** — importing on start blocks OpenMRS (especially on Vagrant). After the module is running, import once:

```bash
# Import only if empty
curl -u admin:password -X POST \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation/import'

# Wipe and reload from packaged CSV
curl -u admin:password -X POST \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation/import?replace=true'
```

Requires privilege: `Ethio Bahmni Core Privilege`.

## Regenerating CSV from Excel

Source workbook (not in git): `../01-01-2017R.xlsx`, sheet `WEREDE AND ZONE LIST`.

```bash
python3 scripts/excel_to_cbhi_csv.py /path/to/01-01-2017R.xlsx
```

Names are stored as in the spreadsheet (underscores, punctuation, quirks). They are **not** mapped to patient Address Hierarchy names.

## What this module does / does not do

- **Does:** CBHI hierarchy table, liquibase person attribute types, REST cascade/search, CSV import.
- **Does not:** Change Address Hierarchy, `person_address`, or bahmnicore. Frontend (`bahmniapps` directive + `default_config`) is out of scope here.
