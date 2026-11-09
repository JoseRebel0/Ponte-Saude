package pt.ulsbe.pontesaude.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MedicamentoDao {
    @Query("SELECT * FROM medicamentos WHERE idPerfil = :idPerfil")
    List<MedicamentoEntity> obterPorPerfil(String idPerfil);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirOuAtualizar(MedicamentoEntity med);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserirVarios(List<MedicamentoEntity> meds);

    @Query("DELETE FROM medicamentos WHERE id = :id")
    void apagarPorId(long id);

    @Query("DELETE FROM medicamentos WHERE idPerfil = :idPerfil")
    void apagarPorPerfil(String idPerfil);

    @Query("SELECT * FROM medicamentos WHERE pendenteSincronizacao = 1")
    List<MedicamentoEntity> obterPendentes();
}
