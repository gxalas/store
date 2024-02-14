package com.example.pdfreader.DAOs;

import com.example.pdfreader.Entities.Main.EntriesFile;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

public class EntriesFileDAO {
    private final SessionFactory sessionFactory;

    public EntriesFileDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public boolean isChecksumAlreadySaved(String checksum) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(e) FROM EntriesFile e WHERE e.checksum = :checksum", Long.class);
            query.setParameter("checksum", checksum);

            Long count = query.getSingleResult();
            return count != null && count > 0;
        }
    }

    public void saveEntriesFile(EntriesFile entriesFile) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(entriesFile);
            session.getTransaction().commit();
        }
    }

    public List<EntriesFile> getAllEntriesFiles() {
        try (Session session = sessionFactory.openSession()) {
            Query<EntriesFile> query = session.createQuery("SELECT e FROM EntriesFile e", EntriesFile.class);
            return query.getResultList();
        }
    }
    public List<String> getAllChecksums() {
        try (Session session = sessionFactory.openSession()) {
            String queryString = "SELECT e.checksum FROM EntriesFile e";
            Query<String> query = session.createQuery(queryString, String.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}