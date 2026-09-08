package com.merkatocircle.iqub.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.merkatocircle.iqub.exception.PaymentInitiationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The real payment provider: <a href="https://developer.chapa.co">Chapa</a>.
 *
 * <p>Endpoints and field names below are taken directly from Chapa's current developer
 * documentation (developer.chapa.co/integrations/accept-payments and .../verify-payments),
 * not reconstructed from memory:
 * <ul>
 *   <li>Initialize: {@code POST https://api.chapa.co/v1/transaction/initialize}</li>
 *   <li>Verify:     {@code GET  https://api.chapa.co/v1/transaction/verify/{tx_ref}}</li>
 *   <li>Auth:        {@code Authorization: Bearer <secret key>} on both calls</li>
 * </ul>
 * Chapa's own documentation is explicit that the redirect and the callback payload should
 * never be trusted on their own — always re-verify server-side, which is exactly what
 * {@link #verify} does and why {@code ContributionService.confirmPayment} calls it rather
 * than trusting whatever query parameters come back on the return page.
 *
 * <p>Only active under the {@code chapa} Spring profile — see {@link PaymentGatewayConfig}
 * for how this and {@link FakePaymentGateway} are chosen.
 */
@Component
@Profile("chapa")
public class ChapaPaymentGateway implements PaymentGateway {

    private static final String BASE_URL = "https://api.chapa.co/v1";

    private final RestClient restClient;
    private final String secretKey;

    public ChapaPaymentGateway(RestClient.Builder builder,
                                @Value("${chapa.secret-key}") String secretKey) {
        this.secretKey = secretKey;
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public PaymentInitiation initiate(PaymentRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", request.amount().toPlainString());
        body.put("currency", request.currency());
        body.put("email", request.email());
        body.put("first_name", request.firstName());
        body.put("last_name", request.lastName());
        body.put("phone_number", request.phoneNumber());
        body.put("tx_ref", request.txRef());
        body.put("callback_url", request.callbackUrl());
        body.put("return_url", request.returnUrl());
        body.put("customization", Map.of(
                "title", "Merkato Circle",
                "description", "Iqub round contribution"));

        try {
            InitializeResponse response = restClient.post()
                    .uri("/transaction/initialize")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(InitializeResponse.class);

            if (response == null || response.data == null || response.data.checkoutUrl == null) {
                throw new PaymentInitiationException(
                        "Chapa accepted the request but returned no checkout_url", null);
            }
            return new PaymentInitiation(response.data.checkoutUrl, request.txRef());

        } catch (RestClientException ex) {
            throw new PaymentInitiationException("Could not reach Chapa to start checkout", ex);
        }
    }

    @Override
    public PaymentVerification verify(String txRef) {
        try {
            VerifyResponse response = restClient.get()
                    .uri("/transaction/verify/{txRef}", txRef)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .retrieve()
                    .body(VerifyResponse.class);

            if (response == null || response.data == null || response.data.status == null) {
                return new PaymentVerification(PaymentStatus.PENDING, txRef, null);
            }
            PaymentStatus status = switch (response.data.status) {
                case "success" -> PaymentStatus.SUCCESS;
                case "failed" -> PaymentStatus.FAILED;
                default -> PaymentStatus.PENDING;
            };
            return new PaymentVerification(status, txRef, response.data.refId);

        } catch (RestClientException ex) {
            // A network hiccup while verifying is not the same claim as "payment failed" —
            // treat it as still-pending so a flaky connection can never wrongly fail a member.
            return new PaymentVerification(PaymentStatus.PENDING, txRef, null);
        }
    }

    // ---- response shapes, matching Chapa's actual JSON (developer.chapa.co) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class InitializeResponse {
        public String status;
        public String message;
        public CheckoutData data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CheckoutData {
        @JsonProperty("checkout_url")
        public String checkoutUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VerifyResponse {
        public String status;
        public String message;
        public VerifyData data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VerifyData {
        public String status;
        @JsonProperty("ref_id")
        public String refId;
    }
}
