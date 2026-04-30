# 📊 Sistema de Gestão de Ações

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

## 📌 Sobre o Projeto

Este projeto consiste em uma **API REST para gestão de investimentos**, desenvolvida com **Java + Spring Boot**.

A aplicação permite o cadastro e consulta de:

- 📈 Ações financeiras  
- 🏦 Corretoras  

Diferente de sistemas CRUD tradicionais, o usuário **não insere todos os dados manualmente**.  
Ele fornece apenas informações básicas (como CNPJ ou ticker), e o sistema:

✔ Consulta APIs externas  
✔ Valida os dados automaticamente  
✔ Converte para o padrão interno  
✔ Persiste no banco de dados  

---

## 🎯 Objetivos

- Implementar uma API REST robusta  
- Aplicar arquitetura em camadas  
- Consumir APIs externas  
- Garantir consistência dos dados  
- Evitar duplicidades  
- Utilizar padrões de projeto  
- Manter código organizado e escalável  

---

## 🏗️ Arquitetura

O projeto segue o padrão **Layered Architecture**:


Controller → Service → Repository → Banco de Dados


### 📦 Camadas

- **Controller (Resource)** → Endpoints da API  
- **Service** → Regras de negócio  
- **Repository** → Persistência (Spring Data JPA)  
- **Domain** → Entidades e DTOs  

### 🔧 Componentes adicionais

- **DTOs** → Segurança na troca de dados  
- **Mappers** → Conversão entre DTO e Entity  
- **Ports** → Contratos de integração  
- **Integrations** → Consumo de APIs externas  

---

## 📁 Estrutura do Projeto


domains/
services/
repositories/
resources/
integrations/
ports/
mappers/


---

## 🔄 Fluxo do Sistema

### Cadastro de Corretora


CNPJ → BrasilAPI → Adapter → ViaCEP → Adapter → Validação → Banco


### Cadastro de Ação


Ticker → Identificação → Strategy → API → Adapter → Banco


### Fluxo Geral


Request → Controller → Service → API → Adapter → Repository → Response


---

## 🌐 APIs Utilizadas

### 📍 ViaCEP
- Consulta de endereço por CEP  
- Gratuita e sem autenticação  

### 🏢 BrasilAPI
- Consulta de empresas por CNPJ  
- Retorna dados como:
  - Razão social  
  - Nome fantasia  
  - CNAE  

### 📊 Brapi
- Ações brasileiras  
- Retorna cotação e dados da empresa  

### 🌎 TwelveData
- Ações internacionais  
- Retorna preço e horário  

### 🧠 Estratégia

- 🇧🇷 Ações brasileiras → Brapi  
- 🌍 Ações internacionais → TwelveData  

---

## 🧩 Padrões de Projeto

### Strategy Pattern
Escolha dinâmica da API de cotação:

- `BrapiStrategy`
- `TwelveDataStrategy`

✔ Evita `if/else`  
✔ Facilita manutenção  

---

### Adapter Pattern
Adaptação de dados externos:

- `BrasilApiAdapter`
- `ViaCepAdapter`

✔ Isola mudanças externas  
✔ Protege o sistema  

---

### Outros padrões

- Repository  
- DTO  
- Injeção de dependência  

---

## 📜 Regras de Negócio

- ✔ Validação de corretoras via CNAE  
- ✔ Bloqueio de dados fictícios  
- ✔ Controle de duplicidade:
- ✔ CNPJ único  
- ✔ Ticker único  

---

## ⚠️ Tratamento de Erros

Implementado com `@ControllerAdvice`:

| Erro | Código |
|------|--------|
| Validação | 422 |
| Duplicidade | 400 |
| API indisponível | 503 |
| Rate limit | 429 |

---

## 🗄️ Banco de Dados

- 🧪 Desenvolvimento → H2  
- 🚀 Produção → PostgreSQL / MySQL  

---

## 🧱 Entidades

### 🏦 Corretora

- id  
- cnpj  
- razaoSocial  
- nomeFantasia  
- cep  
- logradouro  
- bairro  
- cidade  
- uf  
- situacaoCadastral  
- validadaNaCvm  

---

### 📈 Ação

- id  
- ticker  
- nomeEmpresa  
- mercado  
- moeda  
- cotacaoAtual  
- dataHoraCotacao  

---

## ⚙️ Decisão Arquitetural

As ações **não são vinculadas diretamente às corretoras**.

### ✔ Motivos:

- Melhor performance  
- Menor acoplamento  
- Evita consultas complexas  
- Previne problemas de serialização  

---

## 🚀 Diferenciais

- ⚡ Cache com `@Cacheable` (ViaCEP)  
- 📜 Logs estruturados (SLF4J)  
- 📚 Documentação:
  - Swagger (OpenAPI 3)  
  - Postman  

---

## 📦 Entregáveis

- Código-fonte completo  
- README documentado  
- Documentação das APIs  
- Collection do Postman  
- Diagrama de entidades  
- Sistema funcional  

---

## 🧠 Conclusão

Este projeto demonstra, na prática:

- Aplicação de arquitetura em camadas  
- Integração com APIs externas  
- Uso de padrões de projeto  
- Boas práticas de desenvolvimento backend  

O resultado é uma API **organizada, escalável e próxima de sistemas reais do mercado financeiro**.

---
