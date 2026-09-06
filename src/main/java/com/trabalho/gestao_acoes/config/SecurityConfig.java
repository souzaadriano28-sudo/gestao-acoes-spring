package com.trabalho.gestao_acoes.config;

import com.trabalho.gestao_acoes.repositories.AdminUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.time.Clock;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){ return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean Clock authClock(){ return Clock.systemUTC(); }
    @Bean HttpSessionCsrfTokenRepository csrfTokenRepository(){
        HttpSessionCsrfTokenRepository repository=new HttpSessionCsrfTokenRepository(); repository.setHeaderName("X-CSRF-TOKEN"); return repository;
    }
    @Bean HttpSessionSecurityContextRepository securityContextRepository(){ return new HttpSessionSecurityContextRepository(); }
    @Bean UserDetailsService adminUserDetailsService(AdminUserRepository repository, Clock clock){
        return username -> repository.findByUsername(username).map(admin -> User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash()).roles("ADMIN").disabled(!admin.isEnabled()).accountLocked(admin.isLocked(clock.instant())).build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Invalid credentials"));
    }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception { return configuration.getAuthenticationManager(); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorWriter errors,
            HttpSessionCsrfTokenRepository csrfRepository, HttpSessionSecurityContextRepository contextRepository) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler=new CsrfTokenRequestAttributeHandler(); requestHandler.setCsrfRequestAttributeName("_csrf");
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(requestHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,"/auth/csrf","/actuator/health","/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req,res,cause)->errors.write(req,res,HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Autenticação necessária."))
                .accessDeniedHandler((req,res,cause)->errors.write(req,res,HttpStatus.FORBIDDEN,"ACCESS_DENIED","Acesso não autorizado.")))
            .sessionManagement(session -> session.sessionFixation(fix -> fix.changeSessionId()))
            .securityContext(context -> context.securityContextRepository(contextRepository).requireExplicitSave(true))
            .requestCache(cache -> cache.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable());
        return http.build();
    }
}
