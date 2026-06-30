package br.com.iforce.praxis.admin.dto;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;


/**
 * Convite de um novo usuário de acesso para um cliente.
 *
 * <p>Usuários convidados recebem o papel {@code EMPRESA}. O painel ADMIN nunca cria
 * outro {@code ADMIN} dentro de um empresa cliente.</p>
 */
public record InviteUserAdminRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
