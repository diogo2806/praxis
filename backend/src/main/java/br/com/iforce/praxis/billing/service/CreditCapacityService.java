package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.billing.dto.CreditCapacityResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Explica quanto do saldo já está reservado por avaliações ainda não concluídas. */
@Service
public class CreditCapacityService {

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CreditService creditService;

    public CreditCapacityService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CreditService creditService
    ) {
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.creditService = creditService;
    }

    @Transactional(readOnly = true)
    public CreditCapacityResponse getCapacity(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        CommercialPlanType plan = empresa.getCommercialPlanType();
        boolean metered = plan == CommercialPlanType.AVULSO || plan == CommercialPlanType.PROFISSIONAL;
        int balance = creditService.getBalance(empresaId);
        if (!metered) {
            return new CreditCapacityResponse(plan, false, balance, 0, balance);
        }

        long reserved = candidateAttemptRepository.countByEmpresaIdAndStatusIn(
                empresaId,
                List.of(AttemptStatus.NOT_STARTED, AttemptStatus.IN_PROGRESS)
        );
        int reservedCredits = (int) Math.min(reserved, Integer.MAX_VALUE);
        int availableCredits = Math.max(balance - reservedCredits, 0);
        return new CreditCapacityResponse(
                plan,
                true,
                balance,
                reservedCredits,
                availableCredits
        );
    }
}
