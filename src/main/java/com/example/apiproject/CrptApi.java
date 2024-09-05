package com.example.apiproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final long periodInNanos;
    private long lastResetTime;
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.periodInNanos = timeUnit.toNanos(1);
        this.lastResetTime = System.nanoTime();
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        long currentTime = System.nanoTime();
        if (currentTime - lastResetTime > periodInNanos) {
            semaphore.release(semaphore.getQueueLength());
            lastResetTime = currentTime;
        }

        semaphore.acquire();
        try {
            sendRequest(document, signature);
        } finally {
            semaphore.release();
        }
    }

    private void sendRequest(Document document, String signature) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);
            String jsonDocument = objectMapper.writeValueAsString(document);
            StringEntity entity = new StringEntity(jsonDocument);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Signature", signature);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // обработка ответа
            }
        }
    }

    @Getter
    @Setter
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;
    }

    @Getter
    @Setter
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
