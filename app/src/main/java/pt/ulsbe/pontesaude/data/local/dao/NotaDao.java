package pt.ulsbe.pontesaude.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pt.ulsbe.pontesaude.data.local.entity.NotaEntity;

/**
 * Interface DAO para apontamentos e notas clínicas.
 */
@Dao
public interface NotaDao {
    @Query("SELECT * FROM notas WHERE idPerfil = :idPerfil")
    List<NotaEntity> obterPorPerfil(String idPerfil);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserir(NotaEntity nota);

    @Query("DELETE FROM notas WHERE idPerfil = :idPerfil")
    void apagarPorPerfil(String idPerfil);
}
