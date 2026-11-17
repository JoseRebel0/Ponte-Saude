package pt.ulsbe.pontesaude.data

import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import java.util.Random

/**
 * Catálogo e gerador de dados clínicos realistas para o ecossistema de saúde português (SNS e ULSBE).
 * Contém mais de 100 nomes portugueses autênticos, mais de 100 apresentações de medicamentos reais,
 * laboratórios farmacêuticos em Portugal e consultas especializadas da ULSBE (Barcelos / Esposende).
 */
object GeradorDadosPortugal {

    val NOMES_PORTUGAL = listOf(
        Pair("Manuel António Ferreira", "Manuel"),
        Pair("Ana Sofia Costa", "Ana"),
        Pair("Carlos Alberto Martins", "Carlos"),
        Pair("Teresa Maria Pinho", "Teresa"),
        Pair("João Pedro Ribeiro", "João"),
        Pair("Maria da Conceição Sousa", "Maria"),
        Pair("Rui Jorge Barbosa", "Rui"),
        Pair("Helena Isabel Santos", "Helena"),
        Pair("Fernando Miguel Silva", "Fernando"),
        Pair("Rosa Maria Oliveira", "Rosa"),
        Pair("António José Rodrigues", "António"),
        Pair("Maria de Fátima Pereira", "Fátima"),
        Pair("José Manuel Moreira", "José"),
        Pair("Isabel Maria Dias", "Isabel"),
        Pair("Joaquim Alberto Carvalho", "Joaquim"),
        Pair("Maria Manuela Gomes", "Manuela"),
        Pair("Francisco José Lopes", "Francisco"),
        Pair("Paula Cristina Almeida", "Paula"),
        Pair("Vítor Manuel Fernandes", "Vítor"),
        Pair("Maria do Carmo Marques", "Carmo"),
        Pair("Luís Miguel Pinto", "Luís"),
        Pair("Maria Helena Teixeira", "Helena"),
        Pair("Paulo Jorge Correia", "Paulo"),
        Pair("Sílvia Maria Mendes", "Sílvia"),
        Pair("Jorge Manuel Neves", "Jorge"),
        Pair("Maria de Lurdes Castro", "Lurdes"),
        Pair("Eduardo José Ramos", "Eduardo"),
        Pair("Célia Maria Antunes", "Célia"),
        Pair("Domingos António Cardoso", "Domingos"),
        Pair("Maria Teresa Figueiredo", "Teresa"),
        Pair("Álvaro Manuel Tavares", "Álvaro"),
        Pair("Beatriz Sofia Nogueira", "Beatriz"),
        Pair("Bernardo José Guimarães", "Bernardo"),
        Pair("Carla Maria Monteiro", "Carla"),
        Pair("Daniel Filipe Duarte", "Daniel"),
        Pair("Dulce Maria Vasconcelos", "Dulce"),
        Pair("Emídio Jorge Magalhães", "Emídio"),
        Pair("Elvira da Conceição Matos", "Elvira"),
        Pair("Gonçalo Nuno Barreto", "Gonçalo"),
        Pair("Graça Maria Peixoto", "Graça"),
        Pair("Henrique José Machado", "Henrique"),
        Pair("Inês Margarida Coimbra", "Inês"),
        Pair("Jaime Manuel Simões", "Jaime"),
        Pair("Júlia Maria Lourenço", "Júlia"),
        Pair("Leonardo José Faria", "Leonardo"),
        Pair("Lídia da Glória Teles", "Lídia"),
        Pair("Mário Rui Couto", "Mário"),
        Pair("Marta Filipa Aguiar", "Marta"),
        Pair("Nuno Miguel Pacheco", "Nuno"),
        Pair("Odete Maria Valente", "Odete"),
        Pair("Pedro Nuno Azevedo", "Pedro"),
        Pair("Palmira dos Prazeres Maia", "Palmira"),
        Pair("Ricardo Manuel Braga", "Ricardo"),
        Pair("Raquel Maria Macedo", "Raquel"),
        Pair("Sérgio Paulo Morais", "Sérgio"),
        Pair("Sandra Cristina Meireles", "Sandra"),
        Pair("Tiago André Fonseca", "Tiago"),
        Pair("Susana Maria Baptista", "Susana"),
        Pair("Valdemar José Sampaio", "Valdemar"),
        Pair("Vera Lúcia Godinho", "Vera"),
        Pair("Alexandre Miguel Sequeira", "Alexandre"),
        Pair("Amélia de Jesus Pinheiro", "Amélia"),
        Pair("Artur Jorge Viegas", "Artur"),
        Pair("Anabela Sofia Bessa", "Anabela"),
        Pair("Augusto César Salgado", "Augusto"),
        Pair("Branca Florinda Gameiro", "Branca"),
        Pair("César Augusto Resende", "César"),
        Pair("Catarina Maria Amado", "Catarina"),
        Pair("Dinis Manuel Esteves", "Dinis"),
        Pair("Deolinda de Fátima Saraiva", "Deolinda"),
        Pair("Eurico José Sobral", "Eurico"),
        Pair("Esperança Maria Belchior", "Esperança"),
        Pair("Filipe Miguel Cordeiro", "Filipe"),
        Pair("Filomena Maria Caldas", "Filomena"),
        Pair("Guilherme José Trindade", "Guilherme"),
        Pair("Glória Maria Portela", "Glória"),
        Pair("Hélder Manuel Alpoim", "Hélder"),
        Pair("Irene da Piedade Abreu", "Irene"),
        Pair("Ivo Daniel Quintas", "Ivo"),
        Pair("Isilda de Lurdes Cunha", "Isilda"),
        Pair("Júlio César Gouveia", "Júlio"),
        Pair("Joana Filipa Vilar", "Joana"),
        Pair("Lourenço Manuel Frois", "Lourenço"),
        Pair("Leonor Maria Marinho", "Leonor"),
        Pair("Marco Paulo Taborda", "Marco"),
        Pair("Madalena do Céu Lacerda", "Madalena"),
        Pair("Norberto José Castanheira", "Norberto"),
        Pair("Noémia de Jesus Brandão", "Noémia"),
        Pair("Otávio Manuel Raposo", "Otávio"),
        Pair("Olga Maria Campelo", "Olga"),
        Pair("Pascoal António Botelho", "Pascoal"),
        Pair("Purificação Maria Viana", "Purificação"),
        Pair("Quirino Manuel Freire", "Quirino"),
        Pair("Quitéria da Graça Pires", "Quitéria"),
        Pair("Rodrigo José Vilas-Boas", "Rodrigo"),
        Pair("Rute Sofia Barata", "Rute"),
        Pair("Sebastião Manuel Negrão", "Sebastião"),
        Pair("Sara Maria Camilo", "Sara"),
        Pair("Tomás Alberto Fontes", "Tomás"),
        Pair("Zulmira de Jesus Miranda", "Zulmira"),
        Pair("Afonso Miguel Barcelos", "Afonso"),
        Pair("Vasco Manuel Esposende", "Vasco"),
    )

