package software.amazon.rds.tenantdatabase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TagHelper {
    public static Set<software.amazon.awssdk.services.rds.model.Tag> translateTagsToSdk(final List<Tag> tags) {
        return Translator.streamOfOrEmpty(tags)
                .map(tag -> software.amazon.awssdk.services.rds.model.Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static List<software.amazon.rds.tenantdatabase.Tag> translateTagsFromSdk(final List<software.amazon.awssdk.services.rds.model.Tag> tags) {
        return Translator.streamOfOrEmpty(tags)
                .map(tag -> software.amazon.rds.tenantdatabase.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
