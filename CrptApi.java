import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        // Создаем отдельный поток для сброса семафора через определенный интервал времени
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1));
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5); // Максимум 5 запросов в секунду
        DocumentEntity documentEntity = new DocumentEntity();
        crptApi.createDocument(documentEntity);
    }

    private void createDocument(DocumentEntity documentEntity) {
        try {
            semaphore.acquire(); // Запрашиваем разрешение перед вызовом API

            // Преобразуем документ в JSON
            String jsonDocument = objectMapper.writeValueAsString(documentEntity);

            // Создаем POST запрос
            HttpPost httpPost = new HttpPost(apiUrl);
            StringEntity entity = new StringEntity(jsonDocument, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // Выполняем запрос
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            // Обрабатываем ответ
            if (statusCode == 200) {
                System.out.println("Document creation successful");
            } else {
                System.out.println("Document creation failed with status code: " + statusCode);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static class DocumentEntity {
        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("importRequest")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date productionDate;

        @JsonProperty("production_type")
        private String productionType;

        @JsonProperty("products")
        private List<Product> products;

        @JsonProperty("reg_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date regDate;

        @JsonProperty("reg_number")
        private String regNumber;

        // Внутренний класс для описания поля description
        public static class Description {
            @JsonProperty("participantInn")
            private String participantInn;
        }

        // Внутренний класс для описания продукта
        public static class Product {
            @JsonProperty("certificate_document")
            private String certificateDocument;

            @JsonProperty("certificate_document_date")
            @JsonFormat(pattern = "yyyy-MM-dd")
            private Date certificateDocumentDate;

            @JsonProperty("certificate_document_number")
            private String certificateDocumentNumber;

            @JsonProperty("owner_inn")
            private String ownerInn;

            @JsonProperty("producer_inn")
            private String producerInn;

            @JsonProperty("production_date")
            @JsonFormat(pattern = "yyyy-MM-dd")
            private Date productionDate;

            @JsonProperty("tnved_code")
            private String tnvedCode;

            @JsonProperty("uit_code")
            private String uitCode;

            @JsonProperty("uitu_code")
            private String uituCode;
        }
    }
}
