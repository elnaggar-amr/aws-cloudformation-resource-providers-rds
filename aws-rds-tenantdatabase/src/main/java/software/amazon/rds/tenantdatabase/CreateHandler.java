package software.amazon.rds.tenantdatabase;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.TenantDatabase;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.common.handler.Commons;
import software.amazon.rds.common.logging.RequestLogger;

public class CreateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RdsClient> proxyClient,
            final RequestLogger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("rds::create-tenant-database", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToCreateTenantDatabaseRequest)
                                .makeServiceCall((createRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(
                                        createRequest,
                                        proxyInvocation.client()::createTenantDatabase
                                ))
                                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                                    final TenantDatabase tenantDatabase = BaseHandlerStd.getTenantDatabase(model, proxyClient);
                                    return tenantDatabase != null && tenantDatabase.status().equalsIgnoreCase("available");
                                })
                                .handleError((awsRequest, exception, client, model, context) -> Commons.handleException(
                                        ProgressEvent.progress(model, context),
                                        exception,
                                        CREATE_TENANT_DATABASE_ERROR_RULE_SET
                                ))
                                .progress()
                )
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}