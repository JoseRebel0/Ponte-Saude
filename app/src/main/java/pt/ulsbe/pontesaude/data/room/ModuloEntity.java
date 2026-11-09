package pt.ulsbe.pontesaude.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "modulos_saude")
public class ModuloEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String titulo;
    public String subtitulo;
    public String icone;
    public String tipo;
    public String conteudosJson;
    public String fala;

    public ModuloEntity(@NonNull String id, String titulo, String subtitulo, String icone,
                        String tipo, String conteudosJson, String fala) {
        this.id = id;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.icone = icone;
        this.tipo = tipo;
        this.conteudosJson = conteudosJson;
        this.fala = fala;
    }
}
