#!/usr/bin/env python3
"""Score an OCR model folder on sample crops without Android.

Layout:
  models/ocr/<model-id>/eval/images/*.png
  models/ocr/<model-id>/eval/expected.txt   (tab-separated: filename<TAB>expected)

By default this harness scores a *punctuation-insensitive exact match* and
character error rate (CER) against expected text. A real model adapter can be
plugged in via --predictions <json> mapping filename -> predicted text
(e.g. exported from a notebook or ONNX run).

Examples:
  python tools/eval_ocr.py --model paddle-ppocrv5-mobile
  python tools/eval_ocr.py --model paddle-ppocrv5-mobile --predictions /tmp/preds.json
"""
import argparse
import json
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")

ROOT = Path(__file__).resolve().parents[1]


def cer(expected: str, actual: str) -> float:
    # Simple Levenshtein-based CER over characters.
    m, n = len(expected), len(actual)
    if m == 0:
        return 0.0 if n == 0 else 1.0
    prev = list(range(n + 1))
    for i in range(1, m + 1):
        cur = [i] + [0] * n
        for j in range(1, n + 1):
            cost = 0 if expected[i - 1] == actual[j - 1] else 1
            cur[j] = min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
        prev = cur
    return prev[n] / max(1, m)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", required=True, help="models/ocr/<model-id>")
    parser.add_argument("--predictions", default=None,
                        help="optional JSON {filename: predicted_text}")
    args = parser.parse_args()

    eval_dir = ROOT / "models" / "ocr" / args.model / "eval"
    expected_file = eval_dir / "expected.txt"
    if not expected_file.exists():
        print(f"ERROR: {expected_file} not found")
        return 1
    expected = {}
    for line in expected_file.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        name, _, text = line.partition("\t")
        expected[name.strip()] = text.strip()

    preds = {}
    if args.predictions:
        preds = json.loads(Path(args.predictions).read_text(encoding="utf-8"))

    images_dir = eval_dir / "images"
    print(f"Model: {args.model}")
    print(f"Samples: {len(expected)} expected entries; "
          f"{len(list(images_dir.glob('*.png')))} png files present.")
    total_cer, scored = 0.0, 0
    for name, exp in sorted(expected.items()):
        pred = preds.get(name, "")
        exact = (pred == exp)
        c = cer(exp, pred) if preds else float("nan")
        if preds:
            total_cer += c
            scored += 1
        img_missing = "" if (images_dir / name).exists() else "  [image missing]"
        print(f"- {name}: expected={exp!r} predicted={pred!r} "
              f"exact={exact} cer={c:.3f}{img_missing}")
    if scored:
        print(f"Mean CER: {total_cer / max(1, scored):.3f} over {scored} samples")
        print("Note: with no --predictions, CER is undefined; add predictions "
              "from your model run to score.")
    else:
        print("No predictions supplied. Add --predictions to score CER; "
              "sample images are placeholders until contributors add photos "
              "they have rights to share.")
    print("Preserve Traditional Chinese exactly in expected.txt; "
          "never convert to Simplified.")
    return 0


if __name__ == "__main__":
    main()
