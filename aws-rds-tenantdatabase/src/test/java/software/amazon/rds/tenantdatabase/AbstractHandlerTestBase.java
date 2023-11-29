package software.amazon.rds.tenantdatabase;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.OngoingStubbing;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.CreateTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.DeleteTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.DeleteTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.TenantDatabase;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.common.logging.RequestLogger;
import software.amazon.rds.common.printer.FilteredJsonPrinter;
import software.amazon.rds.test.common.core.AbstractTestBase;
import software.amazon.rds.test.common.core.HandlerName;
import software.amazon.rds.test.common.core.MethodCallExpectation;

public abstract class AbstractHandlerTestBase extends AbstractTestBase<TenantDatabase, ResourceModel, CallbackContext> {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final TenantDatabase TENANT_DATABASE;
  protected static final TenantDatabase RENAMED_TENANT_DATABASE;
  protected static final ResourceModel.ResourceModelBuilder BASE_RESOURCE_MODEL_BUILDER;
  protected static final ResourceModel CREATE_RESOURCE_MODEL;
  protected static final ResourceModel MODIFY_RESOURCE_MODEL;
  protected static final TenantDatabase.Builder BASE_TENANT_DATABASE_BUILDER;
  protected static final LoggerProxy logger;
  protected static final String MSG_GENERIC_ERR = "Error";
  protected static final String DB_INSTANCE_IDENTIFIER = "test-multitenant-instance";
  protected static final String TENANT_DATABASE_NAME = "tdb1";
  protected static final String NEW_TENANT_DATABASE_NAME = "tdb2";
  protected static final String MASTER_USER_NAME = "masteruser";
  protected static final String AVAILABLE_STATUS = "available";

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    BASE_TENANT_DATABASE_BUILDER = TenantDatabase.builder()
            .tenantDBName(TENANT_DATABASE_NAME)
            .masterUsername(MASTER_USER_NAME)
            .dbInstanceIdentifier(DB_INSTANCE_IDENTIFIER)
            .tenantDatabaseCreateTime(Instant.now());

