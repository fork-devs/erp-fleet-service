package com.erp.fleet.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Component
public class BranchDirectory {
    private final RestClient client;

    public BranchDirectory(@Value("${erp.tenant-service.url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(4));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public void verify(UUID tenantId, UUID branchId, String authorization) {
        try {
            Branch branch = client.get().uri("/api/v1/branches/{id}", branchId)
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(Branch.class);
            if (branch == null || !branchId.equals(branch.id()) || !tenantId.equals(branch.workshopId())) {
                throw new IllegalArgumentException("La sede no pertenece al taller");
            }
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 403) {
                throw new IllegalArgumentException("La sede no está disponible para el taller");
            }
            throw unavailable(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private ResponseStatusException unavailable(RestClientException cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo comprobar la sede", cause);
    }

    public record Branch(UUID id, UUID workshopId) {}
}
