package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.utils.GeneratorUtils;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static io.swagger.codegen.utils.GeneratorUtils.groupOperationsByOperationId;
import static io.swagger.codegen.utils.GeneratorUtils.processParameterExtensions;
import static org.apache.commons.lang3.StringUtils.isBlank;

/*
 * This class is originally forked from the swagger codegen project. The code quality of the swagger is miserable at best and we don't want to fix everything.
 */
// CS: STOP

/**
 * Forked from https://github.com/swagger-api/swagger-codegen/blob/b6d6356c469fe5548c012f35a6840b848187a8a0/modules/swagger-codegen/src/main/java/io/swagger/codegen/languages/JavaJAXRSSpecServerCodegen.java.
 */
@SuppressWarnings("PMD")
public class InventageTemp extends AbstractJavaJAXRSServerCodegen {

    private static final Logger LOG = LoggerFactory.getLogger(InventageTemp.class);
    private static final String RESOURCE_FOLDER = "resourceFolder";

    private boolean deleteIgnoreFile = false;
    private Map<String, Model> models;

    /**
     * Constructor.
     */
    public InventageTemp() {
        super();
        invokerPackage = "io.swagger.api";
        artifactId = "swagger-jaxrs-server";
        outputFolder = "generated-code/JavaJaxRS-Spec";

        modelTemplateFiles.put("model.mustache", ".java");

        apiTemplateFiles.clear();
        apiTemplateFiles.put("interface.mustache", ".java");
        apiTemplateFiles.put("adapter.mustache", "Adapter.java");

        apiPackage = "io.swagger.api";
        modelPackage = "io.swagger.model";

        apiTestTemplateFiles.clear(); // TODO: add api test template
        modelTestTemplateFiles.clear(); // TODO: add model test template

        // clear model and api doc template as this codegen
        // does not support auto-generated markdown doc at the moment
        //TODO: add doc templates
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");

        additionalProperties.put("title", title);

        typeMapping.put("date", "LocalDate");

        importMapping.put("LocalDate", "org.joda.time.LocalDate");

        super.embeddedTemplateDir = "JaxRsServer";
        templateDir = embeddedTemplateDir;

        for (int i = 0; i < cliOptions.size(); i++) {
            if (CodegenConstants.LIBRARY.equals(cliOptions.get(i).getOpt())) {
                cliOptions.remove(i);
                break;
            }
        }

        final CliOption library = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        library.setDefault(DEFAULT_LIBRARY);

        final Map<String, String> supportedLibraries = new LinkedHashMap<>();

        supportedLibraries.put(DEFAULT_LIBRARY, "JAXRS");
        library.setEnum(supportedLibraries);

        cliOptions.add(library);

        // Http method classes
        importMapping.put("GET", "javax.ws.rs.GET");
        importMapping.put("POST", "javax.ws.rs.POST");
        importMapping.put("PUT", "javax.ws.rs.PUT");

        // Validation classes
        importMapping.put("DecimalMax", "javax.validation.constraints.DecimalMax");
        importMapping.put("DecimalMin", "javax.validation.constraints.DecimalMin");
        importMapping.put("Pattern", "javax.validation.constraints.Pattern");
        importMapping.put("NotNull", "javax.validation.constraints.NotNull");
        importMapping.put("Max", "javax.validation.constraints.Max");
        importMapping.put("Min", "javax.validation.constraints.Min");
        importMapping.put("Size", "javax.validation.constraints.Size");
        importMapping.put("Valid", "javax.validation.Valid");
        importMapping.put("PathSegment", "javax.ws.rs.core.PathSegment");
    }

    @Override
    public void processOpts() {
        super.processOpts();

        String resourceFolder = "resources";

        if (additionalProperties.containsKey(RESOURCE_FOLDER)) {
            resourceFolder = (String) additionalProperties.get(RESOURCE_FOLDER);
        }

        supportingFiles.clear(); // Don't need extra files provided by AbstractJAX-RS & Java Codegen
        supportingFiles.add(new SupportingFile("swagger.mustache", resourceFolder, "swagger.json"));
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);

