from pathlib import Path


def replace_exact(path: str, old: str, new: str, expected: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != expected:
        raise RuntimeError(f"{path}: esperado {expected} ocorrência(s), encontrado {actual}: {old[:160]!r}")
    file.write_text(text.replace(old, new), encoding="utf-8")


controller = "backend/src/main/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationController.java"
replace_exact(
    controller,
    '''     * <p>Devolve a pontuação e o desempenho por competência do candidato.\n     * Exige a empresa (company_id) para garantir que o resultado pertence a\n     * quem está consultando.</p>''',
    '''     * <p>Devolve a pontuação e o desempenho por competência do candidato.\n     * A empresa é resolvida exclusivamente pelo token Bearer da integração,\n     * conforme o contrato oficial da Gupy.</p>''',
)
replace_exact(
    controller,
    '''     * @param resultId identificador do resultado consultado\n     * @param companyId identificador da empresa dona do resultado\n     * @return o resultado da prova no formato esperado pela Gupy''',
    '''     * @param resultId identificador do resultado consultado\n     * @return o resultado da prova no formato esperado pela Gupy''',
)
replace_exact(
    controller,
    '''    public ResponseEntity<TestResultResponse> getTestResult(\n            @RequestHeader(name = "Authorization", required = false) String authorization,\n            @PathVariable String resultId,\n            @RequestParam(name = "company_id") String companyId\n    ) {\n        IntegrationEmpresaContext empresaContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);\n        return ResponseEntity.ok(candidateAttemptService.findResult(resultId, companyId, empresaContext));\n    }''',
    '''    public ResponseEntity<TestResultResponse> getTestResult(\n            @RequestHeader(name = "Authorization", required = false) String authorization,\n            @PathVariable String resultId\n    ) {\n        IntegrationEmpresaContext empresaContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);\n        return ResponseEntity.ok(\n                candidateAttemptService.findResult(resultId, empresaContext.companyId(), empresaContext)\n        );\n    }''',
)

mapper = "backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java"
replace_exact(mapper, "import java.net.URLEncoder;\n\nimport java.nio.charset.StandardCharsets;\n\n", "")
replace_exact(mapper, "resultPageUrl(attempt.resultId(), attempt.companyId())", "resultPageUrl(attempt.resultId())")
replace_exact(mapper, "resultPageUrl(attempt.getResultId(), attempt.getCompanyId())", "resultPageUrl(attempt.getResultId())")
replace_exact(
    mapper,
    '''    private String resultPageUrl(String resultId, String companyId) {\n        return praxisProperties.publicBaseUrl() + "/test/result/" + resultId\n                + "?company_id=" + URLEncoder.encode(companyId, StandardCharsets.UTF_8);\n    }''',
    '''    private String resultPageUrl(String resultId) {\n        return praxisProperties.publicBaseUrl() + "/test/result/" + resultId;\n    }''',
)

gupy_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java"
replace_exact(
    gupy_test,
    '''        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", AUTHORIZATION)\n                        .param("company_id", "empresa-123"))''',
    '''        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", AUTHORIZATION))''',
)
replace_exact(
    gupy_test,
    '''                .andExpect(jsonPath("$.company_result_string").exists())\n                .andExpect(content().string(containsString("\\\"title\\\":\\\"Empatia\\\"")))''',
    '''                .andExpect(jsonPath("$.company_result_string").exists())\n                .andExpect(jsonPath("$.result_page_url").value(containsString("/test/result/" + resultId)))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))))\n                .andExpect(content().string(containsString("\\\"title\\\":\\\"Empatia\\\"")))''',
)
replace_exact(
    gupy_test,
    '''    @Test\n    void getTestResultRequiresMatchingCompanyId() throws Exception {\n        MvcResult createResult = mockMvc.perform(post("/test/candidate")\n                        .header("Authorization", AUTHORIZATION)\n                        .contentType(MediaType.APPLICATION_JSON)\n                        .content(validCandidateRequest("candidate-document-wrong-company")))\n                .andExpect(status().isCreated())\n                .andReturn();\n\n        String responseBody = createResult.getResponse().getContentAsString();\n        String resultId = JsonPath.read(responseBody, "$.test_result_id");\n\n        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", AUTHORIZATION)\n                        .param("company_id", "outra-empresa"))\n                .andExpect(status().isForbidden());\n    }\n\n''',
    "",
)

isolation_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/EmpresaIsolationTest.java"
replace_exact(
    isolation_test,
    '''        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", EMPRESA1_AUTH)\n                        .param("company_id", "empresa-123"))''',
    '''        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", EMPRESA1_AUTH))''',
)
replace_exact(
    isolation_test,
    '''        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", EMPRESA2_AUTH)\n                        .param("company_id", "empresa-456"))''',
    '''        mockMvc.perform(get("/test/result/" + resultId)\n                        .header("Authorization", EMPRESA2_AUTH))''',
)

doc = "docs/INTEGRACAO-GUPY-PROVEDOR.md"
replace_exact(
    doc,
    "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono e redirecionamento final. Permanecem divergências no endpoint de resultado, tipos e enums que ainda impedem declarar a integração homologada.",
    "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono, redirecionamento final e o endpoint oficial de consulta do resultado. Permanecem divergências de tipos e enums que ainda impedem declarar a integração homologada.",
)
replace_exact(
    doc,
    "| `GET /test/result/{resultId}` somente com `resultId` | Exige também `?company_id=...` | **Incompatível** |",
    "| `GET /test/result/{resultId}` somente com `resultId` | Implementado sem query adicional; empresa resolvida pelo token | Compatível |",
)
replace_exact(doc, "GET /test/result/res_123?company_id=empresa-123", "GET /test/result/res_123")
replace_exact(
    doc,
    '''- Bearer token;\n- empresa associada ao token;\n- correspondência entre o `company_id` da query e o token;\n- propriedade do resultado pela empresa;\n- existência do resultado.\n\nA query `company_id` é uma proteção adicional interna, mas não aparece no endpoint oficial publicado pela Gupy. Para homologação, o isolamento deve continuar sendo feito pelo token sem alterar a assinatura esperada pela Gupy.''',
    '''- Bearer token;\n- empresa e `company_id` associados ao token;\n- propriedade do resultado pela empresa autenticada;\n- existência do resultado.\n\nO endpoint não recebe parâmetros de query. O isolamento permanece garantido pelo token, pelo `empresaId` e pelo `companyId` resolvidos em `integration_tokens`.''',
)
replace_exact(
    doc,
    '"result_page_url": "https://app.exemplo.com/test/result/res_123?company_id=empresa-123"',
    '"result_page_url": "https://app.exemplo.com/test/result/res_123"',
)
replace_exact(
    doc,
    "Gupy->>Praxis: GET /test/result/{resultId}?company_id=...",
    "Gupy->>Praxis: GET /test/result/{resultId}",
)
replace_exact(
    doc,
    '''1. Remover a obrigatoriedade de `company_id` da query do endpoint de resultado, mantendo isolamento pelo token.\n2. Definir compatibilidade de tipos para `company_id` e `document_id`.\n3. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n4. Corrigir `result_candidate_page_url` para uma página de navegador.\n5. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n6. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.''',
    '''1. Definir compatibilidade de tipos para `company_id` e `document_id`.\n2. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n3. Corrigir `result_candidate_page_url` para uma página de navegador.\n4. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n5. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.''',
)
replace_exact(
    doc,
    "- [ ] Validar `GET /test/result/{resultId}` sem parâmetros extras.",
    "- [x] Validar `GET /test/result/{resultId}` sem parâmetros extras e com isolamento pelo token.",
)

for path in (controller, mapper, gupy_test, isolation_test, doc):
    text = Path(path).read_text(encoding="utf-8")
    if path != isolation_test and "?company_id=" in text:
        raise RuntimeError(f"{path}: ainda contém query company_id no contrato Gupy")

print("Contrato GET /test/result/{resultId} aplicado com sucesso.")
