package pt.ulsbe.pontesaude.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room que representa a tabela de consultas e exames médicos.
 */
@Entity(tableName = "consultas")
public class ConsultaEntity {
    @PrimaryKey
    public int id;
    public String idPerfil;
    public String dia;
    public String mes;
    public String titulo;
    public String localizacao;
    public String tipo;
    public boolean jejum;
    public String checklistMarcadosJson;
    public String checklistCustomJson;
    public boolean pendenteSincronizacao;

    public ConsultaEntity(int id, String idPerfil, String dia, String mes, String titulo,
                          String localizacao, String tipo, boolean jejum,
                          String checklistMarcadosJson, String checklistCustomJson,
                          boolean pendenteSincronizacao) {
        this.id = id;
        this.idPerfil = idPerfil;
        this.dia = dia;
        this.mes = mes;
        this.titulo = titulo;
        this.localizacao = localizacao;
        this.tipo = tipo;
        this.jejum = jejum;
        this.checklistMarcadosJson = checklistMarcadosJson;
        this.checklistCustomJson = checklistCustomJson;
        this.pendenteSincronizacao = pendenteSincronizacao;
    }
}
