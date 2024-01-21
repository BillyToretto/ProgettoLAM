package com.lam.progetto_lam.Utility;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;

/**
 *
 * Metodi per generare la cella dal punto e per colorare le celle.
 *
 */
public class CelleColoreUtility {
    private static int DIVIDENDO;
    private DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    private DecimalFormat df;
    private String pattern;



    /**
     *
     * Impostazione dei parametri per l'approssimazione in base al tipo di griglia
     * @param tipo il tipo di griglia scelto dall'utente
     *
     */
    private void initializeGridFrZoom(GridType tipo){
        symbols.setDecimalSeparator('.');
        switch (tipo){
            case KILOMETER:
                DIVIDENDO = 1000;
                pattern = "#000";
                break;
            case HUNDRED_METER:
                DIVIDENDO = 100;
                pattern = "#00";
                break;
            case TEN_METER:
                DIVIDENDO = 10;
                pattern = "#0";
                break;
        }
        df = new DecimalFormat(pattern, symbols);
    }

    /**
     *
     * Calcola la cella della griglia che contiene la posizione data.
     * @param mgrsCoor la posizione della MGRS sulla mappa
     * @param mode il gridType necessario per la dimensione della cella
     * @return la cella della griglia (AreaCelle) che racchiude la posizione
     * @throws ParseException se i mgrsCoor sono formattati in modo errato.
     *
     */
    public AreaCelle getCelleArea(String mgrsCoor, GridType mode) throws ParseException {
        initializeGridFrZoom(mode);

        MGRS userMgrs = MGRS.parse(mgrsCoor);
        double norting = userMgrs.getNorthing();
        double easting = userMgrs.getEasting();
        // special treatment if it's ten meter:
        // if the location is exactly on the line, i move it to avoid errors in calulations.
        if(mode.equals(GridType.TEN_METER)){
            String nord = String.valueOf(userMgrs.getNorthing());
            String est = String.valueOf(userMgrs.getEasting());
            if(nord.charAt(nord.length()-1)=='0'){
                norting = norting + 1;
            }
            char last = est.charAt(est.length()-1);

            if(est.charAt(est.length()-1)=='0'){
                easting = easting + 1;
            }
        }
        // es. con nord:



        // calculate the 4 corners points rounding the values and approximating it
        double north = Math.ceil(norting/ DIVIDENDO)* DIVIDENDO;
        double south = Math.floor(norting/ DIVIDENDO)* DIVIDENDO;
        double east = Math.ceil(easting/ DIVIDENDO)* DIVIDENDO;
        double west = Math.floor(easting/ DIVIDENDO)* DIVIDENDO;
        long northCoor = Long.parseLong(df.format(north));
        long southCoor = Long.parseLong(df.format(south));
        long eastCoor = Long.parseLong(df.format(east));
        long westCoor = Long.parseLong(df.format(west));

        // create the 4 corners
        MGRS northEstMGRS = new MGRS(userMgrs.getZone(),userMgrs.getBand(),userMgrs.getColumn(),
                userMgrs.getRow(), eastCoor, northCoor);
        LatLng nortEastLatLng = new LatLng(northEstMGRS.toPoint().getLatitude(), northEstMGRS.toPoint().getLongitude());

        MGRS northWestMGRS = new MGRS(userMgrs.getZone(),userMgrs.getBand(),userMgrs.getColumn(),
                userMgrs.getRow(), westCoor, northCoor);
        LatLng nortWestLatLng = new LatLng(northWestMGRS.toPoint().getLatitude(), northWestMGRS.toPoint().getLongitude());

        MGRS southEstMGRS = new MGRS(userMgrs.getZone(),userMgrs.getBand(),userMgrs.getColumn(),
                userMgrs.getRow(), eastCoor, southCoor);
        LatLng soutEstLatLng = new LatLng(southEstMGRS.toPoint().getLatitude(), southEstMGRS.toPoint().getLongitude());

        MGRS southWestMGRS = new MGRS(userMgrs.getZone(),userMgrs.getBand(),userMgrs.getColumn(),
                userMgrs.getRow(), westCoor, southCoor);
        LatLng soutWestLatLng = new LatLng(southWestMGRS.toPoint().getLatitude(), southWestMGRS.toPoint().getLongitude());

        // create the polygon with the 4 corners
        PolygonOptions polygonOptions = new PolygonOptions()
                .add(nortWestLatLng, nortEastLatLng, soutEstLatLng, soutWestLatLng, nortWestLatLng);

        polygonOptions.fillColor(Color.argb(60,0,255,0));
        polygonOptions.strokeWidth(0);
        AreaCelle c = new AreaCelle(northWestMGRS, northEstMGRS, southEstMGRS, southWestMGRS, mode);
        c.setPolygonOptions(polygonOptions);
        return c;
    }

    /**
     *
     * Colorare la cella in base ai valori medi al suo interno
     * @param media ,delle misure nella cella
     * @param area l'area della cella da colorare
     * @return il poligono colorato
     *
     */
    public PolygonOptions coloreLTE(double media, AreaCelle area){
        if(media>=-80){ // excellent for wifi (green)
            return area.getPolygonOptions().fillColor(Color.argb(120,0,255,0));
        }else if (media > -100) {
            // media connectivity (red/yellow)
            return area.getPolygonOptions().fillColor(Color.argb(120,160,160,60));
        }else{
            // really bad (red)
            return area.getPolygonOptions().fillColor(Color.argb(120,255,0,0));
        }

    }
    /**
     *
     * Colorare la cella in base ai valori medi al suo interno
     * @param media ,media delle misure nella cella
     * @param area l'area della cella da colorare
     * @return il poligono colorato
     *
     */
    public PolygonOptions coloreWifi(double media, AreaCelle area){
        if(media>=-50){ // excellent for wifi (green)
            return area.getPolygonOptions().fillColor(Color.argb(120,0,255,0));
        }else if (media >= -70) {
            // media connectivity (red/yellow)
            return area.getPolygonOptions().fillColor(Color.argb(120,160,160,60));
        }else {
            return area.getPolygonOptions().fillColor(Color.argb(120,255,0,0));
        }

    }

    /**
     *
     * Colorare la cella in base ai valori medi al suo interno
     * @param media ,media delle misure nella cella
     * @param area l'area della cella da colorare
     * @return il poligono colorato
     *
     */
    public PolygonOptions coloreDecibel(double media, AreaCelle area){
        if(media>=-30){ // loud
            return area.getPolygonOptions().fillColor(Color.argb(120,255,0,0));
        }else if (media >= -50) {
            // media noise
            return area.getPolygonOptions().fillColor(Color.argb(120,255,160,60));
        }else {
            // quite
            return area.getPolygonOptions().fillColor(Color.argb(120,0,255,0));
        }

    }

}
