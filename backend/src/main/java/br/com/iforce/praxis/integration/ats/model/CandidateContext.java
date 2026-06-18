package br.com.iforce.praxis.integration.ats.model;

/**
 * Contexto normalizado do candidato após criação no ATS.
 * Contém todas as informações necessárias para prosseguir com a avaliação.
 */
public record CandidateContext(
    String candidateId,
    String tenantId,
    String jobId,
    String evaluationName,
    String resultWebhookUrl,
    String evaluationLink
) {}
