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

/**
 * Porta de entrada (API) das ações de "Minha conta".
 *
 * <p>É por aqui que a tela do usuário conversa com o sistema para consultar
 * os próprios dados e trocar a própria senha. Este componente apenas recebe
 * o pedido vindo da tela e repassa para a regra de negócio
 * ({@link AccountService}), que faz o trabalho de fato.</p>
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Devolve os dados da conta do usuário logado para exibir na tela.
     *
     * @return nome, e-mail e perfis de acesso do usuário atual
     */
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> currentAccount() {
        return ResponseEntity.ok(accountService.currentAccount());
    }

    /**
     * Recebe o pedido de troca de senha feito pela pessoa na tela.
     *
     * @param request senha atual e nova senha informadas pelo usuário
     * @return os dados da conta após a troca de senha
     */
    @PostMapping("/password")
    public ResponseEntity<AccountResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(accountService.changePassword(request));
    }
}
