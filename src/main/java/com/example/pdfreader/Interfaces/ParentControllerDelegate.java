package com.example.pdfreader.Interfaces;

import com.example.pdfreader.Controllers.ChildController;

public interface ParentControllerDelegate {
    void gotoPage(String page, ChildController initiatorController);
}
