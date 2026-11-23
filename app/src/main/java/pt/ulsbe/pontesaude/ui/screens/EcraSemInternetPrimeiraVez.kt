@file:Suppress("SpellCheckingInspection")

package pt.ulsbe.pontesaude.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ulsbe.pontesaude.ui.theme.Atenuado
import pt.ulsbe.pontesaude.ui.theme.MarcaMedio
import pt.ulsbe.pontesaude.ui.theme.Superficie2

/**
 * Ecrã exibido exclusivamente quando o utente abre a aplicação pela primeira vez
 * sem qualquer ligação à Internet para descarregar o seu perfil e conteúdos iniciais.
 */
@Composable
fun EcraSemInternetPrimeiraVez(
    aoTentarNovamente: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Ligação à Internet Necessária",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Para preparar a sua aplicação pela primeira vez e carregar o seu processo clínico, consultas e plano de medicação, é necessária uma ligação inicial à Internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Atenuado,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Superficie2),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Depois da primeira abertura:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MarcaMedio,
                    )
                    Text(
                        text = "• A aplicação guarda tudo no telemóvel (Room Database) e passa a funcionar perfeitamente sem Internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                    )
                    Text(
                        text = "• Quando voltar a ter ligação, as alterações são enviadas automaticamente para a nuvem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atenuado,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = aoTentarNovamente,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MarcaMedio),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Tentar Novamente",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
