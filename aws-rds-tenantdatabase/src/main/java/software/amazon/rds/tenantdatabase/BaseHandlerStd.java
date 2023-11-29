package software.amazon.rds.tenantdatabase;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AuthorizationNotFoundException;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.awssdk.services.rds.model.InvalidDbInstanceStateException;
import software.amazon.awssdk.services.rds.model.TenantDatabase;
import software.amazon.awssdk.services.rds.model.TenantDatabaseAlreadyExistsException;
import software.amazon.awssdk.services.rds.model.TenantDatabaseNotFoundException;
import software.amazon.awssdk.services.rds.model.TenantDatabaseQuotaExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.common.error.ErrorCode;
import software.amazon.rds.common.error.ErrorRuleSet;
import software.amazon.rds.common.error.ErrorStatus;
import software.amazon.rds.common.handler.Commons;
import software.amazon.rds.common.logging.RequestLogger;
import software.amazon.rds.common.printer.FilteredJsonPrinter;
import software.amazon.rds.tenantdatabase.client.RdsClientProvider;


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    public static final String RESOURCE_IDENTIFIER = "tenantdatabase";
    public static final String STACK_NAME = "rds";

    protected RequestLogger logger;
    protected final FilteredJsonPrinter PARAMETERS_FILTER = new FilteredJsonPrinter("MasterUsername", "MasterUserPassword");



    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return RequestLogger.handleRequest(
                logger,
                request,
                PARAMETERS_FILTER,
                requestLogger -> handleRequest(
                        proxy,
                        request,
                        callbackContext != null ? callbackContext : new CallbackContext(),
                        proxy.newProxy(new RdsClientProvider()::getClient),
                        requestLogger
                )
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RdsClient> proxyClient,
            final RequestLogger logger);

    protected static final ErrorRuleSet.Builder DEFAULT_TENANT_DATABASE_ERROR_RULE_SET_BUILDER = ErrorRuleSet
            .extend(Commons.DEFAULT_ERROR_RULE_SET)
            .withErrorCodes(ErrorStatus.failWith(HandlerErrorCode.InvalidRequest),
                    ErrorCode.InvalidParameterCombination,
                    ErrorCode.InvalidParameterValue)
            .withErrorCodes(ErrorStatus.failWith(HandlerErrorCode.NotFound),
                    ErrorCode.TenantDatabaseNotFoundFault)
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.NotFound),
                    TenantDatabaseNotFoundException.class,
                    DbInstanceNotFoundException.class)
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.InvalidRequest),
                    AuthorizationNotFoundException.class);

    protected static final ErrorRuleSet CREATE_TENANT_DATABASE_ERROR_RULE_SET = DEFAULT_TENANT_DATABASE_ERROR_RULE_SET_BUILDER
            .withErrorCodes(ErrorStatus.failWith(HandlerErrorCode.ServiceLimitExceeded),
                    ErrorCode.TenantDatabaseQuotaExceededFault)
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.AlreadyExists),
                    TenantDatabaseAlreadyExistsException.class)
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.ServiceLimitExceeded),
                    TenantDatabaseQuotaExceededException.class)
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.ResourceConflict),
                    InvalidDbInstanceStateException.class)
            .build();

    protected static final ErrorRuleSet MODIFY_TENANT_DATABASE_ERROR_RULE_SET = DEFAULT_TENANT_DATABASE_ERROR_RULE_SET_BUILDER
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.AlreadyExists),
                    TenantDatabaseAlreadyExistsException.class)
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.ResourceConflict),
                    InvalidDbInstanceStateException.class)
            .build();

    protected static final ErrorRuleSet DELETE_TENANT_DATABASE_ERR0R_RULE_SET = DEFAULT_TENANT_DATABASE_ERROR_RULE_SET_BUILDER
            .withErrorClasses(ErrorStatus.failWith(HandlerErrorCode.ResourceConflict),
                    InvalidDbInstanceStateException.class)
            .build();

    protected static final ErrorRuleSet DESCRIBE_TENANT_DATABASES_ERROR_RULE_SET = DEFAULT_TENANT_DATABASE_ERROR_RULE_SET_BUILDER.build();

    /**
     * Calls DescribeTenantDatabases and return a single TenantDatabase.
     * @param resource
     * @param proxyClient
     * @return tenantDatabase
     */
    protected static TenantDatabase getTenantDatabase(final ResourceModel resource, final ProxyClient<RdsClient> proxyClient ) {
       final DescribeTenantDatabasesResponse response =
               proxyClient.client().describeTenantDatabases(Translator.translateToDescribeTenantDatabasesRequest(resource));

       if (CollectionUtils.isEmpty(response.tenantDatabases())) {
           return null;
       }

       return response.tenantDatabases().get(0);
    }

    /**
     * Calls DescribeTenantDatabases for Update handler, it uses the new Tenant Database Name.
     * Returns the renamed Tenant Database.
     * @param resource
     * @param proxyClient
     * @return
     */
    protected static TenantDatabase getRenamedTenantDatabase(final ResourceModel resource,
                                                             final ProxyClient<RdsClient> proxyClient) {
        final DescribeTenantDatabasesResponse response =
                proxyClient.client().describeTenantDatabases(Translator.translateToDescribeTenantDatabasesRequest(
                        ResourceModel.builder()
                                .tenantDBName(StringUtils.isEmpty(resource.getNewTenantDBName()) ? resource.getTenantDBName() : resource.getNewTenantDBName())
                                .dBInstanceIdentifier(resource.getDBInstanceIdentifier())
                                .build()));

        if (CollectionUtils.isEmpty(response.tenantDatabases())) {
            return null;
        }

        return response.tenantDatabases().get(0);
    }
}
