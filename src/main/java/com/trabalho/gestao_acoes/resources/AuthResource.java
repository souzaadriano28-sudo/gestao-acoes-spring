package com.trabalho.gestao_acoes.resources;

import com.trabalho.gestao_acoes.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestController
@RequestMapping("/auth")
public class AuthResource {
    private static final Logger log=LoggerFactory.getLogger(AuthResource.class);
    private final AuthService authService; private final HttpSessionSecurityContextRepository contextRepository; private final HttpSessionCsrfTokenRepository csrfRepository;
    public AuthResource(AuthService authService, HttpSessionSecurityContextRepository contextRepository, HttpSessionCsrfTokenRepository csrfRepository){this.authService=authService;this.contextRepository=contextRepository;this.csrfRepository=csrfRepository;}
    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token){ return noStore(new CsrfResponse(token.getToken(),token.getHeaderName(),token.getParameterName())); }
    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest body,HttpServletRequest request,HttpServletResponse response){
        Authentication authentication=authService.authenticate(body.username(),body.password(),request);
        request.changeSessionId();
        SecurityContext context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context,request,response); csrfRepository.saveToken(null,request,response);
        return noStore(new SessionResponse(true,authentication.getName()));
    }
    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(Authentication authentication){ return noStore(new SessionResponse(true,authentication.getName())); }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response,Authentication authentication){
        new SecurityContextLogoutHandler().logout(request,response,authentication); expireCookie(response); log.info("Administrative logout completed");
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @ExceptionHandler(AuthService.AuthenticationRejectedException.class)
    public ResponseEntity<AuthFailure> rejected(){ return failure(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_FAILED"); }
    @ExceptionHandler(AuthService.AuthenticationLimitedException.class)
    public ResponseEntity<AuthFailure> limited(){ return failure(HttpStatus.TOO_MANY_REQUESTS,"AUTHENTICATION_TEMPORARILY_UNAVAILABLE"); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthFailure> invalidInput(){ return failure(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_FAILED"); }
    private ResponseEntity<AuthFailure> failure(HttpStatus status,String code){ return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new AuthFailure(code,"Não foi possível entrar. Verifique os dados ou tente novamente mais tarde.")); }
    private <T> ResponseEntity<T> noStore(T body){ return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    private void expireCookie(HttpServletResponse response){ Cookie cookie=new Cookie("ATLAS_SESSION",""); cookie.setHttpOnly(true); cookie.setPath("/"); cookie.setMaxAge(0); response.addCookie(cookie); }
    public record LoginRequest(@NotBlank @Size(max=64) String username,@NotBlank @Size(max=128) String password){}
    public record SessionResponse(boolean authenticated,String username){}
    public record CsrfResponse(String token,String headerName,String parameterName){}
    public record AuthFailure(String code,String message){}
}
