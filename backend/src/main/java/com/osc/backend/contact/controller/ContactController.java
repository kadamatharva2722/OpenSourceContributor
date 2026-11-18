package com.osc.backend.contact.controller;

import com.osc.backend.common.security.ClientIpResolver;
import com.osc.backend.common.security.PublicSubmissionRateLimiter;
import com.osc.backend.contact.dto.ContactRequestDto;
import com.osc.backend.contact.dto.ContactResponseDto;
import com.osc.backend.contact.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    private final ContactService contactService;
    private final PublicSubmissionRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public ContactController(
            ContactService contactService,
            PublicSubmissionRateLimiter rateLimiter,
            ClientIpResolver clientIpResolver
    ) {
        this.contactService = contactService;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    public ResponseEntity<ContactResponseDto> submitContactMessage(
            @Valid @RequestBody ContactRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolvePublicSubmissionClientIp(httpRequest);
        rateLimiter.checkRateLimit(clientIp);

        ContactResponseDto response = contactService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
