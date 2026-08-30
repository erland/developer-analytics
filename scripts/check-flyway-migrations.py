#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
migration_dir = root / "backend/src/main/resources/db/migration"

pattern = re.compile(r"^V(?P<version>\d+)__(?P<description>[a-z0-9_]+)\.sql$")
entries = []
errors = []

for path in sorted(migration_dir.glob("*.sql")):
    match = pattern.match(path.name)
    if not match:
        errors.append(
            f"Invalid Flyway migration filename: {path.name}. "
            "Expected V<number>__lower_snake_case.sql"
        )
        continue
    entries.append((int(match.group("version")), path.name))

if not entries:
    errors.append("No Flyway migrations found")

versions = [version for version, _ in entries]
duplicates = sorted({v for v in versions if versions.count(v) > 1})
if duplicates:
    errors.append("Duplicate Flyway version(s): " + ", ".join(map(str, duplicates)))

if versions:
    actual = sorted(set(versions))
    expected = list(range(actual[0], actual[-1] + 1))
    missing = sorted(set(expected) - set(actual))
    if actual[0] != 1:
        errors.append(f"Migration sequence must start at V1, found V{actual[0]}")
    if missing:
        errors.append("Missing Flyway version(s): " + ", ".join(map(str, missing)))

for version, filename in entries:
    path = migration_dir / filename
    sql = path.read_text(encoding="utf-8").strip()
    if not sql:
        errors.append(f"Empty migration: {filename}")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)

ordered = sorted(entries)
print(f"Flyway migration sequence is contiguous: V{ordered[0][0]}..V{ordered[-1][0]}")
for version, filename in ordered:
    print(f"- V{version}: {filename}")
