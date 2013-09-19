package com.altamiracorp.lumify.web.routes.ontology;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.model.ontology.Property;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.web.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class PropertyListByConceptId extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public PropertyListByConceptId(final OntologyRepository repo) {
        ontologyRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String conceptId = getAttributeString(request, "conceptId");
        User user = getUser(request);

        List<Property> properties = ontologyRepository.getPropertiesByConceptId(conceptId, user);

        JSONObject json = new JSONObject();
        json.put("properties", Property.toJsonProperties(properties));

        respondWithJson(response, json);
    }
}
