package com.altamiracorp.reddawn.ontology;

import com.altamiracorp.reddawn.cmdline.RedDawnCommandLineBase;
import com.altamiracorp.reddawn.model.ontology.LabelName;
import com.altamiracorp.reddawn.model.ontology.PropertyName;
import com.altamiracorp.reddawn.model.ontology.PropertyType;
import com.altamiracorp.reddawn.model.ontology.VertexType;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanLabel;
import com.thinkaurelius.titan.core.TitanVertex;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.cli.CommandLine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Base extends RedDawnCommandLineBase {
    protected Map<String, TitanKey> properties = new HashMap<String, TitanKey>();
    protected Map<String, TitanLabel> edges = new HashMap<String, TitanLabel>();

    @Override
    protected int run(CommandLine cmd) throws Exception {
        // concept properties
        TitanGraph graph = (TitanGraph) createRedDawnSession().getGraphSession().getGraph();

        TitanKey typeProperty = (TitanKey) graph.getType(PropertyName.TYPE.toString());
        if (typeProperty == null) {
            typeProperty = graph.makeType().name(PropertyName.TYPE.toString()).dataType(String.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();
        }
        properties.put(typeProperty.getName(), typeProperty);

        TitanKey dataTypeProperty = (TitanKey) graph.getType(PropertyName.DATA_TYPE.toString());
        if (dataTypeProperty == null) {
            dataTypeProperty = graph.makeType().name(PropertyName.DATA_TYPE.toString()).dataType(String.class).unique(Direction.OUT).makePropertyKey();
        }
        properties.put(dataTypeProperty.getName(), dataTypeProperty);

        TitanKey ontologyTitleProperty = (TitanKey) graph.getType(PropertyName.ONTOLOGY_TITLE.toString());
        if (ontologyTitleProperty == null) {
            ontologyTitleProperty = graph.makeType().name(PropertyName.ONTOLOGY_TITLE.toString()).dataType(String.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();
        }
        properties.put(ontologyTitleProperty.getName(), ontologyTitleProperty);

        // concept edges
        TitanLabel hasPropertyEdge = (TitanLabel) graph.getType(LabelName.HAS_PROPERTY.toString());
        if (hasPropertyEdge == null) {
            hasPropertyEdge = graph.makeType().name(LabelName.HAS_PROPERTY.toString()).directed().makeEdgeLabel();
        }
        edges.put(hasPropertyEdge.getName(), hasPropertyEdge);

        TitanLabel hasEdgeEdge = (TitanLabel) graph.getType(LabelName.HAS_EDGE.toString());
        if (hasEdgeEdge == null) {
            hasEdgeEdge = graph.makeType().name(LabelName.HAS_EDGE.toString()).directed().makeEdgeLabel();
        }
        edges.put(hasEdgeEdge.getName(), hasEdgeEdge);

        TitanLabel isAEdge = (TitanLabel) graph.getType(LabelName.IS_A.toString());
        if (isAEdge == null) {
            isAEdge = graph.makeType().name(LabelName.IS_A.toString()).directed().makeEdgeLabel();
        }
        edges.put(isAEdge.getName(), isAEdge);

        // Artifact concept
        TitanKey subTypeProperty = (TitanKey) graph.getType(PropertyName.SUBTYPE.toString());
        if (subTypeProperty == null) {
            subTypeProperty = graph.makeType().name(PropertyName.SUBTYPE.toString()).dataType(String.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();
        }
        properties.put(subTypeProperty.getName(), subTypeProperty);

        TitanKey titleProperty = (TitanKey) graph.getType(PropertyName.TITLE.toString());
        if (titleProperty == null) {
            titleProperty = graph.makeType().name(PropertyName.TITLE.toString()).dataType(String.class).unique(Direction.OUT).indexed("search", Vertex.class).makePropertyKey();
        }
        properties.put(titleProperty.getName(), titleProperty);

        TitanKey glyphIconProperty = (TitanKey) graph.getType(PropertyName.GLYPH_ICON.toString());
        if (glyphIconProperty == null) {
            glyphIconProperty = graph.makeType().name(PropertyName.GLYPH_ICON.toString()).dataType(String.class).unique(Direction.OUT).makePropertyKey();
        }
        properties.put(glyphIconProperty.getName(), glyphIconProperty);

        TitanKey colorProperty = (TitanKey) graph.getType(PropertyName.COLOR.toString());
        if (colorProperty == null) {
            colorProperty = graph.makeType().name(PropertyName.COLOR.toString()).dataType(String.class).unique(Direction.OUT).makePropertyKey();
        }
        properties.put(colorProperty.getName(), colorProperty);

        TitanKey geoLocationProperty = (TitanKey) graph.getType(PropertyName.GEO_LOCATION.toString());
        if (geoLocationProperty == null) {
            geoLocationProperty = graph.makeType().name(PropertyName.GEO_LOCATION.toString()).dataType(Geoshape.class).unique(Direction.OUT).indexed("search", Vertex.class).makePropertyKey();
        }
        properties.put(geoLocationProperty.getName(), geoLocationProperty);

        graph.commit();

        Iterator<Vertex> artifactIter = graph.getVertices(PropertyName.ONTOLOGY_TITLE.toString(), VertexType.ARTIFACT.toString()).iterator();
        TitanVertex artifact;
        if (artifactIter.hasNext()) {
            artifact = (TitanVertex) artifactIter.next();
        } else {
            artifact = (TitanVertex) graph.addVertex(null);
            artifact.setProperty(typeProperty.getName(), VertexType.CONCEPT.toString());
            artifact.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), VertexType.ARTIFACT.toString());
        }
        addPropertyToConcept(graph, artifact, typeProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, artifact, subTypeProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, artifact, titleProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, artifact, geoLocationProperty.getName(), PropertyType.GEO_LOCATION);

        graph.commit();

        // TermMention concept
        TitanKey rowKeyProperty = (TitanKey) graph.getType(PropertyName.ROW_KEY.toString());
        if (rowKeyProperty == null) {
            rowKeyProperty = graph.makeType().name(PropertyName.ROW_KEY.toString()).dataType(String.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();
        }
        properties.put(rowKeyProperty.getName(), rowKeyProperty);

        TitanKey columnFamilyNameProperty = (TitanKey) graph.getType(PropertyName.COLUMN_FAMILY_NAME.toString());
        if (columnFamilyNameProperty == null) {
            columnFamilyNameProperty = graph.makeType().name(PropertyName.COLUMN_FAMILY_NAME.toString()).dataType(String.class).unique(Direction.OUT).makePropertyKey();
        }
        properties.put(columnFamilyNameProperty.getName(), columnFamilyNameProperty);

        graph.commit();

        Iterator<Vertex> termMentionIter = graph.getVertices(PropertyName.ONTOLOGY_TITLE.toString(), VertexType.TERM_MENTION.toString()).iterator();
        TitanVertex termMention;
        if (termMentionIter.hasNext()) {
            termMention = (TitanVertex) termMentionIter.next();
        } else {
            termMention = (TitanVertex) graph.addVertex(null);
            termMention.setProperty(typeProperty.getName(), VertexType.CONCEPT.toString());
            termMention.addProperty(PropertyName.ONTOLOGY_TITLE.toString(), VertexType.TERM_MENTION.toString());
        }
        addPropertyToConcept(graph, termMention, typeProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, termMention, subTypeProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, termMention, rowKeyProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, termMention, columnFamilyNameProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, termMention, titleProperty.getName(), PropertyType.STRING);

        graph.commit();

        // Entity concept
        Iterator<Vertex> entityIter = graph.getVertices(PropertyName.ONTOLOGY_TITLE.toString(), VertexType.ENTITY.toString()).iterator();
        TitanVertex entity;
        if (entityIter.hasNext()) {
            entity = (TitanVertex) entityIter.next();
        } else {
            entity = (TitanVertex) graph.addVertex(null);
            entity.setProperty(typeProperty.getName(), VertexType.CONCEPT.toString());
            entity.addProperty(PropertyName.ONTOLOGY_TITLE.toString(), VertexType.ENTITY.toString());
        }
        addPropertyToConcept(graph, entity, typeProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, entity, subTypeProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, entity, titleProperty.getName(), PropertyType.STRING);
        addPropertyToConcept(graph, entity, PropertyName.GLYPH_ICON.toString(), PropertyType.IMAGE);

        graph.commit();

        // Artifact to TermMention relationship
        TitanLabel hasTermMention = (TitanLabel) graph.getType(LabelName.HAS_TERM_MENTION.toString());
        if (hasTermMention == null) {
            hasTermMention = graph.makeType().name(LabelName.HAS_TERM_MENTION.toString()).directed().makeEdgeLabel();
        }
        edges.put(hasTermMention.getName(), hasTermMention);
        artifact.addEdge(hasEdgeEdge, hasTermMention);
        findOrAddEdge(hasTermMention, termMention, hasEdgeEdge);

        graph.commit();

        int returnCode = defineOntology(graph, entity);

        graph.commit();
        graph.shutdown();
        return returnCode;
    }

    protected abstract int defineOntology(TitanGraph graph, TitanVertex entity);

    protected TitanVertex getOrCreateConcept(TitanGraph graph, TitanVertex parent, String conceptName) {
        Iterator<Vertex> iter = graph.getVertices(PropertyName.ONTOLOGY_TITLE.toString(), conceptName).iterator();
        TitanVertex vertex;
        if (iter.hasNext()) {
            vertex = (TitanVertex) iter.next();
        } else {
            vertex = (TitanVertex) graph.addVertex(null);
            vertex.setProperty(PropertyName.TYPE.toString(), VertexType.CONCEPT.toString());
            vertex.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), conceptName);
        }
        findOrAddEdge(vertex, parent, LabelName.IS_A.toString());
        return vertex;
    }

    protected void getOrCreateRelationshipType(TitanGraph graph, TitanVertex fromVertex, TitanVertex toVertex, String relationshipName) {
        TitanLabel relationshipLabel = (TitanLabel) graph.getType(relationshipName);
        if (relationshipLabel == null) {
            relationshipLabel = graph.makeType().name(relationshipName).directed().makeEdgeLabel();
            relationshipLabel.setProperty(PropertyName.TYPE.toString(), VertexType.RELATIONSHIP.toString());
            relationshipLabel.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), relationshipName);
            graph.commit();
        }

        TitanLabel hasEdgeLabel = this.edges.get(LabelName.HAS_EDGE.toString());
        findOrAddEdge(fromVertex, relationshipLabel, hasEdgeLabel);
        findOrAddEdge(relationshipLabel, toVertex, hasEdgeLabel);
        graph.commit();
    }

    protected void findOrAddEdge(TitanVertex fromVertex, TitanVertex toVertex, TitanLabel edgeLabel) {
        findOrAddEdge(fromVertex, toVertex, edgeLabel.getName());
    }

    protected void findOrAddEdge(TitanVertex fromVertex, TitanVertex toVertex, String edgeLabel) {
        Iterator<Edge> possibleEdgeMatches = fromVertex.getEdges(Direction.OUT, edgeLabel).iterator();
        while (possibleEdgeMatches.hasNext()) {
            Edge possibleEdgeMatch = possibleEdgeMatches.next();
            Vertex possibleMatch = possibleEdgeMatch.getVertex(Direction.IN);
            if (possibleMatch.getId().equals(toVertex.getId())) {
                return;
            }
        }
        fromVertex.addEdge(edgeLabel, toVertex);
    }

    protected TitanVertex addPropertyToConcept(TitanGraph graph, TitanVertex concept, String propertyName, PropertyType dataType) {
        Iterator<Vertex> iter = graph.getVertices(PropertyName.ONTOLOGY_TITLE.toString(), propertyName).iterator();
        TitanVertex propertyVertex;
        if (iter.hasNext()) {
            propertyVertex = (TitanVertex) iter.next();
        } else {
            propertyVertex = (TitanVertex) graph.addVertex(null);
            propertyVertex.setProperty(PropertyName.TYPE.toString(), VertexType.PROPERTY.toString());
            propertyVertex.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), propertyName);
            propertyVertex.setProperty(PropertyName.DATA_TYPE.toString(), dataType.toString());
            graph.commit();
        }

        findOrAddEdge(concept, propertyVertex, LabelName.HAS_PROPERTY.toString());
        graph.commit();

        return propertyVertex;
    }
}
