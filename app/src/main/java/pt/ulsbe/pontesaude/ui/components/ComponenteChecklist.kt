package pt.ulsbe.pontesaude.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ulsbe.pontesaude.data.repository.ConsultaRepository
import pt.ulsbe.pontesaude.model.CategoriaChecklist
import pt.ulsbe.pontesaude.model.ItemChecklist
import pt.ulsbe.pontesaude.ui.screens.consultas.ConsultasViewModel
import pt.ulsbe.pontesaude.util.ServicosDispositivo
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.Superficie2

private enum class FiltroChecklist(val rotulo: String) {
    TODAS("Todas"),
    PENDENTES("Pendentes"),
    CONCLUIDAS("Concluídas"),
}

/**
 * Componente moderno, profissional e altamente interativo para Listas de Verificação (Checklist).
 * Suporta barra de progresso animada, celebração de conclusão, filtros, adição de tarefas
 * pelo utente/cuidador, leitura por voz (TTS) e persistência de dados.
 */
@Composable
fun ChecklistInterativa(
    titulo: String,
    subtitulo: String? = null,
    itensPredefinidos: List<ItemChecklist>,
    chavePersistencia: String,
    idPerfil: String = "",
    viewModel: ConsultasViewModel? = null,
    consultaRepository: ConsultaRepository? = null,
    plataforma: ServicosDispositivo? = null,
    permiteAdicionar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val chaveEfetiva = if (idPerfil.isNotBlank()) "${chavePersistencia}_$idPerfil" else chavePersistencia
    val marcados = remember(chaveEfetiva) {
        val salvos = when {
            viewModel != null -> viewModel.obterChecklistSalva(chavePersistencia, idPerfil)
            idPerfil.isNotBlank() -> {
                val idCons = chavePersistencia.removePrefix("consulta_").toIntOrNull()
                if (idCons != null && consultaRepository != null) {
                    consultaRepository.obterChecklistSalva(idCons, idPerfil)
                } else {
                    consultaRepository?.obterChecklistSalva(chavePersistencia, idPerfil).orEmpty()
                }
            }
            else -> consultaRepository?.obterChecklistSalva(chavePersistencia).orEmpty()
        }
        mutableStateMapOf<String, Boolean>().apply {
            salvos.forEach { put(it, true) }
        }
    }

    val customItens = remember(chaveEfetiva) {
        val salvosCustom = when {
            viewModel != null -> viewModel.obterItensCustomChecklist(chavePersistencia, idPerfil)
            idPerfil.isNotBlank() -> {
                val idCons = chavePersistencia.removePrefix("consulta_").toIntOrNull()
                if (idCons != null && consultaRepository != null) {
                    consultaRepository.obterItensCustomChecklist(idCons, idPerfil)
                } else {
                    consultaRepository?.obterItensCustomChecklist(chavePersistencia, idPerfil).orEmpty()
                }
            }
            else -> consultaRepository?.obterItensCustomChecklist(chavePersistencia).orEmpty()
        }
        mutableStateListOf<ItemChecklist>().apply {
            salvosCustom.forEachIndexed { i, texto ->
                add(
                    ItemChecklist(
                        id = "custom_${chaveEfetiva}_$i",
                        texto = texto,
                        categoria = CategoriaChecklist.GERAL,
                        personalizado = true,
                    ),
                )
            }
        }
    }

    var aAdicionar by remember { mutableStateOf(false) }
    var textoNovoItem by remember { mutableStateOf("") }
    var categoriaNovoItem by remember { mutableStateOf(CategoriaChecklist.GERAL) }
    var filtroAtual by remember { mutableStateOf(FiltroChecklist.TODAS) }

    fun sincronizarMarcados() {
        if (viewModel != null) {
            viewModel.salvarChecklist(chavePersistencia, marcados.filter { it.value }.keys.toSet(), idPerfil)
        } else {
            val idCons = chavePersistencia.removePrefix("consulta_").toIntOrNull()
            if (idCons != null && idPerfil.isNotBlank() && consultaRepository != null) {
                consultaRepository.salvarChecklist(idCons, idPerfil, marcados.filter { it.value }.keys.toSet())
            } else {
                consultaRepository?.salvarChecklist(
                    chavePersistencia,
                    marcados.filter { it.value }.keys.toSet(),
                    idPerfil,
                )
            }
        }
    }

    fun sincronizarCustom() {
        if (viewModel != null) {
            viewModel.salvarItensCustomChecklist(chavePersistencia, customItens.map { it.texto }, idPerfil)
        } else {
            val idCons = chavePersistencia.removePrefix("consulta_").toIntOrNull()
            if (idCons != null && idPerfil.isNotBlank() && consultaRepository != null) {
                consultaRepository.salvarItensCustomChecklist(idCons, idPerfil, customItens.map { it.texto })
            } else {
                consultaRepository?.salvarItensCustomChecklist(
                    chavePersistencia,
                    customItens.map { it.texto },
                    idPerfil,
                )
            }
        }
    }

    val todosOsItens = (itensPredefinidos + customItens).map { item ->
        item.copy(concluido = marcados[item.id] == true)
    }

    val total = todosOsItens.size
    val concluidos = todosOsItens.count { it.concluido }
    val progressoBruto = if (total > 0) concluidos.toFloat() / total.toFloat() else 0f
    val progressoAnimado by animateFloatAsState(targetValue = progressoBruto, label = "progressoChecklist")

    val itensFiltrados = todosOsItens.filter { item ->
        when (filtroAtual) {
            FiltroChecklist.TODAS -> true
            FiltroChecklist.PENDENTES -> !item.concluido
            FiltroChecklist.CONCLUIDAS -> item.concluido
        }
    }

    CartaoApp(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // CABEÇALHO COM TÍTULO E ÍCONES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subtitulo != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = Atenuado,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (plataforma != null) {
                        IconButton(
                            onClick = {
                                val pendentes = todosOsItens.filter { !it.concluido }
                                val fala = if (pendentes.isEmpty()) {
                                    "$titulo. Excelente, todas as $total tarefas da checklist estão concluídas!"
                                } else {
                                    "$titulo. Faltam ${pendentes.size} itens: " + pendentes.joinToString(". ") { it.texto }
                                }
                                plataforma.falar(fala)
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Ouvir tarefas da checklist",
                                tint = MarcaMedio,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // BARRA DE PROGRESSO E PERCENTAGEM
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$concluidos de $total cumpridos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (concluidos == total && total > 0) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${(progressoAnimado * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (concluidos == total && total > 0) Color(0xFF047857) else MarcaMedio,
                    )
                }

                LinearProgressIndicator(
                    progress = { progressoAnimado },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (concluidos == total && total > 0) Color(0xFF10B981) else MarcaMedio,
                    trackColor = Superficie2,
                )
            }

            // MENSAGEM FESTIVA DE CONCLUSÃO
            AnimatedVisibility(
                visible = concluidos == total && total > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "🎉 Excelente! Checklist 100% concluída com segurança!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065F46),
                    )
                }
            }

            // FILTROS RÁPIDOS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FiltroChecklist.entries.forEach { filtro ->
                    val contagem = when (filtro) {
                        FiltroChecklist.TODAS -> total
                        FiltroChecklist.PENDENTES -> total - concluidos
                        FiltroChecklist.CONCLUIDAS -> concluidos
                    }
                    val selecionado = filtroAtual == filtro
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selecionado) MarcaMedio else Superficie2)
                            .clickable { filtroAtual = filtro }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "${filtro.rotulo} ($contagem)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            // LISTA DE ITENS INTERATIVOS
            if (itensFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (filtroAtual == FiltroChecklist.PENDENTES) "Não há tarefas pendentes!" else "Nenhum item nesta vista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itensFiltrados.forEach { item ->
                        LinhaItemChecklist(
                            item = item,
                            aoAlternar = {
                                marcados[item.id] = !(marcados[item.id] ?: false)
                                sincronizarMarcados()
                            },
                            aoApagar = if (item.personalizado) {
                                {
                                    customItens.removeAll { it.id == item.id }
                                    marcados.remove(item.id)
                                    sincronizarCustom()
                                    sincronizarMarcados()
                                }
                            } else null,
                        )
                    }
                }
            }

            // SECÇÃO DE ADICIONAR NOVO ITEM PERSONALIZADO
            if (permiteAdicionar) {
                if (!aAdicionar) {
                    TextButton(
                        onClick = { aAdicionar = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MarcaMedio),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adicionar Tarefa ou Lembrete", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Superficie2)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Novo Lembrete / Tarefa",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        OutlinedTextField(
                            value = textoNovoItem,
                            onValueChange = { textoNovoItem = it },
                            placeholder = { Text("Ex: Levar óculos de leitura, pedir receita...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // SELETOR DE CATEGORIA
                        Text("Categoria:", style = MaterialTheme.typography.labelSmall, color = Atenuado)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                CategoriaChecklist.GERAL,
                                CategoriaChecklist.DOCUMENTOS,
                                CategoriaChecklist.JEJUM,
                                CategoriaChecklist.MEDICACAO,
                            ).forEach { cat ->
                                val selecionada = categoriaNovoItem == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selecionada) Color(cat.corTexto) else Color(cat.corFundo))
                                        .clickable { categoriaNovoItem = cat }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = cat.rotulo,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selecionada) Color.White else Color(cat.corTexto),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = {
                                aAdicionar = false
                                textoNovoItem = ""
                            }) {
                                Text("Cancelar")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (textoNovoItem.isNotBlank()) {
                                        val novoId = "custom_${chavePersistencia}_${System.currentTimeMillis()}"
                                        customItens.add(
                                            ItemChecklist(
                                                id = novoId,
                                                texto = textoNovoItem.trim(),
                                                categoria = categoriaNovoItem,
                                                personalizado = true,
                                            ),
                                        )
                                        sincronizarCustom()
                                        textoNovoItem = ""
                                        aAdicionar = false
                                    }
                                },
                            ) {
                                Text("Adicionar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaItemChecklist(
    item: ItemChecklist,
    aoAlternar: () -> Unit,
    aoApagar: (() -> Unit)?,
) {
    val corFundoCard by animateColorAsState(
        targetValue = if (item.concluido) Color(0xFFF8FAFC) else Superficie2,
        label = "corFundoChecklistItem",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(corFundoCard)
            .clickable(onClick = aoAlternar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // CHECKBOX CIRCULAR ANIMADA
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (item.concluido) Color(0xFF10B981) else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (item.concluido) Color(0xFF10B981) else Atenuado.copy(alpha = 0.6f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.concluido) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // CONTEÚDO DO TEXTO E BADGE
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.concluido) Atenuado else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.concluido) TextDecoration.LineThrough else null,
                fontWeight = if (item.concluido) FontWeight.Normal else FontWeight.Medium,
            )

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // BADGE DE CATEGORIA
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(item.categoria.corFundo))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = item.categoria.rotulo,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(item.categoria.corTexto),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (item.obrigatorio && !item.concluido) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Importante",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (aoApagar != null) {
            IconButton(
                onClick = aoApagar,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Apagar tarefa",
                    tint = Atenuado,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
