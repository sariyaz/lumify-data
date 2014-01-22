package com.altamiracorp.lumify.facebook;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import static com.altamiracorp.lumify.facebook.FacebookConstants.*;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.HdfsLimitOutputStream;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.type.GeoPoint;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class FacebookPost {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FacebookBolt.class);
    private static final String PROCESS = FacebookPost.class.getName();
    private FacebookBolt facebookBolt = new FacebookBolt();


    protected ArtifactExtractedInfo processPostArtifact(JSONObject post) throws Exception {
        //extract knowledge from post
        String message = post.getString(MESSAGE);
        Long name_uid = post.getLong(AUTHOR_UID);
        String author_uid = name_uid.toString();
        Date time = new Date(post.getLong(TIMESTAMP) * 1000);
        //use extracted information
        String rowKey = RowKeyHelper.buildSHA256KeyString(post.toString().getBytes());

        ArtifactExtractedInfo artifactExtractedInfo = new ArtifactExtractedInfo();
        artifactExtractedInfo.setText(message);
        artifactExtractedInfo.setSource(FACEBOOK);
        artifactExtractedInfo.setRaw(post.toString().getBytes());
        artifactExtractedInfo.setMimeType("text/plain");
        artifactExtractedInfo.setRowKey(rowKey);
        artifactExtractedInfo.setConceptType(FACEBOOK_POST);
        artifactExtractedInfo.setAuthor(author_uid);
        artifactExtractedInfo.setDate(time);
        artifactExtractedInfo.setProcess(PROCESS);
        if (message.length() == 0) {
            //TODO: perform the query for the image and create the artifact/vertex and connect
            artifactExtractedInfo.setTitle("Facebook Image Post");
        } else if (message.length() > 140) {
            String shortTitle = message.substring(0, 137) + "...";
            artifactExtractedInfo.setTitle(shortTitle);
        } else {
            artifactExtractedInfo.setTitle(message);
        }

        //write artifact with extracted info to accumulo and create entity
        return artifactExtractedInfo;
    }

    protected Vertex processPostVertex(JSONObject post, Vertex posting, Graph graph, AuditRepository auditRepository, OntologyRepository ontologyRepository, User user) throws Exception {
        //TODO set visibility
        Visibility visibility = new Visibility("");
        Long name_uid = post.getLong(AUTHOR_UID);
        String author_uid = name_uid.toString();
        LOGGER.info("Saving Facebook post to accumulo and as graph vertex: ", posting.getId());
        Concept profileConcept = ontologyRepository.getConceptByName(FACEBOOK_PROFILE);
        String profileConceptId = profileConcept.getId().toString();
        List<String> modifiedProperties = new ArrayList<String>();

        //create entities for each of the ids tagged or author and the relationships
        Vertex authorVertex;
        Iterator<Vertex> verticesIterator = graph.query(user.getAuthorizations()).has(PROFILE_ID, author_uid).vertices().iterator();
        if (!verticesIterator.hasNext()) {
            authorVertex = graph.addVertex(visibility);
            authorVertex.setProperty(PROFILE_ID, author_uid, visibility);
            authorVertex.setProperty(PropertyName.TITLE.toString(), author_uid, visibility);
            authorVertex.setProperty(PropertyName.CONCEPT_TYPE.toString(), profileConceptId, visibility);

            auditRepository.auditEntity(AuditAction.CREATE.toString(), authorVertex.getId(), posting.getId().toString(), profileConceptId, author_uid, PROCESS, "", user);
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), authorVertex, PROFILE_ID, PROCESS, "", user);
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), authorVertex, PropertyName.CONCEPT_TYPE.toString(), PROCESS, "", user);
        } else {
            // TODO what happens if verticesIterator contains multiple users
            authorVertex = verticesIterator.next();
        }
        graph.addEdge(authorVertex, posting, POSTED_RELATIONSHIP, visibility);
        String postedRelationshipLabelDisplayName = ontologyRepository.getDisplayNameForLabel(POSTED_RELATIONSHIP);
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), posting, authorVertex, postedRelationshipLabelDisplayName, PROCESS, "", user);
        if (post.get(TAGGEED_UIDS) instanceof JSONObject) {
            Iterator tagged = post.getJSONObject(TAGGEED_UIDS).keys();
            while (tagged.hasNext()) {
                String next = tagged.next().toString();
                Vertex taggedVertex;
                Iterator<Vertex> taggedUidIterator = graph.query(user.getAuthorizations()).has(PROFILE_ID, next).vertices().iterator();
                if (!taggedUidIterator.hasNext()) {
                    taggedVertex = graph.addVertex(visibility);
                    taggedVertex.setProperty(PROFILE_ID, next, visibility);
                    taggedVertex.setProperty(PropertyName.TITLE.toString(), next, visibility);
                    taggedVertex.setProperty(PropertyName.CONCEPT_TYPE.toString(), profileConceptId, visibility);
                    auditRepository.auditEntity(AuditAction.CREATE.toString(), taggedVertex.getId(), posting.getId().toString(), profileConceptId, next, PROCESS, "", user);
                    auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), taggedVertex, PROFILE_ID, PROCESS, "", user);
                    auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), taggedVertex, PropertyName.CONCEPT_TYPE.toString(), PROCESS, "", user);
                } else {
                    // TODO what happens if taggedUidIterator contains multiple users
                    taggedVertex = taggedUidIterator.next();
                }
                graph.addEdge(posting, taggedVertex, MENTIONED_RELATIONSHIP, visibility);
                String mentionedRelationshipLabelDisplayName = ontologyRepository.getDisplayNameForLabel(MENTIONED_RELATIONSHIP);
                auditRepository.auditRelationships(AuditAction.CREATE.toString(), posting, taggedVertex, mentionedRelationshipLabelDisplayName, PROCESS, "", user);
            }
        }

        if (post.has(COORDS) && !post.getJSONObject(COORDS).equals(JSONObject.NULL)) {
            JSONObject coordinates = post.getJSONObject(COORDS);
            GeoPoint geo = new GeoPoint(coordinates.getDouble("latitude"), coordinates.getDouble("longitude"));
            posting.setProperty(PropertyName.GEO_LOCATION.toString(), geo, visibility);
            modifiedProperties.add(PropertyName.GEO_LOCATION.toString());
            auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), posting, PropertyName.GEO_LOCATION.toString(), PROCESS, "", user);
        }

        return posting;
    }
}
