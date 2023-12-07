package software.amazon.rds.tenantdatabase;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.TenantDatabase;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.common.handler.Commons;
import software.amazon.rds.common.logging.RequestLogger;

public class UpdateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RdsClient> proxyClient,
        final RequestLogger logger) {

        this.logger = logger;
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("rds::modify-tenant-database", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest((model) -> {
                        ModifyTenantDatabaseRequest modifyRequest = Translator.translateToModifyTenantDatabaseRequest(model);
                        if (StringUtils.isEmpty(modifyRequest.tenantDBName())) {
                            final TenantDatabase tenantDatabase = getTenantDatabaseWithTdbResourceId(model, proxyClient);
                            modifyRequest = tenantDatabase == null ? modifyRequest :
                                    modifyRequest.toBuilder().tenantDBName(tenantDatabase.tenantDBName())
                                    .dbInstanceIdentifier(tenantDatabase.dbInstanceIdentifier()).build();
                        }
                        return modifyRequest;
                    })
                    .makeServiceCall((awsRequest, proxyInvocation) -> {
                        ModifyTenantDatabaseResponse response = proxyInvocation.injectCredentialsAndInvokeV2(
                                awsRequest, proxyInvocation.client()::modifyTenantDatabase);
                        updateResourceModel(response.tenantDatabase(), progress.getResourceModel());
                        return response;
                    })
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final TenantDatabase tenantDatabase = BaseHandlerStd.getTenantDatabaseWithTdbResourceId(model, proxyClient);
                        return tenantDatabase != null && tenantDatabase.status().equalsIgnoreCase("available")
                                && (StringUtils.isEmpty(model.getNewTenantDBName()) ||
                                model.getNewTenantDBName().equalsIgnoreCase(tenantDatabase.tenantDBName()));
                    })
                    .handleError((awsRequest, exception, client, model, context) -> Commons.handleException(
                            ProgressEvent.progress(model, context),
                            exception,
                            MODIFY_TENANT_DATABASE_ERROR_RULE_SET
                    ))
                    .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
