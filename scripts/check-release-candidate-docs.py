#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
required = {
    "docs/installation-v1.md": [
        "GitHub application setup",
        "Private repository authorisation",
        "Troubleshooting",
    ],
    "docs/operator-v1.md": [
        "Synchronisation recovery",
        "Database migration behaviour",
        "Upgrading images",
    ],
    "docs/postgres-backup-restore.md": [
        "Create a backup",
        "Restore",
    ],
    "docs/privacy-acceptance-v1.md": [
        "Privacy matrix",
    ],
    "docs/mobile-acceptance-v1.md": [
        "Covered primary flows",
    ],
    "docs/large-account-acceptance-v1.md": [
        "Acceptance coverage",
    ],
    "docs/release-compose-quickstart.md": [
        "Pull and start",
    ],
    "docs/automated-release-verification.md": [
        "Release workflow sequence",
    ],
}

errors = []
for relative, markers in required.items():
    path = root / relative
    if not path.exists():
        errors.append(f"Missing required v1 document: {relative}")
        continue
    text = path.read_text(encoding="utf-8")
    for marker in markers:
        if marker not in text:
            errors.append(f"{relative} is missing required section/text: {marker}")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)

print(f"Version 1 release-candidate documentation gate passed ({len(required)} documents).")
