package br.com.iforce.praxis.account.controller;

import br.com.iforce.praxis.account.dto.AccountResponse;
import br.com.iforce.praxis.account.dto.ChangePasswordRequest;
import br.com.iforce.praxis.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> currentAccount() {
        return ResponseEntity.ok(accountService.currentAccount());
    }

    @PostMapping("/password")
    public ResponseEntity<AccountResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(accountService.changePassword(request));
    }
}
