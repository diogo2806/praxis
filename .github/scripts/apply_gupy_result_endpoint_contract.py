from pathlib import Path
import re


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: esperado 1 ocorrência, encontrado {count}")
    return text.replace(old, new)


def sub_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise RuntimeError(f"{label}: esperado 1 ocorrência, encontrado {count}")
    return updated


controller = "backend/src/main/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationController.java"
text = read(controller)
text = replace_once(
    text,
    """     * <p>Devolve a pontuação e o desempenho por competência do candidato.\n     * Exige a empresa (company_id) para garantir que o resultado pertence a\n     * quem está consultando.</p>""",
    """     * <p>Devolve a pontuação e o desempenho por competência do candidato.\n     * A empresa é resolvida exclusivamente pelo token Bearer da integração,\n     * conforme o contrato oficial da Gupy.</p>""",
    "javadoc do endpoint Gupy",
)
text = text.replace("     * @param companyId identificador da empresa dona do resultado\n", "")
text = sub_once(
    text,
    r'''    public ResponseEntity<TestResultResponse> getTestResult\(\s*\n\s*@RequestHeader\(name = "Authorization", required = false\) String authorization,\s*\n\s*@PathVariable String resultId,\s*\n\s*@RequestParam\(name = "company_id"\) String companyId\s*\n\s*\) \{\s*\n\s*IntegrationEmpresaContext empresaContext = integrationAuthService\.validateBearerToken\(authorization, PROVIDER\);\s*\n\s*return ResponseEntity\.ok\(candidateAttemptService\.findResult\(resultId, companyId, empresaContext\)\);\s*\n\s*\}''',
    '''    public ResponseEntity<TestResultResponse> getTestResult(\n            @RequestHeader(name = "Authorization", required = false) String authorization,\n            @PathVariable String resultId\n    ) {\n        IntegrationEmpresaContext empresaContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);\n        return ResponseEntity.ok(\n                candidateAttemptService.findResult(resultId, empresaContext.companyId(), empresaContext)\n        );\n    }''',
    "método getTestResult Gupy",
)
write(controller, text)

mapper = "backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java"
text = read(mapper)
text = text.replace("import java.net.URLEncoder;\n\n", "")
text = text.replace("import java.nio.charset.StandardCharsets;\n\n", "")
text = replace_once(text, "resultPageUrl(attempt.resultId(), attempt.companyId())", "resultPageUrl(attempt.resultId())", "mapper domínio")
text = replace_once(text, "resultPageUrl(attempt.getResultId(), attempt.getCompanyId())", "resultPageUrl(attempt.getResultId())", "mapper entidade")
text = sub_once(
    text,
    r'''    private String resultPageUrl\(String resultId, String companyId\) \{\s*\n\s*return praxisProperties\.publicBaseUrl\(\) \+ "/test/result/" \+ resultId\s*\n\s*\+ "\?company_id=" \+ URLEncoder\.encode\(companyId, StandardCharsets\.UTF_8\);\s*\n\s*\}''',
    '''    private String resultPageUrl(String resultId) {\n        return praxisProperties.publicBaseUrl() + "/test/result/" + resultId;\n    }''',
    "resultPageUrl",
)
write(mapper, text)

gupy_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java"
text = read(gupy_test)
text = sub_once(
    text,
    r'''\n    @Test\n    void getTestResultRequiresMatchingCompanyId\(\) throws Exception \{.*?\n    \}\n(?=\n    @Test)''',
    "\n",
    "teste antigo de company_id",
)
text, removed_params = re.subn(r'''\n\s*\.param\("company_id", "[^"]+"\)''', "", text)
if removed_params < 1:
    raise RuntimeError("teste Gupy: nenhum company_id removido")
