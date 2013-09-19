package com.altamiracorp.lumify.structuredDataExtraction;

import com.altamiracorp.lumify.ConfigurableMapJobBase;
import com.altamiracorp.lumify.LumifyMapper;
import com.altamiracorp.lumify.config.Configuration;
import com.altamiracorp.lumify.model.AccumuloModelOutputFormat;
import com.altamiracorp.lumify.model.Value;
import com.altamiracorp.lumify.model.graph.GraphRelationship;
import com.altamiracorp.lumify.model.graph.GraphRepository;
import com.altamiracorp.lumify.model.graph.GraphVertex;
import com.altamiracorp.lumify.model.ontology.*;
import com.altamiracorp.lumify.model.termMention.TermMention;
import com.altamiracorp.lumify.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.textExtraction.StructuredDataTextExtractor;
import com.altamiracorp.lumify.ucd.AccumuloArtifactInputFormat;
import com.altamiracorp.lumify.ucd.artifact.Artifact;
import com.altamiracorp.lumify.ucd.artifact.ArtifactRepository;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class StructuredDataExtractionMR extends ConfigurableMapJobBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredDataExtractionMR.class.getName());

    @Override
    protected Class<? extends Mapper> getMapperClass(Job job, Class clazz) {
        return StructuredDataExtractorMapper.class;
    }

    @Override
    protected Class<? extends InputFormat> getInputFormatClassAndInit(Job job) {
        Configuration c = getConfiguration();
        AccumuloArtifactInputFormat.init(job, c.getDataStoreUserName(), c.getDataStorePassword(), getAuthorizations(), c.getZookeeperInstanceName(), c.getZookeeperServerNames());
        return AccumuloArtifactInputFormat.class;
    }

    @Override
    protected Class<? extends OutputFormat> getOutputFormatClass() {
        return AccumuloModelOutputFormat.class;
    }

    public static class StructuredDataExtractorMapper extends LumifyMapper<Text, Artifact, Text, Artifact> {
        private ArtifactRepository artifactRepository;
        private OntologyRepository ontologyRepository;
        private GraphRepository graphRepository;
        private TermMentionRepository termMentionRepository;
        private StructuredDataFactory structuredDataFactory;
        private HashMap<String, Concept> conceptMap = new HashMap<String, Concept>();

        @Override
        protected void setup(Context context, Injector injector) {
            structuredDataFactory = injector.getInstance(StructuredDataFactory.class);
        }

        @Override
        protected void safeMap(Text key, Artifact artifact, Context context) throws Exception {
            JSONObject mappingJson = artifact.getGenericMetadata().getMappingJson();
            if (mappingJson == null) {
                return;
            }
            Value textValue = artifact.getArtifactExtractedText().get(StructuredDataTextExtractor.NAME);
            if (textValue == null || textValue.toString().length() == 0) {
                return;
            }
            String text = textValue.toString();
            String structuredDataType = mappingJson.getString("type");
            LOGGER.info("Extracting structured data from: " + artifact.getRowKey().toString() + ", type: " + structuredDataType);

            StructuredDataExtractorBase structuredDataExtractor = structuredDataFactory.get(structuredDataType);
            if (structuredDataExtractor != null) {
                ExtractedData extractedData = structuredDataExtractor.extract(artifact, text, mappingJson, getUser());

                saveToUcd(artifact, extractedData);
                GraphVertex artifactVertex = saveArtifactToGraph(artifact);
                saveTermsAndGraphVertices(extractedData.getTermsAndGraphVertices(), artifactVertex);
                saveRelationships(extractedData.getRelationships());
            } else {
                throw new Exception("Unknown or unhandled structured data type: " + structuredDataType);
            }
        }

        private void saveToUcd(Artifact artifact, ExtractedData extractedData) {
            artifactRepository.save(artifact, getUser());

            for (TermAndGraphVertex termAndGraphVertex : extractedData.getTermsAndGraphVertices()) {
                if (termAndGraphVertex == null) {
                    continue;
                }

                Concept concept = ontologyRepository.getConceptByName(termAndGraphVertex.getTermMention().getMetadata().getConcept(), getUser());
                if (concept == null) {
                    throw new RuntimeException("Could not find concept: " + termAndGraphVertex.getTermMention().getMetadata().getConcept());
                }
                termAndGraphVertex.getTermMention().getMetadata().setConceptGraphVertexId(concept.getId());

                String termRowKey = termAndGraphVertex.getTermMention().getRowKey().toString();
                TermMention existingTerm = termMentionRepository.findByRowKey(termRowKey, getUser());
                if (existingTerm != null) {
                    existingTerm.update(termAndGraphVertex.getTermMention());
                    termMentionRepository.save(existingTerm, getUser());
                    termAndGraphVertex.getTermMention().update(existingTerm);
                } else {
                    termMentionRepository.save(termAndGraphVertex.getTermMention(), getUser());
                }
            }
        }

        private GraphVertex saveArtifactToGraph(Artifact artifact) {
            GraphVertex artifactVertex = artifactRepository.saveToGraph(artifact, getUser());
            getSession().getGraphSession().commit();
            return artifactVertex;
        }

        private void saveTermsAndGraphVertices(List<TermAndGraphVertex> termAndGraphVertices, GraphVertex artifactVertex) {
            for (TermAndGraphVertex termAndGraphVertex : termAndGraphVertices) {
                if (termAndGraphVertex == null) {
                    continue;
                }
                GraphVertex graphVertex = termAndGraphVertex.getGraphVertex();
                TermMention termMention = termAndGraphVertex.getTermMention();
                if (termMention.getMetadata().getGraphVertexId() == null) {
                    if (graphVertex.getId() == null) {
                        String conceptLabel = termAndGraphVertex.getTermMention().getMetadata().getConcept();
                        Concept concept = conceptMap.get(conceptLabel);
                        if (concept == null) {
                            concept = ontologyRepository.getConceptByName(conceptLabel, getUser());
                            if (concept == null) {
                                throw new RuntimeException("Could not find concept: " + conceptLabel);
                            }
                            conceptMap.put(conceptLabel, concept);
                        }

                        graphVertex.setProperty(PropertyName.SUBTYPE.toString(), concept.getId());
                        if (termAndGraphVertex.isUseExisting()) {
                            GraphVertex existingGraphVertex = graphRepository.findVertexByTitleAndType((String) graphVertex.getProperty(PropertyName.TITLE), VertexType.ENTITY, getUser());
                            if (existingGraphVertex != null) {
                                existingGraphVertex.update(graphVertex);
                                graphVertex.update(existingGraphVertex);
                                graphVertex = existingGraphVertex;
                            }
                        }
                        graphRepository.saveVertex(graphVertex, getUser());
                        getSession().getGraphSession().commit();
                    }

                    termMention.getMetadata().setGraphVertexId(graphVertex.getId());
                    termMentionRepository.save(termAndGraphVertex.getTermMention(), getUser());

                    GraphRelationship artifactRelationship = new GraphRelationship(null, artifactVertex.getId(), graphVertex.getId(), LabelName.HAS_ENTITY.toString());
                    getSession().getGraphSession().save(artifactRelationship, getUser());
                } else {
                    GraphVertex existingGraphVertex = getSession().getGraphSession().findGraphVertex(termMention.getMetadata().getGraphVertexId(), getUser());
                    existingGraphVertex.update(graphVertex);
                    graphVertex.update(existingGraphVertex);
                    getSession().getGraphSession().commit();
                }
            }
            getSession().getGraphSession().commit();
        }

        private void saveRelationships(List<StructuredDataRelationship> relationships) {
            for (StructuredDataRelationship relationship : relationships) {
                String sourceVertexId = relationship.getSource().getGraphVertex().getId();
                String destVertexId = relationship.getDest().getGraphVertex().getId();
                String label = relationship.getLabel();
                if (sourceVertexId == null || destVertexId == null || label == null) {
                    throw new RuntimeException("Invalid relationship: " + sourceVertexId + " -> " + label + " -> " + destVertexId);
                }
                GraphRelationship graphRelationship = new GraphRelationship(null, sourceVertexId, destVertexId, label);
                getSession().getGraphSession().save(graphRelationship, getUser());
                getSession().getGraphSession().commit();
            }
        }

        @Inject
        public void setArtifactRepository(ArtifactRepository artifactRepository) {
            this.artifactRepository = artifactRepository;
        }

        @Inject
        public void setOntologyRepository(OntologyRepository ontologyRepository) {
            this.ontologyRepository = ontologyRepository;
        }

        @Inject
        public void setGraphRepository(GraphRepository graphRepository) {
            this.graphRepository = graphRepository;
        }

        @Inject
        public void setTermMentionRepository(TermMentionRepository termMentionRepository) {
            this.termMentionRepository = termMentionRepository;
        }
    }

    @Override
    protected boolean hasConfigurableClassname() {
        return false;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new StructuredDataExtractionMR(), args);
        if (res != 0) {
            System.exit(res);
        }
    }
}
