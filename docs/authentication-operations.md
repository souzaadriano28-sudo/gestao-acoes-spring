# Autenticação administrativa

O Atlas Carteira usa uma única conta administrativa persistida, sessão no servidor e cookie `ATLAS_SESSION` HttpOnly. Não há JWT, remember-me, cadastro público, recuperação, login social ou tokens em armazenamento persistente do navegador.

## Versões e hash efetivos

O build é governado pelo BOM do Spring Boot 4.0.6. A árvore verificada pelo Maven resolve `spring-security-config`, `spring-security-core`, `spring-security-web`, `spring-security-crypto` e `spring-security-test` todos na versão 7.0.5, sem versões conflitantes. O `DelegatingPasswordEncoder` persiste o identificador do algoritmo junto ao hash; nesta versão, novas senhas usam `{bcrypt}` com custo padrão 10. O formato delegável permite elevar o custo ou migrar o algoritmo posteriormente sem aceitar texto claro.

## Primeiro bootstrap

Forneça `ADMIN_INITIAL_USERNAME` e `ADMIN_INITIAL_PASSWORD` por cofre/variáveis do runtime antes da primeira inicialização. A senha deve ter de 12 a 128 caracteres e não pode ser igual ao usuário. O backend persiste apenas o hash adaptativo no formato delegável do Spring Security. Se uma conta já existir, reiniciar com outros valores não troca usuário nem hash.

Nunca publique `.env`, saída completa de `docker compose config`, cookies, dumps da tabela `admin_user` ou logs de tráfego de `/auth`. Após o primeiro bootstrap, remova a senha inicial do ambiente quando o mecanismo de implantação permitir. Nesta fase não há rotação pela interface: uma rotação emergencial exige gerar um hash compatível em procedimento administrativo controlado, atualizar somente `password_hash`, limpar bloqueios e invalidar sessões reiniciando a única réplica.

## Sessão, HTTPS e limites

- timeout ocioso padrão: 30 minutos;
- cookie: `HttpOnly`, `SameSite=Lax`, `Path=/`; `Secure=true` é obrigatório sob HTTPS;
- o Compose local publica somente HTTP em loopback e declara `SESSION_COOKIE_SECURE=false` como exceção explícita;
- cinco falhas em quinze minutos bloqueiam conta e origem por quinze minutos;
- o bloqueio de conta persiste no PostgreSQL; o de origem é local à única réplica;
- antes de escalar horizontalmente, mova sessões e bloqueios de origem para armazenamento compartilhado.

## Contratos

`GET /auth/csrf` é público e prepara o token em memória; `POST /auth/login` exige esse token. `GET /auth/session`, `POST /auth/logout` e todos os endpoints de negócio exigem sessão. Logout também exige CSRF e invalida o cookie. Healthchecks agregados permanecem públicos e não revelam detalhes.

Produção deve usar HTTPS e `SESSION_COOKIE_SECURE=true`. O proxy confiável deve sobrescrever `X-Forwarded-For`; nunca habilite `AUTH_TRUST_FORWARDED_HEADERS` quando o backend puder ser acessado diretamente por clientes não confiáveis.
