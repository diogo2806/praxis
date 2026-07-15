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

old_signature = """    '''            CandidateAttempt attempt,\\n            PublishedSimulation simulation,\\n            RegistrarRespostaRequest request\\n    ) {''',
    '''            CandidateAttempt attempt,\\n            PublishedSimulation simulation,\\n            RegistrarRespostaRequest request,\\n            CandidateAttemptEntity candidateAttemptEntity\\n    ) {''',"""
new_signature = """    '''    private Optional<RegistrarRespostaResponse> handleDuplicate(\\n            CandidateAttempt attempt,\\n            PublishedSimulation simulation,\\n            RegistrarRespostaRequest request\\n    ) {''',
    '''    private Optional<RegistrarRespostaResponse> handleDuplicate(\\n            CandidateAttempt attempt,\\n            PublishedSimulation simulation,\\n            RegistrarRespostaRequest request,\\n            CandidateAttemptEntity candidateAttemptEntity\\n    ) {''',"""
if text.count(old_signature) != 1:
    raise RuntimeError('Bloco da assinatura de handleDuplicate não encontrado no aplicador.')
text = text.replace(old_signature, new_signature)

path.write_text(text, encoding='utf-8')
