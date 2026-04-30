# 📊 Sistema de Gestão de Ações
![Java](https://img.shields.io/badge/Java-17-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green) ![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

## 📌 Sobre o Projeto
API REST para gestão de investimentos com Java + Spring Boot. Permite cadastro e consulta de 📈 ações e 🏦 corretoras. O usuário informa apenas dados básicos (CNPJ/ticker) e o sistema consulta APIs externas, valida, transforma e salva no banco, garantindo dados reais e consistentes.

## 🎯 Objetivos
- API REST robusta  
- Arquitetura em camadas  
- Integração com APIs externas  
- Consistência e anti-duplicidade  
- Uso de padrões de projeto  
- Código organizado e escalável  

## 🏗️ Arquitetura

Controller → Service → Repository → Banco de Dados

**Camadas:** Controller (endpoints), Service (regras), Repository (JPA), Domain (Entity/DTO)  
**Extras:** DTOs, Mappers, Ports, Integrations

## 📁 Estrutura

domains/ services/ repositories/ resources/ integrations/ ports/ mappers/


## 🔄 Fluxo
**Corretora:** `CNPJ → BrasilAPI → Adapter → ViaCEP → Adapter → Validação → Banco`  
**Ação:** `Ticker → Identificação → Strategy → API → Adapter → Banco`  
**Geral:** `Request → Controller → Service → API → Adapter → Repository → Response`

## 🌐 APIs
- 📍 ViaCEP: endereço por CEP  
- 🏢 BrasilAPI: dados por CNPJ  
- 📊 Brapi: ações brasileiras  
- 🌎 TwelveData: ações internacionais  
**Estratégia:** 🇧🇷 Brapi | 🌍 TwelveData

## 🧩 Padrões
**Strategy:** `BrapiStrategy`, `TwelveDataStrategy`  
**Adapter:** `BrasilApiAdapter`, `ViaCepAdapter`  
Outros: Repository, DTO, Injeção de Dependência

## 📜 Regras de Negócio
- Validação de corretora via CNAE  
- Bloqueio de dados fictícios  
- Duplicidade: CNPJ e ticker únicos  

## ⚠️ Erros
`@ControllerAdvice`  
422 (validação) | 400 (duplicidade) | 503 (API fora) | 429 (rate limit)

## 🗄️ Banco
H2 (dev) | PostgreSQL/MySQL (prod)

## 🧱 Entidades
**Corretora:** id, cnpj, razaoSocial, nomeFantasia, cep, logradouro, bairro, cidade, uf, situacaoCadastral, validadaNaCvm  
**Ação:** id, ticker, nomeEmpresa, mercado, moeda, cotacaoAtual, dataHoraCotacao

## ⚙️ Decisão Arquitetural
Ações não são vinculadas a corretoras → melhor performance, menos acoplamento, evita consultas complexas e problemas de serialização.

## 🚀 Diferenciais
Cache com `@Cacheable` (ViaCEP) | Logs (SLF4J) | Swagger + Postman

## 📦 Entregáveis
Código-fonte | README | Docs API | Postman | Diagrama | Sistema funcional

## 🧠 Conclusão
Projeto aplica arquitetura em camadas, integração com APIs e padrões de projeto, resultando em uma API organizada, escalável e próxima do mercado real.


