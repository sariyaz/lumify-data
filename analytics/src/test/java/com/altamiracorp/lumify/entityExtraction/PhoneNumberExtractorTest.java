package com.altamiracorp.lumify.entityExtraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.model.termMention.TermMention;

@RunWith(JUnit4.class)
public class PhoneNumberExtractorTest extends BaseExtractorTest {
    @Mock
    private Context context;

    @Mock
    private User user;

    private EntityExtractor extractor;

    private String textWith = "This terrorist's phone number is 410-678-2230, and his best buddy's phone number is +44 (0)207 437 0478";
    private String textWithNewLines = "This terrorist's phone\n number is 410-678-2230, and his best buddy's phone number\n is +44 (0)207 437 0478";
    private String textWithout = "This is a sentence without any phone numbers in it.";


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(new Configuration()).when(context).getConfiguration();
        extractor = new PhoneNumberExtractor();
    }

    @Test
    public void testPhoneNumberExtraction() throws Exception {
        extractor.setup(context,user);
        ArrayList<ExtractedEntity> termList = new ArrayList<ExtractedEntity>(extractor.extract(createArtifact(textWith), textWith));

        assertTrue("Incorrect number of phone numbers extracted", termList.size() == 2);
        TermMention firstTerm = termList.get(0).getTermMention();
        assertEquals("First phone number not correctly extracted", "+14106782230", firstTerm.getMetadata().getSign());
        assertEquals(33, firstTerm.getRowKey().getStartOffset());
        assertEquals(45, firstTerm.getRowKey().getEndOffset());

        TermMention secondTerm = termList.get(1).getTermMention();
        assertEquals("Second phone number not correctly extracted", "+442074370478", secondTerm.getMetadata().getSign());
        assertEquals(84, secondTerm.getRowKey().getStartOffset());
        assertEquals(103, secondTerm.getRowKey().getEndOffset());
    }

    @Test
    public void testPhoneNumberExtractionWithNewlines() throws Exception {
        extractor.setup(context,user);
        ArrayList<ExtractedEntity> termList = new ArrayList<ExtractedEntity>(extractor.extract(createArtifact(textWithNewLines), textWithNewLines));

        assertTrue("Incorrect number of phone numbers extracted", termList.size() == 2);
        TermMention firstTerm = termList.get(0).getTermMention();
        assertEquals("First phone number not correctly extracted", "+14106782230", firstTerm.getMetadata().getSign());
        assertEquals(34, firstTerm.getRowKey().getStartOffset());
        assertEquals(46, firstTerm.getRowKey().getEndOffset());

        TermMention secondTerm = termList.get(1).getTermMention();
        assertEquals("Second phone number not correctly extracted", "+442074370478", secondTerm.getMetadata().getSign());
        assertEquals(86, secondTerm.getRowKey().getStartOffset());
        assertEquals(105, secondTerm.getRowKey().getEndOffset());
    }

    @Test
    public void testNegativePhoneNumberExtraction() throws Exception {
        extractor.setup(context,user);
        Collection<ExtractedEntity> terms = extractor.extract(createArtifact(textWithout), textWithout);

        assertNotNull(terms);
        assertTrue("Phone number extracted when there were no phone numbers", terms.isEmpty());
    }
}
