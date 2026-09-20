# Ethio Bahmni Core (ethbahmnicore)

OpenMRS / Bahmni backend module for Ethiopia-specific features:

1. **CBHI location hierarchy** (Region → Zone → Woreda), separate from patient residential Address Hierarchy.
2. **MRU / Registration Fee eligibility** against Odoo invoices for the current payment window (Cash / Free / Credit rules).

Target platform: **Bahmni 0.93 / OpenMRS 2.1.7 / Java 8**.

## Package layout

Feature-oriented packages (similar spirit to bahmnicore):

```
org.openmrs.module.ethbahmnicore
  cbhi/                 # CBHI geography model, dao, service, importer
  registrationfee/      # eligibility contract, Odoo client, service, advice
  scaffold/             # archetype Item sample (legacy)
  web/v1_0/controller/  # REST controllers
```

## Build & deploy

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
mvn clean package
```

Install the omod:

- `omod/target/ethbahmnicore-1.0.0-SNAPSHOT.omod` → OpenMRS *Administration → Manage Modules*, or  
- copy into the OpenMRS modules directory and restart.

Requires module: `webservices.rest`.

---

## Registration Fee / MRU payment eligibility

### Business rules

After a visit exists and a Registration Fee (MRU) order/invoice has been created, clinical and other services must not proceed unless the patient has a **valid MRU invoice for the current payment window**.

| Payment method | Allowed when Odoo `account.invoice` … |
|----------------|----------------------------------------|
| **Cash** | `state = paid` |
| **Free** | `state = paid` (Free is recorded as paid with 100% discount in Odoo) |
| **Credit** | invoice exists and `state = open` (fee collected later; do **not** require paid) |

Important:

- Validity is for the **current payment window only** — not any historical paid MRU.
- Free / Credit are **not** OpenMRS-side exemptions. Cashiers confirm them in Odoo; OpenMRS verifies invoice state there.
- Draft sale orders (atomfeed sync before cashier confirm) do **not** count — there must be an invoice.
- This module does **not** use bahmnicore `/paymentstatus` or `bahmni.sqlGet.orderUuid`.

### Config

#### OpenMRS global properties (non-secrets)

| Property | Default | Meaning |
|----------|---------|---------|
| `ethbahmnicore.enforceRegistrationFee` | `false` | When `true`, deny clinical encounter/order writes without valid MRU payment |
| `ethbahmnicore.registrationFee.paymentWindowDays` | `30` | Current window length (days) |
| `ethbahmnicore.odoo.host` | `localhost` | Odoo host |
| `ethbahmnicore.odoo.port` | `8069` | Odoo XML-RPC port |
| `ethbahmnicore.odoo.database` | `odoo` | Odoo DB name |
| `ethbahmnicore.registrationFee.shopName` | `MRU` | Odoo `sale.shop` name |
| `ethbahmnicore.registrationFee.productNames` | `Regular Registration Fee,Emergency Registration Fee` | Fee product/concept names |
| `ethbahmnicore.registrationFee.orderTypeName` | `Registration Fee` | Order type allowed without eligibility |
| `ethbahmnicore.registrationFee.allowedEncounterTypes` | `REG,Registration` | Encounter types allowed without eligibility (Bahmni uses `REG`) |

#### Environment variables (secrets)

Set on the OpenMRS / Tomcat process — **not** in global properties:

| Variable | Meaning |
|----------|---------|
| `ETHBAHMNICORE_ODOO_USER` | Odoo XML-RPC username (**read-only** technical user; do not use `admin`) |
| `ETHBAHMNICORE_ODOO_PASSWORD` | Odoo XML-RPC password |

Example (systemd / setenv):

```bash
export ETHBAHMNICORE_ODOO_USER=mru_fee_reader
export ETHBAHMNICORE_ODOO_PASSWORD='…'
```

#### Frontend app config (for later UI migration)

Replace the old dual flags (`disable billing` / `disable checking`) with:

```json
"paymentConfig": {
  "enforceRegistrationFee": true,
  "payment window": 30
}
```

- UI should call the eligibility API below when `enforceRegistrationFee` is true.
- Backend enforcement is driven by the OpenMRS GP `ethbahmnicore.enforceRegistrationFee` (source of truth for hard deny). Keep app config and GP aligned.

### REST contract

`GET /openmrs/ws/rest/v1/ethbahmnicore/registrationFee/eligibility`

| Param | Required |
|-------|----------|
| `patientUuid` **or** `identifier` | one required |

Example response:

```json
{
  "allowed": true,
  "status": "CASH_PAID_OK",
  "reason": "Cash invoice is paid",
  "paymentMethod": "Cash",
  "invoiceId": 1234,
  "invoiceState": "paid",
  "windowDays": 30,
  "windowStart": "2026-08-15",
  "windowEnd": "2026-09-13",
  "enforceRegistrationFee": true,
  "patientUuid": "…",
  "identifier": "GAN200000"
}
```

Status codes include: `CASH_PAID_OK`, `FREE_PAID_OK`, `CREDIT_OPEN_OK`, `CASH_UNPAID`, `FREE_UNPAID`, `CREDIT_MISSING_OR_CLOSED`, `NO_INVOICE`, `OUTSIDE_WINDOW`, `ENFORCEMENT_DISABLED`, `PATIENT_NOT_FOUND`, `ODOO_UNAVAILABLE`, `ODOO_NOT_CONFIGURED`, `UNKNOWN_PAYMENT_METHOD`.

```bash
curl -u admin:password \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/registrationFee/eligibility?patientUuid=<uuid>'

