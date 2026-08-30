#!/usr/bin/env python3
from pathlib import Path

text = Path("scripts/test-large-account-acceptance.sh").read_text(encoding="utf-8")

if "WHEN n % 20 = 0 THEN 'search-target-'" in text:
    raise SystemExit(
        "search-target fixtures must not use n % 20 = 0 because those rows are also "
        "excluded by the n % 10 = 0 private-repository rule"
    )

if "WHEN n % 20 = 1 THEN 'search-target-'" not in text:
    raise SystemExit("expected included search-target fixture pattern is missing")

print("Large-account search fixture consistency check passed.")
