package pt.ulsbe.pontesaude.data.local

import org.json.JSONArray
import org.json.JSONObject
import pt.ulsbe.pontesaude.data.local.entity.ConsultaEntity
import pt.ulsbe.pontesaude.data.local.entity.MedicamentoEntity
import pt.ulsbe.pontesaude.data.local.entity.ModuloEntity
import pt.ulsbe.pontesaude.data.local.entity.NotaEntity
import pt.ulsbe.pontesaude.data.local.entity.PerfilEntity
import pt.ulsbe.pontesaude.data.local.entity.RecompensaEntity
import pt.ulsbe.pontesaude.data.local.entity.VideoEntity
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.ModuloSaude
import pt.ulsbe.pontesaude.model.Nota
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.RecompensaSeguranca
import pt.ulsbe.pontesaude.model.TipoModulo
import pt.ulsbe.pontesaude.model.VideoBibliotecaSaude
import pt.ulsbe.pontesaude.model.VoucherResgatado

/**
 * Conversores e extensões de mapeamento entre Modelos de Domínio e Entidades Room SQLite.
 */

fun PerfilEntity.paraModelo(): Perfil =
    paraModelo(emptyList(), emptyList(), emptyList())

fun PerfilEntity.paraModelo(
    consultas: List<Consulta> = emptyList(),
    medicamentos: List<Medicamento> = emptyList(),
    notas: List<Nota> = emptyList(),
): Perfil {
    val produtos = outrosProdutosJson.deserializarListaString()
    val perfisCuidados = perfisCuidadosJson.deserializarListaString()
    val cuidadores = cuidadoresJson.deserializarListaString()
    return Perfil(
        id = id,
        nome = nome,
        nomeCurto = nomeCurto,
        relacao = relacao,
        iniciais = iniciais,
        tipoSanguineo = tipoSanguineo,
        alergias = alergias,
        consultas = consultas,
        medicamentos = medicamentos,
        outrosProdutos = produtos,
        notas = notas,
        nif = nif,
        numeroUtente = numeroUtente,
        cartaoCidadao = cartaoCidadao,
        dataNascimento = dataNascimento,
        telemovel = telemovel,
        email = email,
        perfisCuidadosIds = perfisCuidados,
        cuidadoresIds = cuidadores,
    )
}

fun Perfil.paraEntidade(
    pontos: Int,
    modulosConcluidos: List<String>,
    modulosOuvidos: List<String>,
    videos: List<String>,
    vouchers: List<VoucherResgatado>,
    pendente: Boolean = false,
): PerfilEntity {
    return PerfilEntity(
        id,
        nome,
        nomeCurto,
        relacao,
        iniciais,
        tipoSanguineo,
        alergias,
        nif,
        numeroUtente,
        cartaoCidadao,
        dataNascimento,
        telemovel,
        email,
        pontos,
        outrosProdutos.serializarListaString(),
        modulosConcluidos.serializarListaString(),
        modulosOuvidos.serializarListaString(),
        videos.serializarListaString(),
        vouchers.serializarVouchers(),
        perfisCuidadosIds.serializarListaString(),
        cuidadoresIds.serializarListaString(),
        pendente,
    )
}

fun MedicamentoEntity.paraModelo(): Medicamento {
    return Medicamento(
        id = id,
        nome = nome,
        finalidade = finalidade,
        principioAtivo = principioAtivo,
        hora = hora,
        cor = cor,
        ativo = ativo,
        fotografia = fotografia?.takeIf { it.isNotBlank() && it != "null" },
        laboratorio = laboratorio.orEmpty(),
    )
}

fun Medicamento.paraEntidade(idPerfil: String, pendente: Boolean = false): MedicamentoEntity {
    return MedicamentoEntity(
        id,
        idPerfil,
        nome,
        finalidade,
        principioAtivo,
        hora,
        cor,
        ativo,
        fotografia ?: "",
        laboratorio,
        pendente,
    )
}

fun ConsultaEntity.paraModelo(): Consulta {
    return Consulta(
        id = id,
        dia = dia,
        mes = mes,
        titulo = titulo,
        localizacao = localizacao,
        tipo = tipo,
        jejum = jejum,
    )
}

fun Consulta.paraEntidade(
    idPerfil: String,
    marcados: List<String> = emptyList(),
    custom: List<String> = emptyList(),
    pendente: Boolean = false,
): ConsultaEntity {
    return ConsultaEntity(
        id,
        idPerfil,
        dia,
        mes,
        titulo,
        localizacao,
        tipo,
        jejum,
        marcados.serializarListaString(),
        custom.serializarListaString(),
        pendente,
    )
}

fun NotaEntity.paraModelo(): Nota {
    return Nota(
        texto = texto,
        autor = autor,
    )
}

fun ModuloEntity.paraModelo(): ModuloSaude {
    val tipoEnum = runCatching { TipoModulo.valueOf(tipo) }.getOrDefault(TipoModulo.INFO)
    val conteudosLista = conteudosJson.deserializarListaString()
    return ModuloSaude(
        id = id,
        titulo = titulo,
        subtitulo = subtitulo,
        icone = icone,
        tipo = tipoEnum,
        conteudos = conteudosLista,
        fala = fala,
    )
}

fun ModuloSaude.paraEntidade(): ModuloEntity {
    return ModuloEntity(
        id,
        titulo,
        subtitulo,
        icone,
        tipo.name,
        conteudos.serializarListaString(),
        fala,
    )
}

fun VideoEntity.paraModelo(): VideoBibliotecaSaude {
    return VideoBibliotecaSaude(
        id = id,
        titulo = titulo,
        descricao = descricao,
        duracao = duracao,
        urlYoutube = urlYoutube,
        pontos = pontos,
    )
}

fun VideoBibliotecaSaude.paraEntidade(): VideoEntity {
    return VideoEntity(
        id,
        titulo,
        descricao,
        duracao,
        urlYoutube,
        pontos,
    )
}

fun RecompensaEntity.paraModelo(): RecompensaSeguranca {
    return RecompensaSeguranca(
        id = id,
        titulo = titulo,
        descricao = descricao,
        pontos = pontos,
        icone = icone,
        localLevantamento = localLevantamento,
    )
}

fun RecompensaSeguranca.paraEntidade(): RecompensaEntity {
    return RecompensaEntity(
        id,
        titulo,
        descricao,
        pontos,
        icone,
        localLevantamento,
    )
}

fun List<String>.serializarListaString(): String = JSONArray(this).toString()

fun String?.deserializarListaString(): List<String> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(this)
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())
}

fun List<VoucherResgatado>.serializarVouchers(): String {
    val arr = JSONArray()
    forEach { v ->
        val o = JSONObject().apply {
            put("id", v.id)
            put("idRecompensa", v.idRecompensa)
            put("titulo", v.titulo)
            put("codigo", v.codigo)
            put("dataResgate", v.dataResgate)
            put("localLevantamento", v.localLevantamento)
            put("pontosUsados", v.pontosUsados)
        }
        arr.put(o)
    }
    return arr.toString()
}

fun String?.deserializarVouchers(): List<VoucherResgatado> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(this)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            VoucherResgatado(
                id = o.getString("id"),
                idRecompensa = o.getString("idRecompensa"),
                titulo = o.getString("titulo"),
                codigo = o.getString("codigo"),
                dataResgate = o.getString("dataResgate"),
                localLevantamento = o.getString("localLevantamento"),
                pontosUsados = o.getInt("pontosUsados"),
            )
        }
    }.getOrDefault(emptyList())
}
