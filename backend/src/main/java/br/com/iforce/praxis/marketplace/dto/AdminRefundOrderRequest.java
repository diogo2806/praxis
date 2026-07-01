package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.Size;

public record AdminRefundOrderRequest(
        @Size(max = 500)
        String reason
) {
}
