# Ponte-Saúde (ULSBE)

Aplicação móvel de saúde digital desenvolvida em Kotlin e Jetpack Compose para a **ULSBE (Unidade Local de Saúde de Barcelos / Esposende)**. O seu propósito é apoiar os utentes dos concelhos de Barcelos e Esposende e os seus cuidadores na gestão diária da medicação, agendamento e preparação de consultas, literacia em saúde e segurança do doente.

> Estado do Projeto: Em desenvolvimento (Work in Progress).

---

## Âmbito e Contexto da ULSBE

A Unidade Local de Saúde de Barcelos / Esposende agrega os cuidados de saúde hospitalares (centrados no **Hospital Santa Maria Maior**, em Barcelos) e os cuidados de saúde primários (Centros de Saúde e Unidades de Saúde Familiar de Barcelos e de Esposende).

O projeto **Ponte-Saúde** foi concebido para responder às necessidades reais dos utentes e famílias da região:
- Facilitar o acompanhamento de doentes crónicos e polimedicados em ambulatório e no domicílio.
- Integrar os cuidadores familiares (informais e formais) no plano terapêutico através de gestão multiperfil.
- Assegurar a preparação adequada de consultas e exames (ex.: tempos de jejum e transporte de exames anteriores), reduzindo o desperdício clínico e as faltas.
- Disponibilizar um canal de literacia em saúde acessível à população sénior, baseado nas metas internacionais de segurança do doente.
- Garantir funcionamento contínuo e sem falhas em qualquer ponto dos concelhos através de uma arquitetura estritamente Offline-First.

---

## Funcionalidades Principais

### 1. Painel Inicial (Dashboard)
- Resumo cronológico do dia: próximas tomas de medicação, consultas marcadas e tarefas de saúde pendentes.
- Acesso rápido aos contactos de emergência, SNS 24 (808 24 24 24) e linha direta da ULSBE.
- Cartão de estado com atalhos para os restantes módulos da aplicação.

### 2. Gestão de Medicação
- Registo detalhado de fármacos: nome comercial, princípio ativo (DCI), laboratório, dosagem, frequência e horário das tomas.
- Suporte fotográfico: captura direta pela câmara ou seleção da galeria da caixa do medicamento ou do comprimido, com compressão local automática para identificação visual rápida.
- Assistente Bula Fácil com IA: integração com a Google Gemini API para explicar de forma simples, em português claro e em poucas frases, para que serve o medicamento e qual o cuidado principal a ter.
- Alarmes e Notificações: agendamento de alarmes exatos do sistema (AlarmManager) e notificações persistentes de lembrete.
- Emissão de Guia Oficial em PDF: geração local de um documento A4 formatado com cabeçalho oficial da ULSBE, identificação do utente, número SNS, tipo sanguíneo, alergias e plano completo de tomas para apresentação em consultas ou internamentos.

### 3. Consultas e Exames Médicos
- Agenda de consultas hospitalares (Hospital Santa Maria Maior) e dos cuidados primários (Centros de Saúde e USF de Barcelos e Esposende).
- Listas de verificação pré-consulta: controlo de jejum, transporte de exames, Cartão de Cidadão e lista de fármacos em uso.
- Integração nativa com o calendário do Android (CalendarContract), criando eventos sincronizados identificados como consultas ULSBE.

### 4. Apontamentos, Dúvidas e Tradutor Médico (IA)
- Tradutor Médico com IA: simplificação de terminologia médica e relatórios complexos em explicações breves (2 a 3 frases) adaptadas a utentes idosos e cuidadores, com historial de perguntas e respostas guardado localmente.
- Ditado por voz: suporte ao reconhecimento de voz do Android para registo rápido de sintomas e perguntas sem necessidade de escrever no teclado.
- Registo de notas do utente e do cuidador para levar ao médico de família ou especialista.
- Lista complementar de outros produtos de saúde (suplementos, produtos de ervanária e parafarmácia).

