package com.trabalho.gestao_acoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GestaoAcoesApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestaoAcoesApplication.class, args);
	}

}
