package com.altamiracorp.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.version.VersionService;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Vertex;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class AuditRepository extends Repository<Audit> {
    private final AuditBuilder auditBuilder = new AuditBuilder();
    private final VersionService versionService;

    @Inject
    public AuditRepository(final ModelSession modelSession, final VersionService versionService) {
        super(modelSession);
        this.versionService = versionService;
    }


    @Override
    public Audit fromRow(Row row) {
        return auditBuilder.fromRow(row);
    }

    @Override
    public Row toRow(Audit audit) {
        return audit;
    }

    @Override
    public String getTableName() {
        return auditBuilder.getTableName();
    }

    public Audit auditVertexCreate(Object vertexId, String process, String comment, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit audit = new Audit(AuditRowKey.build(vertexId));
        audit.getAuditCommon()
                .setUser(user)
                .setAction(AuditAction.CREATE.toString())
                .setType(OntologyRepository.TYPE_ENTITY.toString())
                .setComment(comment)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        if (process.length() > 0) {
            audit.getAuditCommon().setProcess(process);
        }

        save(audit, user.getModelUserContext());
        return audit;
    }

    public List<Audit> auditEntity(
            String action,
            Object entityId,
            String artifactId,
            String entityTitle,
            String entitySubtype,
            String process,
            String comment,
            User user) {
        checkNotNull(action, "action cannot be null");
        checkArgument(action.length() > 0, "action cannot be empty");
        checkNotNull(entityId, "entityId cannot be null");
        checkArgument(entityId.toString().length() > 0, "entityId cannot be empty");
        checkNotNull(artifactId, "artifactId cannot be null");
        checkArgument(artifactId.length() > 0, "artifactId cannot be empty");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(entityTitle, "entity title cannot be null");
        checkArgument(entityTitle.length() > 0, "entity title cannot be empty");
        checkNotNull(entitySubtype, "entity subtype cannot be null");
        checkArgument(entitySubtype.length() > 0, "entity subtype cannot be empty");

        Audit auditArtifact = new Audit(AuditRowKey.build(artifactId));
        Audit auditEntity = new Audit(AuditRowKey.build(entityId));

        List<Audit> audits = new ArrayList<Audit>();
        audits.add(auditEntityHelper(auditEntity, action, entityId, entityTitle, entitySubtype, process, comment, user));
        audits.add(auditEntityHelper(auditArtifact, action, entityId, entityTitle, entitySubtype, process, comment, user));
        saveMany(audits, user.getModelUserContext());
        return audits;
    }

    public Audit auditEntityProperties(String action, Vertex entity, String propertyName, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkArgument(action.length() > 0, "action cannot be empty");
        checkNotNull(entity, "entity cannot be null");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkArgument(propertyName.length() > 0, "property name cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit audit = new Audit(AuditRowKey.build(entity.getId()));
        HashMap<String, Object> oldProperties = new HashMap<String, Object>(); // TODO add listener pattern to secure graph to notify of property changes... entity.getOldProperties();

        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_PROPERTY.toString())
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        if (oldProperties.containsKey(propertyName)) {
            audit.getAuditProperty().setPreviousValue(oldProperties.get(propertyName).toString());
        }
        if (action.equals(AuditAction.DELETE.toString())) {
            audit.getAuditProperty().setNewValue("");
        } else {
            audit.getAuditProperty().setNewValue(entity.getPropertyValue(propertyName, 0).toString());
        }
        audit.getAuditProperty().setPropertyName(propertyName);

        save(audit, user.getModelUserContext());
        return audit;
    }

    public List<Audit> auditRelationships(String action, Vertex sourceVertex, Vertex destVertex, String label, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(action.length() > 0, "action cannot be empty");
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(destVertex, "destVertex cannot be null");
        checkNotNull(label, "label cannot be null");
        checkArgument(label.length() > 0, "label cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceVertex.getId(), destVertex.getId()));
        Audit auditDestSource = new Audit(AuditRowKey.build(destVertex.getId(), sourceVertex.getId()));

        List<Audit> audits = new ArrayList<Audit>();
        audits.add(auditRelationshipHelper(auditSourceDest, action, sourceVertex, destVertex, label, process, comment, user));
        audits.add(auditRelationshipHelper(auditDestSource, action, sourceVertex, destVertex, label, process, comment, user));
        saveMany(audits, user.getModelUserContext());
        return audits;
    }

    public List<Audit> auditRelationshipProperties(String action, String sourceId, String destId, String propertyName,
                                                   Object oldValue, Edge edge, String process, String comment, User user) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(action.length() > 0, "action cannot be empty");
        checkNotNull(sourceId, "sourceId cannot be null");
        checkNotNull(sourceId.length() > 0, "sourceId cannot be empty");
        checkNotNull(destId, "destId cannot be null");
        checkNotNull(destId.length() > 0, "destId cannot be empty");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkNotNull(propertyName.length() > 0, "propertyName cannot be empty");
        checkNotNull(oldValue, "oldValue cannot be null");
        checkNotNull(edge, "edge cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        Audit auditSourceDest = new Audit(AuditRowKey.build(sourceId, destId));
        Audit auditDestSource = new Audit(AuditRowKey.build(destId, sourceId));

        auditSourceDest.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_PROPERTY.toString())
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        auditDestSource.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_PROPERTY.toString())
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        if (!oldValue.equals("")) {
            auditDestSource.getAuditProperty().setPreviousValue(oldValue);
            auditSourceDest.getAuditProperty().setPreviousValue(oldValue);
        }
        if (action.equals(AuditAction.DELETE.toString())) {
            auditDestSource.getAuditProperty().setNewValue("");
            auditSourceDest.getAuditProperty().setNewValue("");
        } else {
            // TODO handle multi-valued properties
            auditDestSource.getAuditProperty().setNewValue(edge.getPropertyValue(propertyName, 0));
            auditSourceDest.getAuditProperty().setNewValue(edge.getPropertyValue(propertyName, 0));
        }
        auditDestSource.getAuditProperty().setPropertyName(propertyName);
        auditSourceDest.getAuditProperty().setPropertyName(propertyName);

        List<Audit> audits = Lists.newArrayList(auditSourceDest, auditDestSource);
        saveMany(audits, user.getModelUserContext());
        return audits;
    }

    private Audit auditEntityHelper(Audit audit, String action, Object entityID, String entityTitle, String entitySubtype, String process, String comment, User user) {
        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_ENTITY.toString())
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        audit.getAuditEntity()
                .setTitle(entityTitle)
                .setType(OntologyRepository.TYPE_ENTITY.toString())
                .setSubtype(entitySubtype)
                .setID(entityID.toString());
        return audit;
    }

    private Audit auditRelationshipHelper(Audit audit, String action, Vertex sourceVertex, Vertex destVertex, String label, String process, String comment, User user) {
        audit.getAuditCommon()
                .setUser(user)
                .setAction(action)
                .setType(OntologyRepository.TYPE_RELATIONSHIP.toString())
                .setComment(comment)
                .setProcess(process)
                .setUnixBuildTime(versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L)
                .setScmBuildNumber(versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "")
                .setVersion(versionService.getVersion() != null ? versionService.getVersion() : "");

        audit.getAuditRelationship()
                .setSourceId(sourceVertex.getId())
                .setSourceType(sourceVertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0))
                .setSourceTitle(sourceVertex.getPropertyValue(PropertyName.TITLE.toString(), 0))
                .setDestId(destVertex.getId())
                .setDestTitle(destVertex.getPropertyValue(PropertyName.TITLE.toString(), 0))
                .setDestType(destVertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0))
                .setLabel(label);
        return audit;
    }
}
