package org.example.bajajproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebhookTaskRunner implements CommandLineRunner {

    @Autowired
    private RestTemplate restTemplate;

    private String accessToken;

    @Override
    public void run(String... args) throws Exception {

        String webhookUrl = generateWebhook();
        System.out.println("Webhook URL: " + webhookUrl);
        System.out.println("Access Token: " + accessToken);

        String finalQuery = solveSQL();
        System.out.println("Final SQL Query: " + finalQuery);

        sendSolution(webhookUrl, finalQuery);
    }

    private String generateWebhook() {
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> body = new HashMap<>();
        body.put("name", "prakhar nag");
        body.put("regNo", "0002AL221041");
        body.put("email", "prakharbbx@gmail.com");

        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null || !responseBody.containsKey("accessToken") || !responseBody.containsKey("webhook")) {
            throw new RuntimeException("Failed to get webhook or access token from server.");
        }

        this.accessToken = (String) responseBody.get("accessToken");
        return (String) responseBody.get("webhook");
    }

    private String solveSQL() {
        return "SELECT \n" +
                "    p.AMOUNT AS SALARY,\n" +
                "    CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,\n" +
                "    TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,\n" +
                "    d.DEPARTMENT_NAME\n" +
                "FROM PAYMENTS p\n" +
                "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID\n" +
                "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID\n" +
                "WHERE DAY(p.PAYMENT_TIME) != 1\n" +
                "  AND p.AMOUNT = (\n" +
                "      SELECT MAX(AMOUNT)\n" +
                "      FROM PAYMENTS\n" +
                "      WHERE DAY(PAYMENT_TIME) != 1\n" +
                "  );";
    }

    private void sendSolution(String webhookUrl, String finalQuery) {
        HttpHeaders headers = new HttpHeaders();

        // Use only token if API expects it without "Bearer"
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("finalQuery", finalQuery);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            System.out.println("Webhook submission response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println("Failed to submit solution: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Unexpected error while submitting solution: " + e.getMessage());
        }
    }
}