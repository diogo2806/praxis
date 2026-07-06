package br.com.iforce.praxis.commercial.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/candidate/demo-requests")
public class PublicCandidateDemoRequestController {

    private final DemoRequestController demoRequestController;

    public PublicCandidateDemoRequestController(DemoRequestController demoRequestController) {
        this.demoRequestController = demoRequestController;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DemoRequestController.DemoRequestRequest request) {
        return demoRequestController.create(request);
    }
}
