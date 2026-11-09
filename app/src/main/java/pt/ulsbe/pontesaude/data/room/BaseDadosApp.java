package pt.ulsbe.pontesaude.data.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {
        PerfilEntity.class,
        MedicamentoEntity.class,
        ConsultaEntity.class,
        NotaEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class BaseDadosApp extends RoomDatabase {

    public abstract PerfilDao perfilDao();
    public abstract MedicamentoDao medicamentoDao();
    public abstract ConsultaDao consultaDao();
    public abstract NotaDao notaDao();

    private static volatile BaseDadosApp INSTANCIA;

    public static BaseDadosApp obterInstancia(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (BaseDadosApp.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(
                        contexto.getApplicationContext(),
                        BaseDadosApp.class,
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
