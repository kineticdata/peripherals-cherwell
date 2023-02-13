package com.kineticdata.bridgehub.adapter.cherwell;

import com.kineticdata.bridgehub.adapter.cherwell.CherwellAdapter;
import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CherwellTest extends BridgeAdapterTestBase {

    @Override
    public Class getAdapterClass() {
        return CherwellAdapter.class;
    }

    @Override
    public String getConfigFilePath() {
        return "src/test/resources/bridge-config.yml";
    }

    @Test
    public void test_count() throws Exception {
        BridgeError error = null;

        BridgeRequest request = new BridgeRequest();

        List<String> fields = Arrays.asList("Total");
        request.setFields(fields);

        request.setStructure("Teams");
        request.setQuery("");

        Map parameters = new HashMap();
        request.setParameters(parameters);

        Count count = null;
        try {
            count = getAdapter().count(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(count.getValue() > 0);
    }

    @Test
    public void test_retrieve() throws Exception {
        BridgeError error = null;

        assertNull(error);

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("name");
        fields.add("teamId");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Teams");
        request.setFields(fields);
        request.setQuery("id=<%=parameter[\"Team ID\"]%>");
        Map parameters = new HashMap();
        parameters.put("Team ID", "9365b4e90592c81e3b7a024555a6c0094ba77e8773");
        request.setParameters(parameters);

        Record record = null;
        try {
            record = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(record.getRecord().containsKey("name"));
    }

    @Test
    public void test_search_no_params() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("teamId");
        fields.add("teamName");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Teams");
        request.setFields(fields);
        request.setQuery("");

        Map parameters = new HashMap();
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    @Test
    public void test_run_saved_search_by_id() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        BridgeRequest request = new BridgeRequest();
        String query = "association=<%=parameter[\"Association\"]%>&scope=<%=parameter[\"Scope\"]%>&" +
                "scopeowner=<%=parameter[\"Scope Owner\"]%>&searchid=<%=parameter[\"Search Id\"]%>";
        // Run Saved Search By Internal Id
        request.setStructure("Saved Search > Internal ID");
        request.setQuery(query);

        List<String> fields = new ArrayList<String>();
        fields.add("busObId");
        fields.add("fields");
        request.setFields(fields);

        Map parameters = new HashMap();
        parameters.put("Association", "6dd53665c0c24cab86870a21cf6434ae");
        parameters.put("Scope", "Global");
        parameters.put("Scope Owner", "(None)");
        parameters.put("Search Id", "9401a19db9985372ea8a3a45a5adb05dae09c75a91");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    @Test
    public void test_run_saved_search_by_name() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        BridgeRequest request = new BridgeRequest();
        String query = "association=<%=parameter[\"Association\"]%>&scope=<%=parameter[\"Scope\"]%>&" +
                "scopeowner=<%=parameter[\"Scope Owner\"]%>&searchName=<%=parameter[\"Search Name\"]%>" +
                "&includeschema=false&resultsAsSimpleResultsList=true";

        // Run Saved Search By Name
        request.setStructure("Saved Search > Name");
        request.setQuery(query);

        List<String> fields = new ArrayList<String>();
        fields.add("busObId");
        fields.add("fields");
        request.setFields(fields);

        Map parameters = new HashMap();
        parameters.put("Association", "6dd53665c0c24cab86870a21cf6434ae");
        parameters.put("Scope", "User");
        parameters.put("Scope Owner", "948e322dbe86cb3e6b122e4650b038b854eefe69b4");
        parameters.put("Search Name", "Test Saved Search");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    /*
     *  This test has checks two cases.  Both V1 and V2 are tested with the same config.
     */
    @Test
    public void test_saved_search_by_version() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("busObId");
        fields.add("fields");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Saved Search > Results V1");
        request.setFields(fields);
        request.setQuery("associationName=<%=parameter[\"Association Name\"]%>&scope=<%=parameter[\"Scope\"]%>&" +
                "searchName=<%=parameter[\"Search Name\"]%>");

        Map parameters = new HashMap();
        parameters.put("Association Name", "Incident");
        parameters.put("Scope", "Global");
        parameters.put("Search Name", "All Assigned Incidents");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);

        request.setStructure("Saved Search > Results V2");

        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }


    @Test
    public void test_ad_hoc_filter() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("$.fields[?(@.name == \"IncidentID\")].value");
        fields.add("$.fields[?(@.name == \"CreatedBy\")].value");
        fields.add("$.fields[?(@.name == \"Description\")].value");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Adhoc > Incident");
        request.setFields(fields);
        request.setQuery(
                "dataRequest={" +
                    "\"filters\":[{" +
                        "\"fieldName\":\"Status\"," +
                        "\"operator\":\"eq\"," +
                        "\"value\":\"<%=parameter[\"Status\"]%>\"" +
                    "}]," +
                    "\"includeAllFields\":true," +
                    "\"pageSize\":3" +
                "}");

        Map parameters = new HashMap();
        parameters.put("Status", "Open");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    @Test
    public void test_ad_hoc_filter_2() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("$.fields[?(@.name == \"IncidentID\")].value");
        fields.add("$.fields[?(@.name == \"RecID\")].value");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Adhoc > Incident");
        request.setFields(fields);
        request.setQuery(
            "dataRequest={" +
                "\"filters\":[{" +
                    "\"fieldName\":\"IncidentID\"," +
                    "\"operator\":\"eq\"," +
                    "\"value\":\"<%=parameter[\"Incident ID\"]%>\"" +
                "}]," +
                "\"fields\":[" +
                    "\"BO:6dd53665c0c24cab86870a21cf6434ae\"," +
                    "\"FI:fa03d51b709e4a6eb2d52885b2ef7e04\"," +
                    "\"BO:6dd53665c0c24cab86870a21cf6434ae\"," +
                    "\"FI:6ae282c55e8e4266ae66ffc070c17fa3\"" +
                "]" +
            "}&includeschema=false&resultsAsSimpleResultsList=true");

        Map parameters = new HashMap();
        parameters.put("Incident ID", "102390");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    @Test
    public void test_missing_field_exception() throws Exception {
        RuntimeException error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Adhoc > Incident");
        request.setFields(fields);
        request.setQuery(
                "dataRequest={" +
                        "\"filters\":[{" +
                        "\"fieldName\":\"foo\"," +
                        "\"operator\":\"eq\"," +
                        "\"value\":\"<%=parameter[\"Foo\"]%>\"" +
                        "}]," +
                        "}");

        Map parameters = new HashMap();
        parameters.put("Foo", "Bar");
        request.setParameters(parameters);

        try {
            getAdapter().search(request);
        } catch (RuntimeException e) {
            error = e;
        }

        assertNotNull(error);
    }
}
