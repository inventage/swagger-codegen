package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.languages.features.BeanValidationFeatures;
import io.swagger.codegen.languages.features.GzipFeatures;
import io.swagger.codegen.languages.features.PerformBeanValidationFeatures;
import io.swagger.models.Model;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static io.swagger.codegen.utils.GeneratorUtils.constantName;
import static java.lang.Character.isUpperCase;
import static java.lang.Math.abs;


public class InventageJavaCodegen extends AbstractJavaCodegen
        implements BeanValidationFeatures, PerformBeanValidationFeatures,
        GzipFeatures {

    static final String MEDIA_TYPE = "mediaType";

    @SuppressWarnings("hiding")
    private static final Logger LOGGER = LoggerFactory.getLogger(InventageJavaCodegen.class);

    private static final String USE_RX_JAVA = "useRxJava";
    private static final String USE_RX_JAVA2 = "useRxJava2";
    private static final String DO_NOT_USE_RX = "doNotUseRx";
    private static final String USE_PLAY24_WS = "usePlay24WS";
    private static final String PARCELABLE_MODEL = "parcelableModel";

    private static final String RETROFIT_1 = "retrofit";
    private static final String RETROFIT_2 = "retrofit2";

    private boolean useRxJava = false;
    private boolean useRxJava2 = false;
    private boolean doNotUseRx = true; // backwards compatibility for swagger configs that specify neither rx1 nor rx2 (mustache does not allow for boolean operators so we need this extra field)
    private boolean usePlay24WS = false;
    private boolean parcelableModel = false;
    private boolean useBeanValidation = false;
    private boolean performBeanValidation = false;
    private boolean useGzipFeature = false;

    /**
     * Constructor.
     */
    public InventageJavaCodegen() {
        super();
        outputFolder = "generated-code" + File.separator + "java";
        embeddedTemplateDir = templateDir = "InventageJavaModel";
        invokerPackage = "io.swagger.client";
        artifactId = "swagger-java-client";
        apiPackage = "io.swagger.client.api";
        modelPackage = "io.swagger.client.model";

        cliOptions.add(CliOption.newBoolean(USE_RX_JAVA, "Whether to use the RxJava adapter with the retrofit2 library."));
        cliOptions.add(CliOption.newBoolean(USE_RX_JAVA2, "Whether to use the RxJava2 adapter with the retrofit2 library."));
        cliOptions.add(CliOption.newBoolean(PARCELABLE_MODEL, "Whether to generate models for Android that implement Parcelable with the okhttp-gson library."));
        cliOptions.add(CliOption.newBoolean(USE_PLAY24_WS, "Use Play! 2.4 Async HTTP client (Play WS API)"));
        cliOptions.add(CliOption.newBoolean(SUPPORT_JAVA6, "Whether to support Java6 with the Jersey1 library."));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
        cliOptions.add(CliOption.newBoolean(PERFORM_BEANVALIDATION, "Perform BeanValidation"));
        cliOptions.add(CliOption.newBoolean(USE_GZIP_FEATURE, "Send gzip-encoded requests"));

        supportedLibraries.put("jersey1", "HTTP client: Jersey client 1.19.1. JSON processing: Jackson 2.7.0. Enable Java6 support using '-DsupportJava6=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        supportedLibraries.put("feign", "HTTP client: Netflix Feign 8.16.0. JSON processing: Jackson 2.7.0");
        supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.22.2. JSON processing: Jackson 2.7.0");
        supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.6.2. Enable Parcelable modles on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        supportedLibraries.put(RETROFIT_1, "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.3.1 (Retrofit 1.9.0). IMPORTANT NOTE: retrofit1.x is no longer actively maintained so please upgrade to 'retrofit2' instead.");
        supportedLibraries.put(RETROFIT_2, "HTTP client: OkHttp 3.2.0. JSON processing: Gson 2.6.1 (Retrofit 2.0.2). Enable the RxJava adapter using '-DuseRxJava[2]=true'. (RxJava 1.x or 2.x)");

        final CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        libraryOption.setEnum(supportedLibraries);
        // set okhttp-gson as the default
        libraryOption.setDefault("okhttp-gson");
        cliOptions.add(libraryOption);
        //setLibrary("okhttp-gson");

    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "inventage-java";
    }

    @Override
    public String getHelp() {
        return "Generates Java model classes.";
    }

    @Override
    public String getAlias(String name) {
        return name;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(USE_RX_JAVA) && additionalProperties.containsKey(USE_RX_JAVA2)) {
            LOGGER.warn("You specified both RxJava versions 1 and 2 but they are mutually exclusive. Defaulting to v2.");
        }
        else if (additionalProperties.containsKey(USE_RX_JAVA)) {
            this.setUseRxJava(Boolean.valueOf(additionalProperties.get(USE_RX_JAVA).toString()));
        }
        if (additionalProperties.containsKey(USE_RX_JAVA2)) {
            this.setUseRxJava2(Boolean.valueOf(additionalProperties.get(USE_RX_JAVA2).toString()));
        }
        if (!useRxJava && !useRxJava2) {
            additionalProperties.put(DO_NOT_USE_RX, true);
        }
        if (additionalProperties.containsKey(USE_PLAY24_WS)) {
            this.setUsePlay24WS(Boolean.valueOf(additionalProperties.get(USE_PLAY24_WS).toString()));
        }
        additionalProperties.put(USE_PLAY24_WS, usePlay24WS);

        if (additionalProperties.containsKey(PARCELABLE_MODEL)) {
            this.setParcelableModel(Boolean.valueOf(additionalProperties.get(PARCELABLE_MODEL).toString()));
        }
        // put the boolean value back to PARCELABLE_MODEL in additionalProperties
        additionalProperties.put(PARCELABLE_MODEL, parcelableModel);

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION));
        }

        if (additionalProperties.containsKey(PERFORM_BEANVALIDATION)) {
            this.setPerformBeanValidation(convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION));
        }

        if (additionalProperties.containsKey(USE_GZIP_FEATURE)) {
            this.setUseGzipFeature(convertPropertyToBooleanAndWriteBack(USE_GZIP_FEATURE));
        }

        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
        final String authFolder = (sourceFolder + '/' + invokerPackage + ".auth").replace(".", "/");

        //Common files
        if (performBeanValidation) {
            supportingFiles.add(new SupportingFile("BeanValidationException.mustache", invokerFolder,
                    "BeanValidationException.java"));
        }

        //TODO: add doc to retrofit1 and feign
        if ("feign".equals(getLibrary()) || "retrofit".equals(getLibrary())) {
            modelDocTemplateFiles.remove("model_doc.mustache");
            apiDocTemplateFiles.remove("api_doc.mustache");
        }

        if (!("feign".equals(getLibrary()) || usesAnyRetrofitLibrary())) {
            supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
            supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
            supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        }

        if ("feign".equals(getLibrary())) {
            additionalProperties.put("jackson", "true");
            supportingFiles.add(new SupportingFile("ParamExpander.mustache", invokerFolder, "ParamExpander.java"));
        }
        else if ("okhttp-gson".equals(getLibrary()) || StringUtils.isEmpty(getLibrary())) {
            // the "okhttp-gson" library template requires "ApiCallback.mustache" for async call
            supportingFiles.add(new SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"));
            supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
            supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            supportingFiles.add(new SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"));
            supportingFiles.add(new SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"));
            supportingFiles.add(new SupportingFile("GzipRequestInterceptor.mustache", invokerFolder, "GzipRequestInterceptor.java"));
            additionalProperties.put("gson", "true");
        }
        else if (usesAnyRetrofitLibrary()) {
            supportingFiles.add(new SupportingFile("auth/OAuthOkHttpClient.mustache", authFolder, "OAuthOkHttpClient.java"));
            supportingFiles.add(new SupportingFile("CollectionFormats.mustache", invokerFolder, "CollectionFormats.java"));
            additionalProperties.put("gson", "true");
        }
        else if ("jersey2".equals(getLibrary())) {
            supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            additionalProperties.put("jackson", "true");
        }
        else if ("jersey1".equals(getLibrary())) {
            additionalProperties.put("jackson", "true");
        }
        else {
            LOGGER.error("Unknown library option (-l/--library): " + getLibrary());
        }

        if (Boolean.TRUE.equals(additionalProperties.get(USE_PLAY24_WS))) {
            // remove unsupported auth
            supportingFiles.removeIf(sf -> sf.templateFile.startsWith("auth/"));

            // auth
            supportingFiles.add(new SupportingFile("play24/auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
            supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
            supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));

            // api client
            supportingFiles.add(new SupportingFile("play24/ApiClient.mustache", invokerFolder, "ApiClient.java"));

            // adapters
            supportingFiles
                    .add(new SupportingFile("play24/Play24CallFactory.mustache", invokerFolder, "Play24CallFactory.java"));
            supportingFiles.add(new SupportingFile("play24/Play24CallAdapterFactory.mustache", invokerFolder,
                    "Play24CallAdapterFactory.java"));
            additionalProperties.put("jackson", "true");
            additionalProperties.remove("gson");
        }

        if (additionalProperties.containsKey("jackson")) {
            supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"));
        }

        supportingFiles.clear();
        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        apiTemplateFiles.clear();
        apiTestTemplateFiles.clear();
    }

    private boolean usesAnyRetrofitLibrary() {
        return getLibrary() != null && getLibrary().contains(RETROFIT_1);
    }

    private boolean usesRetrofit2Library() {
        return getLibrary() != null && getLibrary().contains(RETROFIT_2);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        super.postProcessOperations(objs);
        if (usesAnyRetrofitLibrary()) {
            final Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
            if (operations != null) {
                final List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
                for (CodegenOperation operation : ops) {
                    if (operation.hasConsumes == Boolean.TRUE) {

                        if (isMultipartType(operation.consumes)) {
                            operation.isMultipart = Boolean.TRUE;
                        }
                        else {
                            operation.prioritizedContentTypes = prioritizeContentTypes(operation.consumes);
                        }
                    }
                    if (operation.returnType == null) {
                        operation.returnType = "Void";
                    }
                    if (usesRetrofit2Library() && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith("/")) {
                        operation.path = operation.path.substring(1);
                    }
                }
            }
        }
        return objs;
    }

    /**
     *  Prioritizes consumes mime-type list by moving json-vendor and json mime-types up front, but
     *  otherwise preserves original consumes definition order.
     *  [application/vnd...+json,... application/json, ..as is..]
     *
     * @param consumes consumes mime-type list
     * @return
     */
    static List<Map<String, String>> prioritizeContentTypes(List<Map<String, String>> consumes) {
        if (consumes.size() <= 1) {
            return consumes;
        }

        final List<Map<String, String>> prioritizedContentTypes = new ArrayList<>(consumes.size());

        final List<Map<String, String>> jsonVendorMimeTypes = new ArrayList<>(consumes.size());
        final List<Map<String, String>> jsonMimeTypes = new ArrayList<>(consumes.size());

        for (Map<String, String> consume : consumes) {
            if (isJsonVendorMimeType(consume.get(MEDIA_TYPE))) {
                jsonVendorMimeTypes.add(consume);
            }
            else if (isJsonMimeType(consume.get(MEDIA_TYPE))) {
                jsonMimeTypes.add(consume);
            }
            else {
                prioritizedContentTypes.add(consume);
            }
            consume.put("hasMore", "true");
        }

        prioritizedContentTypes.addAll(0, jsonMimeTypes);
        prioritizedContentTypes.addAll(0, jsonVendorMimeTypes);

        prioritizedContentTypes.get(prioritizedContentTypes.size() - 1).put("hasMore", null);

        return prioritizedContentTypes;
    }

    private static boolean isMultipartType(List<Map<String, String>> consumes) {
        final Map<String, String> firstType = consumes.get(0);
        if (firstType != null) {
            if ("multipart/form-data".equals(firstType.get(MEDIA_TYPE))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        final CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);

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
        importMapping.put("Optional", "java.util.Optional;");
        importMapping.put("Collectors", "java.util.stream.Collectors");
        importMapping.put("ArrayList", "java.util.ArrayList");
        importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        importMapping.remove("com.fasterxml.jackson.annotation.JsonProperty");

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

    }

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

    /**
     * {@inheritDoc}
     */
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

    public void setUseRxJava(boolean useRxJava) {
        this.useRxJava = useRxJava;
        doNotUseRx = false;
    }

    public void setUseRxJava2(boolean useRxJava2) {
        this.useRxJava2 = useRxJava2;
        doNotUseRx = false;
    }

    public void setDoNotUseRx(boolean doNotUseRx) {
        this.doNotUseRx = doNotUseRx;
    }

    public void setUsePlay24WS(boolean usePlay24WS) {
        this.usePlay24WS = usePlay24WS;
    }


    public void setParcelableModel(boolean parcelableModel) {
        this.parcelableModel = parcelableModel;
    }

    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    public void setPerformBeanValidation(boolean performBeanValidation) {
        this.performBeanValidation = performBeanValidation;
    }

    public void setUseGzipFeature(boolean useGzipFeature) {
        this.useGzipFeature = useGzipFeature;
    }

    private static final Pattern JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?");
    private static final Pattern JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?");

    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     *   application/json
     *   application/json; charset=UTF8
     *   APPLICATION/JSON
     */
    static boolean isJsonMimeType(String mime) {
        return mime != null && JSON_MIME_PATTERN.matcher(mime).matches();
    }

    /**
     * Check if the given MIME is a JSON Vendor MIME.
     * JSON MIME examples:
     *   application/vnd.mycompany+json
     *   application/vnd.mycompany.resourceA.version1+json
     */
    static boolean isJsonVendorMimeType(String mime) {
        return mime != null && JSON_VENDOR_MIME_PATTERN.matcher(mime).matches();
    }

    @Override
    public String toDefaultValue(Property p) {
        if (p instanceof ArrayProperty) {
            return "null";
        }
        return super.toDefaultValue(p);
    }

}
// CS: RESUME
