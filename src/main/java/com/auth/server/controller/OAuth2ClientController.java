package com.auth.server.controller;

import com.auth.server.domain.dto.request.OAuth2ClientRequest;
import com.auth.server.domain.dto.response.OAuth2ClientResponse;
import com.auth.server.security.rate.annotation.RateLimit;
import com.auth.server.service.OAuth2ClientService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oauth2/clients")
@RequiredArgsConstructor
public class OAuth2ClientController {

  private final OAuth2ClientService clientService;

  @PostMapping
  @RateLimit
  public ResponseEntity<OAuth2ClientResponse> createClient(
      @Valid @RequestBody OAuth2ClientRequest request) {
    OAuth2ClientResponse response = clientService.createClient(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{clientId}")
  @RateLimit
  public ResponseEntity<OAuth2ClientResponse> getClient(@PathVariable String clientId) {
    OAuth2ClientResponse response = clientService.getClient(clientId);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  @RateLimit
  public ResponseEntity<List<OAuth2ClientResponse>> getAllClients() {
    List<OAuth2ClientResponse> clients = clientService.getAllClients();
    return ResponseEntity.ok(clients);
  }

  @DeleteMapping("/{clientId}")
  @RateLimit
  public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
    clientService.deleteClient(clientId);
    return ResponseEntity.noContent().build();
  }
}

