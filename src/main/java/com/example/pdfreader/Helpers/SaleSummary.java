package com.example.pdfreader.Helpers;

import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.PosEntry;

import java.math.BigDecimal;
import java.util.Date;

public class SaleSummary {
    private BigDecimal sum = BigDecimal.ZERO;
    private final Date date;

    public SaleSummary(Date date){
        this.date = date;
    }
    public BigDecimal getSum(){
        return this.sum;
    }
    public void addValueToSum(BigDecimal value){
        this.sum = this.sum.add(value);
    }
    public  Date getDate(){
        return this.date;
    }
}