    val LABORATORIOS_PORTUGAL = listOf(
        "Bial",
        "Generis",
        "Labesfal",
        "Cinfa",
        "Tecnimede",
        "Bluepharma",
        "Basi",
        "Medinfar",
        "Atral",
        "Jaba Recordati",
        "ToLife",
        "Alter",
        "Aurovitas",
        "Mepha",
        "Sandoz",
        "Lusomedicamenta",
        "Farmoz",
    )

    data class TemplateMed(
        val nome: String,
        val finalidade: String,
        val principioAtivo: String,
        val hora: String,
        val cor: Long,
    )

    val MEDICAMENTOS_PORTUGAL = listOf(
        // Analgésicos e Anti-inflamatórios
        TemplateMed("Paracetamol 1000 mg", "Dores e Febre", "paracetamol", "08:00", 0xFF3E63DD),
        TemplateMed("Paracetamol 500 mg", "Dores ligeiras", "paracetamol", "08:00, 20:00", 0xFF3E63DD),
        TemplateMed("Ibuprofeno 600 mg", "Anti-inflamatório e Dores", "ibuprofeno", "09:00, 21:00", 0xFFEF4444),
        TemplateMed("Ibuprofeno 400 mg", "Dores musculares e articulares", "ibuprofeno", "08:00, 20:00", 0xFFEF4444),
        TemplateMed("Metamizol 575 mg", "Dores agudas e cólicas", "metamizol magnésico", "08:00", 0xFF6366F1),
        TemplateMed("Tramadol 50 mg", "Dores moderadas a intensas", "tramadol", "08:00, 20:00", 0xFF8B5CF6),
        TemplateMed("Tramadol + Paracetamol 37.5/325 mg", "Alívio da dor persistente", "tramadol + paracetamol", "08:00, 20:00", 0xFF7C3AED),
        TemplateMed("Naproxeno 500 mg", "Anti-inflamatório articular", "naproxeno", "08:30", 0xFFDC2626),
        TemplateMed("Celecoxib 200 mg", "Dores articulares e artrose", "celecoxib", "09:00", 0xFFEA580C),
        TemplateMed("Diclofenac 50 mg", "Anti-inflamatório", "diclofenac", "08:00, 20:00", 0xFFB91C1C),

        // Gastrointestinal e Protetores Gástricos
        TemplateMed("Omeprazol 20 mg", "Proteção do estômago", "omeprazol", "08:30", 0xFFE5484D),
        TemplateMed("Omeprazol 40 mg", "Úlcera e refluxo gástrico", "omeprazol", "08:30", 0xFFE5484D),
        TemplateMed("Pantoprazol 40 mg", "Refluxo gastroesofágico", "pantoprazol", "08:00", 0xFF06B6D4),
        TemplateMed("Pantoprazol 20 mg", "Proteção gástrica diária", "pantoprazol", "08:00", 0xFF06B6D4),
        TemplateMed("Esomeprazol 20 mg", "Acidez e refluxo", "esomeprazol", "08:30", 0xFF0284C7),
        TemplateMed("Esomeprazol 40 mg", "Refluxo e gastrite", "esomeprazol", "08:30", 0xFF0369A1),
        TemplateMed("Lansoprazol 30 mg", "Úlcera péptica", "lansoprazol", "08:00", 0xFF0E7490),
        TemplateMed("Domperidona 10 mg", "Náuseas e má digestão", "domperidona", "08:00, 13:00, 20:00", 0xFF14B8A6),
        TemplateMed("Metoclopramida 10 mg", "Náuseas e vómitos", "metoclopramida", "08:00", 0xFF0D9488),
        TemplateMed("Mebeverina 200 mg", "Cólicas e cólon irritável", "mebeverina", "08:00, 20:00", 0xFF047857),

        // Colesterol e Lípidos
        TemplateMed("Atorvastatina 10 mg", "Controlo do colesterol", "atorvastatina", "21:00", 0xFFF5D90A),
        TemplateMed("Atorvastatina 20 mg", "Colesterol e proteção cardíaca", "atorvastatina", "21:00", 0xFFF5D90A),
        TemplateMed("Atorvastatina 40 mg", "Colesterol elevado", "atorvastatina", "21:00", 0xFFD97706),
        TemplateMed("Sinvastatina 20 mg", "Controlo de lípidos", "sinvastatina", "21:30", 0xFFF59E0B),
        TemplateMed("Sinvastatina 40 mg", "Colesterol e prevenção vascular", "sinvastatina", "21:30", 0xFFD97706),
        TemplateMed("Rosuvastatina 10 mg", "Proteção cardiovascular e lípidos", "rosuvastatina", "21:00", 0xFFEAB308),
        TemplateMed("Rosuvastatina 20 mg", "Controlo lipídico intensivo", "rosuvastatina", "21:00", 0xFFCA8A04),
        TemplateMed("Ezetimiba 10 mg", "Redução da absorção de colesterol", "ezetimiba", "21:00", 0xFFA16207),
        TemplateMed("Ezetimiba + Sinvastatina 10/20 mg", "Associação para o colesterol", "ezetimiba + sinvastatina", "21:00", 0xFF854D0E),
        TemplateMed("Fenofibrato 145 mg", "Triglicéridos elevados", "fenofibrato", "13:00", 0xFFB45309),

        // Hipertensão e Cardiovascular
        TemplateMed("Amlodipina 5 mg", "Pressão arterial e circulação", "amlodipina", "09:00", 0xFF8B5CF6),
        TemplateMed("Amlodipina 10 mg", "Hipertensão arterial", "amlodipina", "09:00", 0xFF7C3AED),
        TemplateMed("Ramipril 2.5 mg", "Proteção renal e cardíaca", "ramipril", "08:00", 0xFFEC4899),
        TemplateMed("Ramipril 5 mg", "Tensão arterial e coração", "ramipril", "08:00", 0xFFEC4899),
        TemplateMed("Ramipril 10 mg", "Hipertensão arterial", "ramipril", "08:00", 0xFFDB2777),
        TemplateMed("Ramipril + Hidroclorotiazida 5/25 mg", "Hipertensão arterial moderada", "ramipril + hidroclorotiazida", "08:00", 0xFFBE185D),
        TemplateMed("Enalapril 20 mg", "Tensão arterial", "enalapril", "08:00, 20:00", 0xFFF43F5E),
        TemplateMed("Losartan 50 mg", "Pressão arterial e rins", "losartan", "08:00", 0xFF9333EA),
        TemplateMed("Losartan 100 mg", "Hipertensão resistente", "losartan", "08:00", 0xFF7E22CE),
        TemplateMed("Losartan + Hidroclorotiazida 50/12.5 mg", "Controlo da pressão arterial", "losartan + hidroclorotiazida", "08:00", 0xFF6B21A8),
        TemplateMed("Valsartan 80 mg", "Coração e pressão arterial", "valsartan", "08:00", 0xFF4F46E5),
        TemplateMed("Valsartan 160 mg", "Insuficiência cardíaca e hipertensão", "valsartan", "08:00", 0xFF4338CA),
        TemplateMed("Candesartan 8 mg", "Controlo da tensão arterial", "candesartan", "08:00", 0xFF3730A3),
        TemplateMed("Candesartan 16 mg", "Pressão arterial", "candesartan", "08:00", 0xFF312E81),
        TemplateMed("Bisoprolol 2.5 mg", "Frequência cardíaca e arritmias", "bisoprolol", "08:00", 0xFF2563EB),
        TemplateMed("Bisoprolol 5 mg", "Proteção do coração e ritmo", "bisoprolol", "08:00", 0xFF1D4ED8),
        TemplateMed("Carvedilol 6.25 mg", "Insuficiência cardíaca", "carvedilol", "08:00, 20:00", 0xFF1E40AF),
        TemplateMed("Carvedilol 12.5 mg", "Coração e circulação", "carvedilol", "08:00, 20:00", 0xFF1E3A8A),
        TemplateMed("Nebivolol 5 mg", "Pressão arterial e circulação periférica", "nebivolol", "09:00", 0xFF1D4ED8),
        TemplateMed("Furosemida 40 mg", "Diurético para retenção de líquidos", "furosemida", "08:00", 0xFF0284C7),
        TemplateMed("Espironolactona 25 mg", "Diurético poupador de potássio", "espironolactona", "08:00", 0xFF0369A1),
        TemplateMed("Indapamida 1.5 mg", "Diurético para tensão arterial", "indapamida", "08:00", 0xFF0891B2),
        TemplateMed("Lercanidipina 10 mg", "Pressão arterial suave", "lercanidipina", "09:00", 0xFF7C3AED),

        // Diabetes e Metabolismo
        TemplateMed("Metformina 500 mg", "Controlo inicial da diabetes", "metformina", "08:00, 20:00", 0xFF059669),
        TemplateMed("Metformina 850 mg", "Controlo da glicemia / diabetes", "metformina", "08:00, 20:00", 0xFF059669),
        TemplateMed("Metformina 1000 mg", "Diabetes tipo 2", "metformina", "08:00, 20:00", 0xFF047857),
        TemplateMed("Gliclazida 30 mg", "Estimulação de insulina / diabetes", "gliclazida", "08:00", 0xFF10B981),
        TemplateMed("Gliclazida 60 mg", "Controlo da diabetes", "gliclazida", "08:00", 0xFF059669),
        TemplateMed("Sitagliptina 100 mg", "Equilíbrio dos açúcares no sangue", "sitagliptina", "08:00", 0xFF047857),
        TemplateMed("Sitagliptina + Metformina 50/1000 mg", "Associação para a diabetes", "sitagliptina + metformina", "08:00, 20:00", 0xFF065F46),
        TemplateMed("Dapagliflozina 10 mg", "Diabetes, rins e coração", "dapagliflozina", "08:00", 0xFF059669),
        TemplateMed("Empagliflozina 10 mg", "Proteção cardio-renal e diabetes", "empagliflozina", "08:00", 0xFF10B981),

        // Anticoagulantes e Antiagregantes
        TemplateMed("Ácido Acetilsalicílico 100 mg", "Proteção cardiovascular / antiagregante", "ácido acetilsalicílico", "13:00", 0xFFF97316),
        TemplateMed("Clopidogrel 75 mg", "Prevenção de trombos e AVC", "clopidogrel", "13:00", 0xFFEA580C),
        TemplateMed("Varfarina 5 mg", "Anticoagulante para fibrilação auricular", "varfarina", "18:00", 0xFFC2410C),
        TemplateMed("Rivaroxabano 20 mg", "Prevenção de coágulos e tromboses", "rivaroxabano", "20:00", 0xFFB45309),
        TemplateMed("Apixabano 5 mg", "Anticoagulante para proteção de AVC", "apixabano", "08:00, 20:00", 0xFF9A3412),

        // Saúde Mental, Sono e Sistema Nervoso
        TemplateMed("Sertralina 50 mg", "Humor e ansiedade", "sertralina", "09:00", 0xFF3B82F6),
        TemplateMed("Sertralina 100 mg", "Equilíbrio emocional", "sertralina", "09:00", 0xFF2563EB),
        TemplateMed("Escitalopram 10 mg", "Ansiedade e depressão", "escitalopram", "09:00", 0xFF1D4ED8),
        TemplateMed("Escitalopram 20 mg", "Estabilidade emocional", "escitalopram", "09:00", 0xFF1E40AF),
        TemplateMed("Fluoxetina 20 mg", "Estado de ânimo e energia", "fluoxetina", "08:30", 0xFF3B82F6),
        TemplateMed("Paroxetina 20 mg", "Ansiedade e pânico", "paroxetina", "09:00", 0xFF2563EB),
        TemplateMed("Venlafaxina 75 mg", "Depressão e dor crónica", "venlafaxina", "09:00", 0xFF4F46E5),
        TemplateMed("Duloxetina 30 mg", "Dores neuropáticas e humor", "duloxetina", "09:00", 0xFF4338CA),
        TemplateMed("Trazodona 100 mg", "Qualidade do sono e tranquilidade", "trazodona", "22:00", 0xFF6366F1),
        TemplateMed("Lorazepam 1 mg", "Ansiedade e tensão muscular", "lorazepam", "21:30", 0xFF8B5CF6),
        TemplateMed("Alprazolam 0.5 mg", "Crises de ansiedade", "alprazolam", "21:00", 0xFF7C3AED),
        TemplateMed("Diazepam 5 mg", "Relaxante muscular e calmante", "diazepam", "22:00", 0xFF6D28D9),
        TemplateMed("Oxazepam 15 mg", "Sedação suave e ansiedade", "oxazepam", "21:00", 0xFF5B21B6),
        TemplateMed("Pregabalina 75 mg", "Dores nos nervos e dormência", "pregabalina", "08:00, 20:00", 0xFF9333EA),
        TemplateMed("Gabapentina 300 mg", "Sensibilidade e dor neuropática", "gabapentina", "08:00, 20:00", 0xFF7E22CE),
        TemplateMed("Memantina 10 mg", "Memória e funções cognitivas", "memantina", "08:00", 0xFF3730A3),
        TemplateMed("Donepezilo 5 mg", "Apoio à memória e lucidez", "donepezilo", "22:00", 0xFF312E81),

        // Próstata e Urologia
        TemplateMed("Tansulosina 0.4 mg", "Fluxo urinário e próstata", "tansulosina", "08:30", 0xFF0891B2),
        TemplateMed("Silodosina 8 mg", "Saúde prostática e micção", "silodosina", "20:00", 0xFF0E7490),
        TemplateMed("Finasterida 5 mg", "Redução do volume da próstata", "finasterida", "09:00", 0xFF155E75),
        TemplateMed("Dutasterida 0.5 mg", "Conforto urinário masculino", "dutasterida", "09:00", 0xFF164E63),

        // Ácido Úrico e Osteoarticular
        TemplateMed("Alopurinol 100 mg", "Ácido úrico e gota", "alopurinol", "13:00", 0xFF10B981),
        TemplateMed("Alopurinol 300 mg", "Prevenção de crises de ácido úrico", "alopurinol", "13:00", 0xFF059669),
        TemplateMed("Colquicina 1 mg", "Crises agudas de gota", "colquicina", "08:00", 0xFF047857),
        TemplateMed("Alendronato 70 mg", "Fortalecimento dos ossos / osteoporose", "alendronato", "08:00", 0xFF84CC16),
        TemplateMed("Colecalciferol 25 000 UI", "Vitamina D para ossos e imunidade", "colecalciferol", "09:00", 0xFFEAB308),
        TemplateMed("Cálcio + Vitamina D3 600/400", "Densidade óssea diária", "cálcio + colecalciferol", "13:00", 0xFFCA8A04),

        // Alergias e Respiratório
        TemplateMed("Cetirizina 10 mg", "Alergias, rinite e comichão", "cetirizina", "20:00", 0xFF06B6D4),
        TemplateMed("Desloratadina 5 mg", "Alergias respiratórias e pele", "desloratadina", "20:00", 0xFF0891B2),
        TemplateMed("Bilastina 20 mg", "Alergia e espirros sem sonolência", "bilastina", "08:00", 0xFF0284C7),
        TemplateMed("Montelucaste 10 mg", "Asma e alergia brônquica", "montelucaste", "21:00", 0xFF0369A1),
        TemplateMed("Salbutamol 100 mcg/dose", "Alívio rápido da falta de ar", "salbutamol", "SOS", 0xFF2563EB),
        TemplateMed("Fluticasona + Salmeterol 250/50 mcg", "Inalador de prevenção respiratória", "fluticasona + salmeterol", "08:00, 20:00", 0xFF1D4ED8),

        // Tiroide e Oftalmologia
        TemplateMed("Levotiroxina 50 mcg", "Funcionamento da tiroide", "levotiroxina sódica", "07:30", 0xFF9333EA),
        TemplateMed("Levotiroxina 100 mcg", "Equilíbrio hormonal da tiroide", "levotiroxina sódica", "07:30", 0xFF7E22CE),
        TemplateMed("Latanoprost 50 mcg/ml", "Pressão ocular e glaucoma", "latanoprost", "21:00", 0xFF047857),
    )

