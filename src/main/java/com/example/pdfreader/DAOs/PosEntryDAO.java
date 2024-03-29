package com.example.pdfreader.DAOs;
import java.util.*;
import java.util.logging.Logger;

import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.enums.StoreNames;
import jakarta.persistence.*;
import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;

public class PosEntryDAO {

    //private  SessionFactory sessionFactory;
    private static final int BATCH_SIZE = 50; // Adjust this size based on your requirements
    private static final Logger logger = Logger.getLogger(PosEntryDAO.class.getName());
    private static final Logger LOGGER2 = Logger.getLogger(PosEntryDAO.class.getName());


    /*
    static {
        try {
            System.out.println("& & & & initng session");
            Configuration configuration = new Configuration().configure();
            sessionFactory = configuration.buildSessionFactory();
            //sessionFactory = HibernateUtil.getSessionFactory();
        } catch (Throwable ex) {
            System.out.println(" & & & & "+ex.getMessage());
            throw new ExceptionInInitializerError(ex);
        }
    }
     */

    // Method to fetch all shaCode values
    public List<String> findAllShaCodes() {
        try (EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager()) {
            TypedQuery<String> query = em.createQuery(
                    "SELECT pe.shaCode FROM PosEntry pe", String.class);
            return query.getResultList();
        }
        // Ensure the EntityManager is closed
    }

    /*
    public void savePosEntries(List<PosEntry> posEntries) {
        Session session = HibernateUtil.getSessionFactory().openSession();

        for (PosEntry posEntry : posEntries) {
            Transaction tx = null;

            try {
                tx = session.beginTransaction();

                // Check if the Product of the PosEntry is transient and save it first
                StoreBasedAttributes sba = posEntry.getSba();
                if (sba != null && sba.getId() == null) {
                    session.persist(sba);
                    // Now that the product is persisted, Hibernate can manage the cascade
                }

                if (posEntry.getId() == null) {
                    session.persist(posEntry);
                } else {
                    session.merge(posEntry);
                }

                tx.commit();
            } catch (RuntimeException e) {
                if (tx != null) {
                    tx.rollback();
                }

            } finally {
                // Optionally clear the session to handle memory efficiently
                session.clear();
            }
        }

        session.close(); // Close the session after processing all entries
    }
     */

    public void savePosEntries(List<PosEntry> posEntries) {
        Session session = HibernateUtil.getSessionFactory().openSession();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR,2021);
        cal.set(Calendar.MONTH,11);
        cal.set(Calendar.DATE,21);
        cal.set(Calendar.HOUR,12);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        Date testDate = cal.getTime();

        Calendar calD = Calendar.getInstance();
        calD.set(Calendar.YEAR,2022);
        calD.set(Calendar.MONTH,6);
        calD.set(Calendar.DATE,4);
        calD.set(Calendar.HOUR,12);
        calD.set(Calendar.MINUTE,0);
        calD.set(Calendar.SECOND,0);
        calD.set(Calendar.MILLISECOND,0);
        Date testDateD = calD.getTime();


        for (PosEntry posEntry : posEntries) {
            Transaction tx = null;

            if(posEntry.getStoreName().compareTo(StoreNames.PERISTERI)==0&& posEntry.getDate().compareTo(testDate)==0){
                System.out.println("trying to save "+posEntry.getDescription()+" "+posEntry.getMoney()+" "+posEntry.getStoreName());
            }

            if(posEntry.getStoreName().compareTo(StoreNames.DRAPETSONA)==0&& posEntry.getDate().compareTo(testDateD)==0){
                System.out.println("trying to save "+posEntry.getDescription()+" "+posEntry.getMoney()+" "+posEntry.getStoreName());
            }

            try {
                tx = session.beginTransaction();

                if (posEntry.getId() == null) {
                    session.persist(posEntry);
                } else {
                    session.merge(posEntry);
                }

                tx.commit();
            } catch (RuntimeException e) {
                if (tx != null) {
                    tx.rollback();
                }

            } finally {
                // Optionally clear the session to handle memory efficiently
                session.clear();
            }
        }

