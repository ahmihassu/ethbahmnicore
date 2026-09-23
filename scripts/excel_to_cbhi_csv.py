#!/usr/bin/env python3
"""
One-time converter: Excel CBHI sheet -> module CSV.

Source (outside repo by default):
  ../../FOR EMR.CBHI.xlsx  (wachemo folder)
  Sheet: FOR EMR

Output:
  api/src/main/resources/cbhi/cbhi_locations.csv

Layout notes (FOR EMR sheet):
  Column A = region name index (informational)
  Column C = region header (alone on the row) OR zone name
  Columns D+ = woredas for that zone

Usage:
  python3 scripts/excel_to_cbhi_csv.py [path/to/FOR\\ EMR.CBHI.xlsx]
"""
from __future__ import print_function

import csv
import os
import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from collections import OrderedDict


NS = {
    "m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}
SHEET_NAME = "FOR EMR"


def clean(value):
    if value is None:
        return ""
    value = str(value).replace("\xa0", " ").strip()
    return re.sub(r"\s+", " ", value)


def normalize_region(value):
    name = clean(value)
    # "AFAR  REGION" / "SIDAMA REGION" -> "AFAR" / "SIDAMA"
    if re.search(r"\s+REGION\s*$", name, flags=re.IGNORECASE):
        name = re.sub(r"\s+REGION\s*$", "", name, flags=re.IGNORECASE).strip()
    return name


def normalize_woreda(value):
    """Strip leading serial prefixes common in the Oromia block (e.g. '1Digelu', '00.town')."""
    name = clean(value)
    if not name:
        return ""
    stripped = re.sub(r"^\d+\.?", "", name).strip()
    # Drop placeholder cells that are only a serial number
    if not stripped:
        return ""
    return stripped


def colrow(ref):
    match = re.match(r"([A-Z]+)(\d+)", ref)
    col = 0
    for ch in match.group(1):
        col = col * 26 + (ord(ch) - 64)
    return col - 1, int(match.group(2)) - 1


def resolve_sheet_path(zf, sheet_name):
    wb = ET.fromstring(zf.read("xl/workbook.xml"))
    rels = ET.fromstring(zf.read("xl/_rels/workbook.xml.rels"))
    rid_to_target = {rel.attrib["Id"]: rel.attrib["Target"] for rel in rels}
    for sheet in wb.findall("m:sheets/m:sheet", NS):
        if sheet.attrib.get("name") != sheet_name:
            continue
        rid = sheet.attrib[
            "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
        ]
        target = rid_to_target[rid]
        if not target.startswith("xl/"):
            target = "xl/" + target
        return target
    raise KeyError("Sheet not found: %s" % sheet_name)


def load_sheet_rows(xlsx_path, sheet_name=SHEET_NAME):
    zf = zipfile.ZipFile(xlsx_path)
    ss_root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
    strings = []
    for si in ss_root.findall("m:si", NS):
        texts = [t.text or "" for t in si.findall(".//m:t", NS)]
        strings.append("".join(texts))

    sheet = ET.fromstring(zf.read(resolve_sheet_path(zf, sheet_name)))
    rows = {}
    for cell in sheet.findall(".//m:c", NS):
        ref = cell.attrib.get("r")
        if not ref:
            continue
        col, row = colrow(ref)
        node = cell.find("m:v", NS)
        if node is None or node.text is None:
            is_el = cell.find("m:is", NS)
            if is_el is not None:
                val = "".join(t.text or "" for t in is_el.findall(".//m:t", NS))
            else:
                val = ""
        elif cell.attrib.get("t") == "s":
            val = strings[int(node.text)]
        else:
            val = node.text
        rows.setdefault(row, {})[col] = val
    return rows


def extract_triples(rows):
    """Parse FOR EMR wide layout: region header rows, then zone + woreda columns."""
    region = None
    triples = []
    seen = set()
    for r in range(0, max(rows) + 1):
        row = rows.get(r, {})
        c2 = clean(row.get(2, ""))
        if not c2:
            continue
        woredas = []
        for col in sorted(k for k in row.keys() if k >= 3):
            woreda = normalize_woreda(row.get(col, ""))
            if woreda:
                woredas.append(woreda)
        if not woredas:
            # Region header (column C alone)
            region = normalize_region(c2)
            continue
        if not region:
            continue
        zone = clean(c2)
        for woreda in woredas:
            key = (region, zone, woreda)
            if key in seen:
                continue
            seen.add(key)
            triples.append(key)
    return triples


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    # Repo lives at wachemo/repos/ethbahmnicore; Excel sits in wachemo/
    default_xlsx = os.path.abspath(os.path.join(root, "..", "..", "FOR EMR.CBHI.xlsx"))
    xlsx = sys.argv[1] if len(sys.argv) > 1 else default_xlsx
    out_csv = os.path.join(root, "api", "src", "main", "resources", "cbhi", "cbhi_locations.csv")

    rows = load_sheet_rows(xlsx)
    triples = extract_triples(rows)

    os.makedirs(os.path.dirname(out_csv), exist_ok=True)
    with open(out_csv, "w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["region", "zone", "woreda"])
        writer.writerows(triples)

    regions = OrderedDict()
    for region, zone, woreda in triples:
        regions.setdefault(region, OrderedDict()).setdefault(zone, []).append(woreda)

    print("Wrote", out_csv)
    print(
        "regions=%d zones=%d woredas=%d"
        % (len(regions), sum(len(z) for z in regions.values()), len(triples))
    )
    print("regions:", ", ".join(regions.keys()))


if __name__ == "__main__":
    main()
