package pt.ulsbe.pontesaude.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room que representa apontamentos e notas clínicas do utente ou cuidador.
 */
@Entity(tableName = "notas")
public class NotaEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String idPerfil;
    public String texto;
    public String autor;
    public boolean pendenteSincronizacao;

    public NotaEntity(long id, String idPerfil, String texto, String autor, boolean pendenteSincronizacao) {
        this.id = id;
        this.idPerfil = idPerfil;
        this.texto = texto;
        this.autor = autor;
        this.pendenteSincronizacao = pendenteSincronizacao;
    }
}
