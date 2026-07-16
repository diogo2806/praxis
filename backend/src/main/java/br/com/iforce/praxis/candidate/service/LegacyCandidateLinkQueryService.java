package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.candidate.dto.CandidateLinkPageResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Mantém o contrato legado de lista sem perder registros além do primeiro lote.
 * Novos consumidores devem usar a consulta paginada diretamente.
 */
@Service
public class LegacyCandidateLinkQueryService {

    private static final int PAGE_SIZE = 100;

    private final CandidateLinkQueryService candidateLinkQueryService;

    public LegacyCandidateLinkQueryService(CandidateLinkQueryService candidateLinkQueryService) {
        this.candidateLinkQueryService = candidateLinkQueryService;
    }

    @Transactional(readOnly = true)
    public List<CandidateLinkResponse> listAll(boolean blind) {
        List<CandidateLinkResponse> links = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            CandidateLinkPageResponse response = candidateLinkQueryService.search(
                    page,
                    PAGE_SIZE,
                    blind,
                    null,
                    null,
                    null,
                    null
            );
            links.addAll(response.items());
            totalPages = response.totalPages();
            page++;
        } while (page < totalPages);

        return List.copyOf(links);
    }
}
