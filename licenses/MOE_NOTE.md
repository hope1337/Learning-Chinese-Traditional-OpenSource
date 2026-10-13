# MOE reference data — license note

Stroke-order geometry and dictionary sources from the Ministry of Education
of the Republic of China (Taiwan) are used under
**Creative Commons Attribution-NonCommercial-NoDerivatives 3.0 Taiwan
(CC BY-NC-ND 3.0 TW)**.

What this means for this repository:

- **Non-commercial**: this is a personal, non-commercial learning app.
- **No derivatives**: MOE geometry is converted only technically (XML to JSON
  for rendering) without changing stroke shapes, order, or attribution. No new
  distribution or adaptation rights are created.
- **Attribution**: every record keeps `source = "MOE_TAIWAN"`, the official
  site URL, and the license string. The stroke viewer shows
  "Source: Ministry of Education (Taiwan)" at all times.
- **No silent mixing**: PRC and Taiwan stroke standards are tracked with
  separate provenance and are never mixed.

Because this repository is public, **no MOE bytes are committed to git**
(originals or conversions — see `.gitignore`). Reproduce them on the
maintainer PC with:

```powershell
python tools/fetch_moe.py --output tools/data/moe
```

Only small manifests and checksums are committed. The APK bundles converted
assets at build time; a missing asset is a quiet "Not available" state, never
a crash.
