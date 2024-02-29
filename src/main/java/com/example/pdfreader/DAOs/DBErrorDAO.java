package com.example.pdfreader.DAOs;

import com.example.pdfreader.MyCustomEvents.DBError.ErrorEventManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;

public class DBErrorDAO {

    private SessionFactory sessionFactory;
    private final ErrorEventManager eventManager;

    public DBErrorDAO(ErrorEventManager errorEventManager) {
        this.sessionFactory = HibernateUtil.getSessionFactory();
        this.eventManager = errorEventManager;
    }

    public SessionFactory getSessionFactory(){
        return this.sessionFactory;
    }

    public void saveDBErrors(List<DBError> dbErrors) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            for (DBError dbError : dbErrors) {
                session.persist(dbError);
                eventManager.notifyErrorLogged(dbError);
                // Optionally, you could flush and clear the session periodically if the list is very large
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();  // Rollback in case of an exception
            }
            e.printStackTrace();
            // Additional error handling or logging
        } finally {
            if (session != null && session.isOpen()) {
                session.close();  // Ensure the session is closed
            }
        }
    }
    public List<DBError> getAllErrors() {
        System.out.println("trying to get all errors - - - - - - - - - ");
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM DBError", DBError.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle or log the exception as needed
            return Collections.emptyList();
        }
    }
    public void deleteError(long errorId) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            DBError errorLog = session.get(DBError.class, errorId);
            if (errorLog != null) {
                session.delete(errorLog);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            // Handle or log the exception as needed
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Additional methods for other operations like fetching error logs, etc.
}