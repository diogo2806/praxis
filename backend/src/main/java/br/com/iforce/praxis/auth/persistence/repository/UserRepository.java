package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import br.com.iforce.praxis.admin.model.UserStatus;

import java.util.List;

import java.util.Optional;


/**
 * Ponto de consulta dos usuarios da plataforma.
 *
 * <p>Os metodos abaixo apoiam processos de login, convite, reset de senha,
 * administracao de acessos e seguranca por empresa. A documentacao descreve
 * o papel de cada consulta na rotina do produto, sem expor detalhes tecnicos
 * de banco de dados.</p>
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Encontra o cadastro de uma pessoa pelo e-mail dentro de uma empresa.
     *
     * <p>Usado quando o processo precisa confirmar quem e o usuario em um
     * contexto especifico de empresa, como acesso, convite ou validacao de
     * cadastro.</p>
     */
    Optional<UserEntity> findFirstByEmailAndEmpresaId(String email, String empresaId);

    /**
     * Recupera um usuario vinculado a uma empresa.
     *
     * <p>Apoia fluxos que precisam identificar se a empresa ja possui ao menos
     * uma pessoa cadastrada para continuar uma etapa administrativa ou de
     * configuracao.</p>
     */
    Optional<UserEntity> findFirstByEmpresaId(String empresaId);

    /**
     * Lista os usuarios de uma empresa que possuem um papel especifico.
     *
     * <p>Usado para direcionar responsabilidades do processo, como encontrar
     * administradores, avaliadores ou outros perfis que podem executar uma acao
     * dentro da empresa.</p>
     */
    @Query("select distinct u from UserEntity u join u.roles r where u.empresaId = :empresaId and r = :role")
    List<UserEntity> findByEmpresaIdAndRole(@Param("empresaId") String empresaId, @Param("role") String role);

    /**
     * Lista os usuarios de uma empresa na ordem em que foram criados.
     *
     * <p>Fornece uma visao historica de entrada dos usuarios, util para telas
     * administrativas, acompanhamento de cadastro e verificacoes de suporte.</p>
     */
    List<UserEntity> findByEmpresaIdOrderByCreatedAtAsc(String empresaId);

    /**
     * Busca um usuario pelo identificador, garantindo que ele pertence a empresa informada.
     *
     * <p>Protege o processo contra acesso indevido a dados de outra empresa,
     * mantendo cada consulta dentro do contexto correto do cliente.</p>
     */
    Optional<UserEntity> findByIdAndEmpresaId(Long id, String empresaId);

    /**
     * Verifica se ja existe um usuario com o mesmo e-mail dentro da empresa.
     *
     * <p>Evita duplicidade em cadastros e convites, antes de criar uma nova
     * conta ou reenviar uma etapa de entrada na plataforma.</p>
     */
    boolean existsByEmpresaIdAndEmail(String empresaId, String email);

    /**
     * Localiza usuarios de um status especifico que ainda possuem convite em aberto.
     *
     * <p>Apoia processos de acompanhamento, expiracao ou reenvio de convites,
     * identificando contas que ainda dependem de uma acao do usuario convidado.</p>
     */
    List<UserEntity> findByStatusAndInviteTokenHashIsNotNull(UserStatus status);

    /**
     * Localiza usuarios que possuem uma solicitacao de redefinicao de senha em andamento.
     *
     * <p>Permite que o processo de seguranca encontre contas com token de reset
     * registrado, por exemplo para validar, concluir ou limpar solicitacoes
     * pendentes.</p>
     */
    List<UserEntity> findByPasswordResetTokenHashIsNotNull();
}
