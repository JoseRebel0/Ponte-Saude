# Alterações de segurança (cópia `Ponte-Saude-seguranca`)

Base: versão da equipa (`Ponte-Saude-main 2`). Ainda **não foi compilada nem testada**: o código foi
escrito sem acesso ao Android SDK. Compilar no Android Studio e corrigir o que aparecer.

## Ideia

Os dados clínicos (medicação, consultas, notas, produtos, pontos) ficam no Firebase ligados **apenas a um id
aleatório**. O que identifica a pessoa (nome, NIF, nº de utente, cartão de cidadão, data de nascimento,
telemóvel, email) fica **só no telemóvel** (Room). Sem a ligação id ↔ pessoa, o que está na cloud não diz
quem é o doente.

Continua a ser dado de saúde pseudonimizado (não anónimo): perguntar à ULSBE se podem ficar na cloud.

## O que mudou

| Área | Antes | Agora |
|---|---|---|
| Login Firebase | email/password calculados a partir do telemóvel/NIF (qualquer pessoa os calcula) | login **anónimo**: UID aleatório por instalação = dono dos dados |
| Terminar sessão | `signOut` | só local (signOut perderia o UID anónimo) |
| Perfil no Firestore | nome, NIF, nº utente, CC, nascimento, telemóvel, email | só id, dono, sangue, alergias, ligações, pontos, módulos, vouchers |
| Fotografias de medicamentos | enviadas em Base64 | ficam só no telemóvel |
| Cifra `Criptografia` | "AES-256/RGPD" com chave no código | deixa de ser usada ao gravar (só lê dados antigos) |
| Ids de perfis novos | UID da conta / `cuidado_<hora>` | `p…`/`c…` + 16 caracteres aleatórios |
| Gerar perfil de teste | trocava de conta Firebase | não troca de conta |
| Apagar perfil | apagava também a conta Auth | só apaga dados (a conta anónima é da instalação) |
| Regras Firestore | `cuidadoresIds` (ids de perfil) | `cuidadoresUids` (UIDs); cuidador não altera dono nem lista de cuidadores |
| Sync | substituía a identificação pelos valores genéricos do Firestore | identificação vem sempre do Room; perfis só locais não desaparecem |
| Escuta em tempo real | podia nunca arrancar (sem UID no arranque) | arranca depois de garantir a sessão |

## Correções pequenas (iguais às da minha versão anterior)

- Alarmes: removida `SCHEDULE_EXACT_ALARM` (`setAlarmClock` não precisa); o receiver encontra o medicamento
  (`id*100+idx`) e reagenda para o dia seguinte; novo `ReiniciarAlarmesReceiver` volta a agendar após reiniciar o telemóvel.
- `allowBackup=false`; `file_paths.xml` só expõe `cache/pdf/`.
- PDF: já não diz "documento oficial / SNS".
- Login e Definições: textos deixam de dizer "Autenticação.gov / Certificada / RGPD"; o código SMS simulado tem de coincidir.
- Cuidador: a condição `&&` deixava passar qualquer código de 6 caracteres.
- Calendário: permissão pedida uma vez ao abrir as Consultas (não no arranque).
- Gerador de perfis por IA: ano de nascimento já não é fixo (`2026`).
- `libs.versions.toml`: versões iguais às realmente usadas no `app/build.gradle.kts`.

## O que é preciso fazer na consola Firebase (obrigatório)

1. **Authentication → Sign-in method → Anonymous → Ativar.**
2. **Firestore → Regras:** colar o conteúdo de `firestore.rules` e Publicar.
3. Copiar `app/google-services.json` (não vai para o git).

## Limitações

- O acesso aos dados da cloud está ligado à instalação: se desinstalar a app ou limpar os dados, perde-se.
  Para recuperar noutro aparelho seria preciso login real (SMS) — fora do âmbito da demo.
- Na primeira abertura não há perfis: usar "gerar perfil" (precisa do Firebase AI ativo).
- Perfis antigos no Firestore (sem `donoUid`) deixam de ser legíveis; podem ser limpos com `tools/limpar-firestore`.
- Ligar a um cuidador noutro telemóvel **ainda não funciona** (só entre perfis do mesmo telemóvel): falta o fluxo de
  código de convite (próximo passo).
- Perfis criados offline não são enviados à cloud até haver lógica própria (já era assim).
- Notas são texto livre: avisar os utilizadores para não escrever nomes ou números pessoais.

## Como testar

1. Instalar limpo (ou limpar dados). Ligar à Internet. Abrir, gerar um perfil, entrar.
2. Firestore → `perfis/<id>`: **não** deve ter nome, NIF, telemóvel, email, etc.; deve ter `donoUid`.
3. Adicionar um medicamento com fotografia: no Firestore o campo `fotografia` fica vazio; no telemóvel a foto aparece.
4. Fechar a app e abrir de novo: o nome e os dados continuam (vêm do Room).
5. Medicação com alarme: espera a hora, depois reinicia o telemóvel e confirma que o alarme volta.
6. Consultas: a permissão do calendário aparece ao abrir o ecrã.
