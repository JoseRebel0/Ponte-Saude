# Ponte-Saúde (ULSBE)

Aplicação móvel Android desenvolvida em Kotlin e Jetpack Compose para apoiar utentes do Serviço Nacional de Saúde (SNS) e os seus cuidadores na gestão diária de saúde, medicação e consultas médicas.

> Nota: Este projeto encontra-se atualmente em fase de desenvolvimento (Work in Progress).

---

## Objetivo

Criar uma interface simples, acessível e intuitiva que sirva de ligação entre o utente, os seus cuidadores e os serviços de saúde, facilitando a adesão aos tratamentos e a literacia em saúde.

---

## Principais Funcionalidades

- Início (Resumo Diário):
  - Vista rápida das próximas tomas de medicação e consultas agendadas.
  - Indicadores de tarefas do dia e estado geral de saúde.

- Medicação:
  - Registo de medicamentos, doses, horários e instruções de toma.
  - Associação de fotografias aos medicamentos para identificação rápida.
  - Alarmes e notificações de lembrete no dispositivo.

- Consultas e Exames:
  - Agenda de consultas médicas e exames complementares.
  - Lista de verificação para preparação prévia (ex.: jejum, exames a levar).
  - Sincronização direta com a aplicação de Calendário do Android.

- Notas e Tradutor Médico (IA):
  - Registo de sintomas, dúvidas e apontamentos para colocar ao médico na consulta.
  - Suporte a ditado por voz.
  - Explicação e simplificação de termos clínicos através de Inteligência Artificial.

- Literacia em Saúde e Segurança:
  - Módulos informativos e vídeos sobre saúde preventiva e segurança do doente.
  - Questionários interativos com sistema de pontos e incentivos.

- Modo Cuidador:
  - Alternância rápida entre a conta do próprio utilizador e os perfis de familiares ou dependentes ao seu cuidado.

---

## Arquitetura do Projeto

O projeto segue o padrão **MVVM (Model-View-ViewModel)** com uma abordagem **Offline-First**, garantindo que a aplicação permanece totalmente funcional mesmo sem ligação à internet:

1. **Camada de Apresentação (View):** Construída com Jetpack Compose. Observa fluxos de estado imutáveis (`StateFlow`) expostos pelos ViewModels e reage a eventos do utilizador.
2. **Camada de Gestão de Estado (ViewModel):** Responsável por processar a lógica de apresentação, comunicar com os repositórios e atualizar o estado da interface.
3. **Camada de Repositório (Repository & AppContainer):** Centraliza a lógica de dados, coordenando a leitura/escrita na base de dados local, na cache em memória e a sincronização com a nuvem.
4. **Persistência Local (Room SQLite):** Funciona como a fonte de verdade primária no dispositivo. Os dados são carregados instantaneamente e guardados localmente.
5. **Sincronização Remota (Firebase Firestore):** Sincroniza dados em tempo real em segundo plano quando existe conectividade, resolvendo operações pendentes criadas offline.

### Estrutura de Pastas e Pacotes

```text
pt/ulsbe/pontesaude/
├── data/                      Camada de acesso a dados e persistência
│   ├── local/                 Base de dados local (Room / SQLite)
│   │   ├── dao/               Interfaces DAO com consultas SQL (queries de perfis, medicação, etc.)
│   │   └── entity/            Entidades e esquemas das tabelas locais
│   ├── remote/                Comunicação com serviços remotos (Firebase Firestore, Auth e IA)
│   └── repository/            Repositórios de negócio e AppContainer (orquestrador de dependências)
│
├── model/                     Modelos de domínio puros da aplicação (Perfil, Medicamento, Consulta, Nota)
│
├── ui/                        Interface de utilizador (Jetpack Compose)
│   ├── components/            Componentes visuais reutilizáveis (cartões, barras, diálogos e botões)
│   ├── navigation/            Estrutura de navegação, rotas e barra inferior da aplicação
│   ├── screens/               Ecrãs principais da aplicação e respetivos ViewModels
│   │   ├── apontamentos/      Ecrã de notas clínicas, dúvidas médicas e tradutor com IA
│   │   ├── consultas/         Ecrã de marcação e acompanhamento de consultas/exames
│   │   ├── definicoes/        Ecrã de opções de conta, acessibilidade e preferências
│   │   ├── inicio/            Painel principal (dashboard) com o resumo do dia
│   │   ├── login/             Ecrã de autenticação com Chave Móvel Digital / telemóvel
│   │   ├── medicacao/         Ecrã de gestão de fármacos, horários e alarmes
│   │   └── saude/             Ecrã de literacia em saúde, módulos educativos e recompensas
│   └── theme/                 Configurações de tema Material 3 (paleta de cores, tipografia e formas)
│
├── util/                      Serviços auxiliares e utilitários de sistema
│                              (alarmes exatos, integração de câmara, criptografia AES e calendário)
│
└── MainActivity.kt            Ponto de entrada Android da aplicação
```

---

## Tecnologias Utilizadas

- Linguagem: Kotlin
- Interface Gráfica: Jetpack Compose + Material 3
- Arquitetura: MVVM (Model-View-ViewModel) com abordagem Offline-First
- Base de Dados Local: Room (SQLite)
- Base de Dados Remota: Google Firebase Firestore
- Autenticação: Firebase Auth (com fluxo preparado para Chave Móvel Digital)
- Concorrência e Reatividade: Coroutines e Kotlin Flow
- Inteligência Artificial: Google Gemini API (para simplificação de linguagem clínica)

---

## Como Executar o Projeto

1. Abrir a pasta do projeto no Android Studio.
2. Garantir que o JDK 17 (ou o JBR integrado no Android Studio) está selecionado como Gradle JDK.
3. Aguardar a sincronização dos ficheiros Gradle.
4. Executar a aplicação num dispositivo físico ou emulador com Android 8.0 (API 26) ou superior.
