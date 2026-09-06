package com.trabalho.gestao_acoes.config;

import com.trabalho.gestao_acoes.services.AdminBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private final AdminBootstrapService service;
    public AdminBootstrapRunner(AdminBootstrapService service) { this.service = service; }
    @Override public void run(ApplicationArguments args) { service.bootstrap(); }
}
