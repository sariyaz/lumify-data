/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.altamiracorp.lumify.storm.structuredData.mapping.csv;

import static com.google.common.base.Preconditions.*;

import com.altamiracorp.lumify.core.ingest.term.extraction.TermExtractionResult;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermRelationship;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.storm.structuredData.mapping.DocumentMapping;
import com.altamiracorp.lumify.util.LineReader;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * DocumentMapping for CSV files.
 */
@JsonTypeName("csv")
public class CsvDocumentMapping implements DocumentMapping {
    /**
     * The class logger.
     */
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CsvDocumentMapping.class);

    /**
     * The CsvPreference for reading and writing.
     */
    private static final CsvPreference CSV_PREFERENCE = CsvPreference.EXCEL_PREFERENCE;

    /**
     * The CSV document mapping type.
     */
    public static final String CSV_DOCUMENT_TYPE = "csv";

    /**
     * The default subject.
     */
    public static final String DEFAULT_SUBJECT = "";

    /**
     * The default number of rows to skip.
     */
    public static final int DEFAULT_SKIP_ROWS = 0;

    /**
     * The number of rows to skip.
     */
    private final int skipRows;

    /**
     * The subject for this mapping.
     */
    private final String subject;

    /**
     * The term mappings for this CSV.
     */
    private final List<CsvTermColumnMapping> termMappings;

    /**
     * The relationship mappings for this CSV.
     */
    private final List<CsvRelationshipMapping> relationshipMappings;

    /**
     * Create a new CsvDocumentMapping.
     * @param subject
     * @param skipRows
     * @param terms
     * @param relationships
     */
    @JsonCreator
    public CsvDocumentMapping(@JsonProperty(value="subject", required=false) final String subject,
            @JsonProperty(value="skipRows",required=false) final Integer skipRows,
            @JsonProperty(value="terms") final List<CsvTermColumnMapping> terms,
            @JsonProperty(value="relationships") final List<CsvRelationshipMapping> relationships) {
        checkNotNull(terms, "At least one term mapping must be provided.");
        checkArgument(!terms.isEmpty(), "At least one term mapping must be provided.");
        this.subject = subject != null ? subject.trim() : DEFAULT_SUBJECT;
        this.skipRows = skipRows != null && skipRows >= 0 ? skipRows : DEFAULT_SKIP_ROWS;
        List<CsvTermColumnMapping> myTerms = new ArrayList<CsvTermColumnMapping>(terms);
        Collections.sort(myTerms);
        this.termMappings = Collections.unmodifiableList(myTerms);
        List<CsvRelationshipMapping> myRels = new ArrayList<CsvRelationshipMapping>();
        if (relationships != null) {
            myRels.addAll(relationships);
        }
        this.relationshipMappings = Collections.unmodifiableList(myRels);
    }

    /**
     * Get the number of rows to skip.
     * @return the number of rows to skip
     */
    @JsonProperty("skipRows")
    public int getSkipRows() {
        return skipRows;
    }

    @JsonProperty("subject")
    @Override
    public String getSubject() {
        return subject;
    }

    @JsonProperty("terms")
    public List<CsvTermColumnMapping> getTerms() {
        return termMappings;
    }

    @JsonProperty("relationships")
    public List<CsvRelationshipMapping> getRelationships() {
        return relationshipMappings;
    }

    @Override
    public void ingestDocument(final InputStream inputDoc, final Writer outputDoc) throws IOException {
        CsvListReader csvReader = new CsvListReader(new InputStreamReader(inputDoc), CsvDocumentMapping.CSV_PREFERENCE);
        CsvListWriter csvWriter = new CsvListWriter(outputDoc, CsvDocumentMapping.CSV_PREFERENCE);

        List<String> line;
        while ((line = csvReader.read()) != null) {
            csvWriter.write(line);
        }
        csvWriter.close();
    }

    @Override
    public TermExtractionResult mapDocument(final Reader reader, final String processId) throws IOException {
        // read line-by-line, tracking the offset
        LineReader lineReader = new LineReader(reader);
        // skip the first skipRows lines and initialize the current offset
        lineReader.skipLines(skipRows);
        int offset = lineReader.getOffset();

        TermExtractionResult results = new TermExtractionResult();
        CsvListReader csvReader;
        List<String> columns;
        Map<String, TermMention> termMap;
        TermMention mention;
        TermMention tgtMention;
        int lastCol;
        int currentCol;
        for (String line = lineReader.readLine(); line != null && !line.isEmpty(); line = lineReader.readLine()) {
            csvReader = new CsvListReader(new StringReader(line), CsvDocumentMapping.CSV_PREFERENCE);
            columns = csvReader.read();
            if (columns == null) {
                break;
            }
            // extract all identified Terms, adding them to the results and
            // mapping them by the configured map ID for relationship discovery
            termMap = new HashMap<String, TermMention>();
            lastCol = 0;
            for (CsvTermColumnMapping termMapping : termMappings) {
                // term mappings are ordered by column number; update offset
                // so it is set to the start of the column for the current term
                currentCol = termMapping.getColumnIndex();
                for (/* no precondition */; lastCol < currentCol; lastCol++) {
                    offset += (columns.get(lastCol) != null ? columns.get(lastCol).length() : 0) + 1;
                }
                mention = termMapping.mapTerm(columns, offset, processId);
                if (mention != null) {
                    // no need to update offset here, it will get updated by the block
                    // above when the next term is processed or, if this is the last term,
                    // it will be set to the proper offset for the next line
                    termMap.put(termMapping.getMapId(), mention);
                    results.add(mention);
                }
            }
            // parse all configured relationships, generating the relationship only
            // if both Terms were successfully extracted
            List<TermRelationship> relationships = new ArrayList<TermRelationship>();
            for (CsvRelationshipMapping relMapping : relationshipMappings) {
                mention = termMap.get(relMapping.getSourceTermId());
                tgtMention = termMap.get(relMapping.getTargetTermId());
                if (mention != null && tgtMention != null) {
                    relationships.add(new TermRelationship(mention, tgtMention, relMapping.getLabel()));
                }
            }
            results.addAllRelationships(relationships);
            offset = lineReader.getOffset();
        }
        return results;
    }
}
