package pt.ulsbe.pontesaude

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import pt.ulsbe.pontesaude.data.repository.AppContainer
import pt.ulsbe.pontesaude.ui.navigation.NavegacaoApp
import pt.ulsbe.pontesaude.util.ServicosDispositivo

/**
 * Atividade principal da aplicação Ponte Saúde.
 *
 * Responsável por:
 * - Solicitar permissões necessárias do sistema (calendário e notificações).
 * - Inicializar o repositório central e os serviços de dispositivo.
 */
class MainActivity : ComponentActivity() {
    private val pedirPermissoesApp = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        // Permissões processadas pelo utilizador
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissoesParaPedir = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissoesParaPedir.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val emFalta = permissoesParaPedir.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (emFalta.isNotEmpty()) {
            pedirPermissoesApp.launch(emFalta.toTypedArray())
        }

        setContent {
            val container = remember { AppContainer(applicationContext) }
            val plataforma = remember { ServicosDispositivo(this) }
            DisposableEffect(Unit) {
                onDispose { plataforma.fechar() }
            }
            NavegacaoApp(container, plataforma)
        }
    }
}
