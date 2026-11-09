package pt.ulsbe.pontesaude.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pt.ulsbe.pontesaude.data.local.entity.PerfilEntity;

/**
 * Interface DAO para gestão de perfis clínicos (utente e cuidadores).
 */
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
