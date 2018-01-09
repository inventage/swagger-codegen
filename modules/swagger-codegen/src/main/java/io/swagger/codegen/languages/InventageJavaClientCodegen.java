package io.swagger.codegen.languages;

import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

import java.util.List;
import java.util.Map;

import static io.swagger.codegen.utils.GeneratorUtils.groupOperationsIntoSingleGroup;

/**
 * Generates an interface usable by the RESTeasy client proxy framework.
 *
 * @author Simon Marti
 */
public class InventageJavaClientCodegen extends InventageJavaServerCodegen {

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "inventage-java-client";
    }


    /** {@inheritDoc} */
    @Override
    public void processOpts() {
        super.processOpts();

        apiTemplateFiles.clear();

        apiTemplateFiles.put("clientInterface.mustache", ".java");
    }

    /** {@inheritDoc} */
    @Override
    public void preprocessSwagger(final Swagger swagger) {
        super.preprocessSwagger(swagger);

        supportingFiles.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void addOperationToGroup(final String tag, final String resourcePath, final Operation operation, final CodegenOperation codegenOperation,
                                    final Map<String, List<CodegenOperation>> operationGroups) {
        groupOperationsIntoSingleGroup(tag, resourcePath, operation, codegenOperation, operationGroups);
    }

    /** {@inheritDoc} */
    @Override
    public String toApiName(final String name) {
        return shortAppName + "Client";
    }

    /** {@inheritDoc} */
    @Override
    public String getLibrary() {
        return JAX_RS;
    }

}
