from pathlib import Path


def replace(path: str, old: str, new: str, expected: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{path}: esperado {expected}, encontrado {count}: {old[:100]!r}")
    file.write_text(text.replace(old, new), encoding="utf-8")


# Mantém a construção interna utilizada pela Recrutei sem tornar callback obrigatório fora do endpoint Gupy.
replace(
    "backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java",
    ") {\n}",
    ''') {\n    public CreateCandidateRequest(\n            String companyId,\n            String documentId,\n            String testId,\n            String candidateName,\n            String candidateEmail,\n            URI resultWebhookUrl,\n            BigDecimal accommodationTimeMultiplier,\n            String candidateType,\n            String previousResult\n    ) {\n        this(\n                companyId,\n                documentId,\n                testId,\n                candidateName,\n                candidateEmail,\n                null,\n                null,\n                resultWebhookUrl,\n                accommodationTimeMultiplier,\n                candidateType,\n                previousResult\n        );\n    }\n}''',
)
replace(
    "backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptMapper.java",
    "        candidateAttemptEntity.setCallbackUrl(request.callbackUrl().toString());",
    "        candidateAttemptEntity.setCallbackUrl(request.callbackUrl() == null ? null : request.callbackUrl().toString());",
)

service = "backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java"
replace(
    service,
    '''        assertCompanyMatchesToken(request.companyId(), empresaContext);\n        validateCallbackUrl(request.callbackUrl());\n        String empresaId = empresaContext.empresaId();''',
    '''        assertCompanyMatchesToken(request.companyId(), empresaContext);\n        if ("gupy".equalsIgnoreCase(empresaContext.provider())) {\n            validateCallbackUrl(request.callbackUrl());\n        }\n        String empresaId = empresaContext.empresaId();''',
)
replace(
    service,
    "        candidateAttemptEntity.setCallbackUrl(request.callbackUrl().toString());",
    "        candidateAttemptEntity.setCallbackUrl(request.callbackUrl() == null ? null : request.callbackUrl().toString());",
)

# O navegador já realiza GET diretamente na callback_url; não é necessário endpoint intermediário.
redirect_controller = Path("backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateCompletionRedirectController.java")
if not redirect_controller.exists():
    raise RuntimeError("Controller temporário de redirecionamento não encontrado.")
redirect_controller.unlink()

candidate_test = "backend/src/test/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptControllerTest.java"
replace(
    candidate_test,
    "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;\n\n",
    "",
)
replace(
    candidate_test,
    '''    @Test\n    void completedGupyAttemptRedirectsBrowserToCallbackUrl() throws Exception {\n        MvcResult createResult = createAttemptResult("candidate-final-redirect");\n        String responseBody = createResult.getResponse().getContentAsString();\n        String attemptId = attemptIdFromResponse(responseBody);\n        String publicToken = tokenFromResponse(responseBody);\n\n        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")\n                        .contentType(MediaType.APPLICATION_JSON)\n                        .content("""\n                                {\n                                  "nodeId": "turno-1",\n                                  "optionId": "opcao-equilibrada"\n                                }\n                                """))\n                .andExpect(status().isOk())\n                .andExpect(jsonPath("$.finalizado").value(true));\n\n        mockMvc.perform(get("/candidate/attempts/" + publicToken + "/redirect"))\n                .andExpect(status().isFound())\n                .andExpect(header().string("Location", "https://cliente.gupy.io/candidate-return"));\n    }\n\n''',
    "",
)

# Documentação alinhada ao redirecionamento direto feito pelo frontend.
doc = "docs/INTEGRACAO-GUPY-PROVEDOR.md"
replace(
    doc,
    "| Callback GET após conclusão | O navegador acessa `/candidate/attempts/{token}/redirect` e recebe `302` para `callback_url` | Compatível tecnicamente |",
    "| Callback GET após conclusão | O frontend navega para `callback_url`, fazendo o GET no navegador da pessoa candidata | Compatível tecnicamente |",
)
replace(
    doc,
    "Após a conclusão, a API pública devolve `redirectUrl` à tela. O frontend navega para `/candidate/attempts/{token}/redirect`, e esse endpoint responde `302 Location` para a `callback_url` recebida da Gupy. Assim, o GET final ocorre no navegador da pessoa candidata.",
    "Após a conclusão, a API pública devolve `redirectUrl` à tela. O frontend navega diretamente para a `callback_url` recebida da Gupy, fazendo o GET final no navegador da pessoa candidata.",
)
replace(
    doc,
    '''  Praxis-->>Candidato: redirectUrl após a resposta final\n  Candidato->>Praxis: GET /candidate/attempts/{token}/redirect\n  Praxis-->>Candidato: 302 Location: callback_url\n  Candidato->>Gupy: GET callback_url''',
    '''  Praxis-->>Candidato: redirectUrl após a resposta final\n  Candidato->>Gupy: GET callback_url''',
)

print("Ajustes finais do callback Gupy aplicados.")
