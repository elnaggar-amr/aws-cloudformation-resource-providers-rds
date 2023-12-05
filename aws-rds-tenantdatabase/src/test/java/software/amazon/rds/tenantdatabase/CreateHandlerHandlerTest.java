package software.amazon.rds.tenantdatabase;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AuthorizationNotFoundException;
import software.amazon.awssdk.services.rds.model.CreateTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.CreateTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.awssdk.services.rds.model.InvalidDbInstanceStateException;
import software.amazon.awssdk.services.rds.model.TenantDatabaseAlreadyExistsException;
import software.amazon.awssdk.services.rds.model.TenantDatabaseQuotaExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import software.amazon.rds.common.error.ErrorCode;
import software.amazon.rds.test.common.core.HandlerName;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerHandlerTest extends AbstractHandlerTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RdsClient> proxyClient;

    private CreateHandler handler;

    @Mock
    RdsClient rdsClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        rdsClient = mock(RdsClient.class);
        proxyClient = MOCK_PROXY(proxy, rdsClient);
        handler = new CreateHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(rdsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(rdsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        doReturn(DescribeTenantDatabasesResponse.builder().tenantDatabases(TENANT_DATABASE).build())
                .when(rdsClient).describeTenantDatabases(any(DescribeTenantDatabasesRequest.class));

        final CallbackContext context = new CallbackContext();
        context.setCreated(true);

        test_handleRequest_base(
                context,
                () -> TENANT_DATABASE,
                () -> CREATE_RESOURCE_MODEL,
                expectSuccess()
        );


        verify(proxyClient.client(), times(1)).describeTenantDatabases(any(DescribeTenantDatabasesRequest.class));
    }

    @ParameterizedTest
    @ArgumentsSource(CreateTenantDatabasesExceptionArgumentsProvider.class)
    public void handleRequest_CreateTenantDatabase_HandleException(
            final Object requestException,
            final HandlerErrorCode expectResponseCode
    ) {
        test_handleRequest_error(
                expectCreateTenantDatabaseCall(),
                new CallbackContext(),
                () -> CREATE_RESOURCE_MODEL,
                requestException,
                expectResponseCode
        );
    }

    static class CreateTenantDatabasesExceptionArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    // Put error codes below
                    Arguments.of(ErrorCode.TenantDatabaseQuotaExceededFault, HandlerErrorCode.ServiceLimitExceeded),
                    Arguments.of(ErrorCode.InvalidParameterCombination, HandlerErrorCode.InvalidRequest),
                    Arguments.of(ErrorCode.InvalidParameterValue, HandlerErrorCode.InvalidRequest),
                    // Put exception classes below
                    Arguments.of(DbInstanceNotFoundException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.NotFound),
                    Arguments.of(TenantDatabaseQuotaExceededException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.ServiceLimitExceeded),
                    Arguments.of(InvalidDbInstanceStateException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.ResourceConflict),
                    Arguments.of(AuthorizationNotFoundException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.InvalidRequest),
                    Arguments.of(TenantDatabaseAlreadyExistsException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.AlreadyExists)
            );
        }
    }

    @Override
    protected BaseHandlerStd getHandler() {
        return handler;
    }

    @Override
    protected AmazonWebServicesClientProxy getProxy() {
        return proxy;
    }

    @Override
    protected ProxyClient<RdsClient> getRdsProxy() {
        return proxyClient;
    }

    @Override
    public HandlerName getHandlerName() {
        return HandlerName.CREATE;
    }
}
