package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.MessageSenderType;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long threadId,
        MessageSenderType senderType,
        Long senderId,
        String body,
        Instant createdAt
) {
}
