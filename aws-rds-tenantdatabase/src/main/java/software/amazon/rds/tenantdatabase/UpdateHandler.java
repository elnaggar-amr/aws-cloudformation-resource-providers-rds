package software.amazon.rds.tenantdatabase;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.TenantDatabase;
import software.amazon.awssdk.services.rds.model.TenantDatabaseNotFoundException;
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
                    .translateToServiceRequest(Translator::translateToModifyTenantDatabaseRequest)
                    .makeServiceCall((awsRequest, proxyInvocation) -> {
                        if (StringUtils.isEmpty(awsRequest.tenantDBName())
                                || org.apache.commons.lang3.StringUtils.isEmpty(awsRequest.dbInstanceIdentifier())) {
                            TenantDatabase tdb = getTenantDatabaseWithTdbResourceId(progress.getResourceModel(), proxyClient);
                            if (tdb != null) {
                                awsRequest = awsRequest.toBuilder().tenantDBName(tdb.tenantDBName())
                                        .dbInstanceIdentifier(tdb.dbInstanceIdentifier()).build();
                            } else {
                                throw TenantDatabaseNotFoundException.builder().message("Tenant Database not found").build();
                            }
                        }
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