    val CONSULTAS_ULSBE = listOf(
        Triple("Medicina Geral e Familiar", "ULSBE · Centro de Saúde de Barcelos", "Consulta"),
        Triple("Medicina Geral e Familiar", "ULSBE · Centro de Saúde de Esposende", "Consulta"),
        Triple("Consulta de Cardiologia", "ULSBE · Hospital Santa Maria Maior (Barcelos)", "Consulta"),
        Triple("Consulta de Oftalmologia", "ULSBE · Hospital Santa Maria Maior (Barcelos)", "Consulta"),
        Triple("Consulta de Ortopedia", "ULSBE · Hospital Santa Maria Maior (Barcelos)", "Consulta"),
        Triple("Consulta de Diabetes e Pé Diabético", "ULSBE · Unidade de Cuidados na Comunidade", "Consulta"),
        Triple("Consulta de Pneumologia", "ULSBE · Hospital Santa Maria Maior", "Consulta"),
        Triple("Enfermagem de Saúde Familiar", "ULSBE · USF Barcelos Norte", "Enfermagem"),
        Triple("Análises Clínicas de Sangue e Urina", "ULSBE · Laboratório Central", "Exame"),
        Triple("Eletrocardiograma (ECG) de Controlo", "ULSBE · Hospital Santa Maria Maior", "Exame"),
        Triple("Rastreio de Saúde Visual e Retinografia", "ULSBE · Centro de Diagnóstico", "Exame"),
        Triple("Consulta de Cirurgia Geral", "ULSBE · Hospital Santa Maria Maior", "Consulta"),
        Triple("Fisioterapia e Reabilitação Motora", "ULSBE · Serviço de Medicina Física", "Tratamento"),
        Triple("Consulta de Nutrição e Dietética Clínica", "ULSBE · Centro de Saúde", "Consulta"),
    )

