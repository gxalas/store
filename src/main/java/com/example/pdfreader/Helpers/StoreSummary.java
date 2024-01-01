package com.example.pdfreader.Helpers;

import java.math.BigDecimal;

public class StoreSummary {
    private BigDecimal ABTimologia;
    private BigDecimal ABPistotika;
    private BigDecimal triTimologia;
    private BigDecimal triPistotika;
    private final String store;

    public StoreSummary(String store,BigDecimal abT,BigDecimal abP, BigDecimal tT,BigDecimal tP){
        this.store = store;
        ABTimologia = abT;
        ABPistotika = abP;
        triTimologia = tT;
        triPistotika = tP;
    }
    public String getStoreName() {
        return store;
    }
    public BigDecimal getABTimologia() {
        return ABTimologia;
    }
    public BigDecimal getABPistotika() {
        return ABPistotika;
    }
    public BigDecimal getTriTimologia(){
        return triTimologia;
    }
    public BigDecimal getTriPistotika() {
        return triPistotika;
    }
    public void setABTimologia(BigDecimal val){
        ABTimologia = val;
    }
    public void addABTimologia(BigDecimal val){
        ABTimologia = ABTimologia.add(val);
    }

    public void setABPistotika(BigDecimal val) {
        this.ABPistotika = val;
    }
    public void addABPistotika(BigDecimal val) {
        this.ABPistotika = ABPistotika.add(val);
    }

    public void setTriTimologia(BigDecimal val) {
        triTimologia = val;
    }
    public void addTriTimologia(BigDecimal val) {
        triTimologia = triTimologia.add(val);
    }

    public void setTriPistotika(BigDecimal triPistotika) {
        this.triPistotika = triPistotika;
    }
    public void addTriPistotika(BigDecimal val) {
        triPistotika = triPistotika.add(val);
    }
}
