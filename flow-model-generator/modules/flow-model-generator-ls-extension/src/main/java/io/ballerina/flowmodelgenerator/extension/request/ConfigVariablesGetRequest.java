package io.ballerina.flowmodelgenerator.extension.request;

/**
 * Represents the request to get config variables.
 *
 * @param configFilePath path of the config file
 * @since 1.4.0
 */
public record ConfigVariablesGetRequest(String configFilePath) {
}
