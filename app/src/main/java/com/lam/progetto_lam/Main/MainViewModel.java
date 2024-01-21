package com.lam.progetto_lam.Main;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lam.progetto_lam.DataBaseR.Entità.MisuraEntita;
import com.lam.progetto_lam.DataBaseR.MioDatabase;
import com.lam.progetto_lam.Enums.Modalita;
import com.lam.progetto_lam.Utility.Misurazioni.Misurazione;
import com.lam.progetto_lam.Enums.OpzioniZoom;

import java.sql.Timestamp;
import java.util.List;

/**
 * Conteine dati e la connesione al DataBase
 */
public class MainViewModel extends ViewModel {
    private static final String TAG = "MainViewModel";
    // DataBaseR connection:
    private MioDatabase db;
    private LiveData<List<MisuraEntita>> cacheCoordinate;
    private Modalita statusModalita = Modalita.CELLULARE;

    // user location
    private Location posizioneUtente;
    private OpzioniZoom zoomUtente = OpzioniZoom.BASSO;
    private String area;

    // user parameters:
    private MutableLiveData<String> paramUtente;




    public void conessioneDb(Context context){
        db = MioDatabase.getInstance(context);
        Log.d(TAG, "conessioneDataBase: Connesione creata!");
    }

    public LiveData<List<MisuraEntita>> getMisurazioni(){
        if(cacheCoordinate ==null){
//            // 1) prendi area attuale 32TPQ

            if(area==null){
                cacheCoordinate = db.getFromAreaAndType("32TPQ", statusModalita);
            }else{
                cacheCoordinate = db.getFromAreaAndType(area, statusModalita);
            }
            // 2) prendi measure corrispondente al mode.
        }
        return cacheCoordinate;
    }

    

    public void setPosizioneUtente(Location posizione) {
        this.posizioneUtente = posizione;
    }

    public Location getPosizioneUtente(){
        return this.posizioneUtente;
    }
    
    public void resetCache(){
        if(area != null){
            cacheCoordinate = null;
        }
    }

    // salva sul DataBaseR nella posizione attuale
    public void salvaMisurazioniAttuali(Misurazione attualeM, String MGRS) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        MisuraEntita m = new MisuraEntita(attualeM.getTipo(), attualeM.getValore(), MGRS, timestamp);
        db.insertMeasure(m);
    }


    public OpzioniZoom getZoomUtente() {
        return zoomUtente;
    }

    public void setZoomUtente(OpzioniZoom zoomUtente) {
        this.zoomUtente = zoomUtente;
    }
    public void setArea(String area) {
        this.area = area;
    }

    public Modalita getModeAttuale() {
        return statusModalita;
    }
    public void setModeAttuale(Modalita modalitaAttaule) {
        this.statusModalita = modalitaAttaule;
    }


    public LiveData<String> getParamUtente() {
        if(paramUtente ==null){
            paramUtente = new MutableLiveData<>();
        }
        return paramUtente;
    }

    public void setParamUtente(String params){
        this.paramUtente.setValue(params);
    }
}
