# Vinheria Agnello | Infraestrutura, DevOps e Comunicação Segura

Projeto acadêmico da FIAP que simula parte da arquitetura de microsserviços da **Vinheria Agnello**, utilizando duas APIs Java independentes, Docker Compose, DNS interno, autenticação por API Key e comunicação HTTPS/TLS. Inclui um `Jenkinsfile` para **build e publicação simulada em agente Windows** e evidências de tráfego TLS capturadas com Wireshark.

> **Estado em 24/11/2026:** Produtos, Carrinho, Docker Compose, DNS, integração REST, API Key, HTTPS/TLS, validação de certificado, bloqueio da porta HTTP interna de Produtos e captura TLS no Wireshark **implementados e testados localmente**. O `Jenkinsfile` Windows foi desenvolvido, mas **sua execução bem-sucedida ainda precisa ser comprovada pela equipe**. O relatório final em PDF permanece pendente.
>
> **Escopo:** implementação didática de **dois** microsserviços. Não representa a migração integral da aplicação anterior da Vinheria, nem uma arquitetura pronta para produção.

## 1. Arquitetura

```text
                      Computador do desenvolvedor
                                |
                     http://localhost:8081
                                |
                                v
                    +-----------------------+
                    |    carrinho-service   |
                    | Java 21 + Tomcat 10.1 |
                    | HTTP interno: 8080    |
                    | HttpClient + truststore|
                    +-----------+-----------+
                                |
                   HTTPS/TLS + X-API-Key
              https://produtos-service:8443
                                |
                                v
                    +-----------------------+
                    |    produtos-service   |
                    | Java 21 + Tomcat 10.1 |
                    | HTTPS interno: 8443   |
                    | Keystore + API Key    |
                    +-----------------------+
                                |
                Rede Docker vinheria-network (bridge)
                     DNS interno por nome de serviço
```

| Serviço | Acesso a partir do computador | Acesso entre contêineres | Responsabilidade |
| --- | --- | --- | --- |
| Carrinho | `http://localhost:8081` | `http://carrinho-service:8080` | Gerenciar carrinho em memória e consultar Produtos |
| Produtos | `https://localhost:8443` | `https://produtos-service:8443` | Consultar catálogo de vinhos, validando `X-API-Key` |

O Carrinho usa a variável `PRODUTOS_URL` para localizar Produtos. O DNS interno do Docker resolve o nome `produtos-service`, sem necessidade de IP fixo. A consulta entre os serviços utiliza **HTTPS com validação do certificado** e uma **API Key compartilhada**.

**Limite de proteção:** o tráfego **Carrinho → Produtos** está autenticado e criptografado. A entrada **cliente → Carrinho ainda usa HTTP**, sem autenticação própria. A API de Produtos tem somente o conector HTTPS ativo; seu antigo HTTP interno na porta 8080 foi desabilitado.

## 2. Tecnologias

- **Java 21**, **Maven 3.9.x**, **Jakarta Servlet 6.0** e **Gson**.
- **Apache Tomcat 10.1** para servir os WARs.
- **Docker / Docker Compose v2**, rede `bridge` e DNS interno.
- **API Key** via cabeçalho `X-API-Key` e variável de ambiente.
- **HTTPS/TLS** no Tomcat, certificado local autoassinado PKCS#12, keystore e truststore Java.
- **OpenSSL / keytool** para geração e verificação de credenciais locais.
- **tcpdump + Wireshark** para captura e inspeção do tráfego TLS.
- **Git/GitHub** e **Jenkins** para CI/CD com publicação simulada em agente Windows.

Produtos e Carrinho utilizam **dados em memória**. Não é necessário instalar um banco de dados.

## 3. Estrutura do repositório

```text
vinheria-agnello-devops/
├── produtos-service/
│   ├── src/main/java/com/vinheria/produtos/
│   │   ├── controller/ProdutoServlet.java
│   │   ├── model/Vinho.java
│   │   └── service/ProdutoService.java
│   ├── config/server.xml
│   ├── pom.xml
│   └── Dockerfile
├── carrinho-service/
│   ├── src/main/java/com/vinheria/carrinho/
│   │   ├── controller/CarrinhoServlet.java
│   │   ├── model/ItemCarrinho.java
│   │   └── service/CarrinhoService.java
│   ├── pom.xml
│   └── Dockerfile
├── security/
│   └── certs/
│       └── .gitkeep
├── docs/
│   └── evidencias/
│       └── seguranca/                # prints e capturas selecionados
├── docker-compose.yml
├── Jenkinsfile                     # versão Windows da equipe
├── .gitignore
└── README.md
```

