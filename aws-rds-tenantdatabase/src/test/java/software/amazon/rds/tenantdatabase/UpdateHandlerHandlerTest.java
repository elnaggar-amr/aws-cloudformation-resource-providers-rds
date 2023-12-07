package software.amazon.rds.tenantdatabase;

import java.time.Duration;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.AuthorizationNotFoundException;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseResponse;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.awssdk.services.rds.model.InvalidDbInstanceStateException;
import software.amazon.awssdk.services.rds.model.TenantDatabaseAlreadyExistsException;
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
import software.amazon.rds.test.common.core.MethodCallExpectation;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerHandlerTest extends AbstractHandlerTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RdsClient> proxyClient;

    private UpdateHandler handler;

    @Mock
    RdsClient rdsClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        rdsClient = mock(RdsClient.class);
        proxyClient = MOCK_PROXY(proxy, rdsClient);
        handler = new UpdateHandler();
    }

    @AfterEach
    public void tear_down() {
        verify(rdsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(rdsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ModifyTenantDatabaseResponse modifyTenantDatabaseResponse = ModifyTenantDatabaseResponse.builder()
                .tenantDatabase(TENANT_DATABASE)
                .build();

        final DescribeTenantDatabasesResponse describeTenantDatabasesResponse = DescribeTenantDatabasesResponse.builder()
                .tenantDatabases(RENAMED_TENANT_DATABASE)
                .build();

        expectModifyTenantDatabaseCall().setup().thenReturn(modifyTenantDatabaseResponse);
        MethodCallExpectation<DescribeTenantDatabasesRequest, DescribeTenantDatabasesResponse>
        describeTenantDatabasesCallExpectation = expectDescribeTenantDatabasesCall(2);
        describeTenantDatabasesCallExpectation.setup().thenReturn(describeTenantDatabasesResponse);

        final CallbackContext context = new CallbackContext();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                test_handleRequest_base(
                        context,
                        () -> RENAMED_TENANT_DATABASE,
                        () -> MODIFY_RESOURCE_MODEL,
                        expectSuccess()
                );

        System.out.println(describeTenantDatabasesCallExpectation);
        describeTenantDatabasesCallExpectation.verify().getAllValues()
                .forEach(request -> Assertions.assertThat(request.filters().get(0).values().get(0)).isEqualTo(TENANT_DATABASE_RESOURCE_ID));

        verify(proxyClient.client(), times(1)).modifyTenantDatabase(any(ModifyTenantDatabaseRequest.class));

    }

    @ParameterizedTest
    @ArgumentsSource(ModifyTenantDatabasesExceptionArgumentsProvider.class)
    public void handleRequest_ModifyTenantDatabase_HandleException(
            final Object requestException,
            final HandlerErrorCode expectResponseCode
    ) {
        test_handleRequest_error(
                expectModifyTenantDatabaseCall(),
                new CallbackContext(),
                () -> MODIFY_RESOURCE_MODEL,
                requestException,
                expectResponseCode
        );
    }
    static class ModifyTenantDatabasesExceptionArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    // Put error codes below
                    Arguments.of(ErrorCode.InvalidParameterCombination, HandlerErrorCode.InvalidRequest),
                    Arguments.of(ErrorCode.InvalidParameterValue, HandlerErrorCode.InvalidRequest),
                    // Put exception classes below
                    Arguments.of(DbInstanceNotFoundException.builder().message(MSG_GENERIC_ERR).build(), HandlerErrorCode.NotFound),
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
        return HandlerName.UPDATE;
    }
}
