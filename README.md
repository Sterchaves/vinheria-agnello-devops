# Vinheria Agnello | Infraestrutura e DevOps

Simulação acadêmica de uma arquitetura com **dois microsserviços Java**, executados em **contêineres Docker** e integrados por **API REST na rede interna Docker**. Projeto desenvolvido como continuidade da Vinheria Agnello, para a atividade de Infraestrutura, DevOps e Comunicação Segura da FIAP.

> **Estado atual (23/11/2026):** APIs de Produtos e Carrinho implementadas; build Maven, imagens Docker, execução com Docker Compose, resolução de nomes por DNS interno e integração REST testados. **Autenticação, HTTPS/TLS, pipeline Jenkins e demonstração com Wireshark ainda não estão implementados neste repositório**, conforme o progresso informado pela equipe. Os arquivos `Jenkinsfile` e `Dockerfile` já aparecem na estrutura, mas a existência de um arquivo não confirma a execução de seu conteúdo.

## 1. Arquitetura da simulação

```text
           Computador do desenvolvedor
                     |
       +-------------+-------------+
       |                           |
localhost:8081              localhost:8080
       |                           |
       v                           v
+------------------+     +------------------+
| carrinho-service |---->| produtos-service |
| Java + Tomcat    | GET | Java + Tomcat    |
| Porta interna    |     | Porta interna    |
| 8080             |     | 8080             |
+------------------+     +------------------+
       |                           |
       +------ vinheria-network ---+
              (rede bridge Docker,
               DNS interno por nome)
```

O Carrinho consulta o catálogo pelo endereço interno `http://produtos-service:8080`, configurado na variável de ambiente `PRODUTOS_URL`. O DNS integrado da rede Docker resolve `produtos-service` para o endereço IP da instância atual, sem exigir IP fixo no código.

Esta entrega **não transforma toda a aplicação web anterior em microsserviços**: implementa duas APIs independentes para demonstrar os requisitos da atividade.

### Serviços e portas

| Serviço | Endereço no computador | Endereço usado entre contêineres | Responsabilidade |
| --- | --- | --- | --- |
| Produtos | `http://localhost:8080` | `http://produtos-service:8080` | Expor e consultar vinhos do catálogo |
| Carrinho | `http://localhost:8081` | `http://carrinho-service:8080` | Manter itens em memória e consultar Produtos |

## 2. Tecnologias

- **Java 21**
- **Maven** para compilação e empacotamento (`.war`)
- **Jakarta Servlet 6.0** para endpoints HTTP
- **Gson** para serialização e leitura de JSON
- **Apache Tomcat 10.1** como servidor dentro dos contêineres
- **Docker + Docker Compose** para execução e rede dos serviços
- **Git/GitHub** para versionamento
- **Jenkins** previsto para a etapa de pipeline de build e deploy

Os serviços usam dados **em memória** nesta simulação; não é necessário instalar MySQL ou Oracle.

## 3. Estrutura de pastas

```text
vinheria-agnello-devops/
├── produtos-service/
│   ├── src/main/java/com/vinheria/produtos/
│   │   ├── controller/ProdutoServlet.java
│   │   ├── model/Vinho.java
│   │   └── service/ProdutoService.java
│   ├── pom.xml
│   └── Dockerfile
├── carrinho-service/
│   ├── src/main/java/com/vinheria/carrinho/
│   │   ├── controller/CarrinhoServlet.java
│   │   ├── model/ItemCarrinho.java
│   │   └── service/CarrinhoService.java
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml
├── Jenkinsfile
├── docs/
│   └── evidencias/               # se criada pela equipe
└── README.md
```

As pastas `target/` são **geradas pelo Maven** e não precisam ser versionadas no Git.

## 4. Pré-requisitos

Instalar e verificar:

