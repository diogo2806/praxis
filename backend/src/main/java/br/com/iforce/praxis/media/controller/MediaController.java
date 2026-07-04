package br.com.iforce.praxis.media.controller;

import br.com.iforce.praxis.media.dto.MediaUploadResponse;

import br.com.iforce.praxis.media.service.MediaStorageService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;


/**
 * Porta de entrada (API) para envio de mídias usadas nas avaliações.
 *
 * <p>Na visão do processo, é por aqui que a tela de cadastro envia imagens e
 * áudios que ajudam a contextualizar uma etapa ou alternativa da avaliação. A
 * API recebe o arquivo, delega a validação e o armazenamento, e devolve para a
 * tela um endereço público que pode ser salvo no roteiro da avaliação.</p>
 */
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "Upload de imagens e áudios usados no cadastro de testes.")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    /**
     * Conecta a porta de entrada de mídia ao serviço que faz a validação e o armazenamento.
     *
     * <p>Para o processo, este construtor apenas prepara o controller para
     * encaminhar cada arquivo recebido ao responsável por verificar tipo,
     * tamanho, empresa de origem e gravação no storage.</p>
     *
     * @param mediaStorageService serviço que valida, armazena e devolve os dados da mídia enviada
     */
    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    /**
     * Recebe o arquivo de mídia enviado pela tela e devolve os dados para cadastro.
     *
     * <p>Na jornada de criação da avaliação, este método é usado quando a pessoa
     * responsável pelo conteúdo anexa uma imagem ou áudio a um cenário. Ele não
     * decide como a mídia será usada na avaliação; apenas recebe o arquivo,
     * solicita o armazenamento e devolve a URL e os metadados necessários para a
     * tela salvar a referência no passo correto.</p>
     *
     * @param file imagem ou áudio enviado pelo usuário no formulário da tela
     * @return endereço público, tipo identificado, formato do arquivo e tamanho armazenado
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Envia uma mídia",
            description = "Recebe uma imagem ou áudio em multipart/form-data, armazena no storage e devolve a URL pública."
    )
    public ResponseEntity<MediaUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(mediaStorageService.upload(file));
    }
}
