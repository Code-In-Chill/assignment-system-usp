package fis.baolm2.storage.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.logging.Logger;

public class AssignmentUserStorageProviderFactory implements UserStorageProviderFactory<AssignmentUserStorageProvider> {

    public static final String PROVIDER_ID = "assignment-user-storage-jpa";

    private static final Logger logger = Logger.getLogger(String.valueOf(AssignmentUserStorageProviderFactory.class));

    @Override
    public AssignmentUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new AssignmentUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "JPA Example User Storage Provider";
    }

    @Override
    public void close() {
        logger.info("<<<<<< Closing factory");

    }
}
