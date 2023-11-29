package software.amazon.rds.tenantdatabase;

import org.apache.commons.collections.CollectionUtils;
import software.amazon.awssdk.services.rds.model.CreateTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.DeleteTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesRequest;
import software.amazon.awssdk.services.rds.model.DescribeTenantDatabasesResponse;
import software.amazon.awssdk.services.rds.model.ModifyTenantDatabaseRequest;
import software.amazon.awssdk.services.rds.model.TenantDatabase;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

  /**
   * Translates TDB resource model to CreateTenantDatabase request.
   * @param model TDB resource model
   * @return CreateTenantDatabase request
   */
  static CreateTenantDatabaseRequest translateToCreateTenantDatabaseRequest(final ResourceModel model) {
    return CreateTenantDatabaseRequest.builder()
            .dbInstanceIdentifier(model.getDBInstanceIdentifier())
            .tenantDBName(model.getTenantDBName())
            .masterUsername(model.getMasterUsername())
            .masterUserPassword(model.getMasterUserPassword())
            .ncharCharacterSetName(model.getNcharCharacterSetName())
            .characterSetName(model.getCharacterSetName())
            .tags(TagHelper.translateTagsToSdk(model.getTags()))
            .build();
  }

  /**
   * Translates TDB resource model to DescribeTenantDatabases request.
   * @param model TDB resource model
   * @return  DescribeTenantDatabases request
   */
  static DescribeTenantDatabasesRequest translateToDescribeTenantDatabasesRequest(final ResourceModel model) {
    return DescribeTenantDatabasesRequest.builder()
            .dbInstanceIdentifier(model.getDBInstanceIdentifier())
            .tenantDBName(model.getTenantDBName())
            .build();
  }

  /**
   * Extract the tenant databases resource from DescribeTenantDatabases response, expects only 1 TenantDatabase in the response.
   * @param describeTenantDatabasesResponse DescribeTenantDatabases response
   * @return model TDB resource model
   */
  static ResourceModel translateDescribeTenantDatabasesResponseToResourceModel(final DescribeTenantDatabasesResponse describeTenantDatabasesResponse) {
    if (CollectionUtils.isEmpty(describeTenantDatabasesResponse.tenantDatabases())) {
      return null;
    }

    return transformTenantDatabaseToResourceModel(describeTenantDatabasesResponse.tenantDatabases().get(0));
  }

  /**
   * Translates TDB resource model to DeleteTenantDatabase request.
   * @param model TDB resource model to be deleted.
   * @return DeleteTenantDatabase request
   */
  static DeleteTenantDatabaseRequest translateToDeleteTenantDatabaseRequest(final ResourceModel model) {
    return DeleteTenantDatabaseRequest.builder()
            .tenantDBName(model.getTenantDBName())
            .dbInstanceIdentifier(model.getDBInstanceIdentifier())
            .build();
  }

  /**
   * Translates TDB resource model to ModifyTenantDatabase request.
   * @param model TDB resource model to be modified.
   * @return ModifyTenantDatabase request
   */
  static ModifyTenantDatabaseRequest translateToModifyTenantDatabaseRequest(final ResourceModel model) {
    return ModifyTenantDatabaseRequest.builder()
            .tenantDBName(model.getTenantDBName())
            .dbInstanceIdentifier(model.getDBInstanceIdentifier())
            .masterUserPassword(model.getMasterUserPassword())
            .newTenantDBName(model.getNewTenantDBName())
            .build();
  }

  /**
   * Builds DescribeTenantDatabases request using the provided nextToken.
   * @param nextToken DescribeTenantDatabases request marker.
   * @return DescribeTenantDatabases request
   */
  static DescribeTenantDatabasesRequest translateToDescribeTenantDatabasesRequest(final String nextToken) {
    return DescribeTenantDatabasesRequest.builder().marker(nextToken).build();
  }

  /**
   * Translates the DescribeTenantDatabases response to a list of TDB resource model.
   * @param awsResponse DescribeTenantDatabases response
   * @return list of TDB resource models
   */
  static List<ResourceModel> translateToDescribeTenantDatabasesResponse(final DescribeTenantDatabasesResponse awsResponse) {
    return streamOfOrEmpty(awsResponse.tenantDatabases())
        .map(Translator::transformTenantDatabaseToResourceModel)
        .collect(Collectors.toList());
  }

  /**
   * Transforms TenantDatabases to TDB resource model
   * @param tenantDatabase
   * @return TDB resource model
   */
  static ResourceModel transformTenantDatabaseToResourceModel(final TenantDatabase tenantDatabase) {
    return ResourceModel.builder()
            .dBInstanceIdentifier(tenantDatabase.dbInstanceIdentifier())
            .dbiResourceId(tenantDatabase.dbiResourceId())
            .tenantDBName(tenantDatabase.tenantDBName())
            .masterUsername(tenantDatabase.masterUsername())
            .deletionProtection(tenantDatabase.deletionProtection())
            .tenantDatabaseArn(tenantDatabase.tenantDatabaseARN())
            .tenantDatabaseResourceId(tenantDatabase.tenantDatabaseResourceId())
            .tenantDatabaseCreateTime(tenantDatabase.tenantDatabaseCreateTime().toString())
            .characterSetName(tenantDatabase.characterSetName())
            .ncharCharacterSetName(tenantDatabase.ncharCharacterSetName())
            .tags(TagHelper.translateTagsFromSdk(tenantDatabase.tagList()))
            .build();
  }

  public static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }
}
