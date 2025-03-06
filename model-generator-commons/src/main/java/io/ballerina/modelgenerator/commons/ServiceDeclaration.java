package io.ballerina.modelgenerator.commons;

public record ServiceDeclaration(Package packageInfo, String displayName,
                                 int optionalTypeDescriptor, String typeDescriptorLabel,
                                 String typeDescriptorDescription, String typeDescriptorDefaultValue,
                                 int addDefaultTypeDescriptor, int optionalAbsoluteResourcePath,
                                 String absoluteResourcePathLabel, String absoluteResourcePathDescription,
                                 String absoluteResourcePathDefaultValue, int optionalStringLiteral,
                                 String stringLiteralLabel, String stringLiteralDescription,
                                 String stringLiteralDefaultValue, String listenerKind) {

    public record Package(int packageId, String org, String name, String version) {
    }
}