As pastas `target/` são geradas pelo Maven. O arquivo `.env`, as chaves, os keystores e os truststores são **locais** e **não devem ser versionados**. O conteúdo de `docs/evidencias/` pode variar conforme os arquivos efetivamente adicionados pela equipe.

## 4. Pré-requisitos

| Ferramenta | Versão / observação |
| --- | --- |
| Java JDK | **21**, com `keytool` |
| Maven | **3.9.x** |
| Git | Versão recente |
| Docker Desktop / Docker Engine | Docker em execução; contêineres **Linux** |
| Docker Compose | Plugin **v2** (`docker compose`) |
| OpenSSL | Opcional para gerar senhas; veja alternativas abaixo |
| Wireshark | Opcional para reproduzir a análise de pacotes |
| Jenkins | Necessário **somente** para testar a pipeline Windows, não para executar as APIs |

Verifique no terminal (macOS/Linux ou PowerShell):

```bash
java -version
mvn -version
git --version
docker --version
docker compose version
docker info
keytool -help
```

No Windows, abra Docker Desktop em modo de contêineres Linux. Se `docker info` falhar, inicie o Docker antes de continuar.

## 5. Instalação completa em uma nova máquina

> **Ordem obrigatória:** clonar → criar `.env` → gerar certificado e truststore → compilar WARs → iniciar Docker Compose → testar. Os certificados e o `.env` são propositalmente ausentes do GitHub.

### 5.1. Clonar o repositório

```bash
git clone https://github.com/Sterchaves/vinheria-agnello-devops.git
cd vinheria-agnello-devops
```

As instruções a seguir assumem que você já está **na raiz do repositório**.

### 5.2. Criar as credenciais locais

Crie um arquivo `.env` na raiz com **duas chaves aleatórias distintas**:

```dotenv
PRODUTOS_API_KEY=SUBSTITUA_POR_UMA_CHAVE_ALEATORIA
PRODUTOS_TLS_PASSWORD=SUBSTITUA_POR_OUTRA_CHAVE_ALEATORIA
```

