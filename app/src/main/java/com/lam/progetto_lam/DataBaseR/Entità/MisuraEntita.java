package com.lam.progetto_lam.DataBaseR.Entità;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.sql.Timestamp;

/**
 * Entità che rappresenta le misurazioni nel DB
 */
@Entity(tableName = "MisuraEntita")
public class MisuraEntita implements Comparable<MisuraEntita>{

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private long measureID;
    private String tipo;
    private int valore;
    private String mgrsCoordinate;
    @TypeConverters(ConvertitoreTimestamp.class)
    private Timestamp dataCreazione;

    public MisuraEntita() {
    }

    public MisuraEntita(String tipo, int valore, String mgrs, Timestamp dataCreazione) {
        this.tipo = tipo;
        this.valore = valore;
        this.mgrsCoordinate = mgrs;
        this.dataCreazione = dataCreazione;
    }

    public long getMeasureID() {
        return measureID;
    }

    public void setMeasureID(long measureID) {
        this.measureID = measureID;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String type) {
        this.tipo = type;
    }

    public int getValore() {
        return valore;
    }

    public void setValore(int valore) {
        this.valore = valore;
    }

    public String getMgrsCoordinate() {
        return mgrsCoordinate;
    }

    public Timestamp getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Timestamp dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public void setMgrsCoordinate(String mgrsCoordinate) {
        this.mgrsCoordinate = mgrsCoordinate;
    }

    @Override
    public int compareTo(MisuraEntita misuraEntita) {
        return this.getDataCreazione().compareTo(misuraEntita.getDataCreazione());
    }
}