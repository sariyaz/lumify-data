package com.altamiracorp.reddawn.statementExtraction;

import com.altamiracorp.reddawn.model.*;
import com.altamiracorp.reddawn.sentenceExtraction.SentenceExtractor;
import com.altamiracorp.reddawn.ucd.artifact.Artifact;
import com.altamiracorp.reddawn.ucd.artifact.ArtifactRepository;
import com.altamiracorp.reddawn.ucd.sentence.Sentence;
import com.altamiracorp.reddawn.ucd.sentence.SentenceRepository;
import com.altamiracorp.reddawn.ucd.sentence.SentenceTerm;
import com.altamiracorp.reddawn.ucd.statement.Statement;
import com.altamiracorp.reddawn.ucd.statement.StatementArtifact;
import com.altamiracorp.reddawn.ucd.term.Term;
import com.altamiracorp.reddawn.ucd.term.TermMention;
import com.altamiracorp.reddawn.ucd.term.TermRepository;
import junit.framework.Assert;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.lucene.index.Terms;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class SentenceBasedStatementExtractorTest {
    SentenceBasedStatementExtractor statementExtractor;
    private Date createDate = new Date();

    @Before
    public void setUp() throws IOException {
        statementExtractor = new SentenceBasedStatementExtractor() {
            @Override
            protected Date getNow() {
                return createDate;
            }
        };
    }

    @Test
    public void testSentenceWithTwoTermsReturnsStatementRelatingThem() {
        // ARRANGE
        Sentence sentence = new Sentence("urn:sha256:007d1437", 10, 53);
        sentence.getData().setArtifactId("urn:sha256:007d1437");
        sentence.getMetadata().setSecurityMarking("U");
        sentence.getData().setStart(10L);
        sentence.getData().setEnd(53L);

        Term term1 = new Term("jon smith", "ONLP", "Person");
        TermMention termMention1 = new TermMention("urn:sha256:2828282");
        SentenceTerm sentenceTerm1 = new SentenceTerm(termMention1).setTermId(term1);
        sentence.addSentenceTerm(sentenceTerm1);

        Term term2 = new Term("australia", "ONLP", "Place");
        TermMention termMention2 = new TermMention("urn:sha256:83838383");
        SentenceTerm sentenceTerm2 = new SentenceTerm(termMention2).setTermId(term2);
        sentence.addSentenceTerm(sentenceTerm2);

        // ACT
        Collection<Statement> statements = statementExtractor.extractStatements(sentence);

        // ASSERT
        assertEquals(1, statements.size());
        Statement statement = statements.iterator().next();

        assertEquals("jon smith\u001FONLP\u001FPerson\u001Eurn:mil.army.dsc:schema:dataspace\u001Fco-occured in sentence with\u001Eaustralia\u001FONLP\u001FPlace", statement.getRowKey().toString());
        List<StatementArtifact> statementArtifacts = statement.getStatementArtifacts();
        assertEquals(1, statementArtifacts.size());
        StatementArtifact statementArtifact = statementArtifacts.get(0);
        assertEquals("urn:sha256:007d1437", statementArtifact.getArtifactKey());
        assertEquals("SentenceBasedStatementExtractor", statementArtifact.getAuthor());
        assertEquals((Long)createDate.getTime(), statementArtifact.getDate());
        assertEquals("SentenceBasedStatementExtractor", statementArtifact.getExtractorId());
        assertEquals("U", statementArtifact.getSecurityMarking());
        assertEquals("urn:sha256:007d1437:0000000000000053:0000000000000010", statementArtifact.getSentence());
    }
}
