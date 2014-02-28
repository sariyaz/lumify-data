package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexToVertexRelationship extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexToVertexRelationship(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String source = getRequiredParameter(request, "source");
        final String target = getRequiredParameter(request, "target");
        final String id = getRequiredParameter(request, "id");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex sourceVertex = graph.getVertex(source, authorizations);
        Vertex targetVertex = graph.getVertex(target, authorizations);

        JSONObject results = new JSONObject();
        results.put("source", resultsToJson(source, sourceVertex));
        results.put("target", resultsToJson(target, targetVertex));

        Edge edge = graph.getEdge(id, authorizations);

        JSONArray propertyJson = new JSONArray();
        for (Property edgeProperty : edge.getProperties()) {
            JSONObject property = new JSONObject();
            property.put("key", edgeProperty.getName());
            String displayName = ontologyRepository.getDisplayNameForLabel(edgeProperty.getValue().toString());
            if (displayName == null) {
                property.put("value", edgeProperty.getValue());
            } else {
                property.put("value", displayName);
            }
            propertyJson.put(property);
        }

        results.put("properties", propertyJson);

        respondWithJson(response, results);
    }

    private JSONObject resultsToJson(String id, Vertex vertex) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        for (Property property : vertex.getProperties()) {
            json.put(property.getName(), vertex.getPropertyValue(property.getName(), 0));
        }

        return json;
    }
}
