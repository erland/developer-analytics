#!/usr/bin/env python3
import base64
import hashlib
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: hash-auth-token.py <raw-token>")

digest = hashlib.sha256(sys.argv[1].encode("utf-8")).digest()
print(base64.urlsafe_b64encode(digest).decode("ascii").rstrip("="))