| Ferramenta | Versão utilizada no desenvolvimento | Observação |
| --- | --- | --- |
| JDK | 21 | Necessário para compilar; os `pom.xml` usam Java 21 |
| Maven | 3.9.x | Necessário para gerar os dois WARs |
| Git | Qualquer versão recente | Necessário para clonar o repositório |
| Docker Desktop ou Docker Engine | Versão recente | O motor Docker deve estar iniciado |
| Docker Compose | Plugin v2 | Usar `docker compose`, sem hífen |

**macOS/Linux:**

```bash
java -version
mvn -version
git --version
docker --version
docker compose version
docker info
```

**Windows:** use os mesmos comandos no PowerShell, com Docker Desktop aberto (preferencialmente com contêineres Linux).

Se `docker info` não conseguir conectar ao daemon, inicie o Docker Desktop (ou o serviço Docker) antes de prosseguir.

## 5. Clonar e executar em uma máquina nova

### 5.1. Clonar o repositório

```bash
git clone https://github.com/Sterchaves/vinheria-agnello-devops.git
cd vinheria-agnello-devops
```


### 5.2. Compilar os dois serviços (obrigatório)

> **Atenção:** os `Dockerfile` atuais copiam arquivos `target/*.war` já gerados. Portanto, **o comando `docker compose up --build` sozinho não compila o Java**; é preciso rodar Maven antes, inclusive em um clone novo.

Execute a partir da raiz do repositório:

**macOS/Linux:**

```bash
(cd produtos-service && mvn clean package)
(cd carrinho-service && mvn clean package)
```

**Windows (PowerShell):**

```powershell
Push-Location produtos-service
mvn clean package
Pop-Location

Push-Location carrinho-service
mvn clean package
Pop-Location
```

Verifique se ambos mostram `BUILD SUCCESS` e se foram criados:

```text
produtos-service/target/produtos-service.war
carrinho-service/target/carrinho-service.war
```

### 5.3. Iniciar os contêineres

Na **raiz** do repositório:

```bash
docker compose up -d --build
docker compose ps
```

O resultado esperado são **dois serviços em execução**: `vinheria-produtos` e `vinheria-carrinho`.

A rede `vinheria-network` é declarada no Compose com driver `bridge`. O nome real da rede pode receber o prefixo do projeto Docker Compose.

**Conflito de porta ou nome:** se você já executou manualmente um contêiner chamado `vinheria-produtos`, remova **apenas o contêiner antigo**, se não fizer parte de outro trabalho:

```bash
docker stop vinheria-produtos
docker rm vinheria-produtos
```

Depois repita `docker compose up -d --build`. Se as portas 8080 ou 8081 já estiverem ocupadas por outra aplicação, libere-as ou altere o mapeamento do lado esquerdo em `docker-compose.yml` (e adapte os testes no computador).

## 6. Endpoints disponíveis

### Produtos

| Método | Endpoint | Resultado |
| --- | --- | --- |
| GET | `http://localhost:8080/produtos` | Lista os vinhos cadastrados em memória |
| GET | `http://localhost:8080/produtos/1` | Retorna o Barolo Riserva |
| GET | `http://localhost:8080/produtos/999` | Retorna `404` se não existir |

Dados atuais de exemplo:

| ID | Vinho | Preço |
| --- | --- | ---: |
| 1 | Barolo Riserva | R$ 1.250,00 |
| 2 | Chianti Classico | R$ 320,00 |
| 3 | Chablis | R$ 450,00 |

### Carrinho

| Método | Endpoint | Resultado |
| --- | --- | --- |
| GET | `http://localhost:8081/carrinho` | Itens, subtotal, frete e total |
| POST | `http://localhost:8081/carrinho/itens` | Adiciona um vinho após consultar Produtos |
| DELETE | `http://localhost:8081/carrinho/itens/1` | Remove o vinho de ID 1 |

O POST recebe JSON, por exemplo:

```json
{
  "vinhoId": 1,
  "quantidade": 2
}
```

