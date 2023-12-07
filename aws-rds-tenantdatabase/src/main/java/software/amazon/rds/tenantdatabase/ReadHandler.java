package software.amazon.rds.tenantdatabase;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.common.handler.Commons;
import software.amazon.rds.common.logging.RequestLogger;

public class ReadHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RdsClient> proxyClient,
        final RequestLogger logger) {

        this.logger = logger;
        return proxy.initiate("rds::describe-tenant-database", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest((model) -> {
                    if (!StringUtils.isEmpty(model.getTenantDatabaseResourceId())) {
                        return Translator.translateToDescribeTenantDatabasesRequestWithTenantDBResourceId(model);
                    }
                    return Translator.translateToDescribeTenantDatabasesRequest(model);
                })
                .makeServiceCall((describeRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(
                        describeRequest,
                        proxyInvocation.client()::describeTenantDatabases
                ))
                .handleError((describeRequest, exception, client, model, context) -> Commons.handleException(
                        ProgressEvent.progress(model, context),
                        exception,
                        DESCRIBE_TENANT_DATABASES_ERROR_RULE_SET
                ))
                .done((describeRequest, describeResponse, proxyInvocation, resourceModel, context) -> {
                    final ResourceModel response = Translator.translateDescribeTenantDatabasesResponseToResourceModel(describeResponse);

                    if (response == null) {
                        return ProgressEvent.failed(resourceModel, context, HandlerErrorCode.NotFound, "Tenant database was not found");
                    }

                    return ProgressEvent.success(Translator.translateDescribeTenantDatabasesResponseToResourceModel(describeResponse), context);
                });
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequestForRename(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RdsClient> proxyClient,
            final RequestLogger logger) {
        if (!StringUtils.isEmpty(request.getDesiredResourceState().getNewTenantDBName())) {
            request.getDesiredResourceState().setTenantDBName(request.getDesiredResourceState().getNewTenantDBName());
        }
        return this.handleRequest(proxy, request, callbackContext, proxyClient, logger);
    }
}
