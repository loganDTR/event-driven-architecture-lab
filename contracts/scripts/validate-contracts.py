from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import json

from jsonschema import Draft202012Validator
from jsonschema.exceptions import SchemaError, ValidationError


PROJECT_ROOT = Path(__file__).resolve().parents[2]
CONTRACTS_DIR = PROJECT_ROOT / "contracts"
REGISTRY_ORDER_CREATED_DIR = CONTRACTS_DIR / "registry" / "order-created"
EXAMPLES_DIR = CONTRACTS_DIR / "examples"


@dataclass(frozen=True)
class ContractCase:
    name: str
    schema_file: Path
    valid_examples: tuple[Path, ...]
    invalid_examples: tuple[Path, ...]


CONTRACTS = (
    ContractCase(
        name="order-created-v1",
        schema_file=REGISTRY_ORDER_CREATED_DIR / "v1.schema.json",
        valid_examples=(EXAMPLES_DIR / "order-created.valid.json",),
        invalid_examples=(EXAMPLES_DIR / "order-created.invalid.json",),
    ),
    ContractCase(
        name="order-created-v2",
        schema_file=REGISTRY_ORDER_CREATED_DIR / "v2.schema.json",
        valid_examples=(EXAMPLES_DIR / "order-created.v2.valid.json",),
        invalid_examples=(EXAMPLES_DIR / "order-created.invalid.json",),
    ),
    ContractCase(
        name="order-created-breaking",
        schema_file=REGISTRY_ORDER_CREATED_DIR / "breaking.schema.json",
        valid_examples=(),
        invalid_examples=(
            EXAMPLES_DIR / "order-created.valid.json",
            EXAMPLES_DIR / "order-created.v2.valid.json",
        ),
    ),
)


def read_json(path: Path) -> object:
    try:
        with path.open("r", encoding="utf-8") as file:
            return json.load(file)
    except json.JSONDecodeError as exception:
        raise ValueError(
            f"JSON non valido in {path.relative_to(PROJECT_ROOT)}: "
            f"linea {exception.lineno}, colonna {exception.colno}: {exception.msg}"
        ) from exception


def validate_schema(case: ContractCase) -> bool:
    try:
        schema = read_json(case.schema_file)
        Draft202012Validator.check_schema(schema)
        print(f"[PASS] {case.name}: schema formalmente valido")
        return True
    except ValueError as exception:
        print(f"[FAIL] {case.name}: schema non valido")
        print(f"       {exception}")
        return False
    except SchemaError as exception:
        print(f"[FAIL] {case.name}: schema non valido")
        print(f"       {exception.message}")
        return False


def validate_example(schema_file: Path, example_file: Path, expected_valid: bool) -> bool:
    try:
        schema = read_json(schema_file)
        instance = read_json(example_file)
        Draft202012Validator(schema).validate(instance)
        validation_failed = False
    except ValueError as exception:
        print(f"[FAIL] {example_file.relative_to(PROJECT_ROOT)}: {exception}")
        return False
    except ValidationError as exception:
        validation_failed = True
        validation_message = exception.message

    if expected_valid and not validation_failed:
        print(f"[PASS] {example_file.relative_to(PROJECT_ROOT)}: esempio valido accettato")
        return True
    if not expected_valid and validation_failed:
        print(f"[PASS] {example_file.relative_to(PROJECT_ROOT)}: esempio non valido rifiutato")
        return True

    status = "rifiutato" if expected_valid else "accettato"
    print(f"[FAIL] {example_file.relative_to(PROJECT_ROOT)}: esempio {'valido' if expected_valid else 'non valido'} {status}")
    if expected_valid and validation_failed:
        print(f"       {validation_message}")
    return False


def validate_contract(case: ContractCase) -> bool:
    if not case.schema_file.exists():
        print(f"[FAIL] {case.name}: file schema mancante")
        print(f"       {case.schema_file.relative_to(PROJECT_ROOT)}")
        return False

    if not validate_schema(case):
        return False

    results: list[bool] = []

    for example_file in case.valid_examples:
        if example_file.exists():
            results.append(validate_example(case.schema_file, example_file, True))

    for example_file in case.invalid_examples:
        if example_file.exists():
            results.append(validate_example(case.schema_file, example_file, False))

    if not results:
        print(f"[INFO] {case.name}: nessun esempio presente da validare")
        return True

    return all(results)


def main() -> int:
    print("Validazione contratti evento order.created (registry)")
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
