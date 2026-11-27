package edu.sysu.snowfield.payment.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sysu.snowfield.common.web.ApiResponse;
import edu.sysu.snowfield.payment.application.PaymentApplicationService;
import edu.sysu.snowfield.payment.application.PaymentResult;
import edu.sysu.snowfield.payment.application.SimulatePaymentCommand;

@Validated
@RestController
@RequestMapping("/api/v1/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentApplicationService paymentService;

    public PaymentController(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/simulate")
    public ApiResponse<PaymentResponse> simulate(
            @PathVariable long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody SimulatePaymentRequest request,
            Authentication authentication) {
        long authenticatedUserId = Long.parseLong(authentication.getName());
        PaymentResult result = paymentService.simulate(new SimulatePaymentCommand(
                orderId, authenticatedUserId, idempotencyKey, request.desiredOutcome()));
        return ApiResponse.ok(new PaymentResponse(
                Long.toString(result.paymentId()),
                result.paymentNo(),
                Long.toString(result.orderId()),
                result.status(),
                result.amount(),
                result.method(),
                result.paidAt()));
    }
}
