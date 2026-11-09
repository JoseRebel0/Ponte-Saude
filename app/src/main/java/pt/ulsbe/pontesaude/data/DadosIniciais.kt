package pt.ulsbe.pontesaude.data

import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.TipoModulo
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude

/**
 * Dados iniciais e pré-definidos da aplicação Ponte Saúde.
 * Fornece os perfis padrão, os módulos de literacia em saúde e as perguntas recomendadas.
 */
object DadosIniciais {
    val modulos = listOf(
        criarModulo(
            "identificacao", "Identificação inequívoca", "Pulseira e nome certo", "badge",
            TipoModulo.INFO,
            "A pulseira serve para ter a certeza de que o tratamento é para si.",
            "Em cada procedimento, confirme o seu nome e a data de nascimento.",
            "Se os dados estiverem errados, diga logo, antes de continuar.",
        ),
        criarModulo(
            "comunicacao", "Falar com os profissionais", "Perguntas prontas, por voz", "message",
            TipoModulo.PERGUNTAS,
            "Diga quando não percebe. Não tem de ter vergonha.",
            "Repita por palavras suas o que o profissional explicou.",
            "Diga as suas alergias e o que mudou na sua saúde.",
            "Peça informação escrita ou falada no dia da alta.",
        ),
        criarModulo(
            "participacao", "Participar na sua segurança", "Os seus direitos", "heart",
            TipoModulo.CONTACTOS,
            "O doente e o cuidador têm um papel na segurança.",
            "Só assina o consentimento informado depois de perceber.",
            "Se tiver uma preocupação, saiba como a sinalizar.",
        ),
        criarModulo(
            "infecoes", "Evitar infeções", "Lavar bem as mãos", "hand",
            TipoModulo.LAVAGEM_MAOS,
            "Lavar as mãos é a melhor defesa contra infeções.",
            "Ferida ou cateter: mantenha limpo e seco, sem mexer.",
            "Sinais de infeção: vermelhidão, calor, febre.",
        ),
        criarModulo(
            "cirurgia", "Vou ser operado", "Antes e depois da cirurgia", "checklist",
            TipoModulo.CIRURGIA,
            "Prepare a cirurgia: jejum e medicamentos a suspender.",
            "Confirme o local da operação antes de entrar.",
            "Em casa: cuidados à ferida e sinais de alarme.",
        ),
        criarModulo(
            "reconciliacao", "A minha lista de medicação", "Fotografias e lembretes", "pill",
            TipoModulo.MEDICAMENTOS,
            "Mantenha uma lista completa e atualizada.",
            "Leve a lista a todas as consultas, urgências e internamentos.",
            "A lista fica só no seu telemóvel.",
        ),
        criarModulo(
            "erros", "Erros de medicação", "Teste o que sabe", "warning",
            TipoModulo.QUESTIONARIO,
            "Erros comuns: trocar caixas, repetir a dose, esquecer.",
            "Genéricos podem ter nomes diferentes.",
            "Se suspeitar de um erro, diga logo a um profissional.",
        ),
        criarModulo(
            "uso-seguro", "Uso seguro do medicamento", "Ler a embalagem em voz alta", "qr",
            TipoModulo.LEITURA,
            "Saiba como tomar, guardar e eliminar os medicamentos.",
            "Aprenda a interpretar a embalagem e o folheto.",
            "Devolva sobras e embalagens vazias à farmácia.",
        ),
        criarModulo(
            "quedas", "Evitar quedas", "A minha casa é segura?", "steps",
            TipoModulo.QUEDAS,
            "Riscos em casa e nas unidades de cuidados de saúde.",
            "Alguns medicamentos aumentam o risco de queda.",
            "Peça ajuda. Levante-se devagar.",
        ),
        criarModulo(
            "sinais-alarme", "Sinais de Alarme & Emergência", "AVC, Enfarte e Sépsis", "warning",
            TipoModulo.INFO,
            "Sinais dos 3 Fs no AVC: Fala com dificuldade, Força num braço diminuída, Face descaída.",
            "Dor ou aperto intenso no peito que irradia para o braço esquerdo ou mandíbula.",
            "Dificuldade respiratória súbita, confusão repentina ou febre alta com arrepios intensos.",
            "Perante estes sinais, não espere: ligue imediatamente para o 112.",
        ),
        criarModulo(
            "teleconsulta", "Teleconsultas & Saúde Digital", "Como se preparar em segurança", "message",
            TipoModulo.INFO,
            "Tenha consigo o Cartão de Cidadão, a lista de medicação e exames recentes.",
            "Esteja num local calmo, iluminado e com privacidade para falar com o seu médico.",
            "Se o som ou a imagem falharem, o profissional tentará contactar por chamada telefónica.",
            "Anote antecipadamente as suas dúvidas no separador Apontamentos.",
        ),
        criarModulo(
            "alta-hospitalar", "Regresso a Casa após a Alta", "Cuidados e continuidade", "shield",
            TipoModulo.INFO,
            "Confirme que compreendeu a nota de alta e as receitas de medicamentos a manter ou suspender.",
            "Saiba a que sinais de agravamento deve estar atento nas primeiras 48 horas.",
            "Confirme a data e o local da sua consulta de revisão no Centro de Saúde ou Hospital.",
            "Em caso de dúvida nas primeiras horas pós-alta, ligue para a Linha SNS 24 (808 24 24 24).",
        ),
        criarModulo(
            "prevencao", "Prevenção de erros", "Se algo não lhe parece bem, diga", "shield",
            TipoModulo.RESUMO,
            "Doente e cuidador ajudam a detetar falhas.",
            "Identificação, medicação e procedimentos.",
            "Se algo não lhe parece bem, diga.",
        ),
        criarModulo(
            "vacinacao", "Vacinação", "Perguntas frequentes", "vaccine",
            TipoModulo.INFO,
            "As vacinas protegem-no a si e a quem o rodeia.",
            "Tem dúvidas? Fale com o seu médico ou enfermeiro.",
            "Guarde o boletim de vacinas e leve-o às consultas.",
        ),
    )

