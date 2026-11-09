package pt.ulsbe.pontesaude

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.ulsbe.pontesaude.model.Consulta
import pt.ulsbe.pontesaude.model.Medicamento
import pt.ulsbe.pontesaude.model.Perfil
import pt.ulsbe.pontesaude.ui.screens.formatarHoraParaFala
import pt.ulsbe.pontesaude.ui.screens.normalizarTexto
import pt.ulsbe.pontesaude.ui.screens.pesquisarGlossario

class TesteUnitarioExemplo {

    @Test
    fun testarConsistenciaPerfil() {
        val perfil = Perfil(
            id = "utente_1",
            nome = "Maria Inácia Silva",
            nomeCurto = "Maria",
            iniciais = "MS",
            consultas = listOf(Consulta(1, "15", "Nov", "Cardiologia", "ULSBE", "Consulta")),
            medicamentos = listOf(
                Medicamento(1L, "Ramipril 5mg", "Pressão arterial", "Ramipril", "08:00", 0xFF3E63DD, true),
                Medicamento(2L, "Ramipril Generis 5mg", "Pressão arterial", "Ramipril", "20:00", 0xFF3E63DD, true),
            ),
        )

        assertNotNull(perfil.nome)
        assertNotNull(perfil.tipoSanguineo)
        assertNotNull(perfil.alergias)
        assertTrue("O perfil deve ter consultas", perfil.consultas.isNotEmpty())
        assertTrue("O perfil deve ter medicamentos", perfil.medicamentos.isNotEmpty())
    }

    @Test
    fun testarFormatacaoHoraParaFala() {
        assertEquals("às 8 horas", formatarHoraParaFala("08:00"))
        assertEquals("às 8 e meia", formatarHoraParaFala("08:30"))
        assertEquals("às 14 horas e 15 minutos", formatarHoraParaFala("14:15"))
        assertEquals("às 21 horas", formatarHoraParaFala("21:00"))
    }

    @Test
    fun testarNormalizacaoTexto() {
        assertEquals("acucar e infecao", normalizarTexto("Açúcar e Infeção"))
        assertEquals("hipertensao", normalizarTexto("Hipertensão"))
    }

    @Test
    fun testarPesquisaGlossario() {
        val comAcento = pesquisarGlossario("hipertensão")
        val semAcento = pesquisarGlossario("hipertensao")
        assertEquals(comAcento, semAcento)
        assertTrue(comAcento.contains("artérias"))

        val jejum = pesquisarGlossario("jejum")
        assertTrue(jejum.contains("Não comer nem beber"))

        val desconhecido = pesquisarGlossario("termo que nao existe 123")
        assertTrue(desconhecido.contains("peça ao seu médico"))
    }

    @Test
    fun testarDeteccaoDuplicados() {
        val meds = listOf(
            Medicamento(1L, "Ramipril 5mg", "Pressão arterial", "Ramipril", "08:00", 0xFF3E63DD, true),
            Medicamento(2L, "Triatec 5mg", "Pressão arterial", "ramipril", "20:00", 0xFF3E63DD, true),
        )

        val duplicados = meds
            .filter { medicamento ->
                medicamento.principioAtivo.isNotBlank() &&
                    meds.count { it.principioAtivo.trim().equals(medicamento.principioAtivo.trim(), ignoreCase = true) } > 1
            }
            .map { it.principioAtivo.trim().lowercase() }
            .distinct()

        assertTrue("Deve detetar ramipril como duplicado", duplicados.contains("ramipril"))
    }
}
