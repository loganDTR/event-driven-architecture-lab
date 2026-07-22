from __future__ import annotations

import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
CONTRACTS_DIR = PROJECT_ROOT / "contracts"
EXAMPLES_DIR = CONTRACTS_DIR / "examples"


@dataclass(frozen=True)
class ContractCase:
    name: str
    schema_file: Path
    valid_example: Path
    invalid_example: Path


CONTRACTS = [
    ContractCase(
        name="order-created",
        schema_file=CONTRACTS_DIR / "order-created.schema.json",
        valid_example=EXAMPLES_DIR / "order-created.valid.json",
        invalid_example=EXAMPLES_DIR / "order-created.invalid.json",
    ),
    ContractCase(
        name="stock-reserved",
        schema_file=CONTRACTS_DIR / "stock-reserved.schema.json",
        valid_example=EXAMPLES_DIR / "stock-reserved.valid.json",
        invalid_example=EXAMPLES_DIR / "stock-reserved.invalid.json",
    ),
    ContractCase(
        name="fraud-checked",
        schema_file=CONTRACTS_DIR / "fraud-checked.schema.json",
        valid_example=EXAMPLES_DIR / "fraud-checked.valid.json",
        invalid_example=EXAMPLES_DIR / "fraud-checked.invalid.json",
    ),
]


def run_command(arguments: list[str]) -> subprocess.CompletedProcess[str]:
    command = [
        sys.executable,
        "-m",
        "check_jsonschema",
        *arguments,
    ]

    return subprocess.run(
        command,
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )


def validate_schema(case: ContractCase) -> bool:
    result = run_command(
        [
            "--check-metaschema",
            str(case.schema_file),
        ]
    )

    if result.returncode == 0:
        print(f"[PASS] {case.name}: schema formalmente valido")
        return True

    print(f"[FAIL] {case.name}: schema non valido")
    print_output(result)
    return False


def validate_valid_example(case: ContractCase) -> bool:
    result = run_command(
        [
            "--schemafile",
            str(case.schema_file),
            str(case.valid_example),
        ]
    )

    if result.returncode == 0:
        print(f"[PASS] {case.name}: esempio valido accettato")
        return True

    print(f"[FAIL] {case.name}: esempio valido rifiutato")
    print_output(result)
    return False


def validate_invalid_example(case: ContractCase) -> bool:
    result = run_command(
        [
            "--schemafile",
            str(case.schema_file),
            str(case.invalid_example),
        ]
    )

    if result.returncode != 0:
        print(f"[PASS] {case.name}: esempio non valido rifiutato")
        print_validation_errors(result)
        return True

    print(f"[FAIL] {case.name}: esempio non valido accettato")
    return False


def print_output(result: subprocess.CompletedProcess[str]) -> None:
    output = "\n".join(
        part.strip()
        for part in [result.stdout, result.stderr]
        if part and part.strip()
    )

    if not output:
        print("       Nessun dettaglio disponibile")
        return

    for line in output.splitlines():
        print(f"       {line}")


def print_validation_errors(
    result: subprocess.CompletedProcess[str],
) -> None:
    output = "\n".join(
        part.strip()
        for part in [result.stdout, result.stderr]
        if part and part.strip()
    )

    for line in output.splitlines():
        stripped = line.strip()

        if stripped.startswith("contracts") or "::$." in stripped:
            print(f"       {stripped}")


def validate_contract(case: ContractCase) -> bool:
    required_files = [
        case.schema_file,
        case.valid_example,
        case.invalid_example,
    ]

    missing_files = [path for path in required_files if not path.exists()]

    if missing_files:
        print(f"[FAIL] {case.name}: file mancanti")

        for path in missing_files:
            print(f"       {path.relative_to(PROJECT_ROOT)}")

        return False

    schema_valid = validate_schema(case)

    if not schema_valid:
        return False

    valid_example_ok = validate_valid_example(case)
    invalid_example_ok = validate_invalid_example(case)

    return valid_example_ok and invalid_example_ok


def main() -> int:
    print("Validazione contratti evento")
    print("=" * 64)

    results: list[bool] = []

    for index, case in enumerate(CONTRACTS):
        if index > 0:
            print()

        results.append(validate_contract(case))

    passed = sum(results)
    total = len(results)

    print()
    print("=" * 64)
    print(f"Contratti superati: {passed}/{total}")

    if all(results):
        print("ESITO: tutti i contratti sono validi")
        return 0

    print("ESITO: uno o più contratti hanno fallito")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())