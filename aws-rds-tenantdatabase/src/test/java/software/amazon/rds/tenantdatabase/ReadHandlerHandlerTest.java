package software.amazon.rds.tenantdatabase;

import java.time.Duration;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AuthorizationNotFoundException;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
import software.amazon.rds.test.common.core.MethodCallExpectation;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerHandlerTest extends AbstractHandlerTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RdsClient> proxyClient;

    private ReadHandler handler;

    @Mock
    RdsClient rdsClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        rdsClient = mock(RdsClient.class);
        proxyClient = MOCK_PROXY(proxy, rdsClient);
        handler = new ReadHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(rdsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(rdsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeTenantDatabasesResponse describeTenantDatabasesResponse = DescribeTenantDatabasesResponse.builder()
                .tenantDatabases(TENANT_DATABASE)
                .build();

        final MethodCallExpectation<DescribeTenantDatabasesRequest, DescribeTenantDatabasesResponse> describeTenantDatabasesCallExpectation =
                expectDescribeTenantDatabasesCall(1);

        describeTenantDatabasesCallExpectation.setup().thenReturn(describeTenantDatabasesResponse);

        final CallbackContext context = new CallbackContext();

        test_handleRequest_base(
                context,
                () -> TENANT_DATABASE,
                BASE_RESOURCE_MODEL_BUILDER::build,
                expectSuccess());

        DescribeTenantDatabasesRequest capturedRequest = describeTenantDatabasesCallExpectation.verify().getValue();
        Assertions.assertThat(capturedRequest.dbInstanceIdentifier()).isEqualTo(DB_INSTANCE_IDENTIFIER);
        Assertions.assertThat(capturedRequest.tenantDBName()).isEqualTo(TENANT_DATABASE_NAME);
    }

    @Test
    public void handleRequest_TenantDatabaseNotFound() {
        final DescribeTenantDatabasesResponse describeTenantDatabasesResponse = DescribeTenantDatabasesResponse.builder()
                .build();

        final MethodCallExpectation<DescribeTenantDatabasesRequest, DescribeTenantDatabasesResponse> describeTenantDatabasesCallExpectation =
                expectDescribeTenantDatabasesCall(1);

        describeTenantDatabasesCallExpectation.setup().thenReturn(describeTenantDatabasesResponse);

        final CallbackContext context = new CallbackContext();

        test_handleRequest_base(
                context,
                () -> TENANT_DATABASE,
                BASE_RESOURCE_MODEL_BUILDER::build,
                expectFailed(HandlerErrorCode.NotFound));

        DescribeTenantDatabasesRequest capturedRequest = describeTenantDatabasesCallExpectation.verify().getValue();
        Assertions.assertThat(capturedRequest.dbInstanceIdentifier()).isEqualTo(DB_INSTANCE_IDENTIFIER);
        Assertions.assertThat(capturedRequest.tenantDBName()).isEqualTo(TENANT_DATABASE_NAME);
    }

    @ParameterizedTest
    @ArgumentsSource(DescribeTenantDatabasesExceptionArgumentsProvider.class)
    public void handleRequest_ReadHandler_HandleException(
            final Object requestException,
            final HandlerErrorCode expectResponseCode
    ) {
        test_handleRequest_error(
                expectDescribeTenantDatabasesCall(1),
                new CallbackContext(),
                BASE_RESOURCE_MODEL_BUILDER::build,
                requestException,
                expectResponseCode
        );
    }

    static class DescribeTenantDatabasesExceptionArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    // Put error codes below
                    Arguments.of(ErrorCode.InvalidParameterCombination, HandlerErrorCode.InvalidRequest),
                    Arguments.of(ErrorCode.InvalidParameterValue, HandlerErrorCode.InvalidRequest),
                    // Put exception classes below
                    Arguments.of(DbInstanceNotFoundException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.NotFound),
                    Arguments.of(AuthorizationNotFoundException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.InvalidRequest)
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
        return HandlerName.READ;
    }
}
