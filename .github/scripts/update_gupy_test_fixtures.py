from pathlib import Path


def add_callback(path: str, expected: int) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    marker = '                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",'
    count = text.count(marker)
    if count != expected:
        raise RuntimeError(f"{path}: esperado {expected}, encontrado {count}")
    replacement = (
        '                                  "callback_url": "https://cliente.gupy.io/candidate-return",\n'
        + marker
    )
    file.write_text(text.replace(marker, replacement), encoding="utf-8")


add_callback(
    "backend/src/test/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptSecurityEnabledTest.java",
    3,
)
add_callback(
    "backend/src/test/java/br/com/iforce/praxis/gupy/delivery/controller/ResultDeliveryControllerTest.java",
    1,
)

# Estes helpers usam indentação menor.
for path in [
    "backend/src/test/java/br/com/iforce/praxis/gupy/controller/EmpresaIsolationTest.java",
    "backend/src/test/java/br/com/iforce/praxis/simulation/controller/SimulationAdminControllerTest.java",
]:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    marker = '                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",'
    if text.count(marker) != 1:
        raise RuntimeError(f"{path}: fixture não encontrado")
    text = text.replace(
        marker,
        '                  "callback_url": "https://cliente.gupy.io/candidate-return",\n' + marker,
    )
    file.write_text(text, encoding="utf-8")

print("Fixtures Gupy atualizados com callback_url.")