    val perguntasMedico = listOf(
        "Qual é o meu problema, em palavras simples?",
        "Para que serve este medicamento e como o devo tomar?",
        "O que mudou na minha medicação?",
        "Posso tomar isto com os meus outros medicamentos e chás?",
        "O que devo fazer se me sentir pior?",
        "A que sinais de alarme devo estar atento?",
        "Quando devo voltar e a quem devo ligar?",
        "Pode explicar-me outra vez, mais devagar?",
    )

    // Perfil exclusivo para pré-visualização (Previews do Android Studio)
    val perfilPreVisualizacao = Perfil(
        id = "1",
        nome = "Utente Demonstração",
        nomeCurto = "Utente",
        relacao = "Utente",
        iniciais = "UD",
        numeroUtente = "189 423 761",
        tipoSanguineo = "A+",
        alergias = "Sem alergias",
    )
    val perfis = listOf(perfilPreVisualizacao)

    val recompensas = listOf(
        RecompensaSeguranca(
            id = "porta_comprimidos",
            titulo = "Organizador Semanal de Comprimidos",
            descricao = "Caixa semanal com divisórias dia/noite com marcações em alto-relevo para nunca esquecer a medicação.",
            pontos = 100,
            icone = "pill",
            localLevantamento = "Balcão da sua USF / Centro de Saúde",
        ),
        RecompensaSeguranca(
            id = "bolsa_termica",
            titulo = "Bolsa Térmica de Medicamentos ULSBE",
            descricao = "Bolsa isotérmica certificada para transporte seguro de insulina, colírios e vacinas.",
            pontos = 200,
            icone = "bag",
            localLevantamento = "Farmácia Hospitalar ULSBE",
        ),
        RecompensaSeguranca(
            id = "guia_impresso",
            titulo = "Manual Prático: Cuidados & Direitos",
            descricao = "Livro impresso em letra grande com guia anti-quedas, primeiros socorros e contactos úteis.",
            pontos = 150,
            icone = "book",
            localLevantamento = "Gabinete do Cidadão ULSBE",
        ),
        RecompensaSeguranca(
            id = "estacionamento",
            titulo = "Vale de 1 Hora de Estacionamento",
            descricao = "Isenção de 1 hora de estacionamento no parque hospitalar no dia da consulta ou exame.",
            pontos = 250,
            icone = "parking",
            localLevantamento = "Receção Central / Segurança do Hospital",
        ),
        RecompensaSeguranca(
            id = "sorteio_aparelho",
            titulo = "Entrada no Sorteio: Medidor de Tensão",
            descricao = "Participação no sorteio trimestral de 10 tensiómetros digitais validados clinicamente.",
            pontos = 300,
            icone = "ticket",
            localLevantamento = "Sorteio automático com aviso por SMS",
        ),
        RecompensaSeguranca(
            id = "diploma",
            titulo = "Certificado Digital de Utente Seguro",
            descricao = "Distinção oficial de literacia em segurança do doente reconhecida pela ULSBE.",
            pontos = 50,
            icone = "star",
            localLevantamento = "Emissão digital imediata na aplicação",
        ),
    )

    val videosBiblioteca = listOf(
        VideoBibliotecaSaude(
            id = "video_quando_ligar_sns24",
            titulo = "Quando ligar SNS 24 ou INEM 112?",
            descricao = "Vídeo oficial explicativo sobre quando recorrer à Linha SNS 24 e quando ativar a emergência médica.",
            duracao = "2 min",
            urlYoutube = "https://www.youtube.com/watch?v=3TlyiS9RYwM",
            pontos = 30,
        ),
        VideoBibliotecaSaude(
            id = "video_app_sns24_guia",
            titulo = "Como utilizar a App SNS 24 e consultar Receitas",
            descricao = "Guia do Serviço Nacional de Saúde para aceder ao guia de tratamento, receitas digitais e exames.",
            duracao = "3 min",
            urlYoutube = "https://www.youtube.com/watch?v=7SDpkhUqa5c",
            pontos = 30,
        ),
        VideoBibliotecaSaude(
            id = "video_medir_tensao_arterial",
            titulo = "Como Medir a Pressão Arterial no Domicílio",
            descricao = "Técnica correta de repouso, posicionamento da braçadeira e registo de valores para o seu médico.",
            duracao = "3 min",
            urlYoutube = "https://www.youtube.com/results?search_query=SNS+24+como+medir+pressao+arterial",
            pontos = 30,
        ),
        VideoBibliotecaSaude(
            id = "video_sinais_avc",
            titulo = "Reconhecimento dos Sinais de Alerta de AVC",
            descricao = "Os 3 sinais vitais (Face descaída, Força no braço, Fala com dificuldade) e a importância do 112 imediato.",
            duracao = "2 min",
            urlYoutube = "https://www.youtube.com/results?search_query=DGS+sinais+de+alerta+AVC",
            pontos = 30,
        ),
    )

    private fun criarModulo(
        id: String,
        titulo: String,
        subtitulo: String,
        icone: String,
        tipo: TipoModulo,
        vararg conteudos: String,
    ) = ModuloSaude(
        id = id,
        titulo = titulo,
        subtitulo = subtitulo,
        icone = icone,
        tipo = tipo,
        conteudos = conteudos.toList(),
        fala = conteudos.joinToString(" "),
    )
}

