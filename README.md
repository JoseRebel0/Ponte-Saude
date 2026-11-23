# Ponte-Saúde (ULSBE)

Aplicação móvel de saúde digital desenvolvida para apoiar os utentes da Unidade Local de Saúde do Baixo Alentejo (ULSBE) e os seus respetivos cuidadores na gestão clínica diária, adesão à terapêutica e literacia em saúde.

> Estado do Projeto: Em desenvolvimento (Work in Progress).

---

## Contexto e Motivação

A região do Baixo Alentejo caracteriza-se por uma grande dispersão geográfica, uma população acentuadamente envelhecida e desafios significativos no acesso presencial frequente a cuidados de saúde secundários. Adicionalmente, muitas áreas rurais enfrentam conectividade móvel intermitente.

O projeto **Ponte-Saúde** surge para dar resposta a esta realidade, funcionando como um canal direto e simplificado entre o cidadão e os serviços de saúde locais:
- Promove a adesão à medicação em doentes polimedicados e crónicos.
- Apoia ativamente os cuidadores familiares (informais) na gestão de múltiplos dependentes.
- Reduz a ansiedade e as faltas a consultas através de listas de preparação prévia e integração com o calendário do telemóvel.
- Eleva a literacia clínica da população sénior com conteúdos educativos certificados e gamificação positiva.
- Garante total operacionalidade sem ligação à internet através de uma arquitetura estritamente Offline-First.

---

## Pilares Fundamentais

### 1. Acessibilidade e Design Inclusivo para a População Sénior
- Interface com zonas de toque alargadas, navegação simplificada e contrastes visuais elevados.
- Suporte a modo de texto ampliado (acessibilidade visual).
- Leitura em voz alta (Text-to-Speech / TTS) em todos os módulos educativos de saúde.
- Ditado por voz integrado para notas e dúvidas, evitando a necessidade de digitação em teclados pequenos.

### 2. Gestão de Medicação e Segurança do Doente
- Registo completo de fármacos, horários de toma, dosagens e instruções (ex.: em jejum, após refeição).
- Associação de fotografias da caixa ou comprimido para identificação visual imediata.
- Sistema de alarmes exatos do Android e notificações persistentes de lembrete.

### 3. Agenda de Consultas e Preparação Prévia
- Registo e acompanhamento de consultas nos Cuidados de Saúde Primários e Hospitalares da ULSBE.
- Listas de verificação interativas antes da consulta (jejum, análises, transporte, medicação habitual).
- Sincronização automática com o calendário nativo do dispositivo móvel.

### 4. Modo Cuidador (Multi-Perfil)
- Permite que um filho, familiar ou cuidador formal faça a gestão partilhada da saúde de um ou mais utentes.
- Alternância rápida de perfil a partir de qualquer ecrã, mantendo dados clínicos estritamente isolados.

### 5. Literacia em Saúde e Clube de Segurança ULSBE
- Módulos educativos baseados nas metas internacionais de segurança do doente (higiene das mãos, prevenção de quedas, medicação segura, cirurgia segura).
- Catálogo de vídeos clínicos curtos e explicativos.
- Gamificação: a leitura de módulos e resolução de pequenos questionários atribui Pontos de Segurança que podem ser convertidos em vouchers (ex.: organizadores semanais de medicação).

### 6. Apoio à Comunicação Clínica com Inteligência Artificial (Gemini)
- Tradutor Médico: simplificação de linguagem clínica complexa e relatórios para termos claros e compreensíveis pelo utente comum.
- Geração assistida de notas com dúvidas a colocar ao médico ou enfermeiro de família.

---

## Arquitetura do Sistema

A aplicação assenta no padrão **MVVM (Model-View-ViewModel)** combinado com uma estratégia **Offline-First**, estruturada para responder aos desafios de conectividade:

