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
    public void test_saved_search_by_id() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        BridgeRequest request = new BridgeRequest();
        String query = "association=<%=parameter[\"Association\"]%>&scope=<%=parameter[\"Scope\"]%>&" +
                "scopeowner=<%=parameter[\"Scope Owner\"]%>&";
        // Run Saved Search By Internal Id
        request.setStructure("Saved Search > Internal ID");
        request.setQuery(query + "searchid=<%=parameter[\"Search Id\"]%>");

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

        // Run Saved Search By Name
        request.setStructure("Saved Search > Name");
        request.setQuery(query + "searchname=<%=parameter[\"Search Name\"]%>");

        parameters.remove("Search Id");
        parameters.put("Search Name", "Cart ID Item");
        request.setParameters(parameters);

        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }

    @Test
    public void test_saved_search_v1() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("busObId");
        fields.add("fields");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Saved Search > Results V1");
        request.setFields(fields);
        request.setQuery("association=<%=parameter[\"Association\"]%>&scope=<%=parameter[\"Scope\"]%>&" +
                "searchname=<%=parameter[\"Search Name\"]%>");

        Map parameters = new HashMap();
        parameters.put("Association", "6dd53665c0c24cab86870a21cf6434ae");
        parameters.put("Scope", "Global");
        parameters.put("Search Name", "Cart ID Item");
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
}
