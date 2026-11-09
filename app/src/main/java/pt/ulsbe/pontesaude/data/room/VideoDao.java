package pt.ulsbe.pontesaude.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

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
