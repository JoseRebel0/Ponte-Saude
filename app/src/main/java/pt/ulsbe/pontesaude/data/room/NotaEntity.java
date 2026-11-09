package pt.ulsbe.pontesaude.data.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