A quantidade deve estar entre **1 e 99**. Caso o mesmo vinho seja adicionado novamente, a quantidade é somada até esse limite.

**Regra de frete simulada:** subtotal a partir de R$ 500,00 → frete R$ 0,00; abaixo disso → R$ 29,90. Na implementação atual, **um carrinho vazio também retorna frete R$ 29,90**: comportamento conhecido da simulação, ainda não ajustado.

## 7. Testes para reproduzir o resultado

Abra um terminal na raiz do projeto, após os contêineres iniciarem.

### Teste 1. Listar produtos

```bash
curl -i http://localhost:8080/produtos
```

Esperado: HTTP `200` e lista JSON com três vinhos.

### Teste 2. Consultar um produto

```bash
curl -i http://localhost:8080/produtos/1
```

Esperado: HTTP `200` e JSON com o vinho `Barolo Riserva`.

### Teste 3. Verificar carrinho inicial

```bash
curl -i http://localhost:8081/carrinho
```

Esperado: HTTP `200`, lista `itens` vazia e totais da regra atual.

### Teste 4. Adicionar vinho ao carrinho (integração REST)

```bash
curl -i -X POST http://localhost:8081/carrinho/itens \
  -H 'Content-Type: application/json' \
  -d '{"vinhoId":1,"quantidade":2}'
```

Esperado: HTTP `201` e mensagem de inclusão. O Carrinho consulta a API de Produtos antes de adicionar o item.

**Windows (PowerShell), alternativa ao comando acima:**

```powershell
$body = @{ vinhoId = 1; quantidade = 2 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/carrinho/itens' -ContentType 'application/json' -Body $body
```

### Teste 5. Conferir subtotal e frete

```bash
curl -i http://localhost:8081/carrinho
```

Esperado para **duas** unidades de Barolo em um carrinho recém-iniciado:

```json
{
  "subtotal": 2500.00,
  "total": 2500.00,
  "itens": [
    {"vinhoId": 1, "nome": "Barolo Riserva", "preco": 1250.00, "quantidade": 2}
  ],
  "frete": 0
}
```

A ordem das propriedades JSON pode variar. O campo `subtotal` de cada item é calculado pelo modelo Java, mas **não é incluído automaticamente no JSON** porque não é um atributo armazenado.

### Teste 6. Remover item

```bash
curl -i -X DELETE http://localhost:8081/carrinho/itens/1
curl -i http://localhost:8081/carrinho
```

Esperado: mensagem de remoção e carrinho sem o item.

### Teste 7. DNS interno da rede Docker

```bash
docker exec vinheria-carrinho getent hosts produtos-service
```

Esperado: um IP privado seguido do nome `produtos-service`, por exemplo:

```text
172.18.0.2    produtos-service
```

O endereço é **ilustrativo** e pode mudar entre computadores ou recriações dos contêineres.

### Teste 8. HTTP entre os próprios contêineres

```bash
docker exec vinheria-carrinho \
  curl -i http://produtos-service:8080/produtos/1
```

Esperado: HTTP `200` e dados JSON do Barolo. Se a imagem do contêiner não tiver `curl`, use o Teste 4 como demonstração da integração real e o Teste 7 para DNS, ou escolha um contêiner temporário de diagnóstico conectado à mesma rede.

