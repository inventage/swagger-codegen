package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.utils.GeneratorUtils;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static io.swagger.codegen.utils.GeneratorUtils.*;
import static java.lang.Character.isUpperCase;
import static java.lang.Math.abs;


@SuppressWarnings("Duplicates")
public class InventageJavaCodegen extends AbstractJavaCodegen {

    //---- Static

    private static final Logger LOG = LoggerFactory.getLogger(InventageJavaCodegen.class);


    //---- Constructor

    /**
     * Creates a new instance.
     */
    public InventageJavaCodegen() {
        super();
        outputFolder = "generated-code" + File.separator + "java";
        embeddedTemplateDir = templateDir = "InventageJava";
        invokerPackage = "com.inventage.example.client";
        artifactId = "inventage-java-api";
        apiPackage = "com.inventage.example.api";
        modelPackage = "com.inventage.example.model";
    }


    //---- Fields

    private Map<String, Model> models;


    //---- Methods

    /** {@inheritDoc} */
    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "inventage-java";
    }

    /** {@inheritDoc} */
    @Override
    public String getHelp() {
        return "Generates a JAX-RS server stub.";
    }

    /** {@inheritDoc} */
    @Override
    public String getAlias(String name) {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public void processOpts() {
        super.processOpts();

        supportingFiles.clear();
        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        apiTemplateFiles.clear();
        apiTestTemplateFiles.clear();

        additionalProperties.put("jackson", "true");
        additionalProperties.put("useBeanValidation", "true");

        apiTemplateFiles.put("interface.mustache", ".java");
        apiTemplateFiles.put("adapter.mustache", "Adapter.java");

        importMapping.put("DecimalMax", "javax.validation.constraints.DecimalMax");
        importMapping.put("DecimalMin", "javax.validation.constraints.DecimalMin");
        importMapping.put("JsonInclude", "com.fasterxml.jackson.annotation.JsonInclude");
        importMapping.put("JsonInclude.Include", "com.fasterxml.jackson.annotation.JsonInclude.Include");
        importMapping.put("JsonFormat", "com.fasterxml.jackson.annotation.JsonFormat");
        importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        importMapping.put("JsonCreator", "com.fasterxml.jackson.annotation.JsonCreator");
        importMapping.put("Pattern", "javax.validation.constraints.Pattern");
        importMapping.put("NotNull", "javax.validation.constraints.NotNull");
        importMapping.put("Max", "javax.validation.constraints.Max");
        importMapping.put("Min", "javax.validation.constraints.Min");
        importMapping.put("Size", "javax.validation.constraints.Size");
        importMapping.put("Valid", "javax.validation.Valid");
        importMapping.put("Serializable", "java.io.Serializable");
        importMapping.put("EqualsBuilder", "org.apache.commons.lang3.builder.EqualsBuilder");
        importMapping.put("ToStringBuilder", "org.apache.commons.lang3.builder.ToStringBuilder");
        importMapping.put("HashCodeBuilder", "org.apache.commons.lang3.builder.HashCodeBuilder");
        importMapping.put("Locale", "java.util.Locale");
        importMapping.put("Optional", "java.util.Optional");
        importMapping.put("Collectors", "java.util.stream.Collectors");
        importMapping.put("ArrayList", "java.util.ArrayList");
        importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        importMapping.put("PathSegment", "javax.ws.rs.core.PathSegment");
        importMapping.put("GET", "javax.ws.rs.GET");
        importMapping.put("POST", "javax.ws.rs.POST");
        importMapping.put("PUT", "javax.ws.rs.PUT");
        importMapping.put("DELETE", "javax.ws.rs.DELETE");
        importMapping.put("Map", "java.util.Map");
        importMapping.remove("com.fasterxml.jackson.annotation.JsonProperty");
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);

        if ( "/".equals(swagger.getBasePath()) ) {
            swagger.setBasePath("");
        }

        if ( swagger.getPaths() != null ) {
            for ( String pathname : swagger.getPaths().keySet() ) {
                Path path = swagger.getPath(pathname);
                if ( path.getOperations() != null ) {
                    for ( Operation operation : path.getOperations() ) {
                        if ( operation.getTags() != null ) {
                            List<Map<String, String>> tags = new ArrayList<Map<String, String>>();
                            for ( String tag : operation.getTags() ) {
                                Map<String, String> value = new HashMap<String, String>();
                                value.put("tag", tag);
                                value.put("hasMore", "true");
                                tags.add(value);
                            }
                            if ( tags.size() > 0 ) {
                                tags.get(tags.size() - 1).remove("hasMore");
                            }
                            if ( operation.getTags().size() > 0 ) {
                                String tag = operation.getTags().get(0);
                                operation.setTags(Arrays.asList(tag));
                            }
                            operation.setVendorExtension("x-tags", tags);
                        }
                    }
                }
            }
        }

        final String shortAppName = GeneratorUtils.extractShortAppName(additionalProperties, swagger);

        final String apiFolder = sourceFolder + File.separator + apiPackage.replace('.', '/');
        supportingFiles.add(new SupportingFile("application", apiFolder, shortAppName + "Application.java"));

        additionalProperties.put("swaggerFileApplication", true);
        /*
        final boolean swaggerFileApplication = !(isBlank(swagger.getBasePath()) || swagger.getBasePath().equals("/"));
        additionalProperties.put("swaggerFileApplication", swaggerFileApplication);
        if (swaggerFileApplication) {
            supportingFiles.add(new SupportingFile("swaggerApplication", apiFolder, shortAppName + "SwaggerFileApplication.java"));
        }
        */

        if (swagger.getPaths().values().stream()
                .flatMap(path -> path.getOperations().stream())
                .flatMap(operation -> operation.getParameters().stream())
                .anyMatch(parameter -> parameter.getVendorExtensions().containsKey("complexType"))) {
            additionalProperties.put("matrixParameters", "true");
            supportingFiles.add(new SupportingFile("matrixParameterUtils", apiFolder, "MatrixParameterUtils.java"));
        }

        // TODO: Override with additionalProperty
        /*
        final String testFolder = Paths.get(sourceFolder, "..", "..", "test", "java", apiPackage.replace('.', '/')).toString();
        supportingFiles.add(new SupportingFile("test", testFolder, shortAppName + "ApplicationImplementationsTest.java"));
        */

        // add full swagger definition in a mustache parameter
        final String swaggerDef = Json.pretty(swagger);
        this.additionalProperties.put("fullSwagger", swaggerDef);

        models = swagger.getDefinitions();
    }

    /** {@inheritDoc} */
    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        final CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);

        codegenModel.imports.remove("ApiModelProperty");
        codegenModel.imports.remove("ApiModel");
        if (codegenModel.dataType != null && codegenModel.vendorExtensions.containsKey("x-enumeration")) {
            if (additionalProperties.containsKey("jackson")) {
                codegenModel.imports.add("JsonCreator");
                codegenModel.imports.add("JsonValue");
            }

            codegenModel.imports.add("Locale");

            // String definitions usually don't get generated because they're considered an "alias" to the String class
            codegenModel.isAlias = false;
        }
        else if (!codegenModel.isEnum) {
            //final String lib = getLibrary();
            //Needed imports for Jackson based libraries
            if (additionalProperties.containsKey("jackson")) {
                codegenModel.imports.add("JsonProperty");
                codegenModel.imports.add("JsonInclude");
                codegenModel.imports.add("JsonInclude.Include");
                if (codegenModel.vars.stream().anyMatch(p -> p.isEnum)) {
                    codegenModel.imports.add("JsonValue");
                }
            }
            if (additionalProperties.containsKey("gson")) {
                codegenModel.imports.add("SerializedName");
            }

            codegenModel.vendorExtensions.put("hashCodeInitial", abs(codegenModel.name.hashCode() % 57) * 2 + 1);
            codegenModel.vendorExtensions.put("hashCodeMultiplier", abs(codegenModel.classFilename.hashCode() % 61) * 2 + 1);
            codegenModel.imports.add("HashCodeBuilder");
            codegenModel.imports.add("EqualsBuilder");
            codegenModel.imports.add("ToStringBuilder");
            codegenModel.imports.add("Collectors");
            codegenModel.imports.add("Optional");
            codegenModel.imports.add("ArrayList");

            for (final CodegenProperty var : codegenModel.vars) {
                var.vendorExtensions.put("constantName", "PN_" + constantName(var.name));
                var.vendorExtensions.put("cloneable", cloneable(var));
                if (var.isListContainer) {
                    var.items.vendorExtensions.put("cloneable", cloneable(var.items));
                }
            }

            codegenModel.vendorExtensions.put("simple", codegenModel.vars.size() == 1);
        }
        else { // enum class
            //Needed imports for Jackson's JsonValue
            if (additionalProperties.containsKey("jackson")) {
                codegenModel.imports.add("JsonValue");
            }
        }

        if (hasInnerEnum(codegenModel)) {
            codegenModel.imports.add("JsonValue");
        }

        if (serializableModel) {
            codegenModel.imports.add("Serializable");
        }

        for (CodegenProperty cgp : codegenModel.vars) {
            if (cgp.vendorExtensions.containsKey("x-use-offset-date-time")) {
                updateCgP(cgp, codegenModel);
            }
        }

        return codegenModel;
    }

    private void updateCgP(CodegenProperty cgp, CodegenModel codegenModel) {
        System.out.println("This model should use offsetDateTime: " + codegenModel.name + "." + cgp.name);
        cgp.datatype = "OffsetDateTime";
        cgp.datatypeWithEnum = "OffsetDateTime";
    }

    private boolean cloneable(CodegenProperty property) {
        return !(property.isEnum || property.isPrimitiveType || importMapping.containsKey(property.datatype));
    }

    private boolean hasInnerEnum(CodegenModel codegenModel) {
        return codegenModel.vars.stream()
                .anyMatch(codegenProperty -> codegenProperty.isEnum);
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation codegenOperation,
                                    Map<String, List<CodegenOperation>> operationGroups) {
        groupOperationsByOperationId(tag, resourcePath, operation, codegenOperation, operationGroups);
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        if (property.isDate || property.isDateTime) {
            model.imports.add("JsonFormat");

            if (dateLibrary.equals("java8-localdatetime")) {
                property.vendorExtensions.put("noTimeZone", true);
            }
        }

        if (property.pattern != null) {
            model.imports.add("Pattern");
        }

        final boolean isInt = property.isInteger || property.isLong;
        if (property.minimum != null) {
            model.imports.add(isInt ? "Min" : "DecimalMin");
        }
        if (property.maximum != null) {
            model.imports.add(isInt ? "Max" : "DecimalMax");
        }

        if (property.required) {
            model.imports.add("NotNull");
        }

        if (property.minLength != null || property.maxLength != null || property.minItems != null || property.maxItems != null) {
            model.imports.add("Size");
        }

        // Temporary fix for @Valid annotation: instead of all these exclusions, it would be nice to have a "isModelProperty"
        if (!property.isPrimitiveType && !property.isFloat && !property.isDate && !property.isDateTime) {
            model.imports.add("Valid");
        }

        if (property.vendorExtensions.containsKey("x-use-offset-date-time")) {
            model.imports.add("OffsetDateTime");
            property.datatype = "OffsetDateTime";
            property.datatypeWithEnum = "OffsetDateTime";
            property.baseType = "OffsetDateTime";
            System.out.println("Contains Key " + model.name + "." + property.name + ": " + property.datatypeWithEnum);
        }

        model.imports.remove("ApiModelProperty");
        model.imports.remove("ApiModel");
        model.imports.remove("JsonSerialize");
        model.imports.remove("ToStringSerializer");
        model.imports.remove("JsonValue");
        model.imports.remove("JsonProperty");
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        //Needed import for Gson based libraries
        if (additionalProperties.containsKey("gson")) {
            final List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
            final List<Object> models = (List<Object>) objs.get("models");
            for (Object _mo : models) {
                final Map<String, Object> mo = (Map<String, Object>) _mo;
                final CodegenModel cm = (CodegenModel) mo.get("model");
                // for enum model
                if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                    cm.imports.add(importMapping.get("SerializedName"));
                    final Map<String, String> item = new HashMap<String, String>();
                    item.put("import", importMapping.get("SerializedName"));
                    imports.add(item);
                }
            }
        }
        return objs;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if ( operations != null ) {
            @SuppressWarnings("unchecked")
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for ( CodegenOperation operation : ops ) {
                if (operation.hasConsumes == Boolean.TRUE) {
                    Map<String, String> firstType = operation.consumes.get(0);
                    if (firstType != null) {
                        if ("multipart/form-data".equals(firstType.get("mediaType"))) {
                            operation.isMultipart = Boolean.TRUE;
                        }
                    }
                }

                boolean isMultipartPost = false;
                List<Map<String, String>> consumes = operation.consumes;
                if(consumes != null) {
                    for(Map<String, String> consume : consumes) {
                        String mt = consume.get("mediaType");
                        if(mt != null) {
                            if(mt.startsWith("multipart/form-data")) {
                                isMultipartPost = true;
                            }
                        }
                    }
                }

                for(CodegenParameter parameter : operation.allParams) {
                    if(isMultipartPost) {
                        parameter.vendorExtensions.put("x-multipart", "true");
                    }
                }

                List<CodegenResponse> responses = operation.responses;
                if ( responses != null ) {
                    for ( CodegenResponse resp : responses ) {
                        if ( "0".equals(resp.code) ) {
                            resp.code = "200";
                        }

                        if (resp.baseType == null) {
                            resp.dataType = "void";
                            resp.baseType = "Void";
                            // set vendorExtensions.x-java-is-response-void to true as baseType is set to "Void"
                            resp.vendorExtensions.put("x-java-is-response-void", true);
                        }

                        if ("array".equals(resp.containerType)) {
                            resp.containerType = "List";
                        } else if ("map".equals(resp.containerType)) {
                            resp.containerType = "Map";
                        }
                    }
                }

                if ( operation.returnBaseType == null ) {
                    operation.returnType = "void";
                    operation.returnBaseType = "Void";
                    // set vendorExtensions.x-java-is-response-void to true as returnBaseType is set to "Void"
                    operation.vendorExtensions.put("x-java-is-response-void", true);
                }

                if ("array".equals(operation.returnContainer)) {
                    operation.returnContainer = "List";
                } else if ("map".equals(operation.returnContainer)) {
                    operation.returnContainer = "Map";
                }
            }
        }

        if (operations != null) {
            final List<LinkedHashMap> imports = (List<LinkedHashMap>) objs.get("imports");
            final List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (final CodegenOperation operation : ops) {
                LOG.info("Found: " + operation.httpMethod + " in " + operation);

                addImport(operation.httpMethod.toUpperCase(Locale.US), imports);
                importsForParamValidation(operation.pathParams, imports);
                importsForParamValidation(operation.queryParams, imports);
                importsForParamValidation(operation.bodyParams, imports);
            }
        }
        return objs;
    }

    /** {@inheritDoc} */
    @Override
    public CodegenParameter fromParameter(Parameter param, Set<String> imports) {
        final CodegenParameter parameter = super.fromParameter(param, imports);

        processParameterExtensions(imports, parameter, models, this);

        return parameter;
    }

    /** {@inheritDoc} */
    @Override
    public String getterAndSetterCapitalize(String name) {
        name = toVarName(name).codePoints()
                .filter(Character::isLetterOrDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        if (name.length() > 1 && isUpperCase(name.charAt(1))) {
            return name;
        }
        else {
            return camelize(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toDefaultValue(Property p) {
        if (p instanceof ArrayProperty) {
            return "null";
        }
        return super.toDefaultValue(p);
    }

    /** {@inheritDoc} */
    @Override
    public String toApiName(String name) {
        final String apiName = super.toApiName(name);
        if (apiName.endsWith("Api")) {
            return apiName.substring(0, apiName.lastIndexOf("Api"));
        }
        return initialCaps(name);
    }

    private void importsForParamValidation(List<CodegenParameter> params, List<LinkedHashMap> imports) {
        for (final CodegenParameter param : params) {
            if (param.pattern != null) {
                addImport("Pattern", imports);
            }

            final boolean isInt = param.isInteger || param.isLong;
            if (param.minimum != null) {
                addImport(isInt ? "Min" : "DecimalMin", imports);
            }
            if (param.maximum != null) {
                addImport(isInt ? "Max" : "DecimalMax", imports);
            }

            if (param.required && !param.isBodyParam) {
                addImport("NotNull", imports);
            }

            if (param.minLength != null || param.maxLength != null || param.minItems != null || param.maxItems != null) {
                addImport("Size", imports);
            }

            if (param.isBodyParam) {
                addImport("Valid", imports);
                addImport("NotNull", imports);
            }
        }
    }

    private void addImport(String importName, List<LinkedHashMap> imports) {
        final String importClass = importMapping.get(importName);
        if (importClass != null && !hasImport(importClass, imports)) {
            final LinkedHashMap<String, String> newImport = new LinkedHashMap<>();
            newImport.put("import", importClass);
            imports.add(newImport);
        }
    }

    private boolean hasImport(String _import, List<LinkedHashMap> imports) {
        for (final LinkedHashMap map : imports) {
            if (map.containsValue(_import)) {
                return true;
            }
        }
        return false;
    }

}
