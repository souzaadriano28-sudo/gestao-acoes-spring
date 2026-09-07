package com.trabalho.gestao_acoes.e2e;

import com.trabalho.gestao_acoes.GestaoAcoesApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Starts the real application with browser-test-only fault controls. */
public final class E2eTestLauncher {
    private E2eTestLauncher() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(GestaoAcoesApplication.class)
                .sources(E2eConflictFilterConfiguration.class)
                .run(args);
    }
}
