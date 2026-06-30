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
 * Porta de entrada (API) para envio de mídias usadas nas provas.
 *
 * <p>Na visão do processo, é por aqui que a tela de cadastro de provas envia
 * as imagens e áudios que ilustram os cenários. O arquivo é armazenado e o
 * sistema devolve o endereço público para ser usado na prova.</p>
 */
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "Upload de imagens e áudios usados no cadastro de testes.")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    /**
     * Recebe o arquivo de mídia enviado pela tela e o armazena.
     *
     * @param file a imagem ou áudio enviado pelo usuário
     * @return o endereço público da mídia e seus dados (tipo e tamanho)
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
