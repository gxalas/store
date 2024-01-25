package com.example.pdfreader.DAOs;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.enums.StoreNames;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.*;

public class StoreBasedAttributesDAO {
    private static SessionFactory sessionFactory;


    private EntityManager getEntityManager() {
        return HibernateUtil.getSessionFactory().createEntityManager();
        //return sessionFactory.createEntityManager();
    }

    static {
        try {
            //Configuration configuration = new Configuration().configure();
            //sessionFactory = configuration.buildSessionFactory();
            sessionFactory = HibernateUtil.getSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    public StoreBasedAttributesDAO() {
    }

    public void saveSBAs(List<StoreBasedAttributes> sbas) {
        EntityManager entityManager = getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            for (StoreBasedAttributes sba : sbas) {
                entityManager.persist(sba);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }
    public void updateStoreBasedAttributes(List<StoreBasedAttributes> storeBasedAttributesList) {
        EntityManager entityManager = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            for (StoreBasedAttributes sba : storeBasedAttributesList) {
                if (sba.getId() != null && entityManager.find(StoreBasedAttributes.class, sba.getId()) != null) {
                    entityManager.merge(sba);
                } else {
                    // Handle the case where sba is not already in the database
                    // e.g., entityManager.persist(sba);
                }
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }


    // Method to fetch all StoreBasedAttributes
    public List<StoreBasedAttributes> getAllStoreBasedAttributes() {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        EntityManager entityManager = sessionFactory.createEntityManager();

        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();

            TypedQuery<StoreBasedAttributes> query = entityManager.createQuery(
                    "SELECT s FROM StoreBasedAttributes s", StoreBasedAttributes.class);
            //query.setFirstResult(0); // Start from the first element
            //query.setMaxResults(2);  // Limit to two elements

            List<StoreBasedAttributes> result = query.getResultList();
            transaction.commit(); // Commit the transaction
            return result;
        } catch (PersistenceException e) {
            System.out.println("we have an exception thrown");
            System.out.println(e.getMessage());
            // Handle or log the exception as appropriate
            throw e;
        } finally {
            entityManager.close();
        }
    }

    public Map<StoreNames, Map<String,StoreBasedAttributes>> getSbaMap() {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        EntityManager entityManager = sessionFactory.createEntityManager();

        try {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();

            TypedQuery<StoreBasedAttributes> query = entityManager.createQuery(
                    "SELECT s FROM StoreBasedAttributes s", StoreBasedAttributes.class);
            //query.setFirstResult(0); // Start from the first element
            //query.setMaxResults(2);  // Limit to two elements

            List<StoreBasedAttributes> result = query.getResultList();
            transaction.commit(); // Commit the transaction

            Map<StoreNames,Map<String,StoreBasedAttributes>> megaMap = new HashMap<>();
            result.forEach(sba->{
                if(sba.getStore()!=null){
                    if(megaMap.get(sba.getStore())!=null){
                        megaMap.get(sba.getStore()).put(sba.getMasterCode(),sba);
                    } else {
                        Map<String,StoreBasedAttributes> newmap = new HashMap<>();
                        newmap.put(sba.getMasterCode(),sba);
                        megaMap.put(sba.getStore(),newmap);
                    }
                }
            });

            return megaMap;
        } catch (PersistenceException e) {
            System.out.println("we have an exception thrown");
            System.out.println(e.getMessage());
            // Handle or log the exception as appropriate
            throw e;
        } finally {
            entityManager.close();
        }
    }

    // Method to fetch all StoreBasedAttributes with no Product set
    public List<StoreBasedAttributes> getStoreBasedAttributesWithNoProduct() {
        TypedQuery<StoreBasedAttributes> query = getEntityManager().createQuery(
                "SELECT s FROM StoreBasedAttributes s WHERE s.product IS NULL", StoreBasedAttributes.class);
        return query.getResultList();
    }
    public List<StoreBasedAttributes> getStoreBasedAttributesByStore(StoreNames storeName) {
        TypedQuery<StoreBasedAttributes> query = HibernateUtil.getEntityManagerFactory().createEntityManager().createQuery(
                "SELECT sba FROM StoreBasedAttributes sba WHERE sba.store = :storeName", StoreBasedAttributes.class);
        query.setParameter("storeName", storeName);

        return query.getResultList();
    }
    public List<StoreBasedAttributes> getStoreBasedAttributesByProducts(List<Product> products) {
        TypedQuery<StoreBasedAttributes> query = HibernateUtil.getEntityManagerFactory().createEntityManager().createQuery(
                "SELECT sba FROM StoreBasedAttributes sba WHERE sba.product IN :products", StoreBasedAttributes.class);
        query.setParameter("products", products);

        return query.getResultList();
    }
    public List<Product> getProductsByBarcodes(List<String> barcodes) {
        EntityManager entityManager = getEntityManager();
        try {
            TypedQuery<StoreBasedAttributes> query = entityManager.createQuery(
                    "SELECT sba FROM StoreBasedAttributes sba " +
                            "JOIN sba.barcodes barcode " +
                            "WHERE barcode IN :barcodes", StoreBasedAttributes.class);
            query.setParameter("barcodes", barcodes);

            List<StoreBasedAttributes> storeBasedAttributesList = query.getResultList();

            Set<Product> products = new HashSet<>();
            for (StoreBasedAttributes sba : storeBasedAttributesList) {
                if (sba.getProduct() != null) {
                    products.add(sba.getProduct());
                }
            }
            return new ArrayList<>(products);
        } finally {
            entityManager.close();
        }
    }
    public List<StoreBasedAttributes> getStoreBasedAttributesByProduct(Product product) {
        TypedQuery<StoreBasedAttributes> query = HibernateUtil.getEntityManagerFactory().createEntityManager().createQuery(
                "SELECT sba FROM StoreBasedAttributes sba WHERE sba.product = :product", StoreBasedAttributes.class);
        query.setParameter("product", product);

        return query.getResultList();
    }
}
