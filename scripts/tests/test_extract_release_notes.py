import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "extract-release-notes.py"
spec = importlib.util.spec_from_file_location("extract_release_notes", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)


class ReleaseNotesExtractionTest(unittest.TestCase):
    def test_extracts_only_requested_release(self):
        notes = "# Notes\n\n## v1.0.1\n\nNew fix.\n\n## v1.0.0\n\nInitial release.\n"
        self.assertEqual("New fix.\n", module.extract(notes, "v1.0.1"))

    def test_missing_release_fails(self):
        with self.assertRaises(ValueError):
            module.extract("# Notes\n\n## v1.0.0\nText\n", "v1.0.1")

    def test_empty_release_fails(self):
        with self.assertRaises(ValueError):
            module.extract("# Notes\n\n## v1.0.1\n\n## v1.0.0\nText\n", "v1.0.1")


if __name__ == "__main__":
    unittest.main()
