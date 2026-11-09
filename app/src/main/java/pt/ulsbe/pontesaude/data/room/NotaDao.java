package pt.ulsbe.pontesaude.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotaDao {
    @Query("SELECT * FROM notas WHERE idPerfil = :idPerfil")
    List<NotaEntity> obterPorPerfil(String idPerfil);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserir(NotaEntity nota);

    @Query("DELETE FROM notas WHERE idPerfil = :idPerfil")
    void apagarPorPerfil(String idPerfil);
}
