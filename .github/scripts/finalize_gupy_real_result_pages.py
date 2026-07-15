from pathlib import Path


def replace_exact(path: str, old: str, new: str, expected: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != expected:
        raise RuntimeError(f"{path}: esperado {expected} ocorrência(s), encontrado {actual}: {old!r}")
    file.write_text(text.replace(old, new), encoding="utf-8")


route = "frontend/src/routes/candidato.$token.resultado.tsx"
replace_exact(
    route,
    'import { CheckCircle2, Clock3, ExternalLink, ShieldCheck } from "lucide-react";\n',
    'import { CheckCircle2, Clock3, ExternalLink, ShieldCheck } from "lucide-react";\nimport type { ReactNode } from "react";\n',
)
replace_exact(route, "  icon: React.ReactNode;", "  icon: ReactNode;")

doc = "docs/INTEGRACAO-GUPY-PROVEDOR.md"
replace_exact(
    doc,
    "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono, redirecionamento final e o endpoint oficial de consulta do resultado. Permanecem divergências de tipos e enums que ainda impedem declarar a integração homologada.",
    "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono, redirecionamento final, consulta oficial do resultado e páginas web reais para recrutador e candidato. Permanecem divergências de tipos e enums que ainda impedem declarar a integração homologada.",
)
replace_exact(
    doc,
    "| `result_candidate_page_url` como página para a pessoa candidata | Hoje aponta para `/candidate/attempts/{attemptId}`, que é uma API JSON | **Incompatível para experiência de navegador** |",
    "| `result_page_url` para recrutador | Aponta para `/results/{attemptId}`, página autenticada com competências, respostas e decisão humana | Compatível |\n| `result_candidate_page_url` para candidato | Aponta para `/candidato/{token}/resultado`, página assinada e limitada a status, avaliação e retorno ao ATS | Compatível |",
)
replace_exact(
    doc,
    '  "result_page_url": "https://app.exemplo.com/test/result/res_123",\n  "result_candidate_page_url": "https://app.exemplo.com/candidate/attempts/att_123",',
    '  "result_page_url": "https://app.exemplo.com/results/att_123",\n  "result_candidate_page_url": "https://app.exemplo.com/candidato/<token-assinado>/resultado",',
)
replace_exact(
    doc,
    "```\n\nMapeamento de status:",
    "```\n\n`result_page_url` abre a página autenticada do recrutador. `result_candidate_page_url` usa token assinado e abre uma página separada que não expõe pontuação, respostas, e-mail ou regras internas; ela mostra apenas o estado da participação, a avaliação e o retorno ao processo seletivo.\n\nMapeamento de status:",
)
replace_exact(
    doc,
    "  Gupy->>Praxis: GET /test/result/{resultId}\n```",
    "  Gupy->>Praxis: GET /test/result/{resultId}\n  Gupy-->>Recrutador: abre /results/{attemptId}\n  Gupy-->>Candidato: abre /candidato/{token}/resultado\n```",
)
replace_exact(
    doc,
    "1. Definir compatibilidade de tipos para `company_id` e `document_id`.\n2. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n3. Corrigir `result_candidate_page_url` para uma página de navegador.\n4. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n5. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.",
    "1. Definir compatibilidade de tipos para `company_id` e `document_id`.\n2. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n3. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n4. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.",
)
replace_exact(
    doc,
    "- [ ] Validar o `TestResult` exibido para empresa e candidato.",
    "- [x] Validar páginas reais do `TestResult` para recrutador e candidato, incluindo isolamento de dados.",
)

print("Páginas reais da Gupy finalizadas.")
