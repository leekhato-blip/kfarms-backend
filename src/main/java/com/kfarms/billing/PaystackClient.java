package com.kfarms.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaystackClient {

    private static final String PROVIDER = "PAYSTACK";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kfarms.billing.paystack.enabled:false}")
    private boolean enabled;

    @Value("${kfarms.billing.paystack.base-url:https://api.paystack.co}")
    private String baseUrl;

    @Value("${kfarms.billing.paystack.secret-key:}")
    private String secretKey;

    @Value("${kfarms.billing.paystack.pro-monthly-plan-code:}")
    private String proMonthlyPlanCode;

    public String providerName() {
        return PROVIDER;
    }

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(secretKey);
    }

    public String getProMonthlyPlanCode() {
        return StringUtils.hasText(proMonthlyPlanCode) ? proMonthlyPlanCode.trim() : "";
    }

    public JsonNode initializeTransaction(Map<String, Object> payload) {
        return request(HttpMethod.POST, "/transaction/initialize", payload);
    }

    public JsonNode verifyTransaction(String reference) {
        String encodedReference = UriUtils.encodePathSegment(reference, StandardCharsets.UTF_8);
        return request(HttpMethod.GET, "/transaction/verify/" + encodedReference, null);
    }

    public JsonNode createSubscription(String customerCode, String planCode, String authorizationCode) {
        return request(
                HttpMethod.POST,
                "/subscription",
                Map.of(
                        "customer", customerCode,
                        "plan", planCode,
                        "authorization", authorizationCode
                )
        );
    }

    public void disableSubscription(String subscriptionCode, String subscriptionToken) {
        request(
                HttpMethod.POST,
                "/subscription/disable",
                Map.of(
                        "code", subscriptionCode,
                        "token", subscriptionToken
                )
        );
    }

    public String createSubscriptionManageLink(String subscriptionCode) {
        String encodedCode = UriUtils.encodePathSegment(subscriptionCode, StandardCharsets.UTF_8);
        JsonNode data = request(HttpMethod.GET, "/subscription/" + encodedCode + "/manage/link", null);
        String link = readText(data.path("link"));
        if (!StringUtils.hasText(link)) {
            throw new IllegalArgumentException("Paystack did not return a subscription management link.");
        }
        return link;
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) {
        ensureConfigured();
        if (!StringUtils.hasText(signature) || rawBody == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().formatHex(digest).getBytes(StandardCharsets.UTF_8);
            byte[] actual = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private JsonNode request(HttpMethod method, String path, Object body) {
        ensureConfigured();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey.trim());
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + path,
                method,
                entity,
                String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("status").asBoolean(false)) {
                String message = readText(root.path("message"));
                throw new IllegalArgumentException(
                        StringUtils.hasText(message) ? message : "Paystack request failed."
                );
            }
            return root.path("data");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not read Paystack response.");
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalArgumentException(
                    "Paystack billing is not configured. Set the Paystack secret key and enable billing first."
            );
        }
    }

    private String readText(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull() ? node.asText("") : "";
    }
}
