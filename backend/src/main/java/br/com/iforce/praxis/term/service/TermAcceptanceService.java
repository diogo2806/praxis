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
 * Orquestra o processo de aceite dos termos que protegem a empresa, o candidato e a
 * plataforma.
 *
 * <p>Na visão de negócio, este serviço entrega para a tela o termo vigente, confere se
 * a pessoa usuária já aceitou a versão atual e grava um novo registro quando o aceite
 * acontece. O histórico é mantido de forma insert-only para permitir auditoria: quem
 * aceitou, por qual empresa, em qual data e qual versão estava valendo naquele momento.</p>
 */
@Service
public class TermAcceptanceService {

    /**
     * Agrupa os dados fixos de um termo para que o mesmo fluxo sirva tanto para o termo
     * de responsabilidade quanto para o termo de uso da vertical de saúde.
     *
     * @param type identificador usado pelo sistema para diferenciar o tipo de termo
     * @param version versão vigente que deve ser aceita pelo usuário
     * @param text texto apresentado na interface antes do aceite
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
     * Prepara o serviço com os acessos necessários para consultar o usuário atual,
     * identificar a empresa em operação e gravar o histórico de aceites.
     *
     * @param termAcceptanceRepository repositório onde o histórico de aceites é consultado e salvo
     * @param currentEmpresaService serviço que identifica a empresa ativa no processo
     * @param currentUserService serviço que identifica o usuário autenticado no processo
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
     * <p>Esse é o conteúdo exibido ao recrutador antes de ele confirmar que entende que
     * a Práxis apoia a decisão, mas não substitui a revisão humana.</p>
     *
     * @return o conteúdo vigente do termo de responsabilidade
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
     * <p>Esse conteúdo é apresentado quando a empresa usa a plataforma em um contexto
     * sensível de saúde e precisa reconhecer as responsabilidades adicionais do processo.</p>
     *
     * @return o conteúdo vigente do termo de uso em saúde
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
     * Confere se o usuário atual já aceitou a versão vigente do termo de uso em saúde.
     *
     * <p>Na prática, funciona como uma trava de segurança: sem esse aceite atualizado,
     * o processo de publicação na vertical de saúde não deve avançar.</p>
     *
     * @return {@code true} quando o aceite mais recente corresponde à versão vigente
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
     * Transforma o termo configurado no formato usado pela API e pela tela.
     *
     * <p>Assim, a interface recebe sempre o mesmo conjunto de informações: tipo do termo,
     * versão vigente e texto que deve ser exibido ao usuário antes do aceite.</p>
     *
     * @param descriptor termo configurado no backend
     * @return resposta pronta para ser exibida no produto
     */
    private TermResponse termResponse(TermDescriptor descriptor) {
        return new TermResponse(descriptor.type(), descriptor.version(), descriptor.text());
    }

    /**
     * Monta a fotografia atual do aceite para o usuário e a empresa em operação.
     *
     * <p>Esse método centraliza a consulta usada pelas telas de status, evitando que cada
     * endpoint precise repetir a regra de buscar o último aceite registrado.</p>
     *
     * @param descriptor termo cujo status será consultado
     * @return situação do aceite para a versão vigente do termo
     */
    private TermAcceptanceStatusResponse statusFor(TermDescriptor descriptor) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        return toStatus(descriptor, latestAcceptance(descriptor, empresaId, userId));
    }

    /**
     * Executa o registro de aceite de um termo.
     *
     * <p>Primeiro confirma que a versão aceita pelo usuário é a mesma que está vigente no
     * backend. Depois registra uma nova linha de histórico com empresa, usuário, tipo,
     * versão e data do aceite. Isso preserva a trilha de auditoria do processo.</p>
     *
     * @param descriptor termo que está sendo aceito
     * @param request dados informados pela tela no momento do aceite
     * @return situação atualizada do aceite logo após a gravação
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
     * Localiza o aceite mais recente de um termo para uma combinação de empresa e usuário.
     *
     * <p>O histórico nunca é sobrescrito. Por isso, o estado atual é definido pelo último
     * registro encontrado para o mesmo tipo de termo.</p>
     *
     * @param descriptor termo pesquisado no histórico
     * @param empresaId empresa responsável pelo processo em andamento
     * @param userId usuário que está operando o sistema
     * @return aceite mais recente, quando existir
     */
    private Optional<TermAcceptanceEntity> latestAcceptance(
            TermDescriptor descriptor, String empresaId, String userId) {
        return termAcceptanceRepository
                .findFirstByEmpresaIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                        empresaId, userId, descriptor.type());
    }

    /**
     * Converte o registro salvo no banco em uma resposta de status fácil de entender pela tela.
     *
     * <p>Além de informar se o termo está aceito, a resposta também mostra qual versão foi
     * aceita e quando isso aconteceu. Quando não há aceite, os campos históricos ficam vazios.</p>
     *
     * @param descriptor termo vigente que serve de referência para a comparação
     * @param acceptance aceite mais recente encontrado no histórico
     * @return status consolidado para exibição e decisão do fluxo
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
