package com.example.pdfreader.DAOs;

import com.example.pdfreader.DTOs.DocEntryDTO;
import com.example.pdfreader.DocEntry;
import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class DocEntryDAO {
    private static final Logger logger = LoggerFactory.getLogger(DocEntryDAO.class);

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
        logger.info("Starting to get all entries for document: {}", document);
        List<DocEntryDTO> docEntryDTOs = new ArrayList<>();

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            logger.info("Session opened for document: {}", document);

            // Fetch DocEntries
            Query<DocEntry> query = session.createQuery(
                    "SELECT e FROM DocEntry e LEFT JOIN FETCH e.product WHERE e.document = :document", DocEntry.class);
            query.setParameter("document", document);
            List<DocEntry> docEntries = query.list();

            // Fetch Supplier Names within the same session
            Map<Long, String> supplierNamesMap = getSupplierNamesForProducts(docEntries, session);

            // Map to DTOs
            docEntryDTOs = docEntries.stream()
                    .map(entry -> new DocEntryDTO(entry, supplierNamesMap.get(entry.getProduct().getId())))
                    .collect(Collectors.toList());

            transaction.commit();
            logger.info("Transaction committed and session closing for document: {}", document);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            logger.error("Exception in getDocEntriesByDocument", e);
        }
        return docEntryDTOs;
    }

    private Map<Long, String> getSupplierNamesForProducts(List<DocEntry> docEntries, Session session) {
        logger.info("Fetching supplier names for products within the same session");
        Map<Long, String> supplierNamesMap = new HashMap<>();

        for (DocEntry entry : docEntries) {
            Product product = entry.getProduct();
            Query<SupplierProductRelation> query = session.createQuery(
                    "FROM SupplierProductRelation WHERE product.id = :productId", SupplierProductRelation.class);
            query.setParameter("productId", product.getId());
            List<SupplierProductRelation> relations = query.list();

            String supplierNames = relations.stream()
                    .map(relation -> relation.getSupplier().getName())
                    .collect(Collectors.joining(", "));
            supplierNamesMap.put(product.getId(), supplierNames);
        }

        return supplierNamesMap;
    }


    /*
    public List<DocEntryDTO> getDocEntriesByDocument(Document document) {
        logger.info("Starting to get all entries for document: {}", document);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            logger.info("Session opened for document: {}", document);

            Query<DocEntry> query = session.createQuery(
                    "FROM DocEntry WHERE document = :document", DocEntry.class);
            query.setParameter("document", document);
            List<DocEntry> docEntries = query.list();

            List<DocEntryDTO> docEntryDTOs = new ArrayList<>();
            SupplierDAO supplierDAO = new SupplierDAO();

            for (DocEntry entry : docEntries) {
                try {
                    List<String> supplierNames = supplierDAO.getSupplierNamesForProduct(entry.getProduct());
                    DocEntryDTO dto = new DocEntryDTO(entry, String.join(", ", supplierNames));
                    docEntryDTOs.add(dto);
                } catch (Exception e) {
                    logger.error("Error processing DocEntry: {}", entry, e);
                }
            }

            logger.info("Session closing for document: {}", document);
            return docEntryDTOs;
        } catch (HibernateException e) {
            logger.error("Hibernate exception in getDocEntriesByDocument", e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Exception in getDocEntriesByDocument", e);
            return Collections.emptyList();
        }
    }
     */


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