    val ALERGIAS_PORTUGAL = listOf(
        "Sem alergias conhecidas",
        "Penicilina e derivados",
        "Aspirina / Anti-inflamatórios (AINEs)",
        "Iodo e contrastes radiológicos",
        "Sem alergias conhecidas",
        "Sulfa / Sulfamidas",
        "Sem alergias conhecidas",
        "Frutos secos e pólen",
        "Sem alergias conhecidas",
        "Paracetamol (intolerância leve)",
    )

    val NOTAS_PORTUGAL = listOf(
        Pair("Processo clínico integrado na plataforma digital da ULSBE.", "Sistema ULSBE"),
        Pair("Confirmar próxima toma da medicação e esclarecer dúvidas com o médico de família.", "Médico Assistente"),
        Pair("Utente orientado para medição semanal da pressão arterial no posto de enfermagem.", "Enf.ª Responsável"),
        Pair("Vigilância regular dos níveis de glicemia em jejum recomendada.", "Nutrição / Diabetes"),
        Pair("Receituário eletrónico renovado e ativo para dispensa na farmácia comunitária.", "Secretariado Clínico"),
    )

    val PRODUTOS_PORTUGAL = listOf(
        "Suplemento Vitamina C",
        "Magnésio + Vitamina B6",
        "Ómega 3 de Origem Marinha",
        "Chá de Camomila e Cidreira",
        "Complexo Multivitamínico Sénior",
        "Gel de Arnica para Articulações",
        "Probióticos para Equilíbrio Digestivo",
    )

