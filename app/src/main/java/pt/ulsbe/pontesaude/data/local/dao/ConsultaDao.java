package pt.ulsbe.pontesaude.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pt.ulsbe.pontesaude.data.local.entity.ConsultaEntity;

/**
 * Interface DAO para consultas e exames médicos na base de dados SQLite local.
 */
@Dao
public interface ConsultaDao {
    @Query("SELECT * FROM consultas")
    List<ConsultaEntity> obterTodos();

    @Query("SELECT * FROM consultas WHERE id = :id LIMIT 1")
    ConsultaEntity obterPorId(int id);

    @Query("SELECT * FROM consultas WHERE id = :id AND idPerfil = :idPerfil LIMIT 1")
    ConsultaEntity obterPorIdEPerfil(int id, String idPerfil);

    @Query("SELECT * FROM consultas WHERE idPerfil = :idPerfil")
    List<ConsultaEntity> obterPorPerfil(String idPerfil);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirOuAtualizar(ConsultaEntity consulta);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirVarios(List<ConsultaEntity> consultas);

    @Query("DELETE FROM consultas WHERE id = :id")
    void apagarPorId(int id);

    @Query("DELETE FROM consultas WHERE idPerfil = :idPerfil")
    void apagarPorPerfil(String idPerfil);

    @Query("SELECT * FROM consultas WHERE pendenteSincronizacao = 1")
    List<ConsultaEntity> obterPendentes();
}
