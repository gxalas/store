package com.example.pdfreader.DAOs;

import com.example.pdfreader.Entities.ChildEntities.DocEntry;
import com.example.pdfreader.Entities.Main.Document;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.enums.StoreNames;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DocumentDAO {

    private SessionFactory sessionFactory;
    private DBErrorDAO dbErrorDAO;
    private static final Logger logger = Logger.getLogger(DocumentDAO.class.getName());


    public DocumentDAO(DBErrorDAO dbErrorDAO) {
        this.sessionFactory = HibernateUtil.getSessionFactory();
        this.dbErrorDAO = dbErrorDAO;
    }
    public DocumentDAO(){

    }

    public void saveDocument(Document document) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();


            // Use persist instead of save
            session.persist(document);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
    public List<DBError> saveDocuments(List<Document> documents) {
        List<DBError> errors = new ArrayList<>();

        for (Document document : documents) {
            Transaction transaction = null;
            Session session = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                transaction = session.beginTransaction();

                for (DocEntry entry : document.getEntries()) {
                    Product entryProduct = entry.getProduct();

                    // Check if a Product with the same master value already exists
                    String hql = "FROM Product WHERE invmaster = :invmaster";
                    Query<Product> query = session.createQuery(hql, Product.class);
                    query.setParameter("invmaster", entryProduct.getInvmaster());
                    Product managedProduct = query.uniqueResult();

                    if (managedProduct != null) {
                        // The product exists in the database, so use the existing managed entity
                        entry.setProduct(managedProduct);
                    } else {
                        if(entryProduct.getId()==null){
                            session.persist(entryProduct);
                        }
                        // The product does not exist in the database, so persist the new entity
                        // Note: It's important that entryProduct does not reference any unsaved transient instances.

                        // Now, entryProduct is managed and can be associated with entry
                        entry.setProduct(entryProduct);
                    }
                }

                session.persist(document);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                DBError dbError = new DBError();
                dbError.setErrorMessage(e.getMessage());
                dbError.setTimestamp(new Date());
                dbError.setDescription("@ importing Document - duplicate(?) \n " +
                        "docId: " + document.getDocumentId() + " " +
                        "\n " + document.getPath());
                errors.add(dbError);
                e.printStackTrace();
            } finally {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            }
        }

        return errors;
    }


    public Document getDocument(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Document.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*

     */

    public List<Document> getAllDocumentsWithEntriesAndProductsAndErrors() {
        int parallelismLevel =3;
        ForkJoinPool customThreadPool = new ForkJoinPool(parallelismLevel);
        try (Session session = sessionFactory.openSession()) {
            Query<Document> query = session.createQuery(
                    "SELECT DISTINCT d FROM Document d " +
                            "LEFT JOIN FETCH d.entries e " +
                            "LEFT JOIN FETCH e.product p " +
                            "LEFT JOIN FETCH d.errors", Document.class);

            query.setHint("javax.persistence.fetchgraph", session.getEntityGraph("document.entries.product"));



            List<Document> documents = customThreadPool.submit(()->query.getResultStream().parallel().
                    filter(document -> {Hibernate.initialize(document.getErrorList());return true;}).
                    collect(Collectors.toList())).get();
                    //query.getResultList());
            //documents.forEach(document -> Hibernate.initialize(document.getErrorList()));
            return documents;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    public List<Document> getAllDocuments() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Document> query = session.createQuery("FROM Document", Document.class);
            List<Document> documents = query.getResultList(); // Fetch documents sequentially
            // Process documents in parallel if needed, after fetching
            return documents; // Return the fetched documents
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    public List<Document> getDocumentsByYearAndStore(int year, String storeName) {
        logger.info("Opening session");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            logger.info("Session opened");
            String jpql = "SELECT d FROM Document d WHERE YEAR(d.date) = :year AND d.store = :storeName";
            Query<Document> query = session.createQuery(jpql, Document.class);
            query.setParameter("year", year);
            query.setParameter("storeName", StoreNames.getStoreByName(storeName));
            List<Document> docs = query.getResultList(); // Fetch documents sequentially
            logger.info("Query executed");
            return docs; // Return the fetched documents
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in getDocumentsByYearAndStore", e);
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            logger.info("Session closed");
        }
    }
    public List<String> getAllChecksums() {
        System.out.println("trying to get all checksums - - - - - - - - - ");
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("SELECT d.checksum FROM Document d", String.class).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void updateDocuments(List<Document> documents) {
        System.out.println("update triggered");
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            for (Document document : documents) {
                // Check if the document is already in the session
                if (!session.contains(document)) {
                    // If not, reattach it using merge
                    document = (Document) session.merge(document);
                }

                // Now it's safe to save
                session.saveOrUpdate(document);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public Date getMinimumDate() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT MIN(d.date) FROM Document d", Date.class)
                    .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Date getMaximumDate() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT MAX(d.date) FROM Document d", Date.class)
                    .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}