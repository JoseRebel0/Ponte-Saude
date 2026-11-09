package pt.ulsbe.pontesaude.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

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
