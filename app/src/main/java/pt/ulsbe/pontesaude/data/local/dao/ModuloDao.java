package pt.ulsbe.pontesaude.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pt.ulsbe.pontesaude.data.local.entity.ModuloEntity;

/**
 * Interface DAO para módulos de literacia e segurança clínica.
 */
@Dao
public interface ModuloDao {
    @Query("SELECT * FROM modulos_saude")
    List<ModuloEntity> obterTodos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirVarios(List<ModuloEntity> modulos);

    @Query("DELETE FROM modulos_saude")
    void limparTudo();

    @Query("SELECT COUNT(*) FROM modulos_saude")
    int contar();
}
