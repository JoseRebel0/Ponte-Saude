package pt.ulsbe.pontesaude

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TesteInstrumentadoExemplo {
    @Test
    fun testarContextoAplicacao() {
        val contextoApp = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("pt.ulsbe.pontesaude", contextoApp.packageName)
    }
}
