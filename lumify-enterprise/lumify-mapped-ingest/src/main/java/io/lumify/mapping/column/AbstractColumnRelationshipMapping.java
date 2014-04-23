package io.lumify.mapping.column;

import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.ingest.term.extraction.TermRelationship;
import com.altamiracorp.securegraph.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of ColumnRelationshipMapping.
 */
public abstract class AbstractColumnRelationshipMapping implements ColumnRelationshipMapping {
    /**
     * The source entity key.
     */
    private final String sourceKey;

    /**
     * The target entity key.
     */
    private final String targetKey;

    /**
     * Create a new AbstractColumnRelationshipMapping.
     *
     * @param srcKey the source entity key
     * @param tgtKey the target entity key
     */
    protected AbstractColumnRelationshipMapping(final String srcKey, final String tgtKey) {
        checkNotNull(srcKey, "source key must be provided");
        checkArgument(!srcKey.trim().isEmpty(), "source key must be provided");
        checkNotNull(tgtKey, "target key must be provided");
        checkArgument(!tgtKey.trim().isEmpty(), "target key must be provided");
        this.sourceKey = srcKey;
        this.targetKey = tgtKey;
    }

    @JsonProperty("source")
    public final String getSourceKey() {
        return sourceKey;
    }

    @JsonProperty("target")
    public final String getTargetKey() {
        return targetKey;
    }

    /**
     * Get the label for the relationship generated by this key.
     *
     * @param source the source entity
     * @param target the target entity
     * @param row    the columns of the current row
     * @return the relationship label or <code>null</code> if it cannot be determined
     */
    protected abstract String getLabel(final TermMention source, final TermMention target, final List<String> row);

    @Override
    public final TermRelationship createRelationship(final Map<String, TermMention> entities, final List<String> row, Visibility visibility) {
        TermRelationship relationship = null;
        if (entities != null) {
            TermMention source = entities.get(sourceKey);
            TermMention target = entities.get(targetKey);
            if (source != null && target != null) {
                String label = getLabel(source, target, row);
                if (label != null && !label.trim().isEmpty()) {
                    relationship = new TermRelationship(source, target, label, visibility);
                }
            }
        }
        return relationship;
    }
}