package com.osc.backend.request.service;

import com.osc.backend.request.dto.RepositoryRequestCreateDto;
import com.osc.backend.request.dto.RepositoryRequestResponseDto;
import com.osc.backend.request.entity.RepositoryRequest;
import com.osc.backend.request.repository.RepositoryRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryRequestService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryRequestService.class);

    private final RepositoryRequestRepository repositoryRequestRepository;

    public RepositoryRequestService(RepositoryRequestRepository repositoryRequestRepository) {
        this.repositoryRequestRepository = repositoryRequestRepository;
    }

    @Transactional
    public RepositoryRequestResponseDto createRequest(RepositoryRequestCreateDto request) {
        RepositoryRequest repoRequest = new RepositoryRequest(
                request.getRepositoryUrl().trim(),
                request.getRepositoryName().trim(),
                request.getReason() != null ? request.getReason().trim() : null,
                request.getRequestedByUsername() != null ? request.getRequestedByUsername().trim() : "anonymous"
        );

        RepositoryRequest saved = repositoryRequestRepository.save(repoRequest);
        log.info("Repository request created for {} by {}", saved.getRepositoryName(), saved.getRequestedByUsername());
        return RepositoryRequestResponseDto.fromEntity(saved);
    }
}
