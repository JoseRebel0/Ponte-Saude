package pt.ulsbe.pontesaude

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.ulsbe.pontesaude.data.DadosIniciais
import pt.ulsbe.pontesaude.ui.screens.formatarHoraParaFala
import pt.ulsbe.pontesaude.ui.screens.normalizarTexto
import pt.ulsbe.pontesaude.ui.screens.pesquisarGlossario

class TesteUnitarioExemplo {

    @Test
    fun testarConsistenciaDadosIniciais() {
        assertTrue("A lista de módulos não deve estar vazia", DadosIniciais.modulos.isNotEmpty())
        assertTrue("A lista de perfis não deve estar vazia", DadosIniciais.perfis.isNotEmpty())
        assertTrue("A lista de perguntas não deve estar vazia", DadosIniciais.perguntasMedico.isNotEmpty())

        DadosIniciais.perfis.forEach { perfil ->
            assertNotNull(perfil.nome)
            assertNotNull(perfil.tipoSanguineo)
            assertNotNull(perfil.alergias)
            assertTrue("Todos os perfis devem ter consultas", perfil.consultas.isNotEmpty())
            assertTrue("Todos os perfis devem ter medicamentos", perfil.medicamentos.isNotEmpty())
        }
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
        val perfilEu = DadosIniciais.perfis.first { it.id == "self" }
        val meds = perfilEu.medicamentos

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
