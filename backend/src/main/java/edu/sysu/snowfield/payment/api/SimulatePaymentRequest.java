package edu.sysu.snowfield.payment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SimulatePaymentRequest(
        @NotBlank
        @Pattern(regexp = "SUCCESS|FAILURE", message = "只能是 SUCCESS 或 FAILURE")
        String desiredOutcome) {
}
