package com.example.pdfreader.DAOs;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "db_error")
public class DBError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    @Column(name = "description", length = 2000)
    private String description;


    @Column(name = "timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    // Additional fields can be added as needed, such as:
    // - The class/method where the error occurred
    // - The type of error
    // - User information, if applicable

    // Constructors
    public DBError() {
    }

    public DBError(String errorMessage, Date timestamp) {
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // toString method for logging
    @Override
    public String toString() {
        return "DBError{" +
                "id=" + id +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}