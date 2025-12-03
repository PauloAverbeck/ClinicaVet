package com.example.application.classes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
public class ViaCepService {

    private static final Logger log = LoggerFactory.getLogger(ViaCepService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ViaCepService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Optional<ViaCepResponse> lookup(String cepRaw) {
        try {
            String digits = cepRaw == null ? "" : cepRaw.replaceAll("\\D", "");
            if (!digits.matches("\\d{8}")) {
                log.debug("ViaCep: CEP '{}' não possui 8 dígitos após limpeza -> ignorando", cepRaw);
                return Optional.empty();
            }

            String url = "https://viacep.com.br/ws/" + digits + "/json/";
            log.debug("ViaCep: consultando URL {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            log.debug("ViaCep: status code={} body={}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                log.warn("ViaCep retornou status {} para CEP {}", response.statusCode(), digits);
                return Optional.empty();
            }

            ViaCepResponse dto = objectMapper.readValue(response.body(), ViaCepResponse.class);

            if (dto.getErro() != null && dto.getErro()) {
                log.info("ViaCep: CEP {} não encontrado (campo erro = true)", digits);
                return Optional.empty();
            }

            return Optional.of(dto);
        } catch (Exception e) {
            log.error("Erro ao consultar ViaCep para CEP '{}'", cepRaw, e);
            return Optional.empty();
        }
    }
}