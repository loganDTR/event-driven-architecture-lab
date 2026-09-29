from __future__ import annotations

import runpy
from pathlib import Path


TARGET_SCRIPT = (
    Path(__file__).resolve().parent.parent
    / "contracts"
    / "scripts"
    / "validate-contracts.py"
)

if __name__ == "__main__":
    runpy.run_path(str(TARGET_SCRIPT), run_name="__main__")