> **PowerShell:** os comandos Docker com continuação `\` devem ser digitados em **uma única linha** ou adaptados à sintaxe do PowerShell. Exemplo: `docker exec vinheria-carrinho getent hosts produtos-service`.

## 8. Como o DNS funciona neste projeto

- O Docker Compose cria uma rede `bridge` definida pelo usuário.
- O Docker oferece resolução DNS interna para os nomes dos serviços nessa rede.
- O Carrinho acessa Produtos pelo nome `produtos-service`, em vez de usar um IP fixo.
- A rede oferece isolamento de endereçamento entre serviços; **não garante criptografia HTTP**.
- O Docker gerencia os endereços IP dos contêineres. **Isso não equivale à implementação de um servidor DHCP tradicional**. Se a disciplina exigir uma simulação específica de DHCP, ela deverá ser documentada separadamente.

## 9. Comandos úteis para manutenção

**Ver estado dos serviços:**

```bash
docker compose ps
```

**Acompanhar logs:**

```bash
docker compose logs -f produtos-service carrinho-service
```

**Recompilar após alterar Java:**

```bash
(cd produtos-service && mvn clean package)
(cd carrinho-service && mvn clean package)
docker compose up -d --build
```

**Parar e remover contêineres do projeto:**

```bash
docker compose down
```

**Reiniciar sem reconstruir as imagens:**

```bash
docker compose up -d
```

**Inspecionar a rede utilizada:**

```bash
docker network ls
docker inspect vinheria-carrinho
```

## 10. Limitações atuais e tarefas pendentes

### Já demonstrado

- [x] Dois serviços Java independentes, cada um empacotado em WAR.
- [x] Contêineres separados para Produtos e Carrinho.
- [x] Execução conjunta via Docker Compose.
- [x] Comunicação REST entre Carrinho e Produtos.
- [x] Resolução de nomes por DNS interno Docker.
- [x] Testes de inclusão no carrinho e cálculo de valores.

### Próximas atividades da equipe

- [ ] Implementar **autenticação** entre serviços e demonstrar requisições aceitas/recusadas.
- [ ] Demonstrar **criptografia HTTPS/TLS** ou uma simulação controlada que contraste tráfego HTTP e protegido; **token não criptografa transporte**.
- [ ] Construir e executar o **pipeline Jenkins** com build e deploy simulados ou reais, conforme enunciado.
- [ ] Registrar **prints e logs** das configurações, testes, pipeline e evidências de segurança.
- [ ] Explicar o requisito de **DHCP/DNS**, explicitando o que foi simulado pela rede Docker e o que não foi.
- [ ] Preparar o **PDF final** com infraestrutura, fluxo de deploy, práticas de segurança e reflexão da equipe.

### Simplificações deliberadas

- O catálogo e o carrinho utilizam **memória local**, sem banco de dados.
- O carrinho é **único e compartilhado na instância** para fins didáticos; não há segregação por usuário.
- Reiniciar ou recriar o contêiner pode apagar itens do carrinho.
- Os serviços estão acessíveis por **HTTP**, sem TLS ou validação de token na versão atual.
- O pipeline Jenkins ainda não foi validado; a presença do `Jenkinsfile` não comprova execução.
- As APIs foram desenhadas para **simulação acadêmica**, não para uso comercial ou exposição pública.

## 11. Orientações para colaboração

1. Fazer `git pull` antes de iniciar alterações.
2. Criar uma branch para cada atividade, quando possível, por exemplo `feature/seguranca` ou `feature/jenkins`.
3. **Não versionar** `target/`, credenciais, tokens reais ou arquivos de ambiente contendo segredos.
4. Ao mudar código Java, executar os builds Maven antes de recriar os contêineres.
5. Atualizar este README quando novos endpoints, variáveis de ambiente, controles de segurança ou passos de Jenkins forem implementados.
6. Incluir as evidências relevantes em `docs/evidencias/` e revisar em equipe antes da entrega.

## 12. Solução de problemas

| Problema | Verificação / solução |
| --- | --- |
| `mvn: command not found` | Instalar Maven e verificar `mvn -version` |
| Erro de versão do Java | Usar JDK 21 e conferir `java -version` e `mvn -version` |
| Docker não conecta ao daemon | Iniciar Docker Desktop/Engine; testar `docker info` |
| Erro `COPY target/*.war` no build Docker | Executar `mvn clean package` dentro de **cada** serviço |
| Porta 8080/8081 ocupada | Parar outro processo/contêiner ou alterar o mapeamento de porta |
| Nome do contêiner já está em uso | Remover apenas o contêiner antigo em conflito; não apagar dados de outros projetos |
| Carrinho retorna `503` ao adicionar | Verificar `docker compose ps`, logs do serviço Produtos e variável `PRODUTOS_URL` |
| `getent` não resolve `produtos-service` | Verificar se ambos os contêineres participam da mesma rede Compose |
| Alteração Java não aparece | Recompilar Maven e executar `docker compose up -d --build` |
| Erros no VS Code, mas `BUILD SUCCESS` | Recarregar a janela ou reimportar os projetos Java/Maven |

---

**Vinheria Agnello | FIAP | Projeto acadêmico de infraestrutura e DevOps.**



# Pipeline CI/CD com Jenkins

O projeto utiliza Jenkins para automatizar o processo de build e publicação simulada dos microsserviços da Vinheria Agnello.

A pipeline obtém o código-fonte do GitHub, compila os dois microsserviços utilizando Maven e copia os arquivos WAR gerados para uma pasta de publicação.

**Fluxo da pipeline:**

GitHub → Jenkins → Maven Build → Arquivos WAR → Publicação simulada

## Pré-requisitos

A pipeline foi desenvolvida para execução em um **agente Windows**, com as seguintes ferramentas:

- Java JDK 21;
- Apache Maven 3.9.x;
- Git;
- Jenkins com os plugins Pipeline e Git.

O Maven e o Git devem estar acessíveis ao usuário responsável pela execução do agente Jenkins.

## Configuração do Jenkins

No Jenkins, acesse **Manage Jenkins > Tools**.

Cadastre as seguintes ferramentas:

- **JDK:** `JDK 21`
- **Maven:** `Maven 3.9`

Os nomes devem corresponder aos definidos no `Jenkinsfile`.

O agente Windows deve estar conectado e online, utilizando o rótulo configurado no pipeline.

## Criar o job Pipeline

1. No Jenkins, clique em **New Item**.
2. Informe o nome `vinheria-agnello-devops` e selecione **Pipeline**.
3. Na seção **Pipeline**, selecione **Pipeline script from SCM**.
4. Em **SCM**, selecione **Git**.
5. Preencha os campos:

| Campo | Valor |
|---|---|
| Repository URL | `https://github.com/Sterchaves/vinheria-agnello-devops.git` |
| Branches to build | `*/main` |
| Script Path | `Jenkinsfile` |

6. Clique em **Save**.
7. Selecione **Build Now** para executar a pipeline.

## Etapas da pipeline

**1. Obtenção do código-fonte**

O Jenkins obtém o código do repositório GitHub configurado.

**2. Build dos microsserviços**

O Maven compila e empacota os dois microsserviços:

- `produtos-service`
- `carrinho-service`

Os arquivos WAR são gerados nas respectivas pastas `target/`.

**3. Preparação da publicação**

O Jenkins organiza os arquivos WAR em uma pasta de publicação no ambiente Windows.

**4. Deploy simulado**

Os artefatos gerados são copiados para a pasta de publicação, simulando a disponibilização das aplicações em um ambiente de produção.

## Validação

Para verificar a execução:

1. Acesse o histórico de builds do job.
2. Abra **Console Output** para consultar os logs.
3. Confirme que os dois microsserviços foram compilados com sucesso.
4. Verifique a geração e a cópia dos arquivos WAR para a pasta de publicação.
5. Confirme que a pipeline foi concluída com o status `SUCCESS`.

## Observações

A pipeline utiliza comandos `bat` e `powershell`, sendo destinada à execução em um agente Windows.

O processo automatiza a compilação e a publicação simulada dos artefatos, mas não realiza o deploy automático nos contêineres Docker.

A execução dos microsserviços com Docker Compose é documentada separadamente neste README.

A publicação simulada foi adotada para atender aos objetivos acadêmicos da atividade de DevOps da FIAP.