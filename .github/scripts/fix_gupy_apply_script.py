from pathlib import Path

path = Path('.github/scripts/apply_gupy_callback_contract.py')
text = path.read_text(encoding='utf-8')
old = '''                         "testId", request.testId(),\n                         "simulationVersionId", publishedSimulation.versionId(),'''
new = '''                        "testId", request.testId(),\n                        "simulationVersionId", publishedSimulation.versionId(),'''
if old not in text:
    raise RuntimeError('Trecho de auditoria não encontrado no aplicador.')
path.write_text(text.replace(old, new), encoding='utf-8')
