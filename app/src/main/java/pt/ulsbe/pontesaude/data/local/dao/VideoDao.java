package pt.ulsbe.pontesaude.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pt.ulsbe.pontesaude.data.local.entity.VideoEntity;

/**
 * Interface DAO para biblioteca de vídeos de literacia em saúde.
 */
@Dao
public interface VideoDao {
    @Query("SELECT * FROM videos_saude")
    List<VideoEntity> obterTodos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirVarios(List<VideoEntity> videos);

    @Query("DELETE FROM videos_saude")
    void limparTudo();

    @Query("SELECT COUNT(*) FROM videos_saude")
    int contar();
}
