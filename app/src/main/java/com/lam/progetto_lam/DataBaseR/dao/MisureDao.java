package com.lam.progetto_lam.DataBaseR.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.util.concurrent.ListenableFuture;
import com.lam.progetto_lam.DataBaseR.Entità.MisuraEntita;

import java.util.List;


/**
 * Data Access Objects per le operazioni di MisuraEntita
 */
@Dao
public interface MisureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertMeasureEntity(MisuraEntita misuraEntita);

    /**
     * Ottieni tutte le misurazioni della macroarea create dopo ieri.
     * @param mgrsArea Macro area MGRS (primi 5 caratteri delle coordinate MGRS)
     * @return lista delle misurazioni
     */
    @Transaction
    @Query("SELECT * FROM MisuraEntita WHERE MisuraEntita.mgrsCoordinate LIKE '' || :mgrsArea || '%'" +
            "AND datetime(MisuraEntita.dataCreazione) > datetime('now','-1 day')")
    List<MisuraEntita> getAll(String mgrsArea);


    /**
     * Ottieni tutte le misure della macroarea, della tipologia indicata
     * @param mgrsArea Macro area MGRS (primi 5 caratteri delle coordinate MGRS)
     * @param tipo il tipo di misura (decibel, wifi o cellulare)
     * @return lista delle misurazioni
     */
    @Transaction
    @Query("SELECT * FROM MisuraEntita " +
            "WHERE MisuraEntita.mgrsCoordinate LIKE '' || :mgrsArea || '%'" +
            "AND MisuraEntita.tipo = :tipo ")
    LiveData<List<MisuraEntita>> getFromAreaAndType(String mgrsArea, String tipo);

}