### 5. Literacia em Saúde e Clube de Segurança ULSBE
- Módulos educativos interativos dedicados a áreas chave da segurança do doente:
  - Lavagem e higiene das mãos (técnica passo a passo).
  - Prevenção de quedas no domicílio (checklist de segurança em casa).
  - Medicação segura e prevenção de erros de toma.
  - Preparação para cirurgia e procedimentos invasivos.
  - Saúde infantil e contactos essenciais.
  - Questionários interativos de avaliação de conhecimentos.
- Síntese de voz (Text-to-Speech): leitura de todos os módulos educativos em voz alta em português de Portugal, com velocidade adaptada para fácil compreensão.
- Sistema de Gamificação Clínica: atribuição de Pontos de Segurança pela leitura, audição de módulos e conclusão de vídeos, permitindo o resgate de vouchers para artigos de apoio (ex.: caixas organizadoras de comprimidos ou kits de higiene).

### 6. Modo Cuidador (Multi-Perfil)
- Gestão centralizada de múltiplos utentes dependentes (pais idosos, filhos ou outros familiares) a partir de uma única instalação da aplicação.
- Alternância instantânea de perfil, assegurando que o historial médico, tomas e apontamentos de cada utente são mantidos de forma estritamente independente.

### 7. Acessibilidade e Definições
- Modo de texto ampliado e opção de redução de movimento.
- Autenticação e integração simulada com o ecossistema Chave Móvel Digital (Autenticação.gov).
- Ferramenta de teste de notificações locais para diagnóstico no dispositivo.
- Criptografia local AES-256 para proteção dos dados pessoais de saúde em conformidade com o RGPD.

---

## Arquitetura do Sistema

A aplicação adota o padrão de arquitetura **MVVM (Model-View-ViewModel)** orientado a uma estratégia **Offline-First**:

1. **Camada de Apresentação (View):** Desenvolvida em Jetpack Compose com Material Design 3. O estado da interface é reativo e consome exclusivamente fluxos imutáveis (`StateFlow`).
2. **Camada de Gestão de Estado (ViewModel):** Orquestra a lógica de apresentação de cada ecrã, interage com a camada de dados e gere o ciclo de vida da interface.
3. **Camada de Repositório (Repository Pattern):** O `AppContainer` atua como agregador e injetor central de dependências. Os repositórios coordenam o fluxo de leitura e escrita, tratando a base de dados local como fonte primária de verdade.
4. **Persistência Local (Room / SQLite):** Garante que todas as operações (consulta de medicamentos, tomas, notas e alarmes) funcionam de imediato e sem atrasos, mesmo sem rede móvel ou Wi-Fi.
5. **Nuvem e Sincronização (Firebase Firestore):** Sincronização em tempo real quando existe ligação à rede. As alterações efetuadas em modo offline recebem sinalizadores de pendência e são sincronizadas em lote atómico (WriteBatch) assim que a ligação é restabelecida.
6. **Inteligência Artificial (Google Gemini API via Firebase AI):** Utilização do modelo Gemini para tarefas de tradução clínica e resumo de medicação, operando sem dependências de infraestruturas pesadas locais.

### Estrutura de Pastas e Módulos do Código

