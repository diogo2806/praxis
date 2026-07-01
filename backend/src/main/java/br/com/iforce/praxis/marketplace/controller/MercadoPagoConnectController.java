package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.marketplace.service.MercadoPagoConnectService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/marketplace/professionals/me/mercadopago")
public class MercadoPagoConnectController {

    private final MercadoPagoConnectService connectService;
    private final CurrentUserService currentUserService;

    public MercadoPagoConnectController(
            MercadoPagoConnectService connectService,
            CurrentUserService currentUserService
    ) {
        this.connectService = connectService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect() {
        return redirect(connectService.connectUrl(currentUserService.requiredUserId()));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        connectService.handleCallback(state, code);
        return redirect("/profissional/financeiro?connected=true");
    }

    @DeleteMapping
    public ResponseEntity<Void> disconnect() {
        connectService.disconnect(currentUserService.requiredUserId());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<Void> redirect(String location) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
