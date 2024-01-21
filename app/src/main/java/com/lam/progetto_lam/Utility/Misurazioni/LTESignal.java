package com.lam.progetto_lam.Utility.Misurazioni;


public class LTESignal implements Misurazione {

    private static String tipo = "LTE";
    private Integer valore;


    public LTESignal(int valore){
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

    public void setValore(Integer val){
        this.valore = val;
    }
}

