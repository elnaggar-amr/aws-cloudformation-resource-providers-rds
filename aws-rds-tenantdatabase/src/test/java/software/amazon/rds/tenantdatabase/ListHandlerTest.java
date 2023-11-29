package software.amazon.rds.tenantdatabase;

import org.assertj.core.api.Assertions;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import software.amazon.rds.test.common.core.HandlerName;
import software.amazon.rds.test.common.core.MethodCallExpectation;

import java.time.Duration;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractHandlerTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RdsClient> proxyClient;

    @Mock
    RdsClient rdsClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        rdsClient = mock(RdsClient.class);
        proxyClient = MOCK_PROXY(proxy, rdsClient);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeTenantDatabasesResponse describeTenantDatabasesResponse = DescribeTenantDatabasesResponse.builder()
                .tenantDatabases(TENANT_DATABASE)
                .marker("nextToken")
                .build();

        final MethodCallExpectation<DescribeTenantDatabasesRequest, DescribeTenantDatabasesResponse> describeTenantDatabasesCallExpectation =
                expectDescribeTenantDatabasesCall(1);

        describeTenantDatabasesCallExpectation.setup().thenReturn(describeTenantDatabasesResponse);

        final CallbackContext context = new CallbackContext();

        test_handleRequest_base(
                context,
                ResourceHandlerRequest.<ResourceModel>builder().nextToken("token"),
                null,
                null,
                BASE_RESOURCE_MODEL_BUILDER::build,
                expectSuccess());

        Assertions.assertThat(describeTenantDatabasesCallExpectation.verify().getValue().marker()).isEqualTo("token");
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
        return HandlerName.LIST;
    }
}
