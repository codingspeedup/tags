package io.github.codingspeedup.tags.plugin.settings;

import com.azure.ai.openai.OpenAIServiceVersion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import io.github.codingspeedup.tags.ai.boundary.EnvironmentSettingsProvider;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.jspecify.annotations.NonNull;

import static io.github.codingspeedup.tags.plugin.settings.SettingsSecretManager.*;

@State(name = "TagsPluginSettings", storages = @Storage("tagsPluginSettings.xml"))
public class SettingsModel implements PersistentStateComponent<SettingsModel>, EnvironmentSettingsProvider {

    @Getter
    public boolean useAzureOpenAiModel = false;
    @Getter
    public String azureOpenAiUrl = "https://resource-group.cognitiveservices.azure.com";
    @Getter
    public String azureOpenAiDeployment = "gpt-4o-mini";
    @Getter
    public String azureOpenAiApiVersion = OpenAIServiceVersion.V2025_01_01_PREVIEW.getVersion();

    @Getter
    public boolean useAmazonBedrockModel = false;
    @Getter
    public String awsRegion = StringUtils.EMPTY;
    @Getter
    public String amazonBedrockModelId = StringUtils.EMPTY;

    @Getter
    public boolean useGeminiModel = false;
    @Getter
    public String geminiModel = StringUtils.EMPTY;

    @Getter
    public boolean useOllamaModel = false;
    @Getter
    public String ollamaUrl = "http://localhost:11434";
    @Getter
    public String ollamaModel = StringUtils.EMPTY;

    public static SettingsModel getInstance() {
        return ApplicationManager.getApplication().getService(SettingsModel.class);
    }

    @Override
    public SettingsModel getState() {
        return this;
    }

    @Override
    public void loadState(@NonNull SettingsModel state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public String getGeminiApiKey() {
        return SettingsSecretManager.getSecret(GEMINI_API_KEY);
    }

    public String getAzureOpenAiApiKey() {
        return SettingsSecretManager.getSecret(AZURE_OPEN_AI_API_KEY);
    }

    @Override
    public String getAwsAccessKeyId() {
        return SettingsSecretManager.getSecret(AWS_ACCESS_KEY_ID);
    }

    @Override
    public String getAwsSecretAccessKey() {
        return SettingsSecretManager.getSecret(AWS_SECRET_ACCESS_KEY);
    }

    @Override
    public String getAwsSessionToken() {
        return SettingsSecretManager.getSecret(AWS_SESSION_TOKEN);
    }

    @Override
    public String getAmazonBedrockModelToken() {
        return SettingsSecretManager.getSecret(AMAZON_BEDROCK_MODEL_TOKEN);
    }

}