```text
pt/ulsbe/pontesaude/
├── data/                      Camada de acesso a dados e persistência
│   ├── local/                 Base de dados local Room (SQLite)
│   │   ├── dao/               Interfaces DAO com instruções SQL (perfis, medicamentos, consultas, etc.)
│   │   ├── entity/            Tabelas e entidades da base de dados local
│   │   └── BaseDadosLocal.java Configuração principal da base de dados Room
│   ├── remote/                Serviços remotos e nuvem
│   │   ├── ApiCatalogoRemoto.kt Módulos de saúde, vídeos e catálogo de recompensas
│   │   ├── ApiConsultasRemoto.kt Sincronização de consultas no Firestore
│   │   ├── ApiMedicacaoRemoto.kt Sincronização de fármacos no Firestore
│   │   ├── ApiNotasRemoto.kt   Sincronização de notas e dúvidas clínicas em lote
│   │   ├── ApiPerfisRemoto.kt  Gestão remota de perfis e associação de cuidadores
│   │   ├── AutenticadorFirebaseCMD.kt Gestão de sessão Chave Móvel Digital / Firebase
│   │   ├── ClienteFirestore.kt Fachada central unificada de acesso ao Firestore
│   │   └── IntegracaoIA.kt     Integração com a Google Gemini API (Tradutor e Bula Fácil)
│   └── repository/            Repositórios da aplicação e injeção de dependências
│       ├── AppContainer.kt     Contentor central de dependências e sincronização global
│       ├── ConfiguracaoRepository.kt Preferências de acessibilidade e definições do utente
│       ├── ConsultaRepository.kt Gestão de consultas e exames
│       ├── MedicacaoRepository.kt Gestão de medicação, tomas e alarmes
│       ├── NotaRepository.kt   Gestão de apontamentos e produtos de parafarmácia
│       ├── PerfilRepository.kt Gestão de perfis clínicos e modo cuidador
│       └── SaudeRepository.kt  Gestão de literacia, módulos educativos e pontos de segurança
│
├── model/                     Modelos de domínio puros da aplicação
│   └── ModelosDados.kt        Classes de dados (Perfil, Medicamento, Consulta, Nota, ModuloSaude, etc.)
│
├── ui/                        Interface gráfica com Jetpack Compose
│   ├── components/            Componentes de interface modulares e reutilizáveis
│   │   ├── BarraScrollLateralSuave.kt Barra de deslocamento lateral personalizada
│   │   ├── ComponenteChecklist.kt Listas de verificação interativas para consultas e segurança
│   │   ├── ComponenteCuidador.kt Componentes dedicados à alternância e gestão de cuidadores
│   │   └── ComponentesUI.kt   Cartões base, botões de áudio, cabeçalhos e diálogos
│   ├── navigation/            Controlo e rotas de navegação
│   │   └── NavegacaoApp.kt    Barra de navegação inferior, animações e troca de ecrãs
│   ├── screens/               Ecrãs funcionais e respetivos ViewModels
│   │   ├── apontamentos/      Ecrã de notas clínicas, dúvidas e Tradutor Médico com IA
│   │   ├── consultas/         Ecrã de marcação e checklists de consultas
│   │   ├── definicoes/        Ecrã de opções de conta, testes de alarme e acessibilidade
│   │   ├── inicio/            Painel inicial (dashboard diário)
│   │   ├── login/             Ecrã de início de sessão com Chave Móvel Digital
│   │   ├── medicacao/         Ecrã de listagem, alarmes e formulário de medicamentos
│   │   └── saude/             Ecrã de literacia, módulos interativos e recompensas
│   └── theme/                 Estilos visuais institucionais
│       └── Tema.kt            Paleta de cores ULSBE, tipografia legível e temas claro/escuro
│
├── util/                      Serviços de baixo nível e integrações com o sistema Android
│   ├── Criptografia.kt        Cifragem e decifragem de dados sensíveis com AES-256
│   ├── ProcessadorImagem.kt   Compressão, rotação e conversão de fotografias de medicação
│   └── ServicosDispositivo.kt Gestor de alarmes exatos, chamadas, TTS em pt-PT, calendário e PDF
│
└── MainActivity.kt            Atividade principal e ponto de entrada da aplicação Android
```

---

## Tecnologias e Bibliotecas

- Linguagem: Kotlin 2.x
- Interface Gráfica: Jetpack Compose + Material 3
- Persistência Local: AndroidX Room (SQLite)
- Backend e Nuvem: Google Firebase Firestore e Firebase Authentication
- Inteligência Artificial: Google Gemini API (modelo Gemini Flash Lite via Firebase AI Logic)
- Processamento Assíncrono: Kotlin Coroutines e StateFlow
- Integrações Nativas: Android AlarmManager (Exact Alarms), CalendarProvider, TextToSpeech e SpeechRecognizer
- Geração Documental: Android Graphics PdfDocument (emissão de guias A4 em PDF)

---

## Como Executar o Projeto

1. Abrir a pasta do projeto no **Android Studio** (versão Ladybug ou mais recente recomendada).
2. Confirmar que o **JDK 17** (ou o JBR fornecido pelo Android Studio) está configurado como Gradle JDK.
3. Aguardar o descarregamento das dependências e a sincronização do Gradle.
4. Executar a aplicação num emulador ou dispositivo Android físico com **Android 8.0 (API 26)** ou superior.
