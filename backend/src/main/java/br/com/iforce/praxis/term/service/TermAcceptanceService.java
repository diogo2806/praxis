package br.com.iforce.praxis.term.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.CurrentUserService;

import br.com.iforce.praxis.term.dto.AcceptTermRequest;

import br.com.iforce.praxis.term.dto.TermAcceptanceStatusResponse;

import br.com.iforce.praxis.term.dto.TermResponse;

import br.com.iforce.praxis.term.model.HealthUseTerm;

import br.com.iforce.praxis.term.model.ResponsibilityTerm;

import br.com.iforce.praxis.term.persistence.entity.TermAcceptanceEntity;

import br.com.iforce.praxis.term.persistence.repository.TermAcceptanceRepository;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.Optional;


/**
 * Termos aceitos pelo recrutador: termo de responsabilidade (REQ-L5) e termo de uso em saúde
 * (Minuta C). Expõe o texto/versão corrente e registra o aceite (quem, quando, qual versão) de
 * forma insert-only, para comprovar que o cliente assumiu as responsabilidades aplicáveis.
 */
@Service
public class TermAcceptanceService {

    /**
     * Descritor de um termo: tipo, versão corrente e texto exibidos ao usuário.
     *
     * @param type identificador do termo no processo de aceite
     * @param version versão vigente que precisa ser confirmada
     * @param text conteúdo apresentado para leitura antes do aceite
     */
    private record TermDescriptor(String type, String version, String text) {
    }

    private static final TermDescriptor RESPONSIBILITY =
            new TermDescriptor(ResponsibilityTerm.TYPE, ResponsibilityTerm.VERSION, ResponsibilityTerm.TEXT);
    private static final TermDescriptor HEALTH_USE =
            new TermDescriptor(HealthUseTerm.TYPE, HealthUseTerm.VERSION, HealthUseTerm.TEXT);

    private final TermAcceptanceRepository termAcceptanceRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    /**
     * Prepara o serviço com os recursos necessários para saber qual empresa e
     * qual usuário estão realizando o processo de aceite.
     *
     * @param termAcceptanceRepository acesso ao histórico de aceites registrados
     * @param currentEmpresaService identificação da empresa em operação
     * @param currentUserService identificação do usuário responsável pela ação
     */
    public TermAcceptanceService(
            TermAcceptanceRepository termAcceptanceRepository,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.termAcceptanceRepository = termAcceptanceRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    /**
     * Devolve o texto e a versão atual do termo de responsabilidade.
     *
     * @return o conteúdo do termo de responsabilidade
     */
    public TermResponse responsibilityTerm() {
        return termResponse(RESPONSIBILITY);
    }

    /**
     * Informa se o usuário logado já aceitou a versão atual do termo de
     * responsabilidade.
     *
     * @return a situação de aceite do usuário atual
     */
    @Transactional(readOnly = true)
    public TermAcceptanceStatusResponse responsibilityStatus() {
        return statusFor(RESPONSIBILITY);
    }

    /**
     * Registra o aceite do termo de responsabilidade pelo usuário logado.
     *
     * <p>Guarda quem aceitou, quando e qual versão (registro que nunca é
     * apagado), para comprovar que o recrutador assumiu as responsabilidades.
     * Recusa o aceite se a versão informada não for a atual.</p>
     *
     * @param request dados do aceite, incluindo a versão aceita
     * @return a situação de aceite atualizada
     */
    @Transactional
    public TermAcceptanceStatusResponse acceptResponsibility(AcceptTermRequest request) {
        return accept(RESPONSIBILITY, request);
    }

    /**
     * Devolve o texto e a versão atual do termo de uso na vertical de saúde.
     *
     * @return o conteúdo do termo de uso em saúde
     */
    public TermResponse healthUseTerm() {
        return termResponse(HEALTH_USE);
    }

    /**
     * Informa se o usuário logado já aceitou a versão atual do termo de uso
     * em saúde.
     *
     * @return a situação de aceite do usuário atual
     */
    @Transactional(readOnly = true)
    public TermAcceptanceStatusResponse healthUseStatus() {
        return statusFor(HEALTH_USE);
    }

    /**
     * Registra o aceite do termo de uso em saúde pelo usuário logado.
     *
     * <p>Mesmo registro insert-only do termo de responsabilidade. É
     * pré-requisito para publicar provas na vertical de saúde.</p>
     *
     * @param request dados do aceite, incluindo a versão aceita
     * @return a situação de aceite atualizada
     */
    @Transactional
    public TermAcceptanceStatusResponse acceptHealthUse(AcceptTermRequest request) {
        return accept(HEALTH_USE, request);
    }

    /**
     * Indica se o usuário atual aceitou a versão corrente do termo de uso em saúde. Usado como
     * trava de publicação quando o empresa opera na vertical de saúde.
     *
     * @return {@code true} quando a versão vigente já foi aceita pelo usuário atual
     */
    @Transactional(readOnly = true)
    public boolean isHealthUseAcceptedByCurrentUser() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        return latestAcceptance(HEALTH_USE, empresaId, userId)
                .map(entity -> HEALTH_USE.version().equals(entity.getTermVersion()))
                .orElse(false);
    }

