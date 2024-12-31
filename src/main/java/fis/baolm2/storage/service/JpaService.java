package fis.baolm2.storage.service;

import fis.baolm2.storage.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.stream.Stream;

public class JpaService {

    private final EntityManager em;

    public JpaService(EntityManager em) {
        this.em = em;
    }

    public User getUserById(String id) {
        return em.find(User.class, id);
    }

    public User getUserByUsername(String username) {
        TypedQuery<User> query = em.createNamedQuery("getUserByUserName", User.class);
        query.setParameter("username", username);
        return query.getSingleResult();
    }

    public User getUserByEmail(String email) {
        TypedQuery<User> query = em.createNamedQuery("getUserByEmail", User.class);
        query.setParameter("email", email);
        try {
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public int getUserCount() {
        Object count = em.createNamedQuery("getUserCount")
                .getSingleResult();
        return ((Number) count).intValue();
    }

    public List<User> getAllUsers() {
        TypedQuery<User> query = em.createNamedQuery("getAllUsers", User.class);
        return query.getResultList();
    }

    public Stream<User> searchForUser(String searchTerm, Integer firstResult, Integer maxResults) {
        String search = (searchTerm == null) ? "" : searchTerm;
        TypedQuery<User> query = em.createNamedQuery("searchForUser", User.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultStream();
    }

    public Stream<User> searchForRole(String searchTerm, Integer firstResult, Integer maxResults) {
        String search = (searchTerm == null) ? "" : searchTerm;
        TypedQuery<User> query = em.createNamedQuery("searchForRole", User.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultStream();
    }

    public int getUserCountNonImplement() {
        return 0;
    }
}
