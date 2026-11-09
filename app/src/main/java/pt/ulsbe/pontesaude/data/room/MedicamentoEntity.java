package pt.ulsbe.pontesaude.data.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medicamentos")
public class MedicamentoEntity {
    @PrimaryKey
    public long id;
    public String idPerfil;
    public String nome;
    public String finalidade;
    public String principioAtivo;
    public String hora;
    public long cor;
    public boolean ativo;
    public String fotografia;
    public boolean pendenteSincronizacao;

    public MedicamentoEntity(long id, String idPerfil, String nome, String finalidade,
                             String principioAtivo, String hora, long cor, boolean ativo,
                             String fotografia, boolean pendenteSincronizacao) {
        this.id = id;
        this.idPerfil = idPerfil;
        this.nome = nome;
        this.finalidade = finalidade;
        this.principioAtivo = principioAtivo;
        this.hora = hora;
        this.cor = cor;
        this.ativo = ativo;
        this.fotografia = fotografia;
        this.pendenteSincronizacao = pendenteSincronizacao;
    }
}