    TENANT_DATABASE = BASE_TENANT_DATABASE_BUILDER.status(AVAILABLE_STATUS).build();
    RENAMED_TENANT_DATABASE = BASE_TENANT_DATABASE_BUILDER
            .tenantDBName(NEW_TENANT_DATABASE_NAME)
            .status(AVAILABLE_STATUS)
            .build();
    BASE_RESOURCE_MODEL_BUILDER = ResourceModel.builder()
            .dBInstanceIdentifier(DB_INSTANCE_IDENTIFIER)
            .tenantDBName(TENANT_DATABASE_NAME)
            .masterUsername(MASTER_USER_NAME);
    CREATE_RESOURCE_MODEL = BASE_RESOURCE_MODEL_BUILDER.masterUserPassword("password").build();
    MODIFY_RESOURCE_MODEL = BASE_RESOURCE_MODEL_BUILDER
            .masterUserPassword("newPassword")
            .newTenantDBName(NEW_TENANT_DATABASE_NAME)
            .build();
    logger = new LoggerProxy();
  }


  protected abstract BaseHandlerStd getHandler();

  protected abstract AmazonWebServicesClientProxy getProxy();

  protected abstract ProxyClient<RdsClient> getRdsProxy();

  public abstract HandlerName getHandlerName();

  private static final JSONObject resourceSchema = new Configuration().resourceSchemaJsonObject();

  static ProxyClient<RdsClient> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final RdsClient rdsClient) {
    return new ProxyClient<RdsClient>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public RdsClient client() {
        return rdsClient;
      }
    };
  }

  @Override
  protected String getLogicalResourceIdentifier() {
    return "tenantdatabase";
  }

  @Override
  protected void expectResourceSupply(Supplier<TenantDatabase> supplier) {

  }


  @Override
  protected ProgressEvent<ResourceModel, CallbackContext> invokeHandleRequest(
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext context
  ) {
    return getHandler().handleRequest(
            getProxy(),
            request,
            context,
            getRdsProxy(),
            new RequestLogger(logger, request, new FilteredJsonPrinter())
    );
  }

  protected MethodCallExpectation<CreateTenantDatabaseRequest, CreateTenantDatabaseResponse> expectCreateTenantDatabaseCall() {
    return new MethodCallExpectation<CreateTenantDatabaseRequest, CreateTenantDatabaseResponse>() {
      @Override
      public OngoingStubbing<CreateTenantDatabaseResponse> setup() {
        return when(getRdsProxy().client().createTenantDatabase(any(CreateTenantDatabaseRequest.class)));
      }

      @Override
      public ArgumentCaptor<CreateTenantDatabaseRequest> verify() {
        final ArgumentCaptor<CreateTenantDatabaseRequest> captor = ArgumentCaptor.forClass(CreateTenantDatabaseRequest.class);
        Mockito.verify(getRdsProxy().client(), times(1)).createTenantDatabase(captor.capture());
        return captor;
      }
    };
  }

  protected MethodCallExpectation<DeleteTenantDatabaseRequest, DeleteTenantDatabaseResponse> expectDeleteTenantDatabaseCall() {
    return new MethodCallExpectation<DeleteTenantDatabaseRequest, DeleteTenantDatabaseResponse>() {
      @Override
      public OngoingStubbing<DeleteTenantDatabaseResponse> setup() {
        return when(getRdsProxy().client().deleteTenantDatabase(any(DeleteTenantDatabaseRequest.class)));
      }

      @Override
      public ArgumentCaptor<DeleteTenantDatabaseRequest> verify() {
        final ArgumentCaptor<DeleteTenantDatabaseRequest> captor = ArgumentCaptor.forClass(DeleteTenantDatabaseRequest.class);
        Mockito.verify(getRdsProxy().client(), times(1)).deleteTenantDatabase(captor.capture());
        return captor;
      }
    };
  }

  protected MethodCallExpectation<ModifyTenantDatabaseRequest, ModifyTenantDatabaseResponse> expectModifyTenantDatabaseCall() {
    return new MethodCallExpectation<ModifyTenantDatabaseRequest, ModifyTenantDatabaseResponse>() {
      @Override
      public OngoingStubbing<ModifyTenantDatabaseResponse> setup() {
        return when(getRdsProxy().client().modifyTenantDatabase(any(ModifyTenantDatabaseRequest.class)));
      }

      @Override
      public ArgumentCaptor<ModifyTenantDatabaseRequest> verify() {
        final ArgumentCaptor<ModifyTenantDatabaseRequest> captor = ArgumentCaptor.forClass(ModifyTenantDatabaseRequest.class);
        Mockito.verify(getRdsProxy().client(), times(1)).modifyTenantDatabase(captor.capture());
        return captor;
      }
    };
  }

  protected MethodCallExpectation<DescribeTenantDatabasesRequest, DescribeTenantDatabasesResponse> expectDescribeTenantDatabasesCall(final int wantedNumberOfInvocations) {
    return new MethodCallExpectation<DescribeTenantDatabasesRequest, DescribeTenantDatabasesResponse>() {
      @Override
      public OngoingStubbing<DescribeTenantDatabasesResponse> setup() {
        return when(getRdsProxy().client().describeTenantDatabases(any(DescribeTenantDatabasesRequest.class)));
      }

      @Override
      public ArgumentCaptor<DescribeTenantDatabasesRequest> verify() {
        final ArgumentCaptor<DescribeTenantDatabasesRequest> captor = ArgumentCaptor.forClass(DescribeTenantDatabasesRequest.class);
        Mockito.verify(getRdsProxy().client(), times(wantedNumberOfInvocations)).describeTenantDatabases(captor.capture());
        return captor;
      }
    };
  }
}
