package br.com.iforce.praxis.campaign.controller;

import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CampaignResponse;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CommunicationEventRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CreateCampaignRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CsvPreviewRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CsvPreviewResponse;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.MessagePreviewRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.MessagePreviewResponse;
import br.com.iforce.praxis.campaign.service.ParticipationCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participation-campaigns")
@Tag(name = "Participation Campaigns", description = "Convites em lote, lembretes e acompanhamento operacional.")
public class ParticipationCampaignController {

    private final ParticipationCampaignService campaignService;

    public ParticipationCampaignController(ParticipationCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping("/preview-csv")
    @Operation(summary = "Valida um CSV sem criar tentativas nem consumir créditos")
    public ResponseEntity<CsvPreviewResponse> previewCsv(@Valid @RequestBody CsvPreviewRequest request) {
        return ResponseEntity.ok(campaignService.previewCsv(request));
    }

    @PostMapping("/preview-message")
    @Operation(summary = "Renderiza assunto e mensagem usando apenas variáveis permitidas")
    public ResponseEntity<MessagePreviewResponse> previewMessage(
            @Valid @RequestBody MessagePreviewRequest request
    ) {
        return ResponseEntity.ok(campaignService.previewMessage(request));
    }

    @PostMapping
    @Operation(summary = "Confirma a campanha e agenda convites e lembretes idempotentes")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(201).body(campaignService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista campanhas da empresa atual")
    public ResponseEntity<List<CampaignResponse>> list() {
        return ResponseEntity.ok(campaignService.list());
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Consulta totais e participantes de uma campanha")
    public ResponseEntity<CampaignResponse> get(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(campaignService.get(campaignId));
    }

    @PostMapping("/{campaignId}/pause")
    @Operation(summary = "Pausa novos envios sem invalidar resultados já concluídos")
    public ResponseEntity<CampaignResponse> pause(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(campaignService.pause(campaignId));
    }

    @PostMapping("/{campaignId}/resume")
    @Operation(summary = "Retoma o processamento de uma campanha pausada")
    public ResponseEntity<CampaignResponse> resume(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(campaignService.resume(campaignId));
    }

    @PostMapping("/{campaignId}/cancel")
    @Operation(summary = "Cancela mensagens pendentes sem remover resultados concluídos")
    public ResponseEntity<CampaignResponse> cancel(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(campaignService.cancel(campaignId));
    }

    @PostMapping("/{campaignId}/participants/{participantId}/communication-event")
    @Operation(summary = "Registra entrega, bounce ou abertura informada pelo provedor")
    public ResponseEntity<CampaignResponse> registerCommunicationEvent(
            @PathVariable UUID campaignId,
            @PathVariable UUID participantId,
            @Valid @RequestBody CommunicationEventRequest request
    ) {
        return ResponseEntity.ok(campaignService.registerCommunicationEvent(
                campaignId,
                participantId,
                request.event()
        ));
    }

    @GetMapping(value = "/{campaignId}/export.csv", produces = "text/csv")
    @Operation(summary = "Exporta o acompanhamento operacional da campanha")
    public ResponseEntity<byte[]> export(@PathVariable UUID campaignId) {
        byte[] content = campaignService.exportOperationalCsv(campaignId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("campanha-" + campaignId + ".csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
