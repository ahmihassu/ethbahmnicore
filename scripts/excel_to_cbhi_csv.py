#!/usr/bin/env python3
"""
One-time converter: Excel CBHI sheet -> module CSV.

Source (outside repo by default):
  ../01-01-2017R.xlsx
  Sheet: WEREDE AND ZONE LIST

Output:
  api/src/main/resources/cbhi/cbhi_locations.csv

Layout notes:
  Column C = Region (carry-forward; skip wiki/list headers)
  Column E = Zone (carry-forward)
  Column F = Woreda

Usage:
  python3 scripts/excel_to_cbhi_csv.py [path/to/01-01-2017R.xlsx]
"""
from __future__ import print_function

import csv
import os
import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from collections import OrderedDict


NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
SHEET_TARGET = "worksheets/sheet4.xml"  # WEREDE AND ZONE LIST in this workbook


def clean(value):
    if value is None:
        return ""
    value = str(value).replace("\xa0", " ").strip()
    return re.sub(r"\s+", " ", value)


def is_junk_region(val):
    if not val:
        return True
    low = val.lower()
    if any(
        x in low
        for x in (
            "list of",
            "description:",
            "http://",
            "https://",
            "wikipedia",
            "zones and",
            "districts of",
        )
    ):
        return True
    return len(val) > 40


def colrow(ref):
    match = re.match(r"([A-Z]+)(\d+)", ref)
    col = 0
    for ch in match.group(1):
        col = col * 26 + (ord(ch) - 64)
    return col - 1, int(match.group(2)) - 1


def load_sheet_rows(xlsx_path):
    zf = zipfile.ZipFile(xlsx_path)
    ss_root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
    strings = []
    for si in ss_root.findall("m:si", NS):
        texts = [t.text or "" for t in si.findall(".//m:t", NS)]
        strings.append("".join(texts))

    sheet = ET.fromstring(zf.read("xl/" + SHEET_TARGET))
    rows = {}
    for cell in sheet.findall(".//m:c", NS):
        ref = cell.attrib.get("r")
        if not ref:
            continue
        col, row = colrow(ref)
        node = cell.find("m:v", NS)
        if node is None or node.text is None:
            val = ""
        elif cell.attrib.get("t") == "s":
            val = strings[int(node.text)]
        else:
            val = node.text
        rows.setdefault(row, {})[col] = val
    return rows


def extract_triples(rows):
    region = None
    zone = None
    triples = []
    for r in range(0, max(rows) + 1):
        c2 = clean(rows.get(r, {}).get(2, ""))
        c4 = clean(rows.get(r, {}).get(4, ""))
        c5 = clean(rows.get(r, {}).get(5, ""))
        if c2 and not is_junk_region(c2):
            region = c2
            zone = None
        if c4:
            zone = c4
        if c5 and region and zone:
            triples.append((region, zone, c5))
    return triples


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    default_xlsx = os.path.abspath(os.path.join(root, "..", "01-01-2017R.xlsx"))
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
