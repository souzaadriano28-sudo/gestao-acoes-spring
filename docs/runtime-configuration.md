# Configuração externa e proteção de segredos

O backend usa exclusivamente a configuração externa do Spring Boot. Nenhuma biblioteca dotenv foi adicionada e o Spring não lê `.env` automaticamente. O arquivo `.env.example` é apenas um catálogo com valores falsos; copie os nomes para o gerenciador da IDE ou exporte-os no shell, sem criar ou versionar credenciais reais.

## Contrato de variáveis

| Variável | Perfil/consumidor | Obrigatória | Default seguro |
|---|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | Spring | não | `test` |
| `DB_URL` | `dev` / datasource PostgreSQL 17 | não | `jdbc:postgresql://localhost:5432/gestao_acoes` |
| `DB_USERNAME` | `dev` / datasource | não | `postgres` |
| `DB_PASSWORD` | `dev` / datasource | sim | nenhum funcional |
| `BRAPI_TOKEN` | `dev` / Brapi | sim | nenhum funcional |
| `TWELVEDATA_API_KEY` | `dev` / TwelveData | sim | nenhum funcional |
| `INTEGRATIONS_BRAPI_URL` | cliente Brapi | não | endpoint público atual |
| `INTEGRATIONS_TWELVEDATA_URL` | cliente TwelveData | não | endpoint público atual |
| `INTEGRATIONS_BRASILAPI_URL` | cliente BrasilAPI | não | endpoint público atual |
| `INTEGRATIONS_VIACEP_URL` | cliente ViaCEP | não | endpoint público atual |
| `APP_CORS_ALLOWED_ORIGIN` | CORS MVC | não | `http://localhost:4200` |

O perfil `dev` rejeita senha e tokens ausentes, vazios ou compostos apenas por espaços. A mensagem identifica somente o nome da variável. Os perfis de teste usam tokens inequivocamente fictícios e endpoints loopback; os provedores reais devem continuar substituídos por doubles/stubs nos testes.

## Formas de execução

PowerShell, somente para a sessão atual:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DB_PASSWORD = '<obter-do-cofre-local>'
$env:BRAPI_TOKEN = '<obter-do-cofre-local>'
$env:TWELVEDATA_API_KEY = '<obter-do-cofre-local>'
./mvnw.cmd spring-boot:run
```

Shell POSIX, somente para o processo:

```sh
SPRING_PROFILES_ACTIVE=dev \
DB_PASSWORD='<obter-do-cofre-local>' \
BRAPI_TOKEN='<obter-do-cofre-local>' \
TWELVEDATA_API_KEY='<obter-do-cofre-local>' \
./mvnw spring-boot:run
```

Na IDE, configure as mesmas variáveis na configuração de execução e impeça o compartilhamento desse conteúdo. O futuro Compose poderá usar interpolação de ambiente, mas isso pertence à fase 3 e não está implementado.

## Responsabilidade operacional

A plataforma de produção e seu gerenciador de segredos estão fora do escopo. A equipe responsável pelo deploy deve injetar as variáveis no processo, restringir acesso, auditar uso e executar rotação. Alterar `.env`, `.env.example` ou uma variável local não altera uma senha já persistida no PostgreSQL; a rotação exige mudar a credencial no banco e no cofre de forma coordenada.
