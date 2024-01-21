package com.lam.progetto_lam.Utility.Misurazioni;

public class LivelloRumore implements Misurazione {
    private static String tipo = "dB";
    private Integer valore;


    public LivelloRumore(int valore){
        this.valore = valore;
    }
    @Override
    public Integer getValore() {
        return valore;
    }
    @Override
    public String getTipo() {
        return tipo;
    }
}