curl -u admin:password \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/registrationFee/eligibility?identifier=GAN200000'
```

### What is blocked vs allowed (when `enforceRegistrationFee=true`)

| Operation | Behaviour |
|-----------|-----------|
| **Visit** create / start / end | **Allowed** (visit triggers Registration Fee billing) |
| Encounter / order for **Registration Fee** (order type / fee concepts / allowed encounter types) | **Allowed** |
| Other **encounter** saves (clinical consultation, obs, non-fee orders) | **Denied** if no valid MRU invoice |
| Other **order** saves (lab, drug, radiology, etc.) | **Denied** if no valid MRU invoice |

When enforce is `false`, APIs still evaluate when possible but always return `allowed=true` with status `ENFORCEMENT_DISABLED`; advice does not deny.

When enforce is `true` and Odoo is unreachable / unconfigured → **fail closed** (`ODOO_UNAVAILABLE` / `ODOO_NOT_CONFIGURED`).

### Odoo read-only user (recommended privileges)

Create a dedicated internal user (example login `mru_fee_reader`). Do **not** use `admin`.

**Minimum access**

1. Group: **Internal User** / **Employees / Employee** (`base.group_user`) so XML-RPC login works and related `sale.shop` fields on invoices are readable.
2. Group: **Accounting & Finance / Billing** (`account.group_account_invoice`) so `account.invoice` can be searched.
3. **Read** access (Access Rights / `ir.model.access`) on:
   - `res.partner` — find patient by `uuid` / `ref`
   - `account.invoice` — invoice `state`, `payment_method`, `date_invoice`, `shop_id`, `partner_id`, `patient_partner_id`
   - `sale.shop` — filter `shop_id.name = MRU` (often covered by Employee)
   - optionally `account.invoice.line` / `product.product` if you later tighten product filters

Without Billing, Odoo returns an ACL fault that ethbahmnicore maps to `ODOO_UNAVAILABLE`. Without Employee, filtering by `shop_id.name` fails the same way.

Set `ETHBAHMNICORE_ODOO_USER` / `ETHBAHMNICORE_ODOO_PASSWORD` to that user.

Invoice lookup axes: patient partner (`uuid` or `ref`) + shop `MRU` + `date_invoice` in window + Cash/Free/Credit state rules. Credit invoices may bill a payer partner; patient is matched via `patient_partner_id`.

---

## Person attributes (credit registration contract)

When PaymentMethod = Credit, registration captures subtype-specific person attributes (string names must match exactly).

### CBHI

| Attribute name   | Format           | Notes                               |
|------------------|------------------|-------------------------------------|
| CBHI ID          | String           | Already in DB                       |
| CBHIExpiryDate   | Date             | Already in DB                       |
| CBHI Region      | String           | Cascading autocomplete (required)   |
| CBHI Zone        | String           | Cascading autocomplete (required)   |
| CBHI Woreda      | String           | Cascading autocomplete (required)   |
| CBHI Kebele      | String           | Free text (optional)                |

### SHI

| Attribute name | Format | Notes |
|----------------|--------|-------|
| SHI ID         | String | Required |
| SHI Region / Zone / Woreda | String | Cascading autocomplete (required) |
| SHI Kebele     | String | Free text (required) |

### Insurance

| Attribute name | Format | Notes |
|----------------|--------|-------|
| Police Officer Name | String | Required |
| Police Officer Phone | String | Required |
| Insurance Region / Zone / Woreda | String | Cascading autocomplete (required) |

Region/Zone/Woreda values reuse the CBHI location hierarchy API. Kebele is free text (not in the hierarchy CSV). Do **not** write these into Address Hierarchy / `person_address`.

## CBHI REST contract

Base path: `/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation`

### Search / cascade

`GET /openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation`

| Param        | Required | Description |
|--------------|----------|-------------|
| `parentUuid` | no       | Parent location uuid. Omit / empty for root **regions**. |
| `level`      | no       | `REGION` \| `ZONE` \| `WOREDA` |
| `q`          | no       | Case-insensitive name contains search |

```bash
curl -u admin:password \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation'
```

### Import hierarchy

```bash
curl -u admin:password -X POST \
  'http://localhost/openmrs/ws/rest/v1/ethbahmnicore/cbhiLocation/import'
```

Requires privilege: `Ethio Bahmni Core Privilege`.

## Regenerating CBHI CSV from Excel

```bash
python3 scripts/excel_to_cbhi_csv.py /path/to/01-01-2017R.xlsx
```

## What this module does / does not do

- **Does:** CBHI hierarchy; MRU registration-fee eligibility API; optional hard enforcement on encounter/order saves; Odoo invoice lookup for current window.
- **Does not:** Modify bahmnicore; use bahmnicore `paymentstatus`; block visit creation; treat Free/Credit as OpenMRS-only exemptions; accept historical MRU outside the window; change frontend bahmniapps (API is ready for UI migration).
