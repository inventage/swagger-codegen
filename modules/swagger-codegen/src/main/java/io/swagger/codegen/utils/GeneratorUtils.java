package io.swagger.codegen.utils;

import com.google.common.collect.ImmutableMap;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Commonly used helper methods for code generation.
 *
 * @author Simon Marti
 */
public final class GeneratorUtils {

    //---- Static

    private static final String SHORT_APP_NAME = "shortAppName";
    private static final String SERVICE_ENDPOINT_NAME = "serviceEndpointName";
    private static final Pattern BASE_PATH_PATTERN = Pattern.compile("^/*([^/]+)(/.*|$)");

    /**
     * Groups operations by their base path, i.e. the first path segment in their respective URL.
     *
     * <p>For example: The resources /employees, /employees/{id} and /offices would be grouped into two API classes, EmployeesApi and OfficesApi.
     *
     * @param tag tag for operation
     * @param resourcePath full URL for operation
     * @param operation operation Swagger definition
     * @param codegenOperation operation data used for code generation
     * @param operationGroups existing groups of operations (will be modified by this method)
     */
    public static void groupOperationsByBasePath(String tag, String resourcePath, Operation operation, CodegenOperation codegenOperation,
                                                 Map<String, List<CodegenOperation>> operationGroups) {
        final Matcher basePathMatch = BASE_PATH_PATTERN.matcher(resourcePath);
        final String basePath;

        if (!basePathMatch.matches()) {
            basePath = "default";
        }
        else {
            basePath = basePathMatch.group(1).replaceAll("[^a-zA-Z]", "");
        }

        final List<CodegenOperation> operations = operationGroups.computeIfAbsent(basePath, key -> new ArrayList<>());
        if (operations.stream().anyMatch(cdgnOperation -> cdgnOperation.operationId.equals(codegenOperation.operationId))) {
            // This method is called multiple times for every tag of the operation, so we're ignoring everything but the first call.
            return;
        }

        operations.add(codegenOperation);

        codegenOperation.subresourceOperation = !codegenOperation.path.isEmpty();
        codegenOperation.baseName = basePath;
    }

    /**
     * Groups operations by their client group (vendor extension: 'x-client-group') or base path (if the client group is not available).
     *
     * @param tag tag for operation
     * @param resourcePath full URL for operation
     * @param operation operation Swagger definition
     * @param codegenOperation operation data used for code generation
     * @param operationGroups existing groups of operations (will be modified by this method)
     */
    public static void groupOperationsClientGroup(String tag, String resourcePath, Operation operation, CodegenOperation codegenOperation,
                                                  Map<String, List<CodegenOperation>> operationGroups) {
        final Matcher basePathMatch = BASE_PATH_PATTERN.matcher(resourcePath);
        final String basePath;

        if (!basePathMatch.matches()) {
            basePath = "default";
        }
        else {
            basePath = basePathMatch.group(1).replaceAll("[^a-zA-Z]", "");
        }

        String clientGroupKey = basePath;
        if (codegenOperation.vendorExtensions.containsKey("x-client-group")) {
            clientGroupKey = (String) codegenOperation.vendorExtensions.get("x-client-group");
        }

        final List<CodegenOperation> operations = operationGroups.computeIfAbsent(clientGroupKey, key -> new ArrayList<>());
        if (operations.stream().anyMatch(cdgnOperation -> cdgnOperation.operationId.equals(codegenOperation.operationId))) {
            // This method is called multiple times for every tag of the operation, so we're ignoring everything but the first call.
            return;
        }

        operations.add(codegenOperation);

        codegenOperation.subresourceOperation = !codegenOperation.path.isEmpty();
        codegenOperation.baseName = basePath;
    }

    /**
     * Don't group operations, create separate API classes for every operation.
     *
     * @param tag tag for operation
     * @param resourcePath full URL for operation
     * @param operation operation Swagger definition
     * @param codegenOperation operation data used for code generation
     * @param operationGroups existing groups of operations (will be modified by this method)
     */
    public static void groupOperationsByOperationId(String tag, String resourcePath, Operation operation, CodegenOperation codegenOperation,
                                                    Map<String, List<CodegenOperation>> operationGroups) {

        operationGroups.computeIfAbsent(codegenOperation.operationId, key -> new ArrayList<>()).add(codegenOperation);
    }

    /**
     * Returns path to the .swagger-codegen-ignore file.
     *
     * @param outputFolder output folder of the Swagger code generator
     * @return path to the .swagger-codegen-ignore file
     */
    public static File ignoreFile(String outputFolder) {
        final String ignoreFileNameTarget = outputFolder + File.separator + ".swagger-codegen-ignore";
        return new File(ignoreFileNameTarget);
    }

