package org.continuousassurance.swamp.test;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * This reads a test configuration (i.e. a configuration used for running
 * unit tests) from a configuration file.
 * <p>Created by Jeff Gaynor<br>
 * on 9/5/14 at  11:57 AM
 */
public class TestConfigReader {
    public static final String TESTS_TAG = "tests";
    public static final String RWS_ADDRESS = "rws_address";
    public static final String CSA_ADDRESS = "csa_address";
    public static final String REFERER_HEADER = "referer_header";
    public static final String ORIGIN_HEADER = "origin_header";
    public static final String HOST_HEADER = "host_header";
    public static final String TEST_TAG = "test";
    public static final String TEST_NAME_TAG = "name";
    /**
     * In the test file, testers can disable the test by specifying this value as true or false.
     * The default is to run the test.
     */
    public static final String TEST_DISABLE_TAG = "disable";
    public static final String DATA_TAG = "data";
    public static final String DATA_TYPE_TAG = "type";
    public static final String LIST_DATA_TYPE_TAG = "list";
    public static final String DATA_NAME_TAG = "name";
    /**
     * The configuration file path and name is given as an argument on the command line.
     * This tells the system what the name of the parameter (given with the -D option) is.
     */
    public static final String CONFIG_FILE_KEY = "test:config.file";
    public static final String CONFIG_NAME_KEY = "test:config.name";

    static OLDTestMap tests = null;

    public static OLDTestMap getTestData() throws FileNotFoundException, XMLStreamException {
        if (tests == null) {
            tests = load();
    //        PageFactory.setTestMap(tests);
        }
        return tests;
    }

    /**
     * This is just a driver that reads the given test file and spits out the entries. The
     * filename is set in the system property swamp-test.config.file
     *
     * @param args
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
        OLDTestMap tests = getTestData();
        for (String key : tests.keySet()) {
            System.out.println("tests: key=" + key + ", value=" + tests.get(key));
            OLDTestData xx = tests.get(key);
            for (String key2 : xx.keySet()) {
                System.out.println("      key=" + key2 + ", value=" + xx.get(key2));

            }
        }
    }

    /*
    Typical XML file parsed is
    <tests>
       <test name="foo">
         <data name="username">myName</data>
         <data name="password">changeme</data>
         <data name="list" type="list">fnord,bar,baz</data>
       </test>
     </tests>

     This puts everything into a map of data maps for all tests that is keyed by the name "foo".
       This map in turn contains elements that are strings only or lists of strings if the
       data type is "list", keyed by the name of the data.
     */
    public static OLDTestMap load() throws FileNotFoundException, XMLStreamException {

        OLDTestMap tests = new OLDTestMap();
        OLDTestData data = null;
        OLDTestData OLDTestData = new OLDTestData();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(
                new File(System.getProperty(CONFIG_FILE_KEY))));

        // zero these out in the end data tag block.
        String content = "";
        String attribName = null;
        String attribValue = null;
        boolean processAsList = false;

        while (reader.hasNext()) {
            int Event = reader.next();
            LinkedList<String> x = null;
            Object entry = null;
            switch (Event) {
                case XMLStreamConstants.START_ELEMENT: {
                    String tagName = reader.getLocalName();
                    switch (tagName) {
                        case TEST_TAG: {
                            data = new OLDTestData();
                            data.put(OLDTestData.TEST_ENABLE_KEY, Boolean.TRUE); // default is to run test

                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                attribName = reader.getAttributeLocalName(i);
                                attribValue = reader.getAttributeValue(i);
                                if (attribName.equals(TEST_NAME_TAG)) {
                                    tests.put(attribValue, data);
                                }
                                if (attribName.equals(TEST_DISABLE_TAG)) {
                                    data.put(OLDTestData.TEST_ENABLE_KEY, !Boolean.parseBoolean(attribValue));
                                }
                            }// end for
                            break;
                        }
                        case DATA_TAG: {

                            String name = null;
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                attribName = reader.getAttributeLocalName(i);
                                attribValue = reader.getAttributeValue(i);

                                if (attribName.equals(DATA_NAME_TAG)) {
                                    name = attribValue;
                                }
                                if (attribName.equals(DATA_TYPE_TAG)) {
                                    processAsList = attribValue.equals(LIST_DATA_TYPE_TAG);
                                }
                            } // end for
                            break;
                        }
                        default: {
                            break;
                        }
                        case TESTS_TAG: {
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                attribName = reader.getAttributeLocalName(i);
                                attribValue = reader.getAttributeValue(i);
                                if (attribName.equals(RWS_ADDRESS)) {
                                    if (attribValue != null && !attribValue.endsWith("/")) {
                                        attribValue = attribValue + "/";
                                    }
                                    OLDTestData.put(attribName, attribValue);
                                    tests.put(OLDTestMap.SYSTEM_KEY, OLDTestData);
                                }
                                if (attribName.equals(CSA_ADDRESS)) {
                                    if (attribValue != null && !attribValue.endsWith("/")) {
                                        attribValue = attribValue + "/";
                                    }
                                    OLDTestData.put(attribName, attribValue);
                                }
                                if (attribName.equals(REFERER_HEADER)) {
                                    OLDTestData.put(attribName, attribValue);
                                }
                                if (attribName.equals(ORIGIN_HEADER)) {
                                    OLDTestData.put(attribName, attribValue);
                                }
                                if (attribName.equals(HOST_HEADER)) {
                                    OLDTestData.put(attribName, attribValue);
                                }


                            }// end for
                            break;
                        }

                    }
                    break;
                }
                case XMLStreamConstants.CHARACTERS: {
                    content = content + reader.getText().trim();
                    break;
                }
                case XMLStreamConstants.END_ELEMENT: {
                    String tagName = reader.getLocalName();
                    switch (tagName) {
                        case TESTS_TAG: {
                            // end of file or should be.
                            break;
                        }
                        case TEST_TAG: {
                            break;
                        }
                        case DATA_TAG: {
                            if (processAsList) {
                                LinkedList<String> list = new LinkedList<String>();
                                // fill up with a for loop.
                                StringTokenizer st = new StringTokenizer(content, "\n");
                                while (st.hasMoreTokens()) {
                                    list.add(st.nextToken().trim());
                                }
                                entry = list;
                            } else {
                                entry = content;
                            }
                            data.put(attribValue, entry);


                            content = "";

                            attribName = null;
                            attribValue = null;
                            processAsList = false;

                            break;
                        }
                    }
                    break;
                }
            }
        } //end while

        return tests;
    }
}
