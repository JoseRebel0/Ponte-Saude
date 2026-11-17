package pt.ulsbe.pontesaude.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import pt.ulsbe.pontesaude.data.local.dao.ConsultaDao;
import pt.ulsbe.pontesaude.data.local.dao.MedicamentoDao;
import pt.ulsbe.pontesaude.data.local.dao.ModuloDao;
import pt.ulsbe.pontesaude.data.local.dao.NotaDao;
import pt.ulsbe.pontesaude.data.local.dao.PerfilDao;
import pt.ulsbe.pontesaude.data.local.dao.RecompensaDao;
import pt.ulsbe.pontesaude.data.local.dao.VideoDao;
import pt.ulsbe.pontesaude.data.local.entity.ConsultaEntity;
import pt.ulsbe.pontesaude.data.local.entity.MedicamentoEntity;
import pt.ulsbe.pontesaude.data.local.entity.ModuloEntity;
import pt.ulsbe.pontesaude.data.local.entity.NotaEntity;
import pt.ulsbe.pontesaude.data.local.entity.PerfilEntity;
import pt.ulsbe.pontesaude.data.local.entity.RecompensaEntity;
import pt.ulsbe.pontesaude.data.local.entity.VideoEntity;

/**
 * Base de Dados SQLite Local (Room Database) da aplicação Ponte Saúde.
 *
 * Responsável pela persistência offline de todos os dados clínicos:
 * perfis, medicamentos, consultas, notas e catálogo de saúde.
 */
@Database(
    entities = {
        PerfilEntity.class,
        MedicamentoEntity.class,
        ConsultaEntity.class,
        NotaEntity.class,
        ModuloEntity.class,
        VideoEntity.class,
        RecompensaEntity.class
    },
    version = 4,
    exportSchema = false
)
public abstract class BaseDadosLocal extends RoomDatabase {

    public abstract PerfilDao perfilDao();
    public abstract MedicamentoDao medicamentoDao();
    public abstract ConsultaDao consultaDao();
    public abstract NotaDao notaDao();
    public abstract ModuloDao moduloDao();
    public abstract VideoDao videoDao();
    public abstract RecompensaDao recompensaDao();

    private static volatile BaseDadosLocal INSTANCIA;

    public static BaseDadosLocal obterInstancia(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (BaseDadosLocal.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(
                        contexto.getApplicationContext(),
                        BaseDadosLocal.class,
                        "ponte_saude_room.db"
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCIA;
    }
}