        final String shortAppName = GeneratorUtils.extractShortAppName(additionalProperties, swagger);

        final String apiFolder = sourceFolder + File.separator + apiPackage.replace('.', '/');
        supportingFiles.add(new SupportingFile("application", apiFolder, shortAppName + "Application.java"));
        supportingFiles.add(new SupportingFile("nodeStatus", apiFolder, shortAppName + "NodeStatusApplication.java"));
        supportingFiles.add(new SupportingFile("applicationData", apiFolder, shortAppName + "ApplicationData.java"));

        final boolean swaggerFileApplication = !(isBlank(swagger.getBasePath()) || swagger.getBasePath().equals("/"));
        additionalProperties.put("swaggerFileApplication", swaggerFileApplication);
        if (swaggerFileApplication) {
            supportingFiles.add(new SupportingFile("swaggerApplication", apiFolder, shortAppName + "SwaggerFileApplication.java"));
        }

        // TODO: Override with additionalProperty
        final String testFolder = Paths.get(sourceFolder, "..", "..", "test", "java", apiPackage.replace('.', '/')).toString();
        supportingFiles.add(new SupportingFile("test", testFolder, shortAppName + "ApplicationImplementationsTest.java"));

        // add full swagger definition in a mustache parameter
        final String swaggerDef = Json.pretty(swagger);
        this.additionalProperties.put("fullSwagger", swaggerDef);

        // If the ignore file does not exist at this point, it will be generated automatically and should be deleted later.
        deleteIgnoreFile = !GeneratorUtils.ignoreFile(outputFolder).exists();

        models = swagger.getDefinitions();
    }

    @Override
    public void processSwagger(Swagger swagger) {
        super.processSwagger(swagger);

        if (deleteIgnoreFile && GeneratorUtils.ignoreFile(outputFolder).exists() && !GeneratorUtils.ignoreFile(outputFolder).delete()) {
            LOG.warn("Unable to delete swagger-ignore file");
        }
    }

    @Override
    public String getName() {
        return "jaxrs-server";
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation codegenOperation,
                                    Map<String, List<CodegenOperation>> operationGroups) {
        groupOperationsByOperationId(tag, resourcePath, operation, codegenOperation, operationGroups);
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        model.imports.remove("ApiModelProperty");
        model.imports.remove("ApiModel");
        model.imports.remove("JsonSerialize");
        model.imports.remove("ToStringSerializer");
        model.imports.remove("JsonValue");
        model.imports.remove("JsonProperty");
    }

    @Override
    public String getHelp() {
        return "Generates a Java JAXRS Server according to JAXRS 2.0 specification.";
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        final Map<String, Object> newObjs = super.postProcessOperations(objs);

        final Map<String, Object> operations = (Map<String, Object>) newObjs.get("operations");
        if (operations != null) {
            final List<LinkedHashMap> imports = (List<LinkedHashMap>) newObjs.get("imports");
            final List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (final CodegenOperation operation : ops) {
                LOG.info("Found: " + operation.httpMethod + " in " + operation);

                addImport(operation.httpMethod.toUpperCase(Locale.US), imports);
                importsForParamValidation(operation.pathParams, imports);
                importsForParamValidation(operation.queryParams, imports);
                importsForParamValidation(operation.bodyParams, imports);
            }
        }
        return newObjs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CodegenParameter fromParameter(Parameter param, Set<String> imports) {
        final CodegenParameter parameter = super.fromParameter(param, imports);

        processParameterExtensions(imports, parameter, models, this);

        return parameter;
    }

    @Override
    public String getAlias(String name) {
        return name;
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
    
    @Override
    public String toApiName(String name) {
        final String apiName = super.toApiName(name);
        if (apiName.endsWith("Api")) {
            return apiName.substring(0, apiName.lastIndexOf("Api"));
        }
        return initialCaps(name);
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
// CS: RESUME
