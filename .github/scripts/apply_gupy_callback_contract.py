from pathlib import Path


def replace_exact(path: str, old: str, new: str, expected: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != expected:
        raise RuntimeError(f"{path}: esperado {expected} ocorrência(s), encontrado {actual}: {old[:120]!r}")
    file.write_text(text.replace(old, new), encoding="utf-8")


def append_before(path: str, marker: str, content: str) -> None:
    replace_exact(path, marker, content + marker)


# DTO oficial recebido da Gupy.
replace_exact(
    "backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java",
    "import jakarta.validation.constraints.NotBlank;\n\n\nimport java.math.BigDecimal;",
    "import jakarta.validation.constraints.NotBlank;\n\nimport jakarta.validation.constraints.NotNull;\n\n\nimport java.math.BigDecimal;",
)
replace_exact(
    "backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java",
    '''        String candidateEmail,\n\n        @JsonProperty("result_webhook_url")''',
    '''        String candidateEmail,\n\n        @JsonProperty("job_id")\n        @Schema(example = "100", description = "Identificador da vaga na Gupy.")\n        Long jobId,\n\n        @NotNull\n        @JsonProperty("callback_url")\n        @Schema(example = "https://cliente.gupy.io/candidates/return")\n        URI callbackUrl,\n\n        @JsonProperty("result_webhook_url")''',
)

# Persistência do contexto de vaga e retorno.
replace_exact(
    "backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/CandidateAttemptEntity.java",
    '''    @Column(name = "candidate_email", nullable = false, length = 180)\n    private String candidateEmail;\n\n    @Column(name = "result_webhook_url", length = 1000)''',
    '''    @Column(name = "candidate_email", nullable = false, length = 180)\n    private String candidateEmail;\n\n    @Column(name = "gupy_job_id")\n    private Long gupyJobId;\n\n    @Column(name = "callback_url", length = 1000)\n    private String callbackUrl;\n\n    @Column(name = "result_webhook_url", length = 1000)''',
)
replace_exact(
    "backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptMapper.java",
    '''        candidateAttemptEntity.setResultWebhookUrl(request.resultWebhookUrl() == null ? null : request.resultWebhookUrl().toString());\n        return candidateAttemptEntity;''',
    '''        candidateAttemptEntity.setGupyJobId(request.jobId());\n        candidateAttemptEntity.setCallbackUrl(request.callbackUrl().toString());\n        candidateAttemptEntity.setResultWebhookUrl(request.resultWebhookUrl() == null ? null : request.resultWebhookUrl().toString());\n        return candidateAttemptEntity;''',
)

# Respostas públicas incluem o destino apenas quando a avaliação terminou.
replace_exact(
    "backend/src/main/java/br/com/iforce/praxis/candidate/dto/ParticipacaoResponse.java",
    '''        @Schema(example = "false")\n        boolean finalizado,\n\n        @Schema(example = "CONTINUAR_TESTE", allowableValues = {"INICIAR", "CONTINUAR_TESTE", "VER_RESULTADOS"})''',
    '''        @Schema(example = "false")\n        boolean finalizado,\n\n        @Schema(example = "https://cliente.gupy.io/candidates/return", description = "Destino final informado pela Gupy; presente somente após a conclusão.")\n        String redirectUrl,\n\n        @Schema(example = "CONTINUAR_TESTE", allowableValues = {"INICIAR", "CONTINUAR_TESTE", "VER_RESULTADOS"})''',
)
replace_exact(
    "backend/src/main/java/br/com/iforce/praxis/candidate/dto/RegistrarRespostaResponse.java",
    '''        @Schema(example = "true")\n        boolean finalizado,\n\n        ParticipacaoResponse.ProgressoResponse progresso,''',
    '''        @Schema(example = "true")\n        boolean finalizado,\n\n        @Schema(example = "https://cliente.gupy.io/candidates/return", description = "Destino final informado pela Gupy; presente somente após a conclusão.")\n        String redirectUrl,\n\n        ParticipacaoResponse.ProgressoResponse progresso,''',
)

service = "backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java"
replace_exact(
    service,
    "import java.math.BigDecimal;\n\nimport java.math.RoundingMode;",
    "import java.math.BigDecimal;\n\nimport java.math.RoundingMode;\n\nimport java.net.URI;",
)
replace_exact(
    service,
    '''        assertCompanyMatchesToken(request.companyId(), empresaContext);\n        String empresaId = empresaContext.empresaId();''',
    '''        assertCompanyMatchesToken(request.companyId(), empresaContext);\n        validateCallbackUrl(request.callbackUrl());\n        String empresaId = empresaContext.empresaId();''',
)
replace_exact(
    service,
    '''        String idempotencyKey = IdempotencyKeyHasher.sha256Hex(\n                empresaId + "|" + empresaContext.companyId() + "|" + request.documentId() + "|" + request.testId());''',
    '''        String idempotencySource =\n                empresaId + "|" + empresaContext.companyId() + "|" + request.documentId() + "|" + request.testId();\n        if (request.jobId() != null) {\n            idempotencySource += "|" + request.jobId();\n        }\n        String idempotencyKey = IdempotencyKeyHasher.sha256Hex(idempotencySource);''',
)
replace_exact(
    service,
    '''        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository\n                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)\n                .orElseGet(() -> createAndAuditAttemptSafely(empresaId, idempotencyKey, request, publishedSimulation));\n        recordIncomingActivity(empresaContext);''',
    '''        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository\n                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)\n                .orElseGet(() -> createAndAuditAttemptSafely(empresaId, idempotencyKey, request, publishedSimulation));\n        candidateAttemptEntity.setGupyJobId(request.jobId());\n        candidateAttemptEntity.setCallbackUrl(request.callbackUrl().toString());\n        candidateAttemptEntity.setResultWebhookUrl(\n                request.resultWebhookUrl() == null ? null : request.resultWebhookUrl().toString()\n        );\n        candidateAttemptEntity = candidateAttemptRepository.save(candidateAttemptEntity);\n        recordIncomingActivity(empresaContext);''',
)
replace_exact(
    service,
    '''                         "testId", request.testId(),\n                         "simulationVersionId", publishedSimulation.versionId(),''',
    '''                         "testId", request.testId(),\n                         "jobId", request.jobId(),\n                         "simulationVersionId", publishedSimulation.versionId(),''',
)
replace_exact(
    service,
    '''                savedAttempt.status() == AttemptStatus.COMPLETED,\n                suggestedFrontendAction(savedAttempt.status()),''',
    '''                savedAttempt.status() == AttemptStatus.COMPLETED,\n                redirectUrl(candidateAttemptEntity, savedAttempt.status()),\n                suggestedFrontendAction(savedAttempt.status()),''',
)
replace_exact(
    service,
    "handleDuplicate(attempt, simulation, request);",
    "handleDuplicate(attempt, simulation, request, candidateAttemptEntity);",
)
replace_exact(
    service,
    '''            CandidateAttempt attempt,\n            PublishedSimulation simulation,\n            RegistrarRespostaRequest request\n    ) {''',
    '''            CandidateAttempt attempt,\n            PublishedSimulation simulation,\n            RegistrarRespostaRequest request,\n            CandidateAttemptEntity candidateAttemptEntity\n    ) {''',
)
replace_exact(
    service,
    '''                attempt.status() == AttemptStatus.COMPLETED,\n                progressFor(attempt, simulation, currentNode),''',
    '''                attempt.status() == AttemptStatus.COMPLETED,\n                redirectUrl(candidateAttemptEntity, attempt.status()),\n                progressFor(attempt, simulation, currentNode),''',
)
replace_exact(
    service,
    '''                savedAttempt.status() == AttemptStatus.COMPLETED,\n                progressFor(savedAttempt, simulation, nextNode),''',
    '''                savedAttempt.status() == AttemptStatus.COMPLETED,\n                redirectUrl(candidateAttemptEntity, savedAttempt.status()),\n                progressFor(savedAttempt, simulation, nextNode),''',
    expected=2,
)
append_before(
    service,
    '''    private void assertCompanyMatchesToken(String companyId, IntegrationEmpresaContext empresaContext) {''',
    '''    private void validateCallbackUrl(URI callbackUrl) {\n        String scheme = callbackUrl == null ? null : callbackUrl.getScheme();\n        boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);\n        if (!validScheme || callbackUrl.getHost() == null || callbackUrl.getHost().isBlank()) {\n            throw new ResponseStatusException(\n                    HttpStatus.BAD_REQUEST,\n                    "callback_url deve ser uma URL absoluta iniciando com http:// ou https://."\n            );\n        }\n    }\n\n    private String redirectUrl(CandidateAttemptEntity entity, AttemptStatus status) {\n        return status == AttemptStatus.COMPLETED ? entity.getCallbackUrl() : null;\n    }\n\n''',
)

# Frontend recebe o retorno e navega somente depois da confirmação final.
replace_exact(
    "frontend/src/lib/api/praxis.ts",
    '''  finalizado: boolean;\n  acaoSugeridaFrontend?: "INICIAR" | "CONTINUAR_TESTE" | "VER_RESULTADOS";''',
    '''  finalizado: boolean;\n  redirectUrl?: string | null;\n  acaoSugeridaFrontend?: "INICIAR" | "CONTINUAR_TESTE" | "VER_RESULTADOS";''',
)
replace_exact(
    "frontend/src/lib/api/praxis.ts",
    '''  finalizado: boolean;\n  progresso: CandidateProgressResponse;\n  etapaAtual: CandidateNodeResponse | null;\n}\n\nexport type ValidationIssueSeverity''',
    '''  finalizado: boolean;\n  redirectUrl?: string | null;\n  progresso: CandidateProgressResponse;\n  etapaAtual: CandidateNodeResponse | null;\n}\n\nexport type ValidationIssueSeverity''',
)
replace_exact(
    "frontend/src/routes/candidato.tsx",
    '''          finalizado: response.finalizado,\n          acaoSugeridaFrontend: response.finalizado''',
    '''          finalizado: response.finalizado,\n          redirectUrl: response.redirectUrl ?? attempt.redirectUrl ?? null,\n          acaoSugeridaFrontend: response.finalizado''',
)
replace_exact(
    "frontend/src/routes/candidato.tsx",
    '''          progresso: response.progresso,\n          etapaAtual: response.etapaAtual,\n        });''',
    '''          progresso: response.progresso,\n          etapaAtual: response.etapaAtual,\n          verticalSaude: attempt.verticalSaude,\n        });''',
)
replace_exact(
    "frontend/src/routes/candidato.tsx",
    '''  const selectedOption = currentNode?.alternativas.find((option) => option.id === selectedOptionId);\n\n  const submitAnswer = useCallback(''',
    '''  const selectedOption = currentNode?.alternativas.find((option) => option.id === selectedOptionId);\n\n  useEffect(() => {\n    const redirectUrl = attempt?.redirectUrl;\n    if (!finished || !redirectUrl) return;\n\n    const timeout = window.setTimeout(() => {\n      window.location.assign(redirectUrl);\n    }, 1200);\n    return () => window.clearTimeout(timeout);\n  }, [attempt?.redirectUrl, finished]);\n\n  const submitAnswer = useCallback(''',
)
replace_exact(
    "frontend/src/routes/candidato.tsx",
    '''          <p>O resultado será processado e entregue para a equipe responsável.</p>''',
    '''          <p>\n            {attempt?.redirectUrl\n              ? "Avaliação concluída. Redirecionando você de volta para a Gupy..."\n              : "O resultado será processado e entregue para a equipe responsável."}\n          </p>''',
)

# Endpoint final: o navegador acessa esta rota e recebe 302 para callback_url.
Path("backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateCompletionRedirectController.java").write_text('''package br.com.iforce.praxis.candidate.controller;\n\nimport br.com.iforce.praxis.auth.service.JwtService;\nimport br.com.iforce.praxis.gupy.model.AttemptStatus;\nimport br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;\nimport br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;\nimport org.springframework.http.HttpStatus;\nimport org.springframework.http.ResponseEntity;\nimport org.springframework.web.bind.annotation.GetMapping;\nimport org.springframework.web.bind.annotation.PathVariable;\nimport org.springframework.web.bind.annotation.RequestMapping;\nimport org.springframework.web.bind.annotation.RestController;\nimport org.springframework.web.server.ResponseStatusException;\n\nimport java.net.URI;\n\n@RestController\n@RequestMapping("/candidate/attempts")\npublic class CandidateCompletionRedirectController {\n\n    private final JwtService jwtService;\n    private final CandidateAttemptRepository candidateAttemptRepository;\n\n    public CandidateCompletionRedirectController(\n            JwtService jwtService,\n            CandidateAttemptRepository candidateAttemptRepository\n    ) {\n        this.jwtService = jwtService;\n        this.candidateAttemptRepository = candidateAttemptRepository;\n    }\n\n    @GetMapping("/{attemptToken}/redirect")\n    public ResponseEntity<Void> redirectAfterCompletion(@PathVariable String attemptToken) {\n        JwtService.CandidateAttemptToken token = parseToken(attemptToken);\n        CandidateAttemptEntity attempt = candidateAttemptRepository\n                .findByEmpresaIdAndId(token.empresaId(), token.attemptId())\n                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));\n\n        if (attempt.getStatus() != AttemptStatus.COMPLETED) {\n            throw new ResponseStatusException(HttpStatus.CONFLICT, "A avaliação ainda não foi concluída.");\n        }\n        if (attempt.getCallbackUrl() == null || attempt.getCallbackUrl().isBlank()) {\n            return ResponseEntity.noContent().build();\n        }\n\n        URI callbackUrl = URI.create(attempt.getCallbackUrl());\n        String scheme = callbackUrl.getScheme();\n        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))\n                || callbackUrl.getHost() == null) {\n            throw new ResponseStatusException(HttpStatus.CONFLICT, "callback_url armazenada é inválida.");\n        }\n\n        return ResponseEntity.status(HttpStatus.FOUND).location(callbackUrl).build();\n    }\n\n    private JwtService.CandidateAttemptToken parseToken(String token) {\n        try {\n            return jwtService.parseCandidateAttemptToken(token);\n        } catch (RuntimeException exception) {\n            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token público de candidato inválido.");\n        }\n    }\n}\n''', encoding="utf-8")

Path("backend/src/main/resources/db/migration/V72__add_gupy_callback_and_job_context.sql").write_text('''ALTER TABLE candidate_attempts\n    ADD COLUMN IF NOT EXISTS gupy_job_id BIGINT;\n\nALTER TABLE candidate_attempts\n    ADD COLUMN IF NOT EXISTS callback_url VARCHAR(1000);\n\nCREATE INDEX IF NOT EXISTS idx_candidate_attempts_gupy_job\n    ON candidate_attempts (empresa_id, company_id, gupy_job_id);\n''', encoding="utf-8")

# Testes do contrato Gupy.
gupy_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java"
replace_exact(
    gupy_test,
    "package br.com.iforce.praxis.gupy.controller;\n\nimport com.jayway.jsonpath.JsonPath;",
    "package br.com.iforce.praxis.gupy.controller;\n\nimport br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;\nimport br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;\nimport com.jayway.jsonpath.JsonPath;",
)
replace_exact(
    gupy_test,
    '''    @Autowired\n    private MockMvc mockMvc;''',
    '''    @Autowired\n    private MockMvc mockMvc;\n\n    @Autowired\n    private CandidateAttemptRepository candidateAttemptRepository;''',
)
replace_exact(
    gupy_test,
    '''    @Test\n    void createCandidateAttemptIsIdempotentByCompanyDocumentAndTest() throws Exception {''',
    '''    @Test\n    void createCandidateAttemptPersistsCallbackAndJobId() throws Exception {\n        MvcResult result = mockMvc.perform(post("/test/candidate")\n                        .header("Authorization", AUTHORIZATION)\n                        .contentType(MediaType.APPLICATION_JSON)\n                        .content(validCandidateRequest("candidate-contract-context", 901L)))\n                .andExpect(status().isCreated())\n                .andReturn();\n\n        String resultId = JsonPath.read(result.getResponse().getContentAsString(), "$.test_result_id");\n        CandidateAttemptEntity attempt = candidateAttemptRepository\n                .findByEmpresaIdAndResultId("empresa-1", resultId)\n                .orElseThrow();\n\n        assertThat(attempt.getGupyJobId()).isEqualTo(901L);\n        assertThat(attempt.getCallbackUrl()).isEqualTo("https://cliente.gupy.io/candidate-return");\n    }\n\n    @Test\n    void createCandidateAttemptIsIdempotentByCompanyDocumentTestAndJob() throws Exception {''',
)
replace_exact(
    gupy_test,
    '''        assertThat(secondBody).isEqualTo(firstBody);\n    }\n\n    @Test\n    void getTestResultReturnsAttemptStatus()''',
    '''        assertThat(secondBody).isEqualTo(firstBody);\n    }\n\n    @Test\n    void differentJobsCreateDifferentAttemptsForSameCandidateAndTest() throws Exception {\n        MvcResult first = mockMvc.perform(post("/test/candidate")\n                        .header("Authorization", AUTHORIZATION)\n                        .contentType(MediaType.APPLICATION_JSON)\n                        .content(validCandidateRequest("candidate-multiple-jobs", 100L)))\n                .andExpect(status().isCreated())\n                .andReturn();\n        MvcResult second = mockMvc.perform(post("/test/candidate")\n                        .header("Authorization", AUTHORIZATION)\n                        .contentType(MediaType.APPLICATION_JSON)\n                        .content(validCandidateRequest("candidate-multiple-jobs", 200L)))\n                .andExpect(status().isCreated())\n                .andReturn();\n\n        String firstResultId = JsonPath.read(first.getResponse().getContentAsString(), "$.test_result_id");\n        String secondResultId = JsonPath.read(second.getResponse().getContentAsString(), "$.test_result_id");\n        assertThat(secondResultId).isNotEqualTo(firstResultId);\n    }\n\n    @Test\n    void getTestResultReturnsAttemptStatus()''',
)
replace_exact(
    gupy_test,
    '''                                  "email": "thiago@example.com"\n                                }''',
    '''                                  "email": "thiago@example.com",\n                                  "job_id": 100,\n                                  "callback_url": "https://cliente.gupy.io/candidate-return"\n                                }''',
)
replace_exact(
    gupy_test,
    '''                .andExpect(jsonPath("$.fields.candidateName").exists())\n                .andExpect(jsonPath("$.fields.candidateEmail").exists());''',
    '''                .andExpect(jsonPath("$.fields.candidateName").exists())\n                .andExpect(jsonPath("$.fields.candidateEmail").exists())\n                .andExpect(jsonPath("$.fields.callbackUrl").exists());''',
)
replace_exact(
    gupy_test,
    '''    private String validCandidateRequest(String documentId) {\n        return """\n                {\n                  "company_id": "empresa-123",\n                  "document_id": "%s",\n                  "test_id": "sim-atendimento-caos",\n                  "name": "Thiago Souza",\n                  "email": "thiago@example.com",\n                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",\n                  "candidate_type": "external",\n                  "previous_result": "none"\n                }\n                """.formatted(documentId);\n    }''',
    '''    private String validCandidateRequest(String documentId) {\n        return validCandidateRequest(documentId, 100L);\n    }\n\n    private String validCandidateRequest(String documentId, long jobId) {\n        return """\n                {\n                  "company_id": "empresa-123",\n                  "document_id": "%s",\n                  "test_id": "sim-atendimento-caos",\n                  "name": "Thiago Souza",\n                  "email": "thiago@example.com",\n                  "job_id": %d,\n                  "callback_url": "https://cliente.gupy.io/candidate-return",\n                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",\n                  "candidate_type": "external",\n                  "previous_result": "none"\n                }\n                """.formatted(documentId, jobId);\n    }''',
)

candidate_test = "backend/src/test/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptControllerTest.java"
replace_exact(
    candidate_test,
    "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;",
    "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;\n\nimport static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;",
)
replace_exact(
    candidate_test,
    '''                .andExpect(jsonPath("$.finalizado").value(true))\n                .andExpect(jsonPath("$.progresso.passoAtual").value(1))''',
    '''                .andExpect(jsonPath("$.finalizado").value(true))\n                .andExpect(jsonPath("$.redirectUrl").value("https://cliente.gupy.io/candidate-return"))\n                .andExpect(jsonPath("$.progresso.passoAtual").value(1))''',
    expected=1,
)
replace_exact(
    candidate_test,
    '''    @Test\n    void submitSameAnswerTwiceIsIdempotent() throws Exception {''',
    '''    @Test\n    void completedGupyAttemptRedirectsBrowserToCallbackUrl() throws Exception {\n        MvcResult createResult = createAttemptResult("candidate-final-redirect");\n        String responseBody = createResult.getResponse().getContentAsString();\n        String attemptId = attemptIdFromResponse(responseBody);\n        String publicToken = tokenFromResponse(responseBody);\n\n        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")\n                        .contentType(MediaType.APPLICATION_JSON)\n                        .content("""\n                                {\n                                  "nodeId": "turno-1",\n                                  "optionId": "opcao-equilibrada"\n                                }\n                                """))\n                .andExpect(status().isOk())\n                .andExpect(jsonPath("$.finalizado").value(true));\n\n        mockMvc.perform(get("/candidate/attempts/" + publicToken + "/redirect"))\n                .andExpect(status().isFound())\n                .andExpect(header().string("Location", "https://cliente.gupy.io/candidate-return"));\n    }\n\n    @Test\n    void submitSameAnswerTwiceIsIdempotent() throws Exception {''',
)
replace_exact(
    candidate_test,
    '''                                  "email": "thiago@example.com",\n                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",''',
    '''                                  "email": "thiago@example.com",\n                                  "job_id": 100,\n                                  "callback_url": "https://cliente.gupy.io/candidate-return",\n                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",''',
)

# Documentação técnica deixa de declarar estes itens como ausentes.
doc = "docs/INTEGRACAO-GUPY-PROVEDOR.md"
replace_exact(
    doc,
    "O fluxo interno funciona, mas o contrato público atual diverge do contrato oficial da Gupy em pontos obrigatórios, principalmente `callback_url`, redirecionamento final e assinatura do endpoint de resultado.",
    "O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono e redirecionamento final. Permanecem divergências no endpoint de resultado, tipos e enums que ainda impedem declarar a integração homologada.",
)
replace_exact(doc, "| `callback_url` obrigatório | Não existe no DTO atual | **Incompatível** |", "| `callback_url` obrigatório | Recebido, validado, persistido e devolvido ao navegador após conclusão | Compatível tecnicamente |")
replace_exact(doc, "| `job_id` | Não existe no DTO atual | Incompatível para rastreabilidade da vaga |", "| `job_id` | Recebido e persistido; também participa da idempotência quando informado | Compatível |")
replace_exact(doc, "| Callback GET após conclusão | Não implementado | **Incompatível** |", "| Callback GET após conclusão | O navegador acessa `/candidate/attempts/{token}/redirect` e recebe `302` para `callback_url` | Compatível tecnicamente |")
replace_exact(doc, "| Redirecionamento do candidato de volta à Gupy | Não implementado | **Incompatível** |", "| Redirecionamento do candidato de volta à Gupy | Executado automaticamente após a resposta final | Compatível tecnicamente |")
replace_exact(
    doc,
    '''  "email": "candidato@example.com",\n  "result_webhook_url": "https://integracao.gupy.example/webhook",''',
    '''  "email": "candidato@example.com",\n  "job_id": 100,\n  "callback_url": "https://integracao.gupy.example/candidate-return",\n  "result_webhook_url": "https://integracao.gupy.example/webhook",''',
)
replace_exact(
    doc,
    '''| `email` | Sim | Validado como e-mail. |\n| `result_webhook_url` | Não | Se presente, recebe `TestResult` por POST. |''',
    '''| `email` | Sim | Validado como e-mail. |\n| `job_id` | Não | Identificador da vaga; quando informado, diferencia a chave idempotente. |\n| `callback_url` | Sim | URL absoluta HTTP(S), persistida para o retorno final à Gupy. |\n| `result_webhook_url` | Não | Se presente, recebe `TestResult` por POST. |''',
)
replace_exact(
    doc,
    '''Não existem no DTO atual:\n\n- `callback_url`;\n- `job_id`;\n- callback ou estado para redirecionar a pessoa candidata à Gupy.\n\n''',
    '''Após a conclusão, a API pública devolve `redirectUrl` à tela. O frontend navega para `/candidate/attempts/{token}/redirect`, e esse endpoint responde `302 Location` para a `callback_url` recebida da Gupy. Assim, o GET final ocorre no navegador da pessoa candidata.\n\n''',
)
replace_exact(doc, "empresaId | companyId | documentId | testId", "empresaId | companyId | documentId | testId | jobId (quando informado)")
replace_exact(
    doc,
    '''Fluxo oficial ainda ausente:\n\n```mermaid''',
    '''Fluxo de callback e redirecionamento implementado:\n\n```mermaid''',
)
replace_exact(
    doc,
    '''  Praxis->>Gupy: GET callback_url\n  Praxis-->>Candidato: redireciona para a página retornada/esperada pela Gupy''',
    '''  Praxis-->>Candidato: redirectUrl após a resposta final\n  Candidato->>Praxis: GET /candidate/attempts/{token}/redirect\n  Praxis-->>Candidato: 302 Location: callback_url\n  Candidato->>Gupy: GET callback_url''',
)
replace_exact(
    doc,
    '''1. Adicionar `callback_url` ao request e persistir o valor necessário ao fluxo.\n2. Chamar `GET callback_url` após a conclusão.\n3. Redirecionar a pessoa candidata de volta à Gupy.\n4. Remover a obrigatoriedade de `company_id` da query do endpoint de resultado, mantendo isolamento pelo token.\n5. Definir compatibilidade de tipos para `company_id` e `document_id`.\n6. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n7. Receber `job_id` quando enviado.\n8. Corrigir `result_candidate_page_url` para uma página de navegador.\n9. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n10. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.''',
    '''1. Remover a obrigatoriedade de `company_id` da query do endpoint de resultado, mantendo isolamento pelo token.\n2. Definir compatibilidade de tipos para `company_id` e `document_id`.\n3. Aceitar e validar `previous_result` conforme `fail` ou `null`.\n4. Corrigir `result_candidate_page_url` para uma página de navegador.\n5. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.\n6. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.''',
)
replace_exact(doc, "- [ ] Validar callback e redirecionamento.", "- [x] Validar callback e redirecionamento em testes automatizados; falta confirmar na homologação real da Gupy.")
replace_exact(doc, "Última revisão: 12/07/2026.", "Última revisão: 15/07/2026.")

print("Contrato Gupy callback/job/redirecionamento aplicado com sucesso.")