    /**
     * Monta a resposta com o conteúdo do termo para a tela apresentar ao usuário.
     *
     * @param descriptor termo que será exibido
     * @return tipo, versão e texto do termo em formato de resposta
     */
    private TermResponse termResponse(TermDescriptor descriptor) {
        return new TermResponse(descriptor.type(), descriptor.version(), descriptor.text());
    }

    /**
     * Consulta a situação de aceite do termo para a empresa e o usuário logados.
     *
     * @param descriptor termo cuja aceitação será verificada
     * @return resumo indicando se a versão atual já foi aceita
     */
    private TermAcceptanceStatusResponse statusFor(TermDescriptor descriptor) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        return toStatus(descriptor, latestAcceptance(descriptor, empresaId, userId));
    }

    /**
     * Registra um novo aceite depois de confirmar que a versão enviada pela tela
     * é a mesma versão vigente do termo.
     *
     * @param descriptor termo que está sendo aceito
     * @param request dados informados pela tela de confirmação
     * @return situação atualizada após o registro do aceite
     */
    private TermAcceptanceStatusResponse accept(TermDescriptor descriptor, AcceptTermRequest request) {
        if (!descriptor.version().equals(request.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A versão do termo mudou. Recarregue e aceite a versão atual."
            );
        }
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();

        TermAcceptanceEntity acceptance = new TermAcceptanceEntity();
        acceptance.setEmpresaId(empresaId);
        acceptance.setUserId(userId);
        acceptance.setTermType(descriptor.type());
        acceptance.setTermVersion(descriptor.version());
        acceptance.setAcceptedAt(Instant.now());
        termAcceptanceRepository.save(acceptance);

        return toStatus(descriptor, Optional.of(acceptance));
    }

    /**
     * Busca o registro mais recente de aceite para uma empresa, usuário e tipo de
     * termo.
     *
     * @param descriptor termo consultado
     * @param empresaId empresa dona do processo
     * @param userId usuário que pode ter aceitado o termo
     * @return último aceite encontrado, quando existir
     */
    private Optional<TermAcceptanceEntity> latestAcceptance(
            TermDescriptor descriptor, String empresaId, String userId) {
        return termAcceptanceRepository
                .findFirstByEmpresaIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                        empresaId, userId, descriptor.type());
    }

    /**
     * Traduz o histórico encontrado em uma resposta simples para a interface.
     *
     * @param descriptor termo cuja versão vigente será comparada
     * @param acceptance último aceite encontrado para o usuário
     * @return situação do aceite pronta para exibição
     */
    private TermAcceptanceStatusResponse toStatus(
            TermDescriptor descriptor, Optional<TermAcceptanceEntity> acceptance) {
        return acceptance
                .map(entity -> new TermAcceptanceStatusResponse(
                        descriptor.type(),
                        descriptor.version(),
                        descriptor.version().equals(entity.getTermVersion()),
                        entity.getTermVersion(),
                        entity.getAcceptedAt()
                ))
                .orElseGet(() -> new TermAcceptanceStatusResponse(
                        descriptor.type(),
                        descriptor.version(),
                        false,
                        null,
                        null
                ));
    }
}
