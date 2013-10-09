package com.altamiracorp.lumify.storm.document;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.model.graph.GraphVertex;
import com.altamiracorp.lumify.storm.file.AdditionalWorkData;
import com.altamiracorp.lumify.storm.BaseFileProcessingBolt;
import com.altamiracorp.lumify.storm.file.HashCalculationWorker;
import com.altamiracorp.lumify.textExtraction.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.util.ThreadedInputStreamProcess;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentBolt extends BaseFileProcessingBolt {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentBolt.class.getName());

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        List<ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalWorkData>> workers = new ArrayList<ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalWorkData>>();
        workers.add(inject(new TextExtractorWorker()));
        workers.add(inject(new HashCalculationWorker()));
        setThreadedInputStreamProcess(new ThreadedInputStreamProcess<ArtifactExtractedInfo, AdditionalWorkData>("documentBoltWorkers", workers));
    }

    @Override
    public void safeExecute (Tuple input) throws Exception {
        GraphVertex graphVertex = processFile(input);
        pushOnTextQueue(graphVertex);
        getCollector().ack(input);
    }

    private void pushOnTextQueue(GraphVertex graphVertex) {
        JSONObject textQueueDataJson = new JSONObject();
        textQueueDataJson.put("graphVertexId", graphVertex.getId());
        pushOnQueue("text", textQueueDataJson);
    }
}
