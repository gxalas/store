package com.example.pdfreader.DAOs;

import com.example.pdfreader.DTOs.ProductDTO;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.*;

public class ProductDAO {

    private static SessionFactory sessionFactory;
    private static EntityManager entityManager;
    private static final int BATCH_SIZE = 50; // You can adjust this size based on your requirements

    static {
        try {
            sessionFactory = HibernateUtil.getSessionFactory();
            entityManager = HibernateUtil.getEntityManagerFactory().createEntityManager();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public void saveProduct(Product product) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(product);
            entityManager.flush();  // Ensure product is immediately persisted
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
    public void saveProducts(List<Product> products) {
        EntityTransaction transaction = entityManager.getTransaction();
        int batchSize = 50; // Adjust this size based on your environment and requirements

        try {
            transaction.begin();

            for (int i = 0; i < products.size(); i++) {
                if(products.get(i)==null){
                    System.err.println("a null product attempted to get saved at position "+i);
                    continue;
                }
                entityManager.persist(products.get(i));

                // Flush and clear the EntityManager in batches
                if (i % batchSize == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }

            entityManager.flush();
            entityManager.clear();
            transaction.commit();
            System.out.println("saved products");
        } catch (RuntimeException e) {
            System.out.println("error at saving the products");
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void updateProduct(Product product) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(product); // Use merge instead of persist
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public Product getProduct(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Product.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // Method to get a product by its master code
    public Product getProductByMasterCode(String masterCode) {
        Product product = null;
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Product WHERE master = :masterCode";
            Query<Product> query = session.createQuery(hql, Product.class);
            query.setParameter("masterCode", masterCode);
            product = query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception
        }
        return product;
    }
    // Method to get all products
    public List<Product> getAllProducts() {
        List<Product> products = null;
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Product"; // HQL to select all products
            Query<Product> query = session.createQuery(hql, Product.class);
            products = query.list();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception
        }
        return products;
    }
    public Map<String, Product> getAllProductsAsMap() {
        try (Session session = sessionFactory.openSession()) {
            // Create a query to fetch all products
            Query<Product> query = session.createQuery("FROM Product", Product.class);
            List<Product> products = query.getResultList();

            // Create a map to store products with mastercode as the key
            Map<String, Product> productMap = new HashMap<>();

            // Populate the map with products
            for (Product product : products) {
                productMap.put(product.getMaster(), product);
            }

            return productMap;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
    public List<ProductDTO> getAllProductsWithDocumentCountAndDescriptions() {
        List<ProductDTO> productDTOs = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            // Corrected JPQL query
            String jpql = "SELECT p, sba, COUNT(de) "
                    + "FROM Product p "
                    + "LEFT JOIN p.storeBasedAttributes sba " // Ensure this matches your field name
                    + "LEFT JOIN DocEntry de ON de.product = p "
                    + "GROUP BY p, sba";

            List<Object[]> results = session.createQuery(jpql, Object[].class).getResultList();
            Map<Long, ProductDTO> dtoMap = new HashMap<>();

            for (Object[] row : results) {
                Product product = (Product) row[0];
                StoreBasedAttributes storeBasedAttribute = (StoreBasedAttributes) row[1];
                Long documentCount = (Long) row[2];

                ProductDTO dto = dtoMap.computeIfAbsent(product.getId(), id -> new ProductDTO());
                dto.setId(product.getId());
                dto.setCode(product.getCode());
                dto.setMaster(product.getMaster());
                dto.setDescriptions(product.getDescriptions());
                dto.setDocumentCount(documentCount);
                dto.getStoreBasedAttributes().add(storeBasedAttribute);

                // Adding store-based attributes to the DTO
                //if (!dto.getStoreBasedAttributes().contains(storeBasedAttributes)) {
                  //  dto.getStoreBasedAttributes().add(storeBasedAttributes);
                //}
            }

            productDTOs.addAll(dtoMap.values());
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception
        }
        return new ArrayList<>(productDTOs);
    }

    public void addNewProducts(List<Product> newProducts) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            int count = 0;
            for (Product product : newProducts) {
                session.persist(product);

                if (++count % BATCH_SIZE == 0) {
                    // Flush a batch of inserts and release memory:
                    session.flush();
                    session.clear();
                }
            }

            // Flush and clear one last time if there are any remaining products
            if (count % BATCH_SIZE != 0) {
                session.flush();
                session.clear();
            }

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) tx.rollback();
            throw e; // Or handle it as per your application's error handling policy
        } finally {
            session.close();
        }
    }
    public void updateProducts(List<Product> updatedProducts) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            int count = 0;
            for (Product product : updatedProducts) {
                session.update(product);

                if (++count % BATCH_SIZE == 0) {
                    // Flush a batch of updates and release memory:
                    session.flush();
                    session.clear();
                }
            }

            // Flush and clear one last time for any remaining products
            if (count % BATCH_SIZE != 0) {
                session.flush();
                session.clear();
            }

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) tx.rollback();
            throw e; // Or handle it as per your application's error handling policy
        } finally {
            session.close();
        }
    }
    public List<Object[]> getProductsWithSupplierCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT p, COUNT(spr.supplier) FROM Product p LEFT JOIN SupplierProductRelation spr ON p.id = spr.product GROUP BY p";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public int getSupplierCountForProduct(Long productId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(spr) FROM SupplierProductRelation spr WHERE spr.product.id = :productId";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("productId", productId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<String> getSupplierNamesForProduct(Long productId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT s.name FROM SupplierProductRelation spr JOIN spr.supplier s WHERE spr.product.id = :productId";
            Query<String> query = session.createQuery(hql, String.class);
            query.setParameter("productId", productId);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
