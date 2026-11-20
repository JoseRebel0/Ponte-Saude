package pt.ulsbe.pontesaude.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Entidade Room que representa a tabela de consultas e exames médicos.
 * Chave primária composta por id e idPerfil para isolamento rigoroso entre perfis.
 */
@Entity(tableName = "consultas", primaryKeys = {"id", "idPerfil"})
public class ConsultaEntity {
    public int id;
    @NonNull
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

    public ConsultaEntity(int id, @NonNull String idPerfil, String dia, String mes, String titulo,
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
