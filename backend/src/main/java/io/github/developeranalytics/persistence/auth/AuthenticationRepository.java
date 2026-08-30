package io.github.developeranalytics.persistence.auth;

import io.github.developeranalytics.domain.auth.*;
import io.github.developeranalytics.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class AuthenticationRepository {
    @Inject EntityManager em;


    @Transactional
    public void persistUser(AppUser user){ em.persist(user); }

    @Transactional
    public void persistIdentity(ProviderIdentity identity){ em.persist(identity); }

    @Transactional
    public void persistSession(UserSession session){ em.persist(session); }

    @Transactional
    public void saveLoginAttempt(AuthLoginAttempt attempt){ em.persist(attempt); }

    @Transactional
    public Optional<AuthLoginAttempt> consumeLoginAttempt(String stateHash, OffsetDateTime now){
        List<AuthLoginAttempt> list=em.createQuery("select a from AuthLoginAttempt a where a.stateHash=:hash",AuthLoginAttempt.class).setParameter("hash",stateHash).getResultList();
        if(list.isEmpty()) return Optional.empty();
        AuthLoginAttempt a=list.get(0); em.remove(a);
        return a.getExpiresAt().isAfter(now) ? Optional.of(a) : Optional.empty();
    }

    public Optional<ProviderIdentity> findGitHubIdentity(String externalId){
        return em.createQuery("select p from ProviderIdentity p where p.provider='github' and p.externalUserId=:id",ProviderIdentity.class)
            .setParameter("id",externalId).getResultStream().findFirst();
    }

    public Optional<ProviderIdentity> findGitHubIdentityForUser(UUID userId){
        return em.createQuery("select p from ProviderIdentity p where p.provider='github' and p.user.id=:id",ProviderIdentity.class)
            .setParameter("id",userId).getResultStream().findFirst();
    }

    @Transactional
    public ProviderIdentity createIdentity(String externalId,String login,String name){
        AppUser user=AppUser.create(); em.persist(user);
        ProviderIdentity identity=new ProviderIdentity(user,"github",externalId,login,name); em.persist(identity);
        em.persist(new ProviderConnection(user,identity,"github"));
        return identity;
    }

    @Transactional
    public UserSession createSession(AppUser user,String tokenHash,OffsetDateTime expiresAt){
        UserSession session=new UserSession(user,tokenHash,expiresAt); em.persist(session); return session;
    }

    @Transactional
    public Optional<UserSession> findValidSession(String tokenHash, OffsetDateTime now){
        return em.createQuery("select s from UserSession s join fetch s.user where s.tokenHash=:hash and s.expiresAt>:now",UserSession.class)
            .setParameter("hash",tokenHash).setParameter("now",now).getResultStream().findFirst();
    }

    @Transactional
    public void deleteSession(String tokenHash){
        em.createQuery("delete from UserSession s where s.tokenHash=:hash").setParameter("hash",tokenHash).executeUpdate();
    }
}
