@file:Suppress("SpellCheckingInspection")

package pt.ulsbe.pontesaude.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio

/**
 * Barra lateral fluida com dimensão 100% estável e sincronização exata do fim da página.
 *
 * Características:
 * 1. A altura da pílula é pré-calculada de forma estável com base no total de cartões da página.
 *    NUNCA aumenta nem diminui durante o deslize (elimina totalmente o efeito de acordeão).
 * 2. Em páginas curtas ou com pouco scroll, a barra é substancial (45% a 55% da calha),
 *    deslizando de forma compassada e suave, sem disparar descontroladamente para o fundo.
 * 3. A barra só atinge o fundo exatamente quando o último elemento da página encosta ao limite inferior.
 * 4. Deslocamento 1:1 direto na GPU sem qualquer lag.
 *
 * @param estadoLista O [LazyListState] associado à LazyColumn.
 * @param modifier Modificador de layout e posicionamento.
 * @param corIndicador Cor da barra indicadora (padrão MarcaMedio).
 * @param largura Largura fina e discreta da barra.
 */
@Composable
fun BarraScrollLateralSuave(
    estadoLista: LazyListState,
    modifier: Modifier = Modifier,
    corIndicador: Color = MarcaMedio,
    largura: Dp = 3.5.dp,
) {
    val density = LocalDensity.current

    // Determina o tamanho estável da barra com base no número total de elementos da página.
    // Como depende estritamente de totalItemsCount, a dimensão é 100% FIXA durante o scroll.
    val racioEstavel by remember(estadoLista.layoutInfo.totalItemsCount) {
        derivedStateOf {
            val total = estadoLista.layoutInfo.totalItemsCount
            when {
                total <= 2 -> 0.65f
                total <= 4 -> 0.55f
                total <= 6 -> 0.45f
                total <= 8 -> 0.35f
                total <= 12 -> 0.26f
                total <= 20 -> 0.20f
                else -> 0.15f
            }
        }
    }

    // Cálculo da posição de 0f a 1f através da medição precisa de itens percorridos e restantes
    val dadosPosicao by remember(estadoLista) {
        derivedStateOf {
            val info = estadoLista.layoutInfo
            val total = info.totalItemsCount
            val itens = info.visibleItemsInfo
            val alturaViewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()

            if (total <= 1 || itens.isEmpty() || alturaViewport <= 0f) {
                return@derivedStateOf Pair(0f, false)
            }

            val primeiro = itens.first()
            val ultimo = itens.last()

            // 1. Distância percorrida a partir do topo
            val primeiroSize = primeiro.size.toFloat().coerceAtLeast(1f)
            val itemsAcima = primeiro.index.toFloat() + ((-primeiro.offset).toFloat().coerceAtLeast(0f) / primeiroSize)

            // 2. Distância que ainda resta percorrer até ao fundo da página
            val ultimoSize = ultimo.size.toFloat().coerceAtLeast(1f)
            val pixelsAbaixo = (ultimo.offset + ultimo.size - alturaViewport).coerceAtLeast(0f)
            val itensAbaixoDoUltimo = (total - 1 - ultimo.index).coerceAtLeast(0).toFloat()
            val itemsAbaixo = itensAbaixoDoUltimo + (pixelsAbaixo / ultimoSize)

            val percursoTotal = itemsAcima + itemsAbaixo
            val precisaScroll = percursoTotal > 0.08f

            if (!precisaScroll) {
                return@derivedStateOf Pair(0f, false)
            }

            // Atinge 1.0f apenas quando itemsAbaixo chega a 0f (no exato fim do conteúdo)
            val progresso = (itemsAcima / percursoTotal).coerceIn(0f, 1f)

            Pair(progresso, true)
        }
    }

    val progressoCalculado = dadosPosicao.first
    val podeFazerScroll = dadosPosicao.second

    // Micro-interpolação rápida (40ms) para máxima fluidez sem qualquer atraso percetível
    val progressoSuave by animateFloatAsState(
        targetValue = progressoCalculado,
        animationSpec = tween(durationMillis = 40, easing = LinearEasing),
        label = "progresso_scrollbar",
    )

    // Deteção de movimento para destacar a barra enquanto há toque
    var aFazerScroll by remember { mutableStateOf(false) }
    LaunchedEffect(estadoLista.isScrollInProgress) {
        if (estadoLista.isScrollInProgress) {
            aFazerScroll = true
        } else {
            delay(1200.milliseconds)
            aFazerScroll = false
        }
    }

    // Opacidade com transição suave
    val alphaAnimado by animateFloatAsState(
        targetValue = when {
            !podeFazerScroll -> 0f
            aFazerScroll -> 0.85f
            else -> 0.35f
        },
        animationSpec = tween(durationMillis = 180),
        label = "alpha_scrollbar",
    )

    if (podeFazerScroll || alphaAnimado > 0.01f) {
        var alturaPistaPx by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(largura)
                .onSizeChanged { size ->
                    if (size.height > 0) {
                        alturaPistaPx = size.height.toFloat()
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            // Calha de fundo ultra-discreta
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(largura)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.04f * alphaAnimado.coerceAtLeast(0.35f))),
            )

            // Dimensão do cursor 100% FIXA para a página atual (não varia durante o scroll)
            val alturaThumbPx = (alturaPistaPx * racioEstavel).coerceAtLeast(with(density) { 48.dp.toPx() })
            val alturaThumbDp = with(density) { alturaThumbPx.toDp() }
            val percursoDisponivelPx = (alturaPistaPx - alturaThumbPx).coerceAtLeast(0f)

            // Deslocamento direto no RenderNode (zero lag, resposta instantânea 1:1)
            Box(
                modifier = Modifier
                    .width(largura)
                    .height(alturaThumbDp)
                    .graphicsLayer {
                        translationY = percursoDisponivelPx * progressoSuave
                        alpha = alphaAnimado
                    }
                    .clip(CircleShape)
                    .background(corIndicador),
            )
        }
    }
}
