package com.example.pdfreader.DAOs;

import com.example.pdfreader.DTOs.DocEntryDTO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocEntryDAO {

    private static SessionFactory sessionFactory;

    static {
        try {
            Configuration configuration = new Configuration().configure();
            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public void saveDocEntry(DocEntry docEntry) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(docEntry);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public DocEntry getDocEntry(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(DocEntry.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public List<DocEntryDTO> getDocEntriesByDocument(Document document) {
        System.out.println("trying to get all entries - - - - - - - - - ");
        try (Session session = sessionFactory.openSession()) {
            long startTime = System.nanoTime();
            Query<DocEntry> query = session.createQuery(
                    "FROM DocEntry WHERE document = :document", DocEntry.class);
            query.setParameter("document", document);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            System.out.println("Execution time: " + duration / 1_000_000.0 + " ms");

            List<DocEntry> docEntries = query.list();

            List<DocEntryDTO> docEntryDTOs = new ArrayList<>();
            SupplierDAO supplierDAO = new SupplierDAO();
            for (DocEntry entry : docEntries) {
                List<String> supplierNames = supplierDAO.getSupplierNamesForProduct(entry.getProduct());
                DocEntryDTO dto = new DocEntryDTO(entry, String.join(", ", supplierNames));
                docEntryDTOs.add(dto);
            }
            return docEntryDTOs;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<DocEntry> getDocEntriesByProduct(Product product) {
        List<DocEntry> docEntries = null;
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM DocEntry WHERE product = :product";
            Query<DocEntry> query = session.createQuery(hql, DocEntry.class);
            query.setParameter("product", product);
            docEntries = query.list();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception
        }
        return docEntries;
    }

    public List<DocEntry> getDocEntriesByProductMasterCode(String masterCode) {
        List<DocEntry> docEntries = null;
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM DocEntry DE WHERE DE.product.master = :masterCode";
            Query<DocEntry> query = session.createQuery(hql, DocEntry.class);
            query.setParameter("masterCode", masterCode);
            docEntries = query.list();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception
        }
        return docEntries;
    }

    // Additional CRUD methods can be added here as needed.
}
