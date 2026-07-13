# IsKahoot - Jogo Concorrente e Distribuído 🎮

Este projeto consiste numa versão adaptada, concorrente e distribuída do popular jogo *Kahoot!*, desenvolvida em **Java** no âmbito da unidade curricular de **Programação Concorrente e Distribuída (PCD)** no **ISCTE-IUL**.

A aplicação permite a gestão e simulação de múltiplos jogos em simultâneo através de uma arquitetura cliente-servidor, suportando modos de jogo individuais e em equipa com comunicação em tempo real via Sockets.

---

## 🛠️ Funcionalidades e Arquitetura Técnica

Para cumprir os requisitos de concorrência e rede sem utilizar os utilitários padrão do Java, foram desenvolvidos mecanismos próprios de coordenação[cite: 3]:

*   **Arquitetura Multi-Jogo (Servidor):** Cada jogador ligado é gerido por uma thread dedicada (`DealWithClient`), permitindo múltiplos jogos autónomos em simultâneo através de uma interface de texto (TUI)[cite: 3].
*   **Gestão de Sessão (`GameHandler`):** Uma thread dedicada controla o ciclo de jogo, cronómetros e o estado global (`GameState`)[cite: 3].
*   **Rondas Individuais (`Modified CountDownLatch`):** Implementação manual de um *latch* modificado com temporizador para gerir cotações duplicadas aos primeiros jogadores a responder[cite: 3].
*   **Rondas de Equipa (Barreira com Variáveis de Condição):** Sincronização ad-hoc baseada em exclusão mútua e variáveis de condição para validar respostas apenas quando todos os membros da equipa submetem[cite: 3].
*   **Interface Gráfica (GUI):** Interface do utilizador do lado do cliente para visualização de perguntas, cronómetros e placares em tempo real[cite: 3].
*   **Persistência de Dados:** Parse dinâmico de perguntas armazenadas em ficheiros de texto em formato JSON recorrendo à biblioteca **Gson** da Google[cite: 3].

---

## 🚀 Como Executar

O projeto utiliza Maven para a compilação. Siga os passos abaixo para iniciar o Servidor e os Clientes através do terminal:

### 1. Iniciar o Servidor

O servidor gerará os códigos dos jogos através da sua TUI[cite: 3]. Abra um terminal e execute:

java -jar target/server.jar

### 2. Iniciar os Clientes (Jogadores)
Abra um novo terminal para cada jogador. Os argumentos obrigatórios de inicialização são o IP, Porto, Código do Jogo, Nome da Equipa e Nome do Jogador[cite: 3]:

java -jar target/client.jar localhost 12345 [CÓDIGO_DO_JOGO] [NOME_DA_EQUIPA] [NOME_DO_JOGADOR]

###3. Repetir para os restantes jogadores
Abra novos terminais e repita o passo 2 para cada jogador necessário até preencher o número configurado para o jogo[cite: 3].

3. Repetir para os restantes jogadores
Abra novos terminais e repita o passo 2 para cada jogador necessário até preencher o número configurado para o jogo[cite: 3].
