package com.example.pdfreader.DAOs;

import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.PosEntry;
import com.example.pdfreader.enums.StoreNames;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PosEntryDAO {

    //private  SessionFactory sessionFactory;
    private static final int BATCH_SIZE = 50; // Adjust this size based on your requirements
    private static final Logger logger = Logger.getLogger(PosEntryDAO.class.getName());

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



    public void savePosEntries(List<PosEntry> posEntries) {
        Session session = HibernateUtil.getSessionFactory().openSession();

        for (PosEntry posEntry : posEntries) {
            Transaction tx = null;

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
                System.out.println("- - - - -  error at duplicate :: saving pos entries - - - - -");
                System.out.println(e.hashCode()+" . "+e.getCause()+" . "+e.getMessage());
                System.out.println("- - - - -  error at duplicate - - - - -");
                System.out.println("hash "+posEntry.getShaCode());
                System.out.println("store "+posEntry.storeName);
                System.out.println("master "+posEntry.getMaster());
                System.out.println("date "+posEntry.getDate());

                // System.out.println("product"+ posEntry.getProduct());
                // Log the error using a proper logging framework
                // logger.error("Error saving PosEntry with hashCode {}: {}", posEntry.getHashCode(), e.getMessage());
                // Continue with the next PosEntry
            }
            // Optionally clear the session to handle memory efficiently
            session.clear();
        }

        session.close(); // Close the session after processing all entries
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
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<PosEntry> query = session.createQuery(
                    "FROM PosEntry pe WHERE pe.product.master = :masterCode", PosEntry.class);
            query.setParameter("masterCode", masterCode);
            return query.list();
        } finally {
            session.close();
        }
    }
    /*
    public List<PosEntry> getAllPosEntries() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM PosEntry", PosEntry.class).getResultList();
        } catch (Exception e) {
            System.out.println("& & &");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
     */

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
        Transaction transaction = null;
        logger.info("Opening session");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            logger.info("Session opened");
            transaction = session.beginTransaction();


            // Constructing the JPQL query
            String jpql = "SELECT pe FROM PosEntry pe WHERE YEAR(pe.date) = :year AND pe.storeName = :storeName";
            List<PosEntry> posEntries = session.createQuery(jpql, PosEntry.class)
                    .setParameter("year", year)
                    .setParameter("storeName", StoreNames.getStoreByName(storeName))
                    .getResultList();

            transaction.commit();
            //System.out.println("the retrieved");
            logger.info("Query executed");
            return posEntries;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in getEntities", e);
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
    public Date getMinimumDate() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT MIN(pe.date) FROM PosEntry pe", Date.class)
                    .getSingleResult();
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