package software.amazon.rds.tenantdatabase;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DeleteTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.TenantDatabase;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.common.handler.Commons;
import software.amazon.rds.common.logging.RequestLogger;
import software.amazon.rds.common.util.IdentifierFactory;

public class DeleteHandler extends BaseHandlerStd {
    private static final String SNAPSHOT_PREFIX = "Snapshot-";
    private static final int SNAPSHOT_MAX_LENGTH = 255;

    private static final IdentifierFactory snapshotIdentifierFactory = new IdentifierFactory(
            STACK_NAME,
            SNAPSHOT_PREFIX + RESOURCE_IDENTIFIER,
            SNAPSHOT_MAX_LENGTH
    );

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RdsClient> proxyClient,
        final RequestLogger logger) {

        this.logger = logger;
        final String finalSnapshotId;
        if (BooleanUtils.isNotFalse(request.getSnapshotRequested())) {
            finalSnapshotId = snapshotIdentifierFactory.newIdentifier()
                    .withStackId(request.getStackId())
                    .withResourceId(StringUtils.prependIfMissing(request.getLogicalResourceIdentifier(), SNAPSHOT_PREFIX))
                    .withRequestToken(request.getClientRequestToken())
                    .toString();
        } else {
            finalSnapshotId = null;
        }

        logger.log("TestingAelnagg: We are calling Delete with: " + request.getDesiredResourceState());

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("rds::delete-tenant-database", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteTenantDatabaseRequest)
                    .makeServiceCall((awsRequest, proxyInvocation) -> {
                        if (finalSnapshotId != null) {
                            awsRequest = awsRequest.toBuilder().finalDBSnapshotIdentifier(finalSnapshotId).skipFinalSnapshot(false).build();
                        } else {
                            awsRequest = awsRequest.toBuilder().skipFinalSnapshot(true).build();
                        }

                        updateResourceModelForServiceCall(progress.getResourceModel(), proxyClient);
                        awsRequest = awsRequest.toBuilder().tenantDBName(progress.getResourceModel().getTenantDBName())
                                .dbInstanceIdentifier(progress.getResourceModel().getDBInstanceIdentifier()).build();

                        final DeleteTenantDatabaseResponse response = proxyInvocation.injectCredentialsAndInvokeV2(
                                awsRequest, proxyInvocation.client()::deleteTenantDatabase);
                        updateResourceModel(response.tenantDatabase(), progress.getResourceModel());

                        logger.log("TestingAelnagg: Deleting-tenant-database");
                        return response;
                    })
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        final TenantDatabase tenantDatabase = BaseHandlerStd.getTenantDatabase(model, proxyClient);
                        return tenantDatabase == null;
                    })
                    .handleError((awsRequest, exception, client, model, context) -> Commons.handleException(
                            ProgressEvent.progress(model, context),
                            exception,
                            DELETE_TENANT_DATABASE_ERR0R_RULE_SET
                    ))
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
