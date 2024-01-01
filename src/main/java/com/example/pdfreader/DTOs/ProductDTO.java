package com.example.pdfreader.DTOs;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.enums.StoreNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductDTO {
    private Long id;
    private String code;
    private String master;
    private List <String> descriptions;
    private Long documentCount; // Add this field
    private List<StoreBasedAttributes> storeBasedAttributes = new ArrayList<>();

    public ProductDTO(){}
    public ProductDTO(Long id, String code, List<String> descriptions, String master, Long documentCount) {
        this.id = id;
        this.code = code;
        this.descriptions = descriptions;
        this.master = master;
        this.documentCount = documentCount;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentCount() {
        return documentCount;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public void setDescriptions(List<String> description) {
        this.descriptions = description;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public void setStoreBasedAttributes(List<StoreBasedAttributes> storeBasedAttributes) {
        this.storeBasedAttributes = storeBasedAttributes;
    }
    public List<StoreBasedAttributes> getStoreBasedAttributes() {
        return storeBasedAttributes;
    }
    public String getHopeCodeByStrore(StoreNames storeNames){
        if(storeBasedAttributes.isEmpty()||storeBasedAttributes.get(0)==null){
            return "";
        }
        for(StoreBasedAttributes sba: storeBasedAttributes){
            if(sba!=null){
                if (sba.getStore().compareTo(storeNames)==0){
                    return sba.getHope();
                }
            }

        }
        return "("+storeBasedAttributes.get(0).getHope()+")";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProductDTO that = (ProductDTO) obj;
        return Objects.equals(id, that.id) && // assuming 'id' is a unique identifier
                Objects.equals(master, that.master); // you can include more fields as needed
    }
}
