package com.example.pdfreader.DAOs;

import com.example.pdfreader.Entities.Product;
import com.example.pdfreader.Entities.Supplier;
import com.example.pdfreader.Helpers.SupplierProductRelation;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

public class SupplierProductRelationDAO {

    public void save(SupplierProductRelation supplierProductRelation) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            if (supplierProductRelation.getId() == null) {
                session.persist(supplierProductRelation); // For new entities
            } else {
                session.merge(supplierProductRelation); // For updating existing entities
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    /*
    public void saveAll(List<SupplierProductRelation> relations) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            for (SupplierProductRelation relation : relations) {
                Product product = relation.getProduct();
                // Check if a product with the same 'master' value already exists
                Product existingProduct = (Product) session.byNaturalId(Product.class)
                        .using("master", product.getMaster())
                        .load();
                if (existingProduct != null) {
                    // Use the existing product
                    relation.setProduct(existingProduct);
                } else if (!session.contains(product)) {
                    // Save new product
                    session.saveOrUpdate(product);
                }

                if (relation.getId() == null) {
                    session.persist(relation);
                } else {
                    session.merge(relation);
                }
            }

            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

     */




    public void saveAll(List<SupplierProductRelation> relations) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            int batchSize = 50; // Example batch size
            int i = 0;

            for (SupplierProductRelation relation : relations) {
                if (relation.getId() == null) {
                    session.persist(relation); // For new entities
                } else {
                    session.merge(relation); // For updating existing entities
                }

                // Batch processing
                if (i % batchSize == 0) {
                    session.flush();
                    session.clear();
                }
                i++;
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            // Consider logging the exception with more details here
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

    }





    public void saveAllRelations(List<SupplierProductRelation> relations, List<Supplier> suppliersToSaveFirst) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            // First, save or update all suppliers that are not yet in the database
            for (Supplier supplier : suppliersToSaveFirst) {
                System.out.println("Saving supplier: " + supplier.getName());
                session.saveOrUpdate(supplier);
            }

            // Flush and clear the session to ensure all suppliers are persisted
            session.flush();
            session.clear();

            // Now, save or update all supplier-product relations
            for (SupplierProductRelation relation : relations) {
                // Check if the supplier already exists in the current session
                Supplier managedSupplier = relation.getSupplier();
                if (!suppliersToSaveFirst.contains(managedSupplier)) {
                    managedSupplier = (Supplier) session.merge(managedSupplier);
                }
                relation.setSupplier(managedSupplier);

                System.out.println("Saving relation for supplier: " + managedSupplier.getName());
                session.saveOrUpdate(relation);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public boolean existsSupplier(Supplier supplier) {
        boolean exists = false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Assuming 'name' is a unique field for Supplier
            String hql = "SELECT count(*) FROM Supplier WHERE name = :name";
            Query query = session.createQuery(hql);
            query.setParameter("name", supplier.getName());
            Long count = (Long) query.uniqueResult();
            exists = count > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exists;
    }

    public SupplierProductRelation findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(SupplierProductRelation.class, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SupplierProductRelation> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM SupplierProductRelation", SupplierProductRelation.class).list();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void delete(SupplierProductRelation supplierProductRelation) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(supplierProductRelation);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
    public boolean relationExists(Product product, Supplier supplier) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT count(*) FROM SupplierProductRelation spr WHERE spr.product = :product AND spr.supplier = :supplier";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("product", product);
            query.setParameter("supplier", supplier);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<SupplierProductRelation> findRelationsByProductAndSupplier(Product product, Supplier supplier) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM SupplierProductRelation spr WHERE spr.product = :product AND spr.supplier = :supplier";
            Query<SupplierProductRelation> query = session.createQuery(hql, SupplierProductRelation.class);
            query.setParameter("product", product);
            query.setParameter("supplier", supplier);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}