    /**
     * Returns the given string with all whitespace stripped, every letter following whitespace in uppercase and every other letter in lowercase.
     *
     * <p>Example: {@code "Some Sample REST Application"} becomes {@code "someSampleRestApplication"}</p>
     *
     * @param string string to transform
     * @return {@code string} in camel-case
     */
    public static String camelizeSpacedString(String string) {
        final String strippedName = string.toLowerCase(Locale.US).replaceAll("[^a-zA-Z]", "");
        final Matcher matcher = Pattern.compile("\\s+(\\w)").matcher(strippedName);

        final StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group(1).toUpperCase(Locale.US));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extracts the short app name {@code x-short-name} from the swagger definition and stores it as an additional property.
     *
     * <p>If the {@code x-short-name} is not present in the Swagger definition's {@code info} section, the {@code title} is used instead.</p>
     *
     * @param additionalProperties additional properties of the swagger code generator
     * @param swagger Swagger definition
     * @return the short app name
     */
    public static String extractShortAppName(Map<String, Object> additionalProperties, Swagger swagger) {
        final Object configuredShortAppName = swagger.getInfo().getVendorExtensions().get("x-short-name");
        final String serviceEndpointName;
        if (configuredShortAppName instanceof String) {
            serviceEndpointName = camelizeSpacedString((String) configuredShortAppName);
        }
        else {
            serviceEndpointName = camelizeSpacedString(swagger.getInfo().getTitle());
        }
        final String shortAppName = capitalize(serviceEndpointName);
        additionalProperties.put(SHORT_APP_NAME, shortAppName);
        additionalProperties.put(SERVICE_ENDPOINT_NAME, serviceEndpointName);
        return shortAppName;
    }

    /**
     * Returns the given string in uppercase, with underscores before every capital letter of the original string.
     *
     * <p>Example: {@code "partnerId"} becomes {@code "PARTNER_ID"}</p>
     *
     * @param string string to transform
     * @return {@code string} in uppercase
     */
    public static String constantName(String string) {
        final Matcher matcher = Pattern.compile("[A-Z]").matcher(string);

        final StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, "_" + matcher.group(0));
        }
        matcher.appendTail(result);

        return result.toString().toUpperCase(Locale.US);
    }

    /**
     * Processes {@code x-complex-type} parameters by a {@code complexType} vendor extension to the parameter.
     *
     * @param imports list of imported classes
     * @param parameter parameter to process
     * @param models list of available model objects
     * @param generator caller
     */
    @SuppressWarnings("unchecked")
    public static void processParameterExtensions(Set<String> imports, CodegenParameter parameter, Map<String, Model> models, DefaultCodegen generator) {
        // Allow referencing enum definitions from path parameters, which is not allowed by Swagger 2.0
        final String enumReference = (String) parameter.vendorExtensions.get("x-ref");
        if (parameter.isEnum && enumReference != null) {
            parameter.datatypeWithEnum = enumReference;
            parameter.enumName = enumReference;
            imports.add(enumReference);
        }
        else if (parameter.items != null && parameter.items.isEnum) {
            final String enumItemReference = (String) parameter.items.vendorExtensions.get("x-ref");
            if (enumItemReference != null && parameter.isListContainer) {
                parameter.enumName = enumItemReference;
                parameter.datatypeWithEnum = String.format("List<%s>", enumItemReference);
                imports.add(enumItemReference);
            }
        }

        // Process complex types (i.e. matrix parameters)
        if (parameter.vendorExtensions.containsKey("x-complex-type")) {
            final Map<String, Object> complexType = (Map<String, Object>) parameter.vendorExtensions.get("x-complex-type");
            final Map<String, Object> processedComplexType = newHashMap();

            final String reference = (String) complexType.get("ref");
            final String baseProperty = (String) complexType.get("baseProperty");
            final Map<String, String> properties = (Map<String, String>) complexType.get("properties");

            processedComplexType.put("properties", properties.entrySet()
                    .stream()
                    .map(property -> ImmutableMap.of(
                            "key", property.getKey(),
                            "property", property.getValue(),
                            "getter", "get" + generator.getterAndSetterCapitalize(property.getValue()),
                            "type", referencedPropertyType(reference, property.getValue(), models, generator)
                    ))
                    .collect(Collectors.toList()));

            imports.add(reference);
            imports.add("PathSegment");
            imports.addAll(
                    properties.values().stream().map(property -> referencedPropertyType(reference, property, models, generator)).collect(Collectors.toList())
            );

            processedComplexType.put("ref", reference);
            processedComplexType.put("baseProperty", baseProperty);
            processedComplexType.put("baseGetter", "get" + generator.getterAndSetterCapitalize(baseProperty));
            processedComplexType.put("basePropertyType", referencedPropertyType(reference, baseProperty, models, generator));
            parameter.vendorExtensions.put("complexType", processedComplexType);
        }
    }

    private static String referencedPropertyType(String reference, String propertyName, Map<String, Model> models, DefaultCodegen generator) {
        final Model model = models.get(reference);
        if (model == null) {
            throw new IllegalStateException("Referenced model class not found: " + reference);
        }

        final Property property = model.getProperties().get(propertyName);
        if (property == null) {
            throw new IllegalStateException("Referenced model class " + reference + " contains no property '" + propertyName + "'");
        }

        return generator.getSwaggerType(property);
    }


    //---- Constructor

    private GeneratorUtils() {
        // nop
    }

}