    /**
     * Gera um [Perfil] completo, autêntico e aleatório para utentes portugueses da ULSBE.
     */
    fun gerarPerfilAleatorio(proximoId: String, random: Random = Random()): Perfil {
        val nomePar = NOMES_PORTUGAL[random.nextInt(NOMES_PORTUGAL.size)]
        val tiposSangue = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+")
        val tipoSangue = tiposSangue[random.nextInt(tiposSangue.size)]
        val alergias = ALERGIAS_PORTUGAL[random.nextInt(ALERGIAS_PORTUGAL.size)]

        val anoNasc = 1940 + random.nextInt(45)
        val mesNasc = (1 + random.nextInt(12)).toString().padStart(2, '0')
        val diaNasc = (1 + random.nextInt(28)).toString().padStart(2, '0')
        val dataNasc = "$diaNasc/$mesNasc/$anoNasc"

        val numUtente = "${100 + random.nextInt(900)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"
        val nif = "${200 + random.nextInt(99)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"
        val tel = "+351 91${random.nextInt(10)} ${100 + random.nextInt(900)} ${100 + random.nextInt(900)}"

        val agora = System.currentTimeMillis()
        val numMeds = 2 + random.nextInt(4) // 2 a 5 medicamentos
        val medsSorteados = MEDICAMENTOS_PORTUGAL.shuffled(random).take(numMeds)
        val medicamentos = medsSorteados.mapIndexed { idx, med ->
            val lab = LABORATORIOS_PORTUGAL[random.nextInt(LABORATORIOS_PORTUGAL.size)]
            Medicamento(
                id = agora + (idx * 25L),
                nome = med.nome,
                finalidade = med.finalidade,
                principioAtivo = med.principioAtivo,
                hora = med.hora,
                cor = med.cor,
                ativo = true,
                laboratorio = lab,
            )
        }

        val meses = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        val numCons = 2 + random.nextInt(2)
        val consSorteados = CONSULTAS_ULSBE.shuffled(random).take(numCons)
        val consultas = consSorteados.mapIndexed { idx, (titulo, local, tipo) ->
            val dia = (1 + random.nextInt(28)).toString().padStart(2, '0')
            val mes = meses[random.nextInt(meses.size)]
            val jejum = tipo == "Exame"
            Consulta(
                id = idx + 1,
                dia = dia,
                mes = mes,
                titulo = titulo,
                localizacao = local,
                tipo = tipo,
                jejum = jejum,
            )
        }

        val numNotas = 1 + random.nextInt(2)
        val notas = NOTAS_PORTUGAL.shuffled(random).take(numNotas).map { (txt, autor) ->
            Nota(txt, autor)
        }

        val numProds = 1 + random.nextInt(2)
        val outrosProdutos = PRODUTOS_PORTUGAL.shuffled(random).take(numProds)

        return Perfil(
            id = proximoId,
            nome = nomePar.first,
            nomeCurto = nomePar.second,
            relacao = "Utente",
            iniciais = nomePar.first.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "UT" },
            tipoSanguineo = tipoSangue,
            alergias = alergias,
            consultas = consultas,
            medicamentos = medicamentos,
            outrosProdutos = outrosProdutos,
            notas = notas,
            nif = nif,
            numeroUtente = numUtente,
            cartaoCidadao = "${10000000 + random.nextInt(89999999)} ${random.nextInt(9)} ZZ${random.nextInt(9)}",
            dataNascimento = dataNasc,
            telemovel = tel,
            email = "${nomePar.second.lowercase()}.${random.nextInt(99)}@exemplo.pt",
        )
    }
}
