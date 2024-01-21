package com.lam.progetto_lam.DataBaseR;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.common.util.concurrent.ListenableFuture;
import com.lam.progetto_lam.DataBaseR.Entità.MisuraEntita;
import com.lam.progetto_lam.DataBaseR.dao.MisureDao;
import com.lam.progetto_lam.Enums.Modalita;

import java.util.List;


/**
 * Accesso a Room DataBase
 */
@Database(version=11, entities = {MisuraEntita.class}, exportSchema = false)
public abstract class MioDatabase extends RoomDatabase {
    private static final String DB_NAME = "my_db";

    public abstract MisureDao measureEntityDao();


    private static MioDatabase instance;

    public static synchronized MioDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, MioDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return instance;
    }

    public List<MisuraEntita> getAllMeasurements(String area){
        return measureEntityDao().getAll(area);
    }

    public LiveData<List<MisuraEntita>> getFromAreaAndType(String area, Modalita modalita){

        if (modalita.equals(Modalita.CELLULARE)){
            return measureEntityDao().getFromAreaAndType(area, "LTE");
        }else if (modalita.equals(Modalita.WIFI)){
            return measureEntityDao().getFromAreaAndType(area, "dBm");
        }else{ // DECIBEL
            return measureEntityDao().getFromAreaAndType(area, "dB");
        }

       }


    public ListenableFuture<Long> insertMeasure(MisuraEntita misuraEntita){
        return measureEntityDao().insertMeasureEntity(misuraEntita);
    }

}
