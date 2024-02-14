package com.example.pdfreader.DTOs;

import com.example.pdfreader.Entities.ChildEntities.DocEntry;

public class DocEntryDTO {
    private DocEntry docEntry;
    private String suppliers;

    public DocEntryDTO(DocEntry docEntry, String suppliers) {
        this.docEntry = docEntry;
        this.suppliers = suppliers;
    }

    public void setDocEntry(DocEntry docEntry) {
        this.docEntry = docEntry;
    }

    public DocEntry getDocEntry() {
        return docEntry;
    }

    public String getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(String suppliers) {
        this.suppliers = suppliers;
    }
}
