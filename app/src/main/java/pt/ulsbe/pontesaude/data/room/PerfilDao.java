package pt.ulsbe.pontesaude.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PerfilDao {
    @Query("SELECT * FROM perfis")
    List<PerfilEntity> obterTodos();

    @Query("SELECT * FROM perfis WHERE id = :id")
    PerfilEntity obterPorId(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirOuAtualizar(PerfilEntity perfil);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirVarios(List<PerfilEntity> perfis);

    @Query("DELETE FROM perfis WHERE id = :id")
    void apagar(String id);

    @Query("SELECT COUNT(*) FROM perfis")
    int contar();
}
