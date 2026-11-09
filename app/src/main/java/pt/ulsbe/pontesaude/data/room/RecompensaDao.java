package pt.ulsbe.pontesaude.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

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
