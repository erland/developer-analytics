#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
layer_root = root / "frontend/src/test-layers"
required = {
    "component",
    "feature",
    "responsive",
    "api-error",
    "privacy",
}

errors = []
counts = {}
for layer in sorted(required):
    directory = layer_root / layer
    tests = list(directory.glob("*.test.ts")) + list(directory.glob("*.test.tsx"))
    counts[layer] = len(tests)
    if not tests:
        errors.append(f"Frontend test layer '{layer}' has no tests")

unknown = {
    path.name
    for path in layer_root.iterdir()
    if path.is_dir()
} - required if layer_root.exists() else set()
if unknown:
    errors.append("Unknown frontend test layer(s): " + ", ".join(sorted(unknown)))

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)

print("Frontend test layers:")
for layer, count in sorted(counts.items()):
    print(f"- {layer}: {count}")
