package com.example.pdfreader.DAOs;


import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.Entities.Supplier;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SupplierDAO {

    public Supplier findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Supplier.class, id);
        } catch (Exception e) {
            // Handle exception
        }
        return null;
    }

    public void save(Supplier supplier) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(supplier);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            // Handle exception
        }
    }
    public void saveAll(List<Supplier> suppliers) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            for (Supplier supplier : suppliers) {
                session.merge(supplier);
                // Optionally flush and clear the session periodically to control memory usage
                // if (i % batchSize == 0) { // batch size, e.g., 20, 50...
                //     session.flush();
                //     session.clear();
                // }
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            // Handle exception
        }
    }

    // Delete a supplier
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Start a transaction
            transaction = session.beginTransaction();

            // Retrieve the supplier to be deleted
            Supplier supplier = session.get(Supplier.class, id);
            if (supplier != null) {
                // Delete the supplier
                session.remove(supplier);
            }

            // Commit the transaction
            transaction.commit();
        } catch (Exception e) {
            // Check if transaction is active and roll back if needed
            if (transaction != null) {
                transaction.rollback();
            }
            // Handle exception (e.g., logging)
            e.printStackTrace();
        }
    }
    public List<Supplier> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Supplier> query = session.createQuery("FROM Supplier", Supplier.class);
            return query.list();
        } catch (Exception e) {
            // Handle exception (e.g., logging)
            e.printStackTrace();
        }
        return null;
    }
    public void update(Supplier supplier) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(supplier);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace(); // Or handle the exception as you prefer
        }
    }
    public List<Object[]> getSuppliersWithProductCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT s, COUNT(spr.product) FROM Supplier s LEFT JOIN SupplierProductRelation spr ON s.id = spr.supplier GROUP BY s";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public int getProductCountForSupplier(Supplier supplier) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(spr) FROM SupplierProductRelation spr WHERE spr.supplier.id = :supplierId";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("supplierId", supplier.getId());
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    public List<String> getSupplierNamesForProduct(Product product) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SupplierProductRelation> query = session.createQuery(
                    "FROM SupplierProductRelation WHERE product = :product", SupplierProductRelation.class);
            query.setParameter("product", product);
            List<SupplierProductRelation> relations = query.list();
            return relations.stream()
                    .map(relation -> relation.getSupplier().getName()) // Assuming Supplier class has getName method
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}