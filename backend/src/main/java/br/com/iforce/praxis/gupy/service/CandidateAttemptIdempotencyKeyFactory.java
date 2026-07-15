package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;

final class CandidateAttemptIdempotencyKeyFactory {

    private static final String GUPY_PROVIDER = "gupy";
    private static final String RETEST_CYCLE_MARKER = "|cycle:retest:previous_result=fail";

    private CandidateAttemptIdempotencyKeyFactory() {
    }

    static String currentKey(CreateCandidateRequest request, IntegrationEmpresaContext context) {
        return IdempotencyKeyHasher.sha256Hex(currentSource(request, context));
    }

    static String initialKey(CreateCandidateRequest request, IntegrationEmpresaContext context) {
        return IdempotencyKeyHasher.sha256Hex(initialSource(request, context));
    }

    static String currentSource(CreateCandidateRequest request, IntegrationEmpresaContext context) {
        String source = initialSource(request, context);
        return isGupyRetest(request, context) ? source + RETEST_CYCLE_MARKER : source;
    }

    static String initialSource(CreateCandidateRequest request, IntegrationEmpresaContext context) {
        String source = context.empresaId() + "|" + context.companyId() + "|" + request.documentId()
                + "|" + request.testId();
        if (request.jobId() != null) {
            source += "|" + request.jobId();
        }
        return source;
    }

    static boolean isGupyRetest(CreateCandidateRequest request, IntegrationEmpresaContext context) {
        return GUPY_PROVIDER.equalsIgnoreCase(context.provider()) && request.isRetestRequested();
    }
}
