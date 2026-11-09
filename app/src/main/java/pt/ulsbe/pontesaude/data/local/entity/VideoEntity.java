package pt.ulsbe.pontesaude.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room que representa os vídeos educativos da biblioteca em cache local.
 */
@Entity(tableName = "videos_saude")
public class VideoEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String titulo;
    public String descricao;
    public String duracao;
    public String urlYoutube;
    public int pontos;

    public VideoEntity(@NonNull String id, String titulo, String descricao, String duracao,
                       String urlYoutube, int pontos) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.duracao = duracao;
        this.urlYoutube = urlYoutube;
        this.pontos = pontos;
    }
}
