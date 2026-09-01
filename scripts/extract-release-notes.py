#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
import re
import sys


def extract(notes: str, version: str) -> str:
    heading = f"## {version}"
    lines = notes.splitlines()
    start = None
    for index, line in enumerate(lines):
        if line.strip() == heading:
            start = index + 1
            break
    if start is None:
        raise ValueError(f"Release notes section {heading!r} was not found")

    end = len(lines)
    for index in range(start, len(lines)):
        if re.match(r"^##\s+\S", lines[index]):
            end = index
            break

    section = "\n".join(lines[start:end]).strip()
    if not section:
        raise ValueError(f"Release notes section {heading!r} is empty")
    return section + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract one release section from RELEASE_NOTES.md")
    parser.add_argument("version", help="Release tag, for example v1.0.1")
    parser.add_argument("notes_file", nargs="?", default="RELEASE_NOTES.md")
    parser.add_argument("output_file", nargs="?")
    args = parser.parse_args()

    notes_path = Path(args.notes_file)
    if not notes_path.is_file():
        print(f"Release notes file does not exist: {notes_path}", file=sys.stderr)
        return 1

    try:
        section = extract(notes_path.read_text(encoding="utf-8"), args.version)
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1

    if args.output_file:
        Path(args.output_file).write_text(section, encoding="utf-8")
    else:
        sys.stdout.write(section)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
