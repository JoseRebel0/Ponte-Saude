package pt.ulsbe.pontesaude.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import pt.ulsbe.pontesaude.data.repository.AppContainer
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.model.SeparadorApp
import pt.ulsbe.pontesaude.ui.components.iconeSeparador
import pt.ulsbe.pontesaude.ui.screens.EcraSemInternetPrimeiraVez
import pt.ulsbe.pontesaude.ui.screens.apontamentos.EcraApontamentos
import pt.ulsbe.pontesaude.ui.screens.consultas.EcraConsultas
import pt.ulsbe.pontesaude.ui.screens.definicoes.EcraDefinicoes
import pt.ulsbe.pontesaude.ui.screens.inicio.EcraInicio
import pt.ulsbe.pontesaude.ui.screens.login.EcraLoginCMD
import pt.ulsbe.pontesaude.ui.screens.medicacao.EcraMedicacao
import pt.ulsbe.pontesaude.ui.screens.saude.EcraSaude
import pt.ulsbe.pontesaude.ui.theme.TemaPonteSaude
import pt.ulsbe.pontesaude.util.ServicosDispositivo

private val separadoresInferiores = listOf(
    SeparadorApp.INICIO to "Início",
    SeparadorApp.SAUDE to "Saúde",
    SeparadorApp.CONSULTAS to "Consultas",
    SeparadorApp.MEDICACAO to "Medicação",
    SeparadorApp.NOTAS to "Notas",
)

/**
 * Estrutura de navegação principal da aplicação Ponte Saúde.
 * Gere a barra inferior via Scaffold, a transição entre ecrãs e a navegação por botão de retrocesso.
 */
@Composable
fun NavegacaoApp(container: AppContainer, plataforma: ServicosDispositivo) {
    TemaPonteSaude(textoGrande = container.configuracao.textoGrande) {
        LaunchedEffect(Unit) {
            container.sincronizarAutomatico(plataforma.temConexaoInternet())
            container.iniciarEscutaTempoRealFirestore()
        }

        if (container.erroPrimeiraConexao) {
            EcraSemInternetPrimeiraVez(
                aoTentarNovamente = {
                    container.tentarNovamentePrimeiraConexao(plataforma.temConexaoInternet())
                },
            )
        } else if (!container.perfil.autenticado) {
            EcraLoginCMD(
                perfilRepository = container.perfil,
                sincronizador = container,
                aoAutenticar = { idPerfil ->
                    container.perfil.iniciarSessaoCMD(idPerfil)
                },
            )
        } else {
            var separador by remember { mutableStateOf(SeparadorApp.INICIO) }
            var moduloAberto by remember { mutableStateOf<String?>(null) }
            val perfil = container.perfilAtivoCompleto

            val navegarPara: (SeparadorApp) -> Unit = {
                moduloAberto = null
                separador = it
            }

            if (moduloAberto != null) {
                BackHandler { moduloAberto = null }
            } else if (separador != SeparadorApp.INICIO) {
                BackHandler { separador = SeparadorApp.INICIO }
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
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
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                ) {
                    Crossfade(
                        targetState = Triple(separador, moduloAberto, perfil.id),
                        modifier = Modifier.fillMaxSize(),
                        animationSpec = if (container.configuracao.reduzirMovimento) snap() else tween(220),
                        label = "ecra",
                    ) { (separadorAtual, idModulo) ->
                        when (separadorAtual) {
                            SeparadorApp.INICIO -> EcraInicio(
                                perfil = perfil,
                                perfilRepository = container.perfil,
                                configuracaoRepository = container.configuracao,
                                aoSincronizar = { container.sincronizarComFirestore() },
                                plataforma = plataforma,
                                navegarPara = navegarPara,
                            )
                            SeparadorApp.SAUDE -> EcraSaude(
                                perfil = perfil,
                                idModuloAberto = idModulo,
                                concluidos = container.saude.modulosConcluidos,
                                saudeRepository = container.saude,
                                perfilRepository = container.perfil,
                                configuracaoRepository = container.configuracao,
                                plataforma = plataforma,
                                aoAbrirModulo = { moduloAberto = it },
                                aoAlternarModulo = { idMod -> container.saude.alternarModulo(idMod, perfil.id) },
                                aoNavegar = navegarPara,
                            )
                            SeparadorApp.CONSULTAS -> EcraConsultas(
                                perfil = perfil,
                                consultaRepository = container.consultas,
                                perfilRepository = container.perfil,
                                configuracaoRepository = container.configuracao,
                                aoSincronizar = { container.sincronizarComFirestore() },
                                plataforma = plataforma,
                            )
                            SeparadorApp.MEDICACAO -> EcraMedicacao(
                                perfil = perfil,
                                medicacaoRepository = container.medicacao,
                                configuracaoRepository = container.configuracao,
                                notaRepository = container.notas,
                                perfilRepository = container.perfil,
                                aoSincronizar = { container.sincronizarComFirestore() },
                                plataforma = plataforma,
                            )
                            SeparadorApp.NOTAS -> EcraApontamentos(
                                perfil = perfil,
                                notaRepository = container.notas,
                                saudeRepository = container.saude,
                                perfilRepository = container.perfil,
                                configuracaoRepository = container.configuracao,
                                plataforma = plataforma,
                            )
                            SeparadorApp.DEFINICOES -> EcraDefinicoes(
                                perfil = perfil,
                                configuracaoRepository = container.configuracao,
                                perfilRepository = container.perfil,
                                medicacaoRepository = container.medicacao,
                                plataforma = plataforma,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Aliases para compatibilidade
@Composable
fun AplicacaoPonteSaude(container: AppContainer, plataforma: ServicosDispositivo) =
    NavegacaoApp(container, plataforma)

@Preview(name = "Aplicação Ponte Saúde Completa", showBackground = true)
@Composable
fun PreVisualizacaoNavegacaoApp() {
    val perfil = Perfil(
        id = "1",
        nome = "Maria Inácia Silva",
        nomeCurto = "Maria",
        iniciais = "MS",
        consultas = listOf(
            Consulta(
                id = 1,
                dia = "14",
                mes = "Nov",
                titulo = "Consulta de Cardiologia",
                localizacao = "Hospital de Santa Maria Maior - Barcelos",
                tipo = "Consulta Externa",
                jejum = false,
            ),
        ),
    )
    TemaPonteSaude(textoGrande = false) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    separadoresInferiores.forEach { (item, rotulo) ->
                        NavigationBarItem(
                            selected = item == SeparadorApp.INICIO,
                            onClick = {},
                            icon = { Icon(iconeSeparador(item), contentDescription = rotulo) },
                            label = { Text(rotulo) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                EcraInicio(
                    perfil = perfil,
                    plataforma = ServicosDispositivo(null),
                    navegarPara = {},
                )
            }
        }
    }
}
