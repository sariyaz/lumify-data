package com.altamiracorp.reddawn.web;

import com.altamiracorp.reddawn.web.routes.admin.AdminQuery;
import com.altamiracorp.reddawn.web.routes.admin.AdminTables;
import com.altamiracorp.reddawn.web.routes.admin.AdminUploadOntology;
import com.altamiracorp.reddawn.web.routes.artifact.*;
import com.altamiracorp.reddawn.web.routes.chat.ChatNew;
import com.altamiracorp.reddawn.web.routes.chat.ChatPostMessage;
import com.altamiracorp.reddawn.web.routes.entity.EntityObjectDetectionCreate;
import com.altamiracorp.reddawn.web.routes.entity.EntityRelationships;
import com.altamiracorp.reddawn.web.routes.entity.EntitySearch;
import com.altamiracorp.reddawn.web.routes.entity.EntityTermCreate;
import com.altamiracorp.reddawn.web.routes.graph.*;
import com.altamiracorp.reddawn.web.routes.map.MapInitHandler;
import com.altamiracorp.reddawn.web.routes.map.MapTileHandler;
import com.altamiracorp.reddawn.web.routes.ontology.*;
import com.altamiracorp.reddawn.web.routes.relationship.SetRelationshipProperty;
import com.altamiracorp.reddawn.web.routes.resource.ResourceGet;
import com.altamiracorp.reddawn.web.routes.statement.Relationships;
import com.altamiracorp.reddawn.web.routes.statement.StatementCreate;
import com.altamiracorp.reddawn.web.routes.user.MeGet;
import com.altamiracorp.reddawn.web.routes.user.MessagesGet;
import com.altamiracorp.reddawn.web.routes.vertex.*;
import com.altamiracorp.reddawn.web.routes.workspace.WorkspaceByRowKey;
import com.altamiracorp.reddawn.web.routes.workspace.WorkspaceDelete;
import com.altamiracorp.reddawn.web.routes.workspace.WorkspaceList;
import com.altamiracorp.reddawn.web.routes.workspace.WorkspaceSave;
import com.altamiracorp.web.Handler;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Request;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class Router extends HttpServlet {
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
    private WebApp app;
    final File rootDir = new File("./web/src/main/webapp");

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        final Injector injector = (Injector) config.getServletContext().getAttribute(Injector.class.getName());

        app = new WebApp(config, injector);

        Class<? extends Handler> authenticator = X509Authenticator.class;
        if (app.get("env").equals("dev")) {
            authenticator = DevBasicAuthenticator.class;
        }

        app.get("/ontology/concept/{conceptId}/properties", PropertyListByConceptId.class);
        app.get("/ontology/{relationshipLabel}/properties", PropertyListByRelationshipLabel.class);
        app.get("/ontology/concept/", ConceptList.class);
        app.get("/ontology/property/", PropertyList.class);
        app.get("/ontology/relationship/", RelationshipLabelList.class);

        app.get("/resource/{_rowKey}", ResourceGet.class);

        app.get("/artifact/search", authenticator, ArtifactSearch.class);
        app.get("/artifact/{_rowKey}/raw", authenticator, ArtifactRawByRowKey.class);
        app.get("/artifact/{_rowKey}/poster-frame", authenticator, ArtifactPosterFrameByRowKey.class);
        app.get("/artifact/{_rowKey}/video-preview", authenticator, ArtifactVideoPreviewImageByRowKey.class);
        app.get("/artifact/{_rowKey}", authenticator, ArtifactByRowKey.class);
        app.post("/artifact/import", authenticator, ArtifactImport.class);

        app.post("/statement/create", authenticator, StatementCreate.class);
        app.get("/statement/relationship/", Relationships.class);

        app.post("/entity/relationships", authenticator, EntityRelationships.class);
        app.get("/entity/search", authenticator, EntitySearch.class);
        app.post("/entity/createTerm", authenticator, EntityTermCreate.class);
        app.post("/entity/createEntity", authenticator, EntityObjectDetectionCreate.class);

        app.post("/vertex/{graphVertexId}/property/set", authenticator, VertexSetProperty.class);
        app.get("/vertex/{graphVertexId}/properties", authenticator, VertexProperties.class);
        app.get("/vertex/{graphVertexId}/relationships", authenticator, VertexRelationships.class);
        app.get("/vertex/relationship", authenticator, VertexToVertexRelationship.class);
        app.get("/vertex/removeRelationship", authenticator, VertexRelationshipRemoval.class);
        app.get("/vertex/multiple", authenticator, VertexMultiple.class);

        app.post("/relationship/property/set", authenticator, SetRelationshipProperty.class);

        app.get("/graph/findPath", authenticator, GraphFindPath.class);
        app.get("/graph/{graphVertexId}/relatedVertices", authenticator, GraphRelatedVertices.class);
        app.get("/graph/vertex/search", authenticator, GraphVertexSearch.class);
        app.get("/graph/vertex/geoLocationSearch", authenticator, GraphGeoLocationSearch.class);
        app.post("/graph/vertex/{graphVertexId}/uploadImage", authenticator, GraphVertexUploadImage.class);
        app.get("/graph/vertex/{graphVertexId}", authenticator, GraphGetVertex.class);

        app.get("/workspace/", authenticator, WorkspaceList.class);
        app.post("/workspace/save", authenticator, WorkspaceSave.class);
        app.post("/workspace/{workspaceRowKey}/save", authenticator, WorkspaceSave.class);
        app.get("/workspace/{workspaceRowKey}", authenticator, WorkspaceByRowKey.class);
        app.delete("/workspace/{workspaceRowKey}", authenticator, WorkspaceDelete.class);

        app.get("/user/messages", authenticator, MessagesGet.class);
        app.get("/user/me", authenticator, MeGet.class);

        app.get("/map/map-init.js", MapInitHandler.class);
        app.get("/map/{z}/{x}/{y}.png", MapTileHandler.class);

        app.post("/chat/new", authenticator, ChatNew.class);
        app.post("/chat/{chatId}/post", authenticator, ChatPostMessage.class);

        app.get("/admin/query", authenticator, AdminQuery.class);
        app.get("/admin/tables", authenticator, AdminTables.class);
        app.post("/admin/uploadOntology", authenticator, AdminUploadOntology.class);

        LessRestlet.init(rootDir);
        app.get("/css/{file}.css", LessRestlet.class);
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        try {
            if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {
                req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
            }

            HttpServletResponse httpResponse = (HttpServletResponse) resp;
            httpResponse.addHeader("Accept-Ranges", "bytes");
            app.handle((HttpServletRequest) req, httpResponse);
            app.close(req);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