        session.close(); // Close the session after processing all entries
    }


    public void savePosEntriesNew(List<PosEntry> posEntries) {
        Session session = null;
        Transaction tx = null;

        for (PosEntry posEntry : posEntries) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();

                if (posEntry.getSba() != null && (posEntry.getSba().getId() == null || session.get(StoreBasedAttributes.class, posEntry.getSba().getId()) == null)) {
                    session.saveOrUpdate(posEntry.getSba());
                }

                session.saveOrUpdate(posEntry);
                tx.commit();
            } catch (HibernateException e) {
                if (tx != null) tx.rollback();

                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof ConstraintViolationException) {
                        System.out.println("Skipping due to unique constraint violation: " + posEntry.getShaCode());
                        break; // Found the specific exception we were looking for
                    }
                    cause = cause.getCause();
                }

                if (cause == null) { // If we didn't find our specific exception in the cause chain
                    System.out.println("An error occurred, not related to unique constraint violation: " + e.getMessage());
                }
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    /*
    public void savePosEntriesNew(List<PosEntry> posEntries) {
        Session session = HibernateUtil.getSessionFactory().openSession();


        try {
            // Start a transaction
            Transaction tx = session.beginTransaction();

            for (PosEntry posEntry : posEntries) {
                // Check if StoreBasedAttributes instance is transient
                if (posEntry.getSba() != null && (posEntry.getSba().getId() == null || session.get(StoreBasedAttributes.class, posEntry.getSba().getId()) == null)) {
                    // Save the transient StoreBasedAttributes instance
                    session.saveOrUpdate(posEntry.getSba());
                }

                // Now save or merge the PosEntry instance
                if (posEntry.getId() == null) {
                    session.persist(posEntry);
                } else {
                    session.merge(posEntry);
                }
            }

            // Commit the transaction
            tx.commit();
        } catch (ConstraintViolationException e) {
            // Catch the ConstraintViolationException and skip the object
            if (tx != null) tx.rollback();
            System.out.println("Skipping entry due to constraint violation: " + posEntry.getShaCode());
            // Optionally log the exception or handle it as needed

        } catch (RuntimeException e) {
            System.err.println("Transaction failed! " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }
    */


    public Map<StoreNames, Set<Date>> getDatesByStoreName() {
        Map<StoreNames, Set<Date>> storeDatesMap = new HashMap<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT pe.storeName, pe.date FROM PosEntry pe";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            List<Object[]> results = query.list();

            for (Object[] result : results) {
                StoreNames storeName = (StoreNames) result[0];
                Date date = (Date) result[1];

                storeDatesMap.computeIfAbsent(storeName, k -> new HashSet<>()).add(date);
            }
        }
        return storeDatesMap;
    }

    public List<PosEntry> getPosEntriesByProduct(Product product) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery("FROM PosEntry WHERE product = :product", PosEntry.class)
                    .setParameter("product", product)
                    .list();
        } finally {
            session.close();
        }
    }
    public List<PosEntry> getPosEntriesByProductMasterCode(String masterCode) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Corrected the attribute name to "masterCode" in the WHERE clause
            String hql = "FROM PosEntry pe WHERE pe.sba.masterCode = :masterCode";
            Query<PosEntry> query = session.createQuery(hql, PosEntry.class);
            query.setParameter("masterCode", masterCode);
            return query.list();
        }
    }
    public List<PosEntry> getPosEntriesByProductMasterCodes(List<String> masterCodes) {
        if (masterCodes == null || masterCodes.isEmpty()) {
            return Collections.emptyList(); // Return an empty list if the input is empty to avoid unnecessary database query
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM PosEntry pe WHERE pe.sba.masterCode IN (:masterCodes)";
            Query<PosEntry> query = session.createQuery(hql, PosEntry.class);
            query.setParameter("masterCodes", masterCodes);
            return query.list();
        }
    }
    public List<PosEntry> getPosEntriesByDateAndStoreName(Date date, StoreNames storeName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM PosEntry pe WHERE pe.date = :date AND pe.storeName = :storeName";
            Query<PosEntry> query = session.createQuery(hql, PosEntry.class);
            query.setParameter("date", date);
            query.setParameter("storeName", storeName);
            return query.list();
        }
    }

    public List<PosEntry> getAllPosEntries() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            List<PosEntry> posEntries = session.createQuery("FROM PosEntry", PosEntry.class).getResultList();
            transaction.commit();
            return posEntries;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<PosEntry> getPosEntriesByYearAndStore(int year, String storeName) {
        logger.info("getting pos entries of "+storeName+" at "+year);
        Transaction transaction = null;
        //logger.info("Opening session");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            //logger.info("Session opened");
            transaction = session.beginTransaction();


            // Constructing the JPQL query
            String jpql = "SELECT pe FROM PosEntry pe WHERE YEAR(pe.date) = :year AND pe.storeName = :storeName";
            List<PosEntry> posEntries = session.createQuery(jpql, PosEntry.class)
                    .setParameter("year", year)
                    .setParameter("storeName", StoreNames.getStoreByName(storeName))
                    .getResultList();

            transaction.commit();
            //System.out.println("the retrieved");

            //logger.info("Query executed");
            return posEntries;
        } catch (Exception e) {
            //logger.log(Level.SEVERE, "Error in getEntities", e);
            System.out.println("\n\n\n we have an error at loading pos entries \n\n\n");
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return Collections.emptyList();
        }finally {
            logger.info("Session closed");
        }
    }

    public List<PosEntry> findEntriesByProductStoreAndDateRange(Product product, StoreNames storeName, Date start, Date end) {
        // Adjusted JPQL to navigate through sba to match storeName
        String jpql = "SELECT pe FROM PosEntry pe WHERE pe.sba.product = :product AND pe.sba.store = :storeName AND pe.date BETWEEN :start AND :end";
        TypedQuery<PosEntry> query = HibernateUtil.getEntityManagerFactory().createEntityManager().createQuery(jpql, PosEntry.class);
        query.setParameter("product", product);
        query.setParameter("storeName", storeName);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }
    public Date getMinimumDate() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            try {
                return session.createQuery("SELECT MIN(pe.date) FROM PosEntry pe", Date.class).getSingleResult();
            } catch (NoResultException e) {
                return null; // Return null or handle appropriately when no data is found
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Date getMaximumDate() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT MAX(pe.date) FROM PosEntry pe", Date.class)
                    .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}