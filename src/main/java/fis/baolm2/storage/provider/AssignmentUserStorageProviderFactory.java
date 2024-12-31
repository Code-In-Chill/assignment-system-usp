package fis.baolm2.storage.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;
import java.util.logging.Logger;

import static fis.baolm2.storage.constant.JpaStorageProviderConstants.ADD_ROLES_TO_TOKEN;
import static fis.baolm2.storage.constant.JpaStorageProviderConstants.PROVIDER_NAME;

public class AssignmentUserStorageProviderFactory implements UserStorageProviderFactory<AssignmentUserStorageProvider> {

    private static final Logger logger = Logger.getLogger(String.valueOf(AssignmentUserStorageProviderFactory.class));
    protected final List<ProviderConfigProperty> configMetadata;

    public AssignmentUserStorageProviderFactory() {
        this.configMetadata = ProviderConfigurationBuilder.create()
                .property().name(ADD_ROLES_TO_TOKEN).label("Add Roles to Token").type(ProviderConfigProperty.BOOLEAN_TYPE).defaultValue(true).helpText("Add roles to token. This will help you to use roles in your application.").required(true).add()
                .build();
    }

    @Override
    public AssignmentUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new AssignmentUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}
