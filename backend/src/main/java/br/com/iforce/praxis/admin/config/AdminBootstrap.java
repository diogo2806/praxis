package br.com.iforce.praxis.admin.config;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Garante a existência de um operador ADMIN inicial vinculado ao tenant técnico {@code PLATFORM}.
 *
 * <p>O ADMIN é o operador da plataforma ({@code roles = ADMIN}, {@code tenantId = PLATFORM}).
 * A criação é opcional e idempotente: só ocorre quando {@code praxis.admin.bootstrap.email} e
 * {@code praxis.admin.bootstrap.password} estão configurados e ainda não existe usuário com
 * aquele e-mail no tenant PLATFORM. As credenciais ficam apenas em variáveis de ambiente.</p>
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String PLATFORM_TENANT_ID = "PLATFORM";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            @Value("${praxis.admin.bootstrap.email:}") String email,
            @Value("${praxis.admin.bootstrap.password:}") String password,
            @Value("${praxis.admin.bootstrap.name:Operador da plataforma}") String name
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (tenantRepository.findById(PLATFORM_TENANT_ID).isEmpty()) {
            log.warn("Tenant técnico PLATFORM ausente; operador ADMIN inicial não foi criado.");
            return;
        }
        if (userRepository.findFirstByEmailAndTenantId(email, PLATFORM_TENANT_ID).isPresent()) {
            return;
        }

        UserEntity admin = new UserEntity();
        admin.setTenantId(PLATFORM_TENANT_ID);
        admin.setEmail(email);
        admin.setName(name);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRoles(Set.of(ADMIN_ROLE));
        admin.setStatus(UserStatus.ATIVO);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);

        log.info("Operador ADMIN inicial criado no tenant PLATFORM (e-mail mascarado: {}).", mask(email));
    }

    private static String mask(String value) {
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }
}
