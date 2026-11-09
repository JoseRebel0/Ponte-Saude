package pt.ulsbe.pontesaude.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pt.ulsbe.pontesaude.data.local.entity.RecompensaEntity;

/**
 * Interface DAO para catálogo de recompensas da gamificação em saúde.
 */
@Dao
public interface RecompensaDao {
    @Query("SELECT * FROM recompensas_seguranca")
    List<RecompensaEntity> obterTodas();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirVarias(List<RecompensaEntity> recompensas);

    @Query("DELETE FROM recompensas_seguranca")
    void limparTudo();

    @Query("SELECT COUNT(*) FROM recompensas_seguranca")
    int contar();
}
