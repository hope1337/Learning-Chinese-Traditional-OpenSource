#!/usr/bin/env python3
"""PC-only MOE fetch with provenance. Never runs on Android.

Downloads the official stroke-order catalog and per-character XML records,
then converts them to rendering JSON. Only manifests and checksums are
committed; originals and conversions are git-ignored by design.

Example:
  python tools/fetch_moe.py --output tools/data/moe
"""
import argparse
import csv
import io
import json
import re
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.request import Request, urlopen

CATALOG = ("https://stroke-order.learningweb.moe.edu.tw/file/files/"
           "%E5%85%A8%E7%AD%86%E9%A0%86%E5%8B%95%E7%95%AB%E5%B5%8C%E5%85%A5"
           "%E7%A2%BC%E7%B6%B2%E5%9D%80%E5%88%97%E8%A1%A8.csv")
BASE = "https://stroke-order.learningweb.moe.edu.tw/"
LICENSE = "CC BY-NC-ND 3.0 Taiwan"
ATTRIBUTION = "Ministry of Education (Taiwan), https://stroke-order.learningweb.moe.edu.tw/"


def download(url: str, limit: int = 2_000_001) -> bytes:
    with urlopen(Request(url, headers={"User-Agent": "Chinese2-MOE-Fetch/1.0"}),
                 timeout=45) as response:
        return response.read(limit)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    catalog = args.output / "catalog.csv"
    if not catalog.exists():
        catalog.write_bytes(download(CATALOG))
    rows = list(csv.reader(io.StringIO(catalog.read_text(encoding="utf-8-sig"))))[1:]
    codes = sorted({int(r[1]) for r in rows if len(r) >= 4 and r[1].strip().isdigit()})

    def fetch(code: int) -> None:
        target = args.output / f"U+{code:04X}.xml"
        if target.exists():
            return
        for attempt in range(4):
            try:
                page = download(BASE + f"dictFrame.jsp?ID={code}").decode("utf-8-sig")
                matches = re.findall(r'var xml\s*=\s*\{\d+:\s*("(?:\\.|[^"\\])*")', page)
                if len(matches) != 1:
                    raise ValueError("expected one official Word XML")
                raw = json.loads(matches[0]).encode("utf-8")
                (args.output / f"U+{code:04X}.pending.xml").write_bytes(raw)
                Path(args.output / f"U+{code:04X}.pending.xml").replace(target)
                time.sleep(0.1)
                return
            except Exception:
                if attempt == 3:
                    raise
                time.sleep(2 ** attempt)

    failed = []
    with ThreadPoolExecutor(max_workers=2) as pool:
        jobs = {pool.submit(fetch, c): c for c in codes}
        for done, job in enumerate(as_completed(jobs), 1):
            try:
                job.result()
            except Exception as error:  # noqa: BLE001 - collected into report
                failed.append({"codepoint": jobs[job], "error": str(error)})
            if done % 100 == 0:
                print(f"{done}/{len(codes)} processed; {len(failed)} failures", flush=True)
    report = {"catalogUrl": CATALOG, "license": LICENSE,
              "attribution": ATTRIBUTION, "expected": len(codes), "failed": failed}
    (args.output / "fetch-report.json").write_text(
        json.dumps(report, indent=2), encoding="utf-8")
    print(f"Finished {len(codes)} records; {len(failed)} failures", flush=True)
    if failed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