1. **Camada de Apresentação (View):** Desenvolvida integralmente em Jetpack Compose com componentes Material 3. Reage em tempo real a fluxos imutáveis expostos pelos ViewModels.
2. **Camada de Gestão de Estado (ViewModel):** Gere o ciclo de vida dos ecrãs, expõe dados através de `StateFlow` e recebe as intenções do utilizador.
3. **Camada de Repositório (Repository Pattern):** Coordenada pelo `AppContainer`, gere as operações de leitura e escrita garantindo que a base de dados local tem prioridade.
4. **Persistência Local (Room / SQLite):** Base de dados local no dispositivo. Assegura que toda a informação (medicação, alarmes, consultas e notas) está acessível sem rede.
5. **Nuvem e Sincronização (Firebase Firestore):** Sincronização bidirecional em tempo real. As alterações feitas offline são marcadas com sinalizadores de pendência e sincronizadas em lote com a nuvem quando a rede é restabelecida.
6. **Privacidade e Proteção de Dados (RGPD):** Criptografia local simétrica (AES-256) em campos sensíveis de identificação antes do envio para serviços de nuvem.

### Organização de Pastas e Ficheiros

```text
pt/ulsbe/pontesaude/
├── data/                      Camada de dados e persistência
│   ├── local/                 Base de dados local Room (SQLite)
│   │   ├── dao/               Interfaces de acesso a dados (Consultas SQL locais)
│   │   └── entity/            Entidades e esquemas das tabelas Room
│   ├── remote/                Serviços de rede (Firebase Firestore, Auth e API Gemini)
│   └── repository/            Repositórios de negócio e AppContainer (injeção manual)
│
├── model/                     Modelos de dados puros de domínio (Perfil, Medicamento, Consulta, Nota)
│
├── ui/                        Interface gráfica de utilizador (Jetpack Compose)
│   ├── components/            Componentes visuais modulares (cartões, diálogos, barras, cabeçalhos)
│   ├── navigation/            Controlo de navegação, rotas e barra inferior
│   ├── screens/               Ecrãs da aplicação e respetivos ViewModels
│   │   ├── apontamentos/      Notas do utente e Tradutor Médico com IA
│   │   ├── consultas/         Gestão e checklist de consultas/exames
│   │   ├── definicoes/        Acessibilidade, alarmes, sessão CMD e modo cuidador
│   │   ├── inicio/            Painel inicial (dashboard) com o resumo do dia
│   │   ├── login/             Autenticação via Chave Móvel Digital / telemóvel
│   │   ├── medicacao/         Gestão de medicamentos, dosagens e alarmes
│   │   └── saude/             Literacia clínica, vídeos, questionários e recompensas
│   └── theme/                 Paleta institucional ULSBE, tipografia e estilos
│
├── util/                      Serviços de baixo nível e utilitários de sistema
│                              (alarmes exatos Android, câmara, TTS, criptografia AES e calendário)
│
└── MainActivity.kt            Atividade principal e ciclo de vida da aplicação
```

---

## Tecnologias e Bibliotecas

- Linguagem: Kotlin 2.x
- Interface: Jetpack Compose, Material 3, Accompanist
- Persistência Local: AndroidX Room SQLite
- Nuvem: Firebase Firestore, Firebase Authentication
- Inteligência Artificial: Google Gemini API (modelo generativo para simplificação de texto)
- Assincronismo: Kotlin Coroutines e StateFlow
- Integrações de Sistema: Android AlarmManager (Exact Alarms), CalendarProvider, TextToSpeech e SpeechRecognizer

---

## Como Executar o Projeto

1. Abrir a pasta raiz do projeto no Android Studio.
2. Certificar-se de que o JDK 17 (ou o JBR integrado no Android Studio) está selecionado nas definições de compilação do Gradle.
3. Aguardar a sincronização inicial das dependências Gradle.
4. Executar o módulo `app` num dispositivo físico ou emulador com Android 8.0 (API 26) ou superior.