Para gerar cada valor de forma segura no macOS/Linux, execute `openssl rand -hex 32` **duas vezes**. No Windows PowerShell, caso não tenha OpenSSL, gere cada valor com:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
```

Execute a geração novamente para obter o segundo valor. **Nunca comite o `.env`, compartilhe as chaves em prints ou utilize as strings de exemplo.**

Confirme que o `.gitignore` inclui pelo menos:

```gitignore
**/target/
.env
.env.*
!.env.example
*.key
*.pem
*.p12
*.pfx
*.jks
*.keystore
*.csr
security/certs/*
!security/certs/.gitkeep
```

### 5.3. Carregar as variáveis no terminal

**macOS/Linux (bash/zsh)**, se o `.env` contiver somente as duas linhas simples do exemplo:

```bash
set -a
source .env
set +a
```

**Windows PowerShell**, para esse formato simples de `.env`:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

> Faça isso apenas com um `.env` criado por você e confiável. O Docker Compose lê `.env` automaticamente, mas o `keytool` precisa das variáveis carregadas na sessão de terminal para os comandos seguintes.

### 5.4. Gerar o keystore HTTPS de Produtos

Crie a pasta, se ainda não existir:

```bash
mkdir -p security/certs
```

**macOS/Linux:**

```bash
keytool -genkeypair \
  -alias produtos-service \
  -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
  -validity 365 -storetype PKCS12 \
  -keystore security/certs/produtos-keystore.p12 \
  -storepass "$PRODUTOS_TLS_PASSWORD" \
  -keypass "$PRODUTOS_TLS_PASSWORD" \
  -dname "CN=produtos-service, OU=DevOps, O=Vinheria Agnello, C=BR" \
  -ext "SAN=dns:produtos-service,dns:localhost"
```

**Windows PowerShell** (mesma configuração em uma linha):

```powershell
keytool -genkeypair -alias produtos-service -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 365 -storetype PKCS12 -keystore security/certs/produtos-keystore.p12 -storepass $env:PRODUTOS_TLS_PASSWORD -keypass $env:PRODUTOS_TLS_PASSWORD -dname "CN=produtos-service, OU=DevOps, O=Vinheria Agnello, C=BR" -ext "SAN=dns:produtos-service,dns:localhost"
```

O certificado local é **autoassinado** e válido por 365 dias desde sua criação. O SAN inclui `produtos-service` para o Carrinho e `localhost` para os testes feitos no próprio computador.

### 5.5. Exportar o certificado público e criar o truststore

**macOS/Linux:**

```bash
keytool -exportcert \
  -alias produtos-service -storetype PKCS12 \
  -keystore security/certs/produtos-keystore.p12 \
  -storepass "$PRODUTOS_TLS_PASSWORD" \
  -rfc -file security/certs/produtos-cert.pem

keytool -importcert \
  -alias produtos-service \
  -file security/certs/produtos-cert.pem \
  -keystore security/certs/carrinho-truststore.p12 \
  -storetype PKCS12 -storepass changeit -noprompt
```

**Windows PowerShell:**

```powershell
keytool -exportcert -alias produtos-service -storetype PKCS12 -keystore security/certs/produtos-keystore.p12 -storepass $env:PRODUTOS_TLS_PASSWORD -rfc -file security/certs/produtos-cert.pem
keytool -importcert -alias produtos-service -file security/certs/produtos-cert.pem -keystore security/certs/carrinho-truststore.p12 -storetype PKCS12 -storepass changeit -noprompt
```

O **keystore** contém o certificado e a **chave privada** de Produtos. O **truststore** contém apenas o certificado público confiável para o Carrinho. A senha `changeit` aplica-se somente ao truststore de certificado público nesta simulação; a senha privada do keystore é a gerada no `.env`.

Verifique os arquivos e os tipos de entrada:

**macOS/Linux:**

```bash
keytool -list -storetype PKCS12 -keystore security/certs/produtos-keystore.p12 -storepass "$PRODUTOS_TLS_PASSWORD"
keytool -list -storetype PKCS12 -keystore security/certs/carrinho-truststore.p12 -storepass changeit
```

**Windows PowerShell:**

```powershell
keytool -list -storetype PKCS12 -keystore security/certs/produtos-keystore.p12 -storepass $env:PRODUTOS_TLS_PASSWORD
keytool -list -storetype PKCS12 -keystore security/certs/carrinho-truststore.p12 -storepass changeit
```

Esperado: `PrivateKeyEntry` no keystore de Produtos e `trustedCertEntry` no truststore do Carrinho.

> **Atenção:** não recrie um desses arquivos isoladamente mantendo o outro antigo. Se gerar outro certificado, exporte-o novamente e recrie o truststore correspondente. O arquivo `.p12` é montado no Docker como volume, não incluído na imagem.

### 5.6. Compilar os dois microsserviços

Os Dockerfiles copiam WARs previamente produzidos em `target/`. **`docker compose up --build` não compila o Java automaticamente**.

**macOS/Linux:**

```bash
(cd produtos-service && mvn clean package)
(cd carrinho-service && mvn clean package)
```

**Windows PowerShell:**

```powershell
Push-Location produtos-service
mvn clean package
Pop-Location
Push-Location carrinho-service
mvn clean package
Pop-Location
```

Os dois builds devem terminar com `BUILD SUCCESS`. Os artefatos esperados são:

```text
produtos-service/target/produtos-service.war
carrinho-service/target/carrinho-service.war
```

### 5.7. Iniciar com Docker Compose

Na raiz do repositório:

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

A opção `--quiet` valida o Compose sem imprimir os valores das variáveis, que incluem credenciais. O resultado esperado de `docker compose ps` são **dois contêineres em execução**:

| Contêiner | Porta publicada | Finalidade |
| --- | --- | --- |
| `vinheria-produtos` | `8443:8443` | API Produtos com HTTPS |
| `vinheria-carrinho` | `8081:8080` | API Carrinho com HTTP de entrada |

A rede Docker costuma se chamar `vinheria-agnello-devops_vinheria-network` e pode variar conforme o nome do projeto Compose.

Se existir um contêiner antigo criado manualmente com o mesmo nome, pare e remova **somente** esse contêiner, depois repita o Compose. A porta HTTP `8080` **não deve estar publicada** por Produtos.

## 6. Como a segurança foi implementada

### 6.1. API Key entre serviços

O `ProdutoServlet` exige o cabeçalho `X-API-Key` antes de consultar o catálogo. O `CarrinhoService` lê `PRODUTOS_API_KEY` do ambiente e envia a chave nas requisições para Produtos.

- **Sem chave ou chave incorreta:** HTTP `401 Unauthorized`.
- **Chave válida:** o endpoint de Produtos responde normalmente, por exemplo HTTP `200`.
- **Variável não configurada no servidor:** HTTP `503` na verificação da autenticação.

O Compose injeta `PRODUTOS_API_KEY` em ambos os contêineres. A chave não aparece no código-fonte. **API Key é autenticação simples de serviço, não autenticação de usuário nem substituto para HTTPS.**

### 6.2. HTTPS/TLS e certificados

O arquivo `produtos-service/config/server.xml` configura o Tomcat para HTTPS na porta `8443`, usando `security/certs/produtos-keystore.p12`, montado como volume somente de leitura. A senha vem da variável `PRODUTOS_TLS_PASSWORD`. O Dockerfile de Produtos utiliza o `server.xml` personalizado e habilita a leitura dessa variável por `EnvironmentPropertySource`.

O serviço Carrinho usa:

```text
PRODUTOS_URL=https://produtos-service:8443
```

A JVM do Carrinho é configurada via `JAVA_TOOL_OPTIONS` para utilizar `security/certs/carrinho-truststore.p12`, montado em `/usr/local/tomcat/conf/certs/`. Assim, o cliente HTTP Java valida o certificado e o nome DNS de Produtos. **Não foi desabilitada a verificação TLS no Java.**

O conector HTTP `8080` de Produtos foi removido do `server.xml`, e o Compose publica somente `8443`. O `EXPOSE 8443` documenta a porta da imagem, mas a efetiva desativação de HTTP ocorre pela ausência do conector HTTP no Tomcat.

### 6.3. Limites do modelo de segurança

- O acesso externo ao Carrinho ainda é **HTTP** (`localhost:8081`): não há TLS de ponta a ponta.
- Produtos usa certificado **autoassinado local**. Cada máquina gera seus próprios certificados.
- Uma única API Key compartilhada não fornece identidade individual por usuário, rotação automática ou autorização granular.
- O `.env` e os arquivos de credenciais são apropriados para **simulação acadêmica local**, não para ambiente de produção. Variáveis de ambiente e argumentos de ferramentas não são um gerenciador de segredos.
- O projeto não implementa mTLS, gateway seguro, autenticação de usuários, rate limiting ou persistência de dados.

## 7. Endpoints e testes funcionais

### 7.1. Endpoints

| Método | Endpoint | Comportamento |
| --- | --- | --- |
| GET | `https://localhost:8443/produtos` | Lista vinhos, exige API Key |
| GET | `https://localhost:8443/produtos/{id}` | Consulta vinho, exige API Key; `404` se não encontrado |
| GET | `http://localhost:8081/carrinho` | Consulta itens, subtotal, frete e total |
| POST | `http://localhost:8081/carrinho/itens` | Adiciona item após consulta HTTPS autenticada a Produtos |
| DELETE | `http://localhost:8081/carrinho/itens/{id}` | Remove o vinho indicado |

Vinhos de exemplo: Barolo Riserva (ID 1, R$ 1.250,00), Chianti Classico (ID 2, R$ 320,00) e Chablis (ID 3, R$ 450,00). Quantidade por vinho: de 1 a 99 unidades. Frete simulado: **R$ 0,00** para subtotal de pelo menos R$ 500,00; caso contrário, **R$ 29,90**. Por simplificação da implementação, o carrinho vazio também pode exibir frete R$ 29,90.

### 7.2. Testes de autenticação e certificado

Os testes abaixo usam **macOS/Linux** com as variáveis da seção 5.3 carregadas. No PowerShell, utilize `curl.exe` com os argumentos adaptados para uma linha, ou `Invoke-WebRequest` com os cabeçalhos correspondentes.

**Sem API Key, espera HTTP 401:**

```bash
curl -i --cacert security/certs/produtos-cert.pem \
  https://localhost:8443/produtos/1
```

**API Key incorreta, espera HTTP 401:**

```bash
curl -i --cacert security/certs/produtos-cert.pem \
  https://localhost:8443/produtos/1 \
  -H 'X-API-Key: chave-incorreta-teste'
```

**API Key correta, espera HTTP 200 e JSON do Barolo Riserva:**

```bash
curl -i --cacert security/certs/produtos-cert.pem \
  https://localhost:8443/produtos/1 \
  -H "X-API-Key: $PRODUTOS_API_KEY"
```

> **Não use `-k` como teste de validação do certificado.** Essa opção ignora a verificação TLS e só foi utilizada provisoriamente no diagnóstico inicial. Não exponha a chave em prints ou logs detalhados (`curl -v`, `--trace`).

### 7.3. Testes de Carrinho e integração REST/HTTPS

**Consultar carrinho:**

```bash
curl -i http://localhost:8081/carrinho
```

**Adicionar duas unidades do Barolo:**

```bash
curl -i -X POST http://localhost:8081/carrinho/itens \
  -H 'Content-Type: application/json' \
  -d '{"vinhoId":1,"quantidade":2}'
```

Esperado: **HTTP 201**; o Carrinho consulta Produtos em `https://produtos-service:8443` com a API Key e o truststore. Em um carrinho recém-iniciado com duas unidades de Barolo, o subtotal e o total devem ser **R$ 2.500,00**, com frete **R$ 0,00**.

**Windows PowerShell, alternativa para o POST:**

```powershell
$body = @{ vinhoId = 1; quantidade = 2 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/carrinho/itens' -ContentType 'application/json' -Body $body
```

**Remover um vinho:**

```bash
curl -i -X DELETE http://localhost:8081/carrinho/itens/1
```

Os itens são mantidos em memória e **reiniciar/recriar o Carrinho pode apagá-los**. O carrinho é único na instância da simulação.

### 7.4. Testar DNS e bloqueio do HTTP interno

**DNS interno:**

```bash
docker exec vinheria-carrinho getent hosts produtos-service
```

Esperado: IP privado e nome `produtos-service`. O IP pode mudar entre execuções.

**HTTP interno antigo desabilitado:**

```bash
docker exec vinheria-carrinho \
  curl --max-time 5 -i http://produtos-service:8080/produtos/1
```

Esperado: **`Connection refused`** (por exemplo, erro `curl: (7)`), porque Produtos não escuta na porta HTTP `8080`. Esse erro é **sucesso no teste de bloqueio**. A imagem utilizada nos testes continha `curl`; se outra imagem não tiver, utilize um contêiner de diagnóstico na mesma rede.

**Observação sobre DHCP:** o Docker atribui endereços IP aos contêineres e oferece DNS de serviço na rede `bridge`. Isso **não comprova a configuração de um servidor DHCP tradicional**; caso a disciplina exija DHCP explícito, a simulação e suas evidências devem ser descritas separadamente.

## 8. Captura e análise com Wireshark

Foi utilizado um contêiner auxiliar com `tcpdump` para observar o tráfego na rede do contêiner Produtos. Essa técnica é útil no Docker Desktop para macOS, porque as redes dos contêineres ficam na VM do Docker.

### 8.1. Capturar tráfego TLS

Crie a pasta, se necessário:

```bash
mkdir -p docs/evidencias/seguranca/capturas
```

**macOS/Linux**, com o Docker em execução e os dois serviços iniciados:

```bash
docker run --rm -it \
  --name vinheria-captura \
  --network container:vinheria-produtos \
  --cap-add NET_RAW --cap-add NET_ADMIN \
  -v "$(pwd)/docs/evidencias/seguranca/capturas:/capturas" \
  nicolaka/netshoot \
  tcpdump -i any -nn -s 0 \
  -w /capturas/https-produtos.pcap 'tcp port 8443'
```

**Windows PowerShell**, equivalente:

```powershell
docker run --rm -it --name vinheria-captura --network container:vinheria-produtos --cap-add NET_RAW --cap-add NET_ADMIN -v "${PWD}/docs/evidencias/seguranca/capturas:/capturas" nicolaka/netshoot tcpdump -i any -nn -s 0 -w /capturas/https-produtos.pcap "tcp port 8443"
```

Com a captura em execução, **em outro terminal**, faça um POST no Carrinho conforme a seção 7.3 para gerar uma nova conexão HTTPS entre os serviços. Depois pressione **Ctrl+C** no terminal do `tcpdump` para finalizar e gravar o `.pcap`.

### 8.2. Analisar no Wireshark

Abra `docs/evidencias/seguranca/capturas/https-produtos.pcap` no Wireshark e aplique o filtro de exibição:

```text
tcp.port == 8443
```

Na captura de referência do projeto foram observados **17 pacotes, nenhum descartado**, incluindo:

| Registro | Evidência |
| --- | --- |
| `TCP SYN / SYN-ACK / ACK` | Estabelecimento da conexão |
| `Client Hello`, com `SNI=produtos-service` | Início da negociação TLS e nome de destino |
| `Server Hello` | Resposta TLS do servidor |
| `Application Data` | Dados transportados em registros TLS criptografados |

Ao selecionar um pacote `Application Data`, o Wireshark mostra **Encrypted Application Data**, sem revelar diretamente o conteúdo JSON ou a API Key. Isso documenta a presença de tráfego protegido por TLS; não prova, isoladamente, todas as demais propriedades de segurança da aplicação.

> O campo `Version: TLS 1.2 (0x0303)` no cabeçalho do registro pode ser um **valor legado** mesmo em conexões TLS 1.3; a versão efetivamente negociada deve ser conferida nos dados do handshake. Não é necessário descriptografar o tráfego para este exercício.

### 8.3. Evidências recomendadas

Organize em `docs/evidencias/seguranca/` os prints de: API Key ausente (401), inválida (401), válida (200), HTTPS com `--cacert` (200), Carrinho → Produtos (201), keystore/truststore, HTTP interno recusado e Wireshark com `Application Data` expandido. Remova ou oculte qualquer credencial antes de versionar. O arquivo `.pcap` é uma evidência opcional: revise seu conteúdo antes de publicá-lo, pois capturas podem conter metadados de rede.

## 9. Pipeline CI/CD com Jenkins (versão Windows da equipe)

O `Jenkinsfile` mantido no repositório foi elaborado para um **agente Windows**. Usa `bat` para compilar os dois serviços e `powershell` para copiar os WARs para uma pasta de publicação. **Não foi adaptado para macOS/Linux** e **não inicia os contêineres Docker**.

**Fluxo previsto:** GitHub → Jenkins → Maven Build → WARs → pasta de publicação → deploy simulado.

### 9.1. Requisitos do agente

- **Windows**, com agente conectado e online. O pipeline declara `agent any`; portanto, garanta que ele seja executado em um agente **Windows**, pois não há rótulo obrigatório no arquivo atual.
- **JDK 21**, **Maven 3.9.x** e **Git** disponíveis ao usuário do agente.
- Jenkins com plugins **Pipeline** e **Git**.
- Em **Manage Jenkins > Tools**, registrar os nomes exatos **`JDK 21`** e **`Maven 3.9`**, conforme o `Jenkinsfile`.
- Permissão de escrita em `C:\Jenkins\publicacao\vinheria`.

### 9.2. Criar e executar o job

1. Clique em **New Item**, escolha um nome (por exemplo, `vinheria-agnello-devops`) e selecione **Pipeline**.
2. Em **Pipeline**, escolha **Pipeline script from SCM** e selecione **Git**.
3. Configure:

| Campo | Valor |
| --- | --- |
| Repository URL | `https://github.com/Sterchaves/vinheria-agnello-devops.git` |
| Branches to build | `*/main` após o merge da branch de segurança; para testes anteriores, indique a branch correspondente |
| Script Path | `Jenkinsfile` |

4. Clique em **Save** e **Build Now**.
5. Abra **Console Output**, confirme os dois builds Maven e a etapa de publicação.

### 9.3. O que o Jenkinsfile faz

- Executa `mvn -B clean package` em `produtos-service` e `carrinho-service`.
- Limpa e prepara a pasta `C:\Jenkins\publicacao\vinheria`.
- Copia `produtos-service.war` e `carrinho-service.war` para subpastas separadas.
- Copia `docker-compose.yml` e os dois `Dockerfile` e lista os arquivos publicados.

**Importante:** a pasta de publicação **não contém os certificados locais, as variáveis `.env`, o `server.xml` de Produtos nem necessariamente toda a estrutura exigida pelos Dockerfiles**. Portanto, essa etapa é **publicação simulada**, não um pacote Docker autossuficiente nem deploy real. A execução em Jenkins **ainda precisa ser validada e documentada** com logs, resultado `SUCCESS` e prints dos artefatos.

## 10. Manutenção e solução de problemas

**Status e logs:**

```bash
docker compose ps
docker compose logs --tail=100 produtos-service carrinho-service
```

**Aplicar alterações de código Java:**

```bash
(cd produtos-service && mvn clean package)
(cd carrinho-service && mvn clean package)
docker compose up -d --build --force-recreate
```

No Windows PowerShell, compile cada serviço com `Push-Location` e `Pop-Location` conforme a seção 5.6, depois execute o Compose. A recriação limpa os dados em memória do Carrinho.

**Parar o projeto:**

```bash
docker compose down
```

| Problema | Verificação / correção |
| --- | --- |
| `mvn: command not found` | Instale Maven 3.9.x e confira o `PATH`. |
| Erro de Java | Verifique JDK 21 em `java -version` e `mvn -version`. |
| Falha `COPY target/...war` | Execute `mvn clean package` em ambos os serviços antes do build Docker. |
| Falta `PRODUTOS_API_KEY` ou `PRODUTOS_TLS_PASSWORD` | Crie o `.env` conforme a seção 5.2 e valide `docker compose config --quiet`. |
| Certificado ou truststore ausente | Gere os três arquivos da seção 5 antes de executar o Compose. |
| `keystore password was incorrect` | Confirme que a senha no `.env` é a mesma usada na criação do keystore. |
| `SSLHandshakeException` / `PKIX path building failed` | Confira truststore, certificado exportado, nome `produtos-service` no SAN e URL HTTPS. |
| Porta 8081/8443 ocupada | Libere a porta ou ajuste apenas o mapeamento do host no Compose. |
| `Connection refused` na porta interna 8080 de Produtos | **Esperado**: o HTTP do serviço Produtos está desabilitado. |
| HTTP 401 em Produtos | Inclua a API Key correta; não utilize `-k` para tentar resolver autenticação. |
| Carrinho falha ao adicionar um vinho | Verifique logs, `PRODUTOS_URL`, API Key, certificado, truststore e estado de Produtos. |
| `getent` não resolve `produtos-service` | Confira se os dois contêineres estão na rede Compose. |
| Pipeline Jenkins falha em `bat`/`powershell` | Execute o job em **agente Windows**, com as ferramentas cadastradas corretamente. |
| Arquivos alterados não aparecem nos contêineres | Recompile WARs e execute `docker compose up -d --build --force-recreate`. |

## 11. Colaboração e conclusão acadêmica

### Funcionalidades demonstradas

- [x] Dois microsserviços Java independentes e empacotamento WAR.
- [x] Contêineres Docker separados e rede `bridge` com DNS interno.
- [x] Integração REST Carrinho → Produtos.
- [x] Autenticação entre serviços com API Key e testes 401/200/201.
- [x] HTTPS/TLS em Produtos, validação do certificado e truststore no Carrinho.
- [x] Porta HTTP interna de Produtos desabilitada e teste de conexão recusada.
- [x] Captura de tráfego TLS com `tcpdump` e análise no Wireshark.
- [x] Jenkinsfile Windows para build e publicação simulada preparado.

### Pendências da equipe

- [ ] Validar a execução do **Jenkins** no agente Windows e anexar as evidências.
- [ ] Confirmar a apresentação do requisito **DHCP** de acordo com o enunciado da disciplina, sem confundir atribuição de IP no Docker com servidor DHCP tradicional.
- [ ] Selecionar, revisar e anexar os prints/logs necessários.
- [ ] Elaborar e entregar o **PDF final**, com arquitetura, infraestrutura, segurança, pipeline e reflexão da equipe.

### Recomendações de trabalho em equipe

1. Faça `git pull` antes de trabalhar, e use branches para alterações independentes.
2. Execute os testes da seção 7 antes de abrir um pull request.
3. Nunca suba `.env`, keystores, truststores, certificados privados ou dados sensíveis; confira `git status` antes do commit.
4. Os certificados são criados **em cada máquina**, conforme a seção 5. Não são transferidos via GitHub.
5. Depois do merge de `feature/seguranca` em `main`, revise a branch configurada no job Jenkins e atualize as evidências da equipe.

---

**Vinheria Agnello | FIAP | Simulação acadêmica de Infraestrutura, DevOps e Comunicação Segura.**
