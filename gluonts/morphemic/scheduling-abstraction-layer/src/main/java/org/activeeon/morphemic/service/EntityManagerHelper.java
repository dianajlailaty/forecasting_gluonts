package org.activeeon.morphemic.service;

import org.hibernate.CacheMode;
import org.hibernate.jpa.QueryHints;

import javax.persistence.*;

public class EntityManagerHelper {

    private static final EntityManagerFactory emf;

    private static final ThreadLocal<EntityManager> threadLocal;

    static {
        emf = Persistence.createEntityManagerFactory("model");
        threadLocal = new ThreadLocal<EntityManager>();
    }

    public static EntityManager getEntityManager() {
        EntityManager em = threadLocal.get();

        if (em == null) {
            em = emf.createEntityManager();
            // set your flush mode here
            em.setFlushMode(FlushModeType.COMMIT);
            threadLocal.set(em);
        }
        return em;
    }

    public static void closeEntityManager() {
        EntityManager em = threadLocal.get();
        if (em != null) {
            em.close();
            threadLocal.set(null);
        }
    }

    public static void closeEntityManagerFactory() {
        emf.close();
    }

    public static void begin() {
        if(!getEntityManager().getTransaction().isActive())
            getEntityManager().getTransaction().begin();
    }

    public static void persist(Object entity) {
        getEntityManager().persist(entity);
    }

    public static void merge(Object entity) {
        getEntityManager().merge(entity);
    }

    public static void refresh(Object entity) {
        getEntityManager().refresh(entity);
    }

    public static void remove(Object entity) {
        getEntityManager().remove(entity);
    }

    public static <T> T find(Class<T> entityClass, Object primaryKey) {
        return getEntityManager().find(entityClass, primaryKey);
    }

    public static <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return getEntityManager().createQuery(qlString, resultClass).setHint(QueryHints.HINT_CACHE_MODE, CacheMode.REFRESH);
    }

    public static void rollback() {
        getEntityManager().getTransaction().rollback();
    }

    public static void commit() {
        getEntityManager().getTransaction().commit();
    }
}