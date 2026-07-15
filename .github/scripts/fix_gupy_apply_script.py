from pathlib import Path

path = Path('.github/scripts/apply_gupy_callback_contract.py')
text = path.read_text(encoding='utf-8')
old = '"testId", request.testId(),\\n                         "simulationVersionId"'
new = '"testId", request.testId(),\\n                        "simulationVersionId"'
count = text.count(old)
if count != 2:
    raise RuntimeError(f'Esperadas 2 ocorrências no aplicador, encontradas {count}.')
path.write_text(text.replace(old, new), encoding='utf-8')
