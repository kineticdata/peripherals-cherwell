package com.kineticdata.bridgehub.adapter.cherwell;

import com.kineticdata.bridgehub.adapter.cherwell.CherwellAdapter;
import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import static com.sun.tools.javac.jvm.PoolConstant.LoadableConstant.Long;
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
    public void test_count() throws Exception{
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
    public void test_count_param() throws Exception {
        BridgeError error = null;

        BridgeRequest request = new BridgeRequest();

        List<String> fields = Arrays.asList("Total");
        request.setFields(fields);

        request.setStructure("Contact");
        request.setFields(fields);
        request.setQuery("GetTotal=<%=parameter[\"Get Total\"]%>&xUser=<%=parameter[\"X-User\"]%>");
        
        Map parameters = new HashMap();
        parameters.put("Get Total", "false");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
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
    public void test_search() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("FirstName");
        fields.add("LastName");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contact");
        request.setFields(fields);
        request.setQuery("GetTotal=<%=parameter[\"Get Total\"]%>&PageToken=<%=parameter[\"Page Token\"]%>&xUser=<%=parameter[\"X-User\"]%>");
                
        Map parameters = new HashMap();
        parameters.put("Get Total", "true");
        parameters.put("Page Token", "");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/contact?accessor=Data&GetTotal=<%=parameter[\"Get Total\"]%> & xUser=<%=parameter[\"X-User\"]%>");
        
        RecordList adhocList = null;
        try {
            adhocList = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(list.getRecords().size() == adhocList.getRecords().size());
        
        request.setStructure("Adhoc");
        
        list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }
    
     @Test
    public void test_json_path() throws Exception {
         BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("$.Contact.ContactID");
        fields.add("$.Contact.FirstName");
        fields.add("$.Contact.LastName");
        
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("User");
        request.setFields(fields);
        request.setQuery("GetTotal=<%=parameter[\"Get Total\"]%>&xUser=<%=parameter[\"X-User\"]%>");
                
        Map parameters = new HashMap();
        parameters.put("Get Total", "true");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);
        
        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        // Assume that the client has currency set to USD.
        assertTrue(list.getRecords().get(0).getValue("$.Contact.ContactID").equals(Long.valueOf(16245)));
    }
    
    @Test
    public void test_pagination() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        Map<String, String> metadata = new HashMap<String, String>();
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("FirstName");
        fields.add("LastName");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contact");
        request.setFields(fields);
        request.setQuery("GetTotal=<%=parameter[\"Get Total\"]%> & xUser=<%=parameter[\"X-User\"]%>");        
        request.setMetadata(metadata);
        
        Map parameters = new HashMap();
        parameters.put("Get Total", "true");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        String firstName = (String) list.getRecords().get(0).getRecord().get("FirstName");

        // Paginate forward one page
        metadata.put("next_page", list.getMetadata().get("next_page"));
        request.setMetadata(metadata);
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        assertTrue(!firstName.equals((String) list.getRecords().get(0).getRecord().get("FirstName")));
    
        // Paginate backwards one page
        metadata.put("next_page", "");
        metadata.put("prev_page", list.getMetadata().get("prev_page"));
        request.setMetadata(metadata);
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        assertTrue(firstName.equals((String) list.getRecords().get(0).getRecord().get("FirstName")));
    }
    
    @Test
    public void test_pagination_error() throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        Map<String, String> metadata = new HashMap<String, String>();
        
        metadata.put("next_page", "foo");
        metadata.put("prev_page", "bar");
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("FirstName");
        fields.add("LastName");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contact");
        request.setFields(fields);
        request.setQuery("GetTotal=<%=parameter[\"Get Total\"]%> & xUser=<%=parameter[\"X-User\"]%>");        
        request.setMetadata(metadata);
        
        Map parameters = new HashMap();
        parameters.put("Get Total", "true");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNotNull(error);
    }

    @Test
    public void test_contacts() throws Exception {
        BridgeError error = null;

        assertNull(error);

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("FirstName");
        fields.add("LastName");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Contact");
        request.setFields(fields);
        request.setQuery("id=<%=parameter[\"Contact ID\"]%>&xUser=<%=parameter[\"X-User\"]%>");
        Map parameters = new HashMap();
        parameters.put("Contact ID", "16242");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);

        Record record = null;
        try {
            record = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(record.getRecord().containsKey("FirstName"));
        
        request.setStructure("Adhoc");
        request.setQuery("/contact/<%=parameter[\"Contact ID\"]%>?accessor=Data & xUser=<%=parameter[\"X-User\"]%>");
        
        Record adhocRecord = null;
        try {
            adhocRecord = getAdapter().retrieve(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertEquals(record.getRecord(),adhocRecord.getRecord());
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
    public void test_order_by() throws Exception {
        BridgeError error = null;
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("IncidentID");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Service Request");
        request.setFields(fields);
        request.setQuery("PageSize=<%=parameter[\"Page Size\"]%>"
                + "&GetTotal=<%=parameter[\"Get Total\"]%>"
                + "&PageToken=<%=parameter[\"Page Token\"]%>"
                + "&PageNum=<%=parameter[\"Page Number\"]%>"
                + "&OrderBy=<%=parameter[\"Sort Field\"]%> <%=parameter[\"Asc Desc\"]%>"
                + "&xUser=<%=parameter[\"X-User\"]%>");
                
        Map parameters = new HashMap();
        parameters.put("Page Size", "50");
        parameters.put("Get Total", "true");
        parameters.put("Page Token", "NzQ1NTk=");
        parameters.put("Page Number", "2");
        parameters.put("Sort Field", "");
        parameters.put("Asc Desc", "");
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);
        
        Map<String, String> metadata = new HashMap<String, String>();
        request.setMetadata(metadata);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/contact?accessor=Data&GetTotal=<%=parameter[\"Get Total\"]%>"
                + "&xUser=<%=parameter[\"X-User\"]%>");
        metadata.remove("next_page");
        metadata.remove("count");
        
        RecordList adhocList = null;
        try {
            adhocList = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(list.getRecords().get(0).getRecord().get("ContactID").equals(adhocList.getRecords().get(0).getRecord().get("ContactID")));
        
        metadata.remove("next_page");
        metadata.remove("count");
        request.setStructure("Contact");
        request.setQuery("/contact?accessor=Data&GetTotal=<%=parameter[\"Get Total\"]%>"
                + "&xUser=<%=parameter[\"X-User\"]%>");
        
        RecordList listTwo = null;
        try {
            listTwo = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertFalse(list.getRecords().get(0).getRecord().get("ContactID").equals(listTwo.getRecords().get(0).getRecord().get("ContactID")));
        
        request.setStructure("Adhoc");
        request.setQuery("/contact?accessor=Data&GetTotal=<%=parameter[\"Get Total\"]%>"
                + "&xUser=<%=parameter[\"X-User\"]%>");
        metadata.remove("next_page");
        metadata.remove("count");
        
        RecordList adhocListTwo = null;
        try {
            adhocListTwo = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertFalse(adhocList.getRecords().get(0).getRecord().get("ContactID").equals(adhocListTwo.getRecords().get(0).getRecord().get("ContactID")));
        assertTrue(listTwo.getRecords().get(0).getRecord().get("ContactID").equals(adhocListTwo.getRecords().get(0).getRecord().get("ContactID")));
    }
    
    @Test
    public void test_dropdown () throws Exception {
        BridgeError error = null;

        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("IncidentClassID");
        fields.add("IncidentClass");

        BridgeRequest request = new BridgeRequest();
        request.setStructure("Adhoc");
        request.setFields(fields);
        request.setQuery("/dropdown/class?accessor=Data");
                
        Map parameters = new HashMap();
        parameters.put("X-User", "{\"UserName\":\"kineticadmin1@kinetic.com\",\"PrimaryContactID\":104903,\"CCWebUserID\":3346,\"DisplayName\":\"Admin1, Kinetic \",\"TimeZoneCode\":35,\"IsPrimary\":false,\"ContactID\":237647,\"UserType\":\"Administrator\",\"CID\":\"DDX\"}");
        request.setParameters(parameters);

        RecordList list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
        
        request.setStructure("Adhoc");
        request.setQuery("/contact?accessor=Data&GetTotal=<%=parameter[\"Get Total\"]%> & xUser=<%=parameter[\"X-User\"]%>");
        
        RecordList adhocList = null;
        try {
            adhocList = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(list.getRecords().size() == adhocList.getRecords().size());
        
        request.setStructure("Adhoc");
        
        list = null;
        try {
            list = getAdapter().search(request);
        } catch (BridgeError e) {
            error = e;
        }

        assertNull(error);
        assertTrue(list.getRecords().size() > 0);
    }
}
