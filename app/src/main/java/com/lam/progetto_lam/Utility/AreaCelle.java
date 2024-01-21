package com.lam.progetto_lam.Utility;

import com.google.android.gms.maps.model.PolygonOptions;

import java.util.Objects;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;

/**
 * Rappresenta una singola cella della griglia sulla mappa.
 * È costruito dal metodo getCelleArea(...) di CelleColoreUtility
 */
public class AreaCelle {
    private MGRS nordEst;
    private MGRS nordOvest;
    private MGRS sudEst;
    private MGRS sudOvest;
    private GridType gridType;
    private PolygonOptions polygonOptions;


    public AreaCelle(MGRS nordOvest, MGRS nordEst, MGRS sudEst, MGRS sudOvest, GridType gridType) {
        this.nordOvest = nordOvest;
        this.nordEst = nordEst;
        this.sudEst = sudEst;
        this.sudOvest = sudOvest;
        this.gridType = gridType;
    }

    public void colorPolygon(int color){
        this.polygonOptions.fillColor(color);
    }

    public MGRS getNordEst() {
        return nordEst;
    }

    public void setNordEst(MGRS nordEst) {
        this.nordEst = nordEst;
    }
    public MGRS getNordOvest() {
        return nordOvest;
    }

    public void setNordOvest(MGRS nordOvest) {
        this.nordOvest = nordOvest;
    }

    public MGRS getSudEst() {
        return sudEst;
    }

    public void setSudEst(MGRS sudEst) {
        this.sudEst = sudEst;
    }

    public MGRS getSudOvest() {
        return sudOvest;
    }

    public void setSudOvest(MGRS sudOvest) {
        this.sudOvest = sudOvest;
    }

    public GridType getGridType() {
        return gridType;
    }

    public void setGridType(GridType gridType) {
        this.gridType = gridType;
    }

    public PolygonOptions getPolygonOptions() {
        return polygonOptions;
    }

    public void setPolygonOptions(PolygonOptions polygonOptions) {
        this.polygonOptions = polygonOptions;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof AreaCelle)) return false;
        AreaCelle areaCelle = (AreaCelle) o;
        return getNordOvest().toString()
                .equals(areaCelle.getNordOvest().toString())
                && getNordEst().toString().equals(areaCelle.getNordEst().toString())
                && getSudEst().toString().equals(areaCelle.getSudEst().toString())
                && getSudOvest().toString().equals(areaCelle.getSudOvest().toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNordOvest().toString(), getNordEst().toString(), getSudEst().toString(), getSudOvest().toString());
    }
}
