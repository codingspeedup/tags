package io.github.codingspeedup.tags.plugin;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class TagsSettingsSecretManager {

    public static final String GEMINI_API_KEY = "geminiApiKey";

    private static final String SERVICE_NAME = "T.A.G.S.+_Secret_Service";

    private static CredentialAttributes createAttributes(String secretKey) {
        // Creates a unique identifier for the secret in the OS keychain
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(SERVICE_NAME, secretKey));
    }

    public static void saveSecret(String secretKey, String secretValue) {
        PasswordSafe.getInstance().setPassword(createAttributes(secretKey), secretValue);
    }

    public static String getSecret(String secretKey) {
        return PasswordSafe.getInstance().getPassword(createAttributes(secretKey));
    }

}
