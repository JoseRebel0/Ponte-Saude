package pt.ulsbe.pontesaude.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import pt.ulsbe.pontesaude.data.RepositorioApp
import pt.ulsbe.pontesaude.model.SeparadorApp
import pt.ulsbe.pontesaude.ui.components.iconeSeparador
import pt.ulsbe.pontesaude.ui.screens.EcraApontamentos
import pt.ulsbe.pontesaude.ui.screens.EcraConsultas
import pt.ulsbe.pontesaude.ui.screens.EcraDefinicoes
import pt.ulsbe.pontesaude.ui.screens.EcraInicio
import pt.ulsbe.pontesaude.ui.screens.EcraLoginCMD
import pt.ulsbe.pontesaude.ui.screens.EcraMedicacao
import pt.ulsbe.pontesaude.ui.screens.EcraSaude
import pt.ulsbe.pontesaude.ui.screens.EcraSemInternetPrimeiraVez
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude

private val separadoresInferiores = listOf(
    SeparadorApp.INICIO to "Início",
    SeparadorApp.SAUDE to "Saúde",
    SeparadorApp.CONSULTAS to "Consultas",
    SeparadorApp.MEDICACAO to "Medicação",
    SeparadorApp.NOTAS to "Notas",
)

/**
 * Estrutura de navegação principal da aplicação Ponte Saúde.
 * Gere a barra inferior, a transição entre ecrãs e a navegação por botão de retrocesso.
 */
@Composable
fun NavegacaoApp(repositorio: RepositorioApp, plataforma: ServicosDispositivo) {
    TemaPonteSaude(textoGrande = repositorio.textoGrande) {
        // Ao abrir a aplicação, sincroniza com o Firestore e liga a escuta em tempo real ("na hora")
        LaunchedEffect(Unit) {
            repositorio.sincronizarAutomatico(plataforma.temConexaoInternet())
            repositorio.iniciarEscutaTempoRealFirestore()
        }

        if (repositorio.erroPrimeiraConexao) {
            EcraSemInternetPrimeiraVez(
                aoTentarNovamente = {
                    repositorio.tentarNovamentePrimeiraConexao(plataforma.temConexaoInternet())
                },
            )
        } else if (!repositorio.autenticado) {
            EcraLoginCMD(
                repositorio = repositorio,
                aoAutenticar = { idPerfil ->
                    repositorio.iniciarSessaoCMD(idPerfil)
                },
            )
        } else {
            var separador by remember { mutableStateOf(SeparadorApp.INICIO) }
            var moduloAberto by remember { mutableStateOf<String?>(null) }
            val perfil = repositorio.perfilAtivo
            val navegarPara: (SeparadorApp) -> Unit = {
                moduloAberto = null
                separador = it
            }

            if (moduloAberto != null) {
                BackHandler { moduloAberto = null }
            } else if (separador != SeparadorApp.INICIO) {
                BackHandler { separador = SeparadorApp.INICIO }
            }

            Column(Modifier.fillMaxSize()) {
                Crossfade(
                    targetState = Triple(separador, moduloAberto, perfil.id),
                    modifier = Modifier.weight(1f),
                    animationSpec = if (repositorio.reduzirMovimento) snap() else tween(220),
                    label = "ecra",
                ) { (separadorAtual, idModulo) ->
                    when (separadorAtual) {
                        SeparadorApp.INICIO -> EcraInicio(perfil, repositorio, plataforma, navegarPara)
                        SeparadorApp.SAUDE -> EcraSaude(
                            perfil = perfil,
                            idModuloAberto = idModulo,
                            concluidos = repositorio.modulosConcluidos,
                            repositorio = repositorio,
                            plataforma = plataforma,
                            aoAbrirModulo = { moduloAberto = it },
                            aoAlternarModulo = repositorio::alternarModulo,
                            aoNavegar = navegarPara,
                        )
                        SeparadorApp.CONSULTAS -> EcraConsultas(perfil, repositorio, plataforma)
                        SeparadorApp.MEDICACAO -> EcraMedicacao(perfil, repositorio, plataforma)
                        SeparadorApp.NOTAS -> EcraApontamentos(perfil, repositorio, plataforma)
                        SeparadorApp.DEFINICOES -> EcraDefinicoes(perfil, repositorio, plataforma)
                    }
                }
                NavigationBar {
                    separadoresInferiores.forEach { (destino, rotulo) ->
                        NavigationBarItem(
                            selected = separador == destino,
                            onClick = { navegarPara(destino) },
                            icon = { Icon(iconeSeparador(destino), rotulo) },
                            label = { Text(rotulo) },
                        )
                    }
                }
            }
        }
    }
}

// Aliases para compatibilidade
@Composable
fun AplicacaoPonteSaude(repositorio: RepositorioApp, plataforma: ServicosDispositivo) =
    NavegacaoApp(repositorio, plataforma)

// ==========================================
// PREVIEWS COM COMPOSABLE NO ANDROID STUDIO
// ==========================================

@Preview(name = "Aplicação Ponte Saúde Completa", showBackground = true)
@Composable
fun PreVisualizacaoNavegacaoApp() {
    val contexto = LocalContext.current
    val repositorio = remember { RepositorioApp(contexto) }
    val plataforma = remember { ServicosDispositivo(null) }
    NavegacaoApp(repositorio = repositorio, plataforma = plataforma)
}
