package pt.ulsbe.pontesaude.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room que representa os itens de recompensa da gamificação em cache local.
 */
@Entity(tableName = "recompensas_seguranca")
public class RecompensaEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String titulo;
    public String descricao;
    public int pontos;
    public String icone;
    public String localLevantamento;

    public RecompensaEntity(@NonNull String id, String titulo, String descricao, int pontos,
                            String icone, String localLevantamento) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.pontos = pontos;
        this.icone = icone;
        this.localLevantamento = localLevantamento;
    }
}
