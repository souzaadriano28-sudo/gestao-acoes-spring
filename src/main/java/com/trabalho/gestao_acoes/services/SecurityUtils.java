package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.UserAccount;
import com.trabalho.gestao_acoes.repositories.UserAccountRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    private final UserAccountRepository userRepository;

    public SecurityUtils(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("UNAUTHORIZED", "Acesso não autorizado.");
        }
        return userRepository.findByEmail(auth.getName()).or(() -> userRepository.findByUsername(auth.getName()))
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Acesso não autorizado."));
    }
    
    public Long currentOwnerId() {
        return currentUser().getId();
    }
}
