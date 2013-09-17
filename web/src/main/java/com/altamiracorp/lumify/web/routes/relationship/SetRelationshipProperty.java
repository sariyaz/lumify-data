package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.AppSession;
import com.altamiracorp.lumify.model.graph.GraphRepository;
import com.altamiracorp.lumify.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.model.ontology.Property;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.vertex.VertexProperties;
import com.altamiracorp.web.HandlerChain;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class SetRelationshipProperty extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetRelationshipProperty.class.getName());
    private GraphRepository graphRepository = new GraphRepository();
    private OntologyRepository ontologyRepository = new OntologyRepository();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String relationshipLabel = getRequiredParameter(request, "relationshipLabel");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
        final String sourceId = getRequiredParameter(request, "source");
        final String destId = getRequiredParameter(request, "dest");

        AppSession session = app.getAppSession(request);

        Property property = ontologyRepository.getProperty(session.getGraphSession(), propertyName);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        Object value;
        try {
            value = property.convertString(valueStr);
        } catch (Exception ex) {
            LOGGER.warn("Validation error propertyName: " + propertyName + ", valueStr: " + valueStr, ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        graphRepository.setPropertyEdge(session.getGraphSession(), sourceId, destId, relationshipLabel, propertyName, value);

        Map<String, String> properties = graphRepository.getEdgeProperties(session.getGraphSession(), sourceId, destId, relationshipLabel);
        for (Map.Entry<String, String> p : properties.entrySet()) {
            String displayName = ontologyRepository.getDisplayNameForLabel(session.getGraphSession(), p.getValue());
            if (displayName != null) {
                p.setValue(displayName);
            }
        }
        JSONArray resultsJson = VertexProperties.propertiesToJson(properties);

        respondWithJson(response, resultsJson);
    }
}
