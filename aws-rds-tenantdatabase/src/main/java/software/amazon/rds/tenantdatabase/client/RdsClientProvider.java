package software.amazon.rds.tenantdatabase.client;

import lombok.SneakyThrows;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.rds.common.client.BaseSdkClientProvider;

import java.net.URI;

public class RdsClientProvider extends BaseSdkClientProvider<RdsClientBuilder, RdsClient> {


    @Override
    @SneakyThrows
    public RdsClient getClient() {
        return setUserAgent(setHttpClient(RdsClient.builder())).build();
    }
}
