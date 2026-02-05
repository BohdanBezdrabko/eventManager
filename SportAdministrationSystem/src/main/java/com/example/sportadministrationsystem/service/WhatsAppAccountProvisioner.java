package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.UserWhatsapp;
import com.example.sportadministrationsystem.repository.UserWhatsappRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WhatsAppAccountProvisioner {

    private final UserWhatsappRepository repo;

    @Transactional
    public UserWhatsapp ensure(String waId, String profileName) {
        if (waId == null || waId.isBlank()) throw new IllegalArgumentException("waId is blank");

        return repo.findByWaId(waId)
                .map(existing -> {
                    if (profileName != null && !profileName.isBlank()
                            && (existing.getProfileName() == null || existing.getProfileName().isBlank())) {
                        existing.setProfileName(profileName.trim());
                    }
                    return existing;
                })
                .orElseGet(() -> repo.save(
                        UserWhatsapp.builder()
                                .waId(waId.trim())
                                .profileName(profileName != null ? profileName.trim() : null)
                                .build()
                ));
    }
}
