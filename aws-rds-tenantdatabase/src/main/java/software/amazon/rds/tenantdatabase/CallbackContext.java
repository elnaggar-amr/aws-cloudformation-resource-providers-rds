package software.amazon.rds.tenantdatabase;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private boolean created;
    private boolean deleted;

    public CallbackContext() {
        super();
    }
}
