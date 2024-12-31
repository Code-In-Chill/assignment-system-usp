package fis.baolm2.storage.provider;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fis.baolm2.storage.entity.User;
import fis.baolm2.storage.entity.UserAdapter;
import fis.baolm2.storage.service.JpaService;
import jakarta.persistence.EntityManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class AssignmentUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(String.valueOf(AssignmentUserStorageProvider.class));

    protected EntityManager em;
    protected JpaService js;

    protected ComponentModel model;
    protected KeycloakSession session;

    AssignmentUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        EntityManager em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
        this.js = new JpaService(em);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.endsWith(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        logger.info("isConfiguredFor(realm=" + realm.getName() + ", user=" + user.getUsername() + ", credentialType=" + credentialType + ")");
        return true;
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
        String cred = input.getChallengeResponse();
        try {
            User userEntity = js.getUserByUsername(user.getUsername());

            if (userEntity == null) return false;

            BCrypt.Result checkPassResult = BCrypt.verifyer()
                    .verify(cred.toCharArray(), userEntity.getPassword());

            if (!checkPassResult.verified) {
                logger.warning(String.format("Failed password validation for user %s ", user.getUsername()));
                return false;
            }
        } catch (Throwable t) {
            logger.warning("Error when validating user password");
            logger.warning(t.getCause().toString());
            return false;
        }

        return true;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return js.getUserCount();
    }

    @Override
    public int getUsersCount(RealmModel realm, Set<String> groupIds) {
        return js.getUserCountNonImplement();
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        return js.getUserCountNonImplement();
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        return js.getUserCountNonImplement();
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        return js.getUserCountNonImplement();
    }


    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String persistenceId = StorageId.externalId(id);
        logger.info("getUserById: " + persistenceId + " in realm: " + realm.getName());

        User userEntity = js.getUserById(persistenceId);

        if (userEntity == null) {
            logger.info("could not find user by id: " + id + " in realm: " + realm.getId());
            return null;
        }

        return new UserAdapter(session, realm, model, userEntity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        User userEntity = js.getUserByUsername(username);

        if (userEntity == null) return null;


        return new UserAdapter(session, realm, model, userEntity);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        User userEntiy = js.getUserByEmail(email);

        if (userEntiy == null) return null;

        return new UserAdapter(session, realm, model, userEntiy);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.get(UserModel.SEARCH);

        Stream<User> users = js.searchForUser(search, firstResult, maxResults);
        return users.map(entity -> new UserAdapter(session, realm, model, entity));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
        return searchForUserStream(realm, params, null, null);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        String search = role.getName();

        Stream<User> users = js.searchForRole(search, firstResult, maxResults);
        return users.map(entity -> new UserAdapter(session, realm, model, entity));
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        return getRoleMembersStream(realm, role, null, null);
    }

    /**
     * Returns empty stream as the remote user provider does not support group membership.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @return an empty stream.
     */
    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        return Stream.empty();
    }

    /**
     * Returns empty stream as the remote user provider does not support group membership.
     *
     * @param realmModel a reference to the realm.
     * @param groupModel a reference to the group.
     * @param integer    first result to return. Ignored if negative, zero, or {@code null}.
     * @param integer1   maximum number of results to return. Ignored if negative or {@code null}.
     * @return an empty stream.
     */
    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer integer, Integer integer1) {
        return Stream.empty();
    }

    /**
     * Returns empty stream as the remote user provider does not support group membership.
     *
     * @param realmModel a reference to the realm.
     * @param s          the attribute name.
     * @param s1         the attribute value.
     * @return an empty stream.
     */
    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realmModel, String s, String s1) {
        return Stream.empty();
    }

    /**
     * Logs a message indicating that the realm is about to be removed. <br>
     * As the remote user provider does not need to perform any cleanup operations, this method does nothing.
     *
     * @param realm a reference to the realm.
     */
    @Override
    public void preRemove(RealmModel realm) {
        logger.info("pre-remove realm");
    }

    /**
     * Logs a message indicating that the group is about to be removed. <br>
     * As the remote user provider does not need to perform any cleanup operations, this method does nothing.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     */
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        logger.info("pre-remove group");
    }

    /**
     * Logs a message indicating that the role is about to be removed. <br>
     * As the remote user provider does not need to perform any cleanup operations, this method does nothing.
     *
     * @param realm a reference to the realm.
     * @param role  a reference to the role.
     */
    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        logger.info("pre-remove role");
    }

    /**
     * Return null as this provider does not provide any create user feature!
     *
     * @param realm    a reference to the realm
     * @param username a username the created user will be assigned
     * @return null as this provider does nothing
     */
    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return null;
    }

    /**
     * Return false as this provider does not provide any remove user feature!
     *
     * @param realm a reference to the realm
     * @param user  a reference to the user that is removed
     * @return false as this provider does nothing
     */
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return false;
    }
}
