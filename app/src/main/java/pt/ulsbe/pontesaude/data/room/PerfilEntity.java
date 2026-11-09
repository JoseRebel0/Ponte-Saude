package pt.ulsbe.pontesaude.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "perfis")
public class PerfilEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String nome;
    public String nomeCurto;
    public String relacao;
    public String iniciais;
    public String tipoSanguineo;
    public String alergias;
    public String nif;
    public String numeroUtente;
    public String cartaoCidadao;
    public String dataNascimento;
    public String telemovel;
    public String email;
    public int pontosSeguranca;
    public String outrosProdutosJson;
    public String modulosConcluidosJson;
    public String modulosOuvidosJson;
    public String videosAssistidosJson;
    public String vouchersResgatadosJson;
    public String perfisCuidadosJson;
    public String cuidadoresJson;
    public boolean pendenteSincronizacao;

    public PerfilEntity(@NonNull String id, String nome, String nomeCurto, String relacao,
                        String iniciais, String tipoSanguineo, String alergias, String nif,
                        String numeroUtente, String cartaoCidadao, String dataNascimento,
                        String telemovel, String email, int pontosSeguranca,
                        String outrosProdutosJson, String modulosConcluidosJson,
                        String modulosOuvidosJson, String videosAssistidosJson,
                        String vouchersResgatadosJson, String perfisCuidadosJson,
                        String cuidadoresJson, boolean pendenteSincronizacao) {
        this.id = id;
        this.nome = nome;
        this.nomeCurto = nomeCurto;
        this.relacao = relacao;
        this.iniciais = iniciais;
        this.tipoSanguineo = tipoSanguineo;
        this.alergias = alergias;
        this.nif = nif;
        this.numeroUtente = numeroUtente;
        this.cartaoCidadao = cartaoCidadao;
        this.dataNascimento = dataNascimento;
        this.telemovel = telemovel;
        this.email = email;
        this.pontosSeguranca = pontosSeguranca;
        this.outrosProdutosJson = outrosProdutosJson;
        this.modulosConcluidosJson = modulosConcluidosJson;
        this.modulosOuvidosJson = modulosOuvidosJson;
        this.videosAssistidosJson = videosAssistidosJson;
        this.vouchersResgatadosJson = vouchersResgatadosJson;
        this.perfisCuidadosJson = perfisCuidadosJson;
        this.cuidadoresJson = cuidadoresJson;
        this.pendenteSincronizacao = pendenteSincronizacao;
    }
}
