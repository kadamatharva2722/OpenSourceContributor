package com.osc.backend.contact.service;

import com.osc.backend.contact.dto.ContactRequestDto;
import com.osc.backend.contact.dto.ContactResponseDto;
import com.osc.backend.contact.entity.ContactMessage;
import com.osc.backend.contact.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContactMessageRepository contactMessageRepository;

    public ContactService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Transactional
    public ContactResponseDto submitMessage(ContactRequestDto request) {
        String refCode = generateReferenceCode();
        ContactMessage message = new ContactMessage(
                refCode,
                request.getName().trim(),
                request.getEmail().trim(),
                request.getTopic().trim(),
                request.getSubject().trim(),
                request.getMessage().trim()
        );

        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Contact message received with reference code {} from {}", refCode, saved.getEmail());
        return ContactResponseDto.fromEntity(saved);
    }

    private String generateReferenceCode() {
        int randomSuffix = 1000 + RANDOM.nextInt(9000);
        return "OSC-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT) + "-" + randomSuffix;
    }
}
