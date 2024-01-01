package com.example.pdfreader;

import com.example.pdfreader.Entities.Document;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_errors")
public class DocumentErrors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "errors", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Document document;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_error_messages", joinColumns = @JoinColumn(name = "document_errors_id"))
    @Column(name = "error_message")
    private List<String> errorMessages = new ArrayList<>();

    // Constructors, getters, and setters
    public DocumentErrors(){
    }

    public DocumentErrors(String errorMessage) {
        // Default constructor
        errorMessages.add(errorMessage);
    }

    public DocumentErrors(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public List<String> getErrorMessages(){
        return this.errorMessages;
    }
    public void addErrorMessage(String message){
        errorMessages.add(message);
    }

    // Getter and setter methods for id, document, and errorMessages
}