package com.example.pdfreader.Controllers.States;

import javafx.collections.ObservableList;

import java.util.Date;

public record FilterInvoicesState(ObservableList<String> stores, int type, int prom, Date start, Date end) {
}
