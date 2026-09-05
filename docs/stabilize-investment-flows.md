# Fluxos estabilizados de investimento

## Configuração

O backend lê `BRAPI_TOKEN`, `TWELVEDATA_API_KEY` e, no perfil `dev`, `DB_PASSWORD`. Valores ausentes ou compostos somente por espaços impedem a criação das estratégias de cotação. Use valores fictícios em teste; nunca coloque segredos em propriedades, fixtures, documentação ou coleções Postman. A remoção do repositório não revoga um segredo já exposto: o responsável deve substituí-lo nos provedores Brapi/Twelve Data e no banco.

## Contratos

O corpo de CNPJ aceita 14 dígitos ou a máscara `00.000.000/0000-00`, com espaços somente nas extremidades; a URL `/corretoras/cnpj/{cnpj}` aceita apenas 14 dígitos. Os dígitos verificadores são obrigatórios. Tickers são aparados e convertidos para maiúsculas. `NACIONAL` e `BRASIL` persistem como `BRASIL`; `INTERNACIONAL` e `AMERICANO` persistem como `AMERICANO`.

Erros retornam `timestamp`, `status`, `code`, `error`, `message`, `path` e `fieldErrors`. JSON malformado, tipo incorreto ou parâmetro de rota não convertível usa 400/MALFORMED_REQUEST; validação e regra de negócio usam 422, incluindo venda sem posição ou saldo suficiente; recurso local/externo ausente usa 404; duplicidade ou conflito concorrente usa 409; limite externo usa 429; cotação/resposta inválida usa 502; indisponibilidade externa usa 503; falha inesperada usa 500. Método e mídia inválidos preservam 405 e 415.

Quantidade é inteiro positivo. Preços e médias usam `numeric(19,8)` e `HALF_UP`. O patrimônio soma todos os valores sem arredondamento intermediário e arredonda o resultado a duas casas. Cotações são externas; ativos americanos usam o câmbio fixo 5,30 já existente. Não há preço manual nem idempotência persistente: uma mutação cuja resposta se perdeu deve ser conferida pela leitura das posições antes de nova tentativa.

## Verificação

Backend padrão, sem rede externa: `mvnw.cmd verify` (ou `mvn.cmd verify` se o wrapper local não funcionar). A suíte inicia um PostgreSQL embarcado e isolado para os testes de persistência concorrente, com barreiras e timeouts limitados. Para conferir também contra uma instância fornecida pela equipe, use `SPRING_PROFILES_ACTIVE=postgresql-test`, `TEST_DB_URL`, `TEST_DB_USER` e `TEST_DB_PASSWORD`.

Frontend: `npm ci`, `npm run build`, `npm test -- --watch=false` e `npm run e2e`. O comando E2E inicia Angular e Spring reais, com H2 isolado, e um servidor Node local que simula somente Brapi, Twelve Data, BrasilAPI e ViaCEP. As URLs Feign podem ser direcionadas aos stubs pelas propriedades `integrations.brapi.url`, `integrations.twelvedata.url`, `integrations.brasilapi.url` e `integrations.viacep.url`; os padrões de execução continuam sendo os provedores reais. O navegador valida 10 PETR4 a BRL 20, 2 AAPL a USD 100 e venda de 4 PETR4, deixando 6 PETR4 e 2 AAPL, com patrimônio JSON `1180.00`; também cobre recusa 422, confirmação seguida de falha do provedor durante a atualização e resultado desconhecido descartando a resposta de uma compra realmente confirmada pelo backend.

O baseline desta mudança encontrou o wrapper Maven incompatível com o ambiente PowerShell local e testes Angular com imports/expectativas obsoletos; o Maven do sistema foi usado sem ignorar testes, e os testes Angular foram alinhados aos componentes e serviços reais.
