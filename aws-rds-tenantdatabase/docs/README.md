# AWS::RDS::TenantDatabase

The AWS::RDS::TenantDatabase resource creates a Tenant Database in an Amazon RDS DB instance running on Multi-tenant configuration

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::RDS::TenantDatabase",
    "Properties" : {
        "<a href="#charactersetname" title="CharacterSetName">CharacterSetName</a>" : <i>String</i>,
        "<a href="#dbinstanceidentifier" title="DBInstanceIdentifier">DBInstanceIdentifier</a>" : <i>String</i>,
        "<a href="#masterusername" title="MasterUsername">MasterUsername</a>" : <i>String</i>,
        "<a href="#masteruserpassword" title="MasterUserPassword">MasterUserPassword</a>" : <i>String</i>,
        "<a href="#ncharcharactersetname" title="NcharCharacterSetName">NcharCharacterSetName</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#tenantdbname" title="TenantDBName">TenantDBName</a>" : <i>String</i>,
        "<a href="#newtenantdbname" title="NewTenantDBName">NewTenantDBName</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::RDS::TenantDatabase
Properties:
    <a href="#charactersetname" title="CharacterSetName">CharacterSetName</a>: <i>String</i>
    <a href="#dbinstanceidentifier" title="DBInstanceIdentifier">DBInstanceIdentifier</a>: <i>String</i>
    <a href="#masterusername" title="MasterUsername">MasterUsername</a>: <i>String</i>
    <a href="#masteruserpassword" title="MasterUserPassword">MasterUserPassword</a>: <i>String</i>
    <a href="#ncharcharactersetname" title="NcharCharacterSetName">NcharCharacterSetName</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#tenantdbname" title="TenantDBName">TenantDBName</a>: <i>String</i>
    <a href="#newtenantdbname" title="NewTenantDBName">NewTenantDBName</a>: <i>String</i>
</pre>

## Properties

#### CharacterSetName

Indicates that the tenant DB should be associated with the specified character set.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### DBInstanceIdentifier

The parent DB instance of the tenant DB.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>63</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### MasterUsername

The master user name for the tenant DB.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>128</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### MasterUserPassword

The password for the master user.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NcharCharacterSetName

The name of the NCHAR character set for the tenant DB.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

Tags to assign to the tenant DB.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TenantDBName

A name for the tenant DB. If you specify a name, AWS CloudFormation converts it to lowercase. If you don't specify a name, AWS CloudFormation generates a unique physical ID and uses that ID for the tenant DB.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>8</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NewTenantDBName

The new name of the tenant database when renaming a tenant database. This parameter isn't case-sensitive.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>8</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the TenantDatabaseResourceId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### DeletionProtection

A value that indicates whether the tenant DB has deletion protection enabled. The tenant database can't be deleted when deletion protection is enabled. By default, deletion protection is disabled.

#### DbiResourceId

The AWS Region-unique, immutable identifier for the parent DB instance of the tenant DB. This identifier is found in AWS CloudTrail log entries whenever the AWS KMS key for the DB instance is accessed.

#### TenantDatabaseArn

The Amazon Resource Name (ARN) for the tenant DB.

#### TenantDatabaseCreateTime

The time the tenant database was created.

#### TenantDatabaseResourceId

The AWS Region-unique, immutable identifier for the tenant DB. This identifier is found in AWS CloudTrail log entries whenever the AWS KMS key for the tenant DB is accessed.

