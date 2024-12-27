package fis.baolm2.storage.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.*;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class AssignmentUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialProvider<PasswordCredentialModel>,
        CredentialInputUpdater,
        CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(String.valueOf(AssignmentUserStorageProvider.class));

    protected EntityManager em;

    protected ComponentModel model;
    protected KeycloakSession session;

    AssignmentUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String persistenceId = StorageId.externalId(id);
        logger.info("getUserById: " + persistenceId + " in realm: " + realm.getId());
        // use named query to get user by id
        UserEntity entity = em.find(UserEntity.class, persistenceId);
        if (entity == null) {
            logger.info("could not find user by id: " + id + " in realm: " + realm.getId());
            return null;
        }
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.info("getUserByUsername: " + username);
        TypedQuery<UserEntity> query = em.createNamedQuery("getUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        List<UserEntity> result = query.getResultList();
        if (result.isEmpty()) {
            logger.info("could not find username: " + username);
            return null;
        }

        return new UserAdapter(session, realm, model, result.getFirst());
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        List<UserEntity> result = query.getResultList();
        if (result.isEmpty()) return null;
        return new UserAdapter(session, realm, model, result.getFirst());
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserEntity entity = new UserEntity();
        UUID uuid = UUID.randomUUID();
        entity.setId(uuid.toString());
        entity.setUsername(username);
        em.persist(entity);

        // sync user with spring boot app
        try {
            int status = SimpleHttp.doPost("http://host.docker.internal:9789/api/users/create", session)
                    .json(new UserDto(entity.getUsername(), StorageId.keycloakId(model, entity.getId()))).asStatus();

            if (status == 200) {
                logger.info("================================================");
                logger.info("Spring boot app response(Create): sync user with spring boot app successfully");
                logger.info("================================================");
            } else {
                logger.warning("================================================");
                logger.warning("Spring boot app response(Create): could not sync user with spring boot app");
                logger.warning("================================================");
            }
        } catch (IOException e) {
            logger.warning("================================================");
            logger.warning("Could not sync user with spring boot app");
            logger.warning("================================================");
        }

        logger.info("added user: " + username);
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String persistenceId = StorageId.externalId(user.getId());
        UserEntity entity = em.find(UserEntity.class, persistenceId);
        if (entity == null) return false;
        em.remove(entity);

        // sync user with spring boot app
        try {
            int status = SimpleHttp.doPost("http://host.docker.internal:9789/api/users/delete", session)
                    .param("keycloakId", "'" + StorageId.keycloakId(model, entity.getId()) + "'")
                    .asStatus();

            if (status == 200) {
                logger.info("================================================");
                logger.info("Spring boot app response(Remove): sync user with spring boot app successfully");
                logger.info("================================================");
            } else {
                logger.warning("================================================");
                logger.warning("Spring boot app response(Remove): could not sync user with spring boot app");
                logger.warning("================================================");
            }
        } catch (IOException e) {
            logger.warning("================================================");
            logger.warning("Could not sync user with spring boot app");
            logger.warning("================================================");
        }
        return true;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    protected PasswordHashProvider getHashProvider(PasswordPolicy policy) {
        if (policy != null && policy.getHashAlgorithm() != null) {
            PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
            if (provider != null) {
                return provider;
            } else {
                logger.warning(String.format("Realm PasswordPolicy PasswordHashProvider %s not found", policy.getHashAlgorithm()));
            }
        }

        return session.getProvider(PasswordHashProvider.class);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        return createCredential(realm, user, input.getChallengeResponse());
    }

    public UserAdapter getUserAdapter(UserModel user) {
        if (user instanceof CachedUserModel) {
            return (UserAdapter) ((CachedUserModel) user).getDelegateForUpdate();
        } else {
            return (UserAdapter) user;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        supportsCredentialType(credentialType);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return getPassword(realm, user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) {
            logger.warning("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        if (input.getChallengeResponse() == null) {
            logger.warning(String.format("Input password was null for user %s", user.getUsername()));
            return false;
        }
        PasswordCredentialModel password = getPassword(realm, user);
        if (password == null) {
            logger.warning(String.format("No password stored for user %s", user.getUsername()));
            return false;
        }
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, password.getPasswordCredentialData().getAlgorithm());
        if (hash == null) {
            logger.warning(String.format("PasswordHashProvider %s not found for user %s ", password.getPasswordCredentialData().getAlgorithm(), user.getUsername()));
            return false;
        }
        try {
            if (!hash.verify(input.getChallengeResponse(), password)) {
                logger.warning(String.format("Failed password validation for user %s ", user.getUsername()));
                return false;
            }

            rehashPasswordIfRequired(session, realm, user, input, password);
        } catch (Throwable t) {
            logger.warning("Error when validating user password");
            logger.warning(t.getCause().toString());
            return false;
        }

        return true;
    }

    private void rehashPasswordIfRequired(KeycloakSession session, RealmModel realm, UserModel user, CredentialInput input, PasswordCredentialModel password) {
        PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
        PasswordHashProvider provider;
        if (passwordPolicy != null && passwordPolicy.getHashAlgorithm() != null) {
            provider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        } else {
            provider = session.getProvider(PasswordHashProvider.class);
        }

        if (!provider.policyCheck(passwordPolicy, password)) {
            int iterations = passwordPolicy != null ? passwordPolicy.getHashIterations() : -1;

            PasswordCredentialModel newPassword = provider.encodedCredential(input.getChallengeResponse(), iterations);
            newPassword.setId(password.getId());
            newPassword.setCreatedDate(password.getCreatedDate());
            newPassword.setUserLabel(password.getUserLabel());
            user.credentialManager().updateStoredCredential(newPassword);
        }
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        Object count = em.createNamedQuery("getUserCount")
                .getSingleResult();
        return ((Number) count).intValue();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.get(UserModel.SEARCH);
        TypedQuery<UserEntity> query = em.createNamedQuery("searchForUser", UserEntity.class);
        String lower = search != null ? search.toLowerCase() : "";
        query.setParameter("search", "%" + lower + "%");
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.getResultStream().map(entity -> new UserAdapter(session, realm, model, entity));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public String getType() {
        return PasswordCredentialModel.TYPE;
    }

    public PasswordCredentialModel getPassword(RealmModel realm, UserModel user) {
        List<CredentialModel> passwords = user.credentialManager().getStoredCredentialsByTypeStream(getType()).toList();
        if (passwords.isEmpty()) return null;
        return PasswordCredentialModel.createFromCredentialModel(passwords.getFirst());
    }

    public boolean createCredential(RealmModel realm, UserModel user, String password) {
        PasswordPolicy policy = realm.getPasswordPolicy();

        PolicyError error = session.getProvider(PasswordPolicyManagerProvider.class).validate(realm, user, password);
        if (error != null) throw new ModelException(error.getMessage(), error.getParameters());

        PasswordHashProvider hash = getHashProvider(policy);
        if (hash == null) {
            return false;
        }
        try {
            PasswordCredentialModel credentialModel = hash.encodedCredential(password, policy.getHashIterations());
            credentialModel.setCreatedDate(Time.currentTimeMillis());
            createCredential(realm, user, credentialModel);
        } catch (Throwable t) {
            throw new ModelException(t.getMessage(), t);
        }
        return true;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, PasswordCredentialModel credentialModel) {
        PasswordPolicy policy = realm.getPasswordPolicy();
        int expiredPasswordsPolicyValue = policy.getExpiredPasswords();
        int passwordAgeInDaysPolicy = Math.max(0, policy.getPasswordAgeInDays());

        // 1) create new or reset existing password
        CredentialModel createdCredential;
        CredentialModel oldPassword = getPassword(realm, user);
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        if (oldPassword == null) { // no password exists --> create new
            createdCredential = user.credentialManager().createStoredCredential(credentialModel);
        } else { // password exists --> update existing
            credentialModel.setId(oldPassword.getId());
            user.credentialManager().updateStoredCredential(credentialModel);
            createdCredential = credentialModel;

            // 2) add a password history item based on the old password
            if (expiredPasswordsPolicyValue > 1 || passwordAgeInDaysPolicy > 0) {
                oldPassword.setId(null);
                oldPassword.setType(PasswordCredentialModel.PASSWORD_HISTORY);
                oldPassword = user.credentialManager().createStoredCredential(oldPassword);
            }
        }

        // 3) remove old password history items, if both history policies are set, more restrictive policy wins
        final int passwordHistoryListMaxSize = Math.max(0, expiredPasswordsPolicyValue - 1);

        final long passwordMaxAgeMillis = Time.currentTimeMillis() - Duration.ofDays(passwordAgeInDaysPolicy).toMillis();

        CredentialModel finalOldPassword = oldPassword;
        user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.PASSWORD_HISTORY)
                .sorted(CredentialModel.comparingByStartDateDesc())
                .skip(passwordHistoryListMaxSize)
                .filter(credentialModel1 -> !(credentialModel1.getId().equals(finalOldPassword.getId())))
                .filter(credential -> passwordAgePredicate(credential, passwordMaxAgeMillis))
                .toList()
                .forEach(p -> user.credentialManager().removeStoredCredentialById(p.getId()));

        return createdCredential;
    }

    private boolean passwordAgePredicate(CredentialModel credential, long passwordMaxAgeMillis) {
        return credential.getCreatedDate() < passwordMaxAgeMillis;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public PasswordCredentialModel getCredentialFromModel(CredentialModel model) {
        return PasswordCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        CredentialTypeMetadata.CredentialTypeMetadataBuilder metadataBuilder = CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.BASIC_AUTHENTICATION)
                .displayName("password-display-name")
                .helpText("password-help-text")
                .iconCssClass("kcAuthenticatorPasswordClass");

        // Check if we are creating or updating password
        UserModel user = metadataContext.getUser();
        if (user != null && user.credentialManager().isConfiguredFor(getType())) {
            metadataBuilder.updateAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        } else {
            metadataBuilder.createAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        }

        return metadataBuilder
                .removeable(false)
                .build(session);
    }
}
