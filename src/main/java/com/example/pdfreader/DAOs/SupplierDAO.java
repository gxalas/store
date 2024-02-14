package com.example.pdfreader.DAOs;


import com.example.pdfreader.DTOs.SupplierWithProductCount;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.Supplier;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SupplierDAO {
    private static final Logger logger = LoggerFactory.getLogger(SupplierDAO.class);

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
    public List<SupplierWithProductCount> getSuppliersWithProductCount() {
        List<SupplierWithProductCount> resultList = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT s.id, COUNT(spr.product_id) " +
                    "FROM Supplier s LEFT JOIN SupplierProductRelation spr ON s.id = spr.supplier_id " +
                    "GROUP BY s.id";
            List<Object[]> queryResults = session.createNativeQuery(sql).getResultList();
            for (Object[] row : queryResults) {
                Long supplierId = ((Number) row[0]).longValue();
                Integer productCount = ((Number) row[1]).intValue();
                // Fetch the supplier entity based on the id
                Supplier supplier = session.find(Supplier.class, supplierId);
                resultList.add(new SupplierWithProductCount(supplier, productCount));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
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

    public List<Supplier> getSuppliersByProduct(Product product) {
        logger.info("Fetching suppliers for product: {}", product);

        List<Supplier> suppliers = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SupplierProductRelation> query = session.createQuery(
                    "FROM SupplierProductRelation WHERE product = :product", SupplierProductRelation.class);
            query.setParameter("product", product);
            List<SupplierProductRelation> relations = query.list();

            for (SupplierProductRelation relation : relations) {
                suppliers.add(relation.getSupplier());
            }
        } catch (Exception e) {
            logger.error("Exception in getSuppliersByProduct", e);
        }
        return suppliers;
    }
    public Map<Long, String> getSupplierNamesForProducts(List<Product> products) {
        logger.info("Fetching supplier names for products");
        Map<Long, String> supplierNamesMap = new HashMap<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            for (Product product : products) {
                Query<SupplierProductRelation> query = session.createQuery(
                        "FROM SupplierProductRelation WHERE product.id = :productId", SupplierProductRelation.class);
                query.setParameter("productId", product.getId());
                List<SupplierProductRelation> relations = query.list();

                String supplierNames = relations.stream()
                        .map(relation -> relation.getSupplier().getName())
                        .collect(Collectors.joining(", "));
                supplierNamesMap.put(product.getId(), supplierNames);
            }
        } catch (Exception e) {
            logger.error("Exception in getSupplierNamesForProducts", e);
        }
        return supplierNamesMap;
    }
}