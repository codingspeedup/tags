package io.github.codingspeedup.tags.plugin.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationInfo;
import io.github.codingspeedup.tags.plugin.core.TagsMessageBundle;

public class SettingsSecretManager {

    public static final String AZURE_OPEN_AI_API_KEY = "azureOpenAiApiKey";
    public static final String GEMINI_API_KEY = "geminiApiKey";

    private static CredentialAttributes createAttributes(String secretKey) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(getServiceName(), secretKey));
    }

    public static void saveSecret(String secretKey, String secretValue) {
        PasswordSafe.getInstance().setPassword(createAttributes(secretKey), secretValue);
    }

    public static String getSecret(String secretKey) {
        return PasswordSafe.getInstance().getPassword(createAttributes(secretKey));
    }

    private static String getServiceName() {
        var appInfo = ApplicationInfo.getInstance();
        return String.format("%s %s - (%s plugin)",
                appInfo.getVersionName(),
                appInfo.getFullVersion(),
                TagsMessageBundle.message("plugin.label"));
    }

}
