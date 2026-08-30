#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
test_root = root / "backend/src/test/java"
required = {
    "unit",
    "persistence",
    "github-adapter",
    "authorization",
    "worker-job",
    "privacy",
}
counts = {tag: 0 for tag in required}
untagged = []

for path in test_root.rglob("*Test.java"):
    text = path.read_text(encoding="utf-8")
    tags = set(re.findall(r'@Tag\("([^"]+)"\)', text))
    if not tags:
        untagged.append(path.relative_to(root))
    for tag in tags & required:
        counts[tag] += 1

errors = []
for tag, count in sorted(counts.items()):
    if count == 0:
        errors.append(f"Backend test layer '{tag}' has no tests")

if untagged:
    errors.append(
        "Untagged backend tests:\n  " +
        "\n  ".join(str(path) for path in untagged)
    )

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)

print("Backend test layers:")
for tag, count in sorted(counts.items()):
    print(f"- {tag}: {count}")
