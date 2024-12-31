package fis.baolm2.storage.entity;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static fis.baolm2.storage.constant.JpaStorageProviderConstants.ADD_ROLES_TO_TOKEN;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

    private static final Logger logger = Logger.getLogger(String.valueOf(UserAdapter.class));

    protected ComponentModel model;
    protected User entity;
    protected String keycloakId;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, User entity) {
        super(session, realm, model);
        this.entity = entity;
        this.keycloakId = StorageId.keycloakId(model, entity.getId());
        this.model = model;
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        logger.info("[credentialManager] new UserCredentialManager...");
        return new UserCredentialManager(session, realm, this);
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public String getUsername() {
        return entity.getUserName();
    }

    @Override
    public void setUsername(String username) {
        entity.setUserName(username);
    }

    @Override
    public boolean isEmailVerified() {
        return entity.isEmailVerified();
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    @Override
    public String getFirstName() {
        return entity.getFirstName();
    }

    @Override
    public String getLastName() {
        return entity.getLastName();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        attributes.add(UserModel.USERNAME, getUsername());
        attributes.add(UserModel.EMAIL, getEmail());
        attributes.add(UserModel.FIRST_NAME, getFirstName());
        attributes.add(UserModel.LAST_NAME, getLastName());
        for (Map.Entry<String, String> param : entity.getEntityAttributes().entrySet()) {
            attributes.add(param.getKey(), param.getValue());
        }
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        Map<String, List<String>> attributes = getAttributes();
        return (attributes.containsKey(name)) ? attributes.get(name).stream() : Stream.empty();
    }

    @Override
    public String getFirstAttribute(String name) {
        List<String> list = getAttributes().getOrDefault(name, List.of());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        Stream<RoleModel> roleMappings = super.getRoleMappingsStream();
        boolean addFederationRoles = Boolean.parseBoolean(model.get(ADD_ROLES_TO_TOKEN));
        if (!addFederationRoles) {
            return roleMappings;
        }

        String[] roleArr = entity.getRoles().split(",");
        for (String role : roleArr) {
            RoleModel roleModel = realm.getRole(role);
            if (roleModel == null) {
                roleModel = realm.addRole(role);
            }
//            log("Granting role %s to user %s during user import from Remote", role, username);
//            this.grantRole(roleModel);
            roleMappings = Stream.concat(roleMappings, Stream.of(roleModel));
        }
        return roleMappings;
    }
}
