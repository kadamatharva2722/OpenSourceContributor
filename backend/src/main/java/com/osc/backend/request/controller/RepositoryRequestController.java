package com.osc.backend.request.controller;

import com.osc.backend.common.security.ClientIpResolver;
import com.osc.backend.common.security.PublicSubmissionRateLimiter;
import com.osc.backend.request.dto.RepositoryRequestCreateDto;
import com.osc.backend.request.dto.RepositoryRequestResponseDto;
import com.osc.backend.request.service.RepositoryRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests/repositories")
public class RepositoryRequestController {

    private final RepositoryRequestService repositoryRequestService;
    private final PublicSubmissionRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public RepositoryRequestController(
            RepositoryRequestService repositoryRequestService,
            PublicSubmissionRateLimiter rateLimiter,
            ClientIpResolver clientIpResolver
    ) {
        this.repositoryRequestService = repositoryRequestService;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    public ResponseEntity<RepositoryRequestResponseDto> submitRepositoryRequest(
            @Valid @RequestBody RepositoryRequestCreateDto request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolvePublicSubmissionClientIp(httpRequest);
        rateLimiter.checkRateLimit(clientIp);

        RepositoryRequestResponseDto response = repositoryRequestService.createRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
