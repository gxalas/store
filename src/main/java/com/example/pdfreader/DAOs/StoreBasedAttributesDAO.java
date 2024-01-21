package com.example.pdfreader.DAOs;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.hibernate.SessionFactory;

import java.util.List;

public class StoreBasedAttributesDAO {
    private static SessionFactory sessionFactory;
    public StoreBasedAttributesDAO() {
        sessionFactory = HibernateUtil.getSessionFactory();
    }

    private EntityManager getEntityManager() {
        return sessionFactory.createEntityManager();
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

    // Method to fetch all StoreBasedAttributes with no Product set
    public List<StoreBasedAttributes> getStoreBasedAttributesWithNoProduct() {
        TypedQuery<StoreBasedAttributes> query = getEntityManager().createQuery(
                "SELECT s FROM StoreBasedAttributes s WHERE s.product IS NULL", StoreBasedAttributes.class);
        return query.getResultList();
    }
}