text = replace_once(
    text,
    '''                .andExpect(jsonPath("$.company_result_string").exists())\n                .andExpect(content().string(containsString("\\\"title\\\":\\\"Empatia\\\"")))''',
    '''                .andExpect(jsonPath("$.company_result_string").exists())\n                .andExpect(jsonPath("$.result_page_url").value(containsString("/test/result/" + resultId)))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))))\n                .andExpect(content().string(containsString("\\\"title\\\":\\\"Empatia\\\"")))''',
    "asserções de result_page_url",
)
write(gupy_test, text)

isolation_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/EmpresaIsolationTest.java"
text = read(isolation_test)
text, removed_params = re.subn(r'''\n\s*\.param\("company_id", "[^"]+"\)''', "", text)
if removed_params != 2:
    raise RuntimeError(f"isolamento: esperado remover 2 parâmetros, removidos {removed_params}")
write(isolation_test, text)

doc = "docs/INTEGRACAO-GUPY-PROVEDOR.md"
text = read(doc)
replacements = [
    (
        "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono e redirecionamento final. Permanecem divergências no endpoint de resultado, tipos e enums que ainda impedem declarar a integração homologada.",
        "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono, redirecionamento final e o endpoint oficial de consulta do resultado. Permanecem divergências de tipos e enums que ainda impedem declarar a integração homologada.",
        "resumo",
    ),
    (
        "| `GET /test/result/{resultId}` somente com `resultId` | Exige também `?company_id=...` | **Incompatível** |",
        "| `GET /test/result/{resultId}` somente com `resultId` | Implementado sem query adicional; empresa resolvida pelo token | Compatível |",
        "tabela de compatibilidade",
    ),
    ("GET /test/result/res_123?company_id=empresa-123", "GET /test/result/res_123", "exemplo de chamada"),
    (
        """- Bearer token;\n- empresa associada ao token;\n- correspondência entre o `company_id` da query e o token;\n- propriedade do resultado pela empresa;\n- existência do resultado.\n\nA query `company_id` é uma proteção adicional interna, mas não aparece no endpoint oficial publicado pela Gupy. Para homologação, o isolamento deve continuar sendo feito pelo token sem alterar a assinatura esperada pela Gupy.""",
        """- Bearer token;\n- empresa e `company_id` associados ao token;\n- propriedade do resultado pela empresa autenticada;\n- existência do resultado.\n\nO endpoint não recebe parâmetros de query. O isolamento permanece garantido pelo token, pelo `empresaId` e pelo `companyId` resolvidos em `integration_tokens`.""",
        "regras de segurança",
    ),
    (
        '"result_page_url": "https://app.exemplo.com/test/result/res_123?company_id=empresa-123"',
        '"result_page_url": "https://app.exemplo.com/test/result/res_123"',
        "result_page_url",
    ),
    (
        "Gupy->>Praxis: GET /test/result/{resultId}?company_id=...",
        "Gupy->>Praxis: GET /test/result/{resultId}",
        "diagrama",
    ),
    (
        """1. Remover a obrigatoriedade de `company_id` da query do endpoint de resultado, mantendo isolamento pelo token.\n2. Definir compatibilidade de tipos para `company_id` e `document_id`.\n3. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n4. Corrigir `result_candidate_page_url` para uma página de navegador.\n5. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n6. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.""",
        """1. Definir compatibilidade de tipos para `company_id` e `document_id`.\n2. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n3. Corrigir `result_candidate_page_url` para uma página de navegador.\n4. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n5. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.""",
        "bloqueadores",
    ),
    (
        "- [ ] Validar `GET /test/result/{resultId}` sem parâmetros extras.",
        "- [x] Validar `GET /test/result/{resultId}` sem parâmetros extras e com isolamento pelo token.",
        "checklist",
    ),
]
for old, new, label in replacements:
    text = replace_once(text, old, new, f"documentação: {label}")
write(doc, text)

for path in (controller, mapper, doc):
    if "?company_id=" in read(path):
        raise RuntimeError(f"{path}: ainda contém query company_id no contrato Gupy")

print("Contrato GET /test/result/{resultId} aplicado com sucesso.")
