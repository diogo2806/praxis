from pathlib import Path

path = Path('.github/scripts/apply_gupy_callback_contract.py')
text = path.read_text(encoding='utf-8')
replacements = [
    ("'''                         \"testId\"", "'''                        \"testId\"", 2),
    ('\\n                         "jobId"', '\\n                        "jobId"', 1),
    ('\\n                         "simulationVersionId"', '\\n                        "simulationVersionId"', 2),
]
for old, new, expected in replacements:
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f'Padrão {old!r}: esperado {expected}, encontrado {count}.')
    text = text.replace(old, new)
path.write_text(text, encoding='utf-8')
