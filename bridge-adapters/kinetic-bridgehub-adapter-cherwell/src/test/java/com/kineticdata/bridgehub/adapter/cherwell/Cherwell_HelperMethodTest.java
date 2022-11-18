/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kineticdata.bridgehub.adapter.cherwell;


import com.kineticdata.bridgehub.adapter.cherwell.CherwellAdapter;
import com.kineticdata.bridgehub.adapter.cherwell.AdapterMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.Record;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author chadrehm
 */
public class Cherwell_HelperMethodTest { 
    @Test
    public void test_get_parameters() throws BridgeError {
        CherwellAdapter helper = new CherwellAdapter();
        
        AdapterMapping mapping = helper.getMapping("Contacts");
        
        Map<String, String> parameters = 
            helper.getParameters("foo=bar&fizz=buzz", mapping);
        
        Map<String, String> parametersControl = new HashMap<String, String>() {{
            put("foo","bar");
            put("fizz","buzz");
        }};
        
        assertTrue(parameters.equals(parametersControl));
        
        mapping = helper.getMapping("Adhoc");
        
        parameters = helper.getParameters("_noOp_?foo=bar&fizz=buzz", mapping);
        
        parametersControl.put("adapterPath", "_noOp_");
        
        assertTrue(parameters.equals(parametersControl));
    }
    
    @Test
    public void test_get_mapping_error() throws BridgeError {
        BridgeError error = null;
        CherwellAdapter helper = new CherwellAdapter();
        
        try {
            helper.getMapping("Foo");
        } catch (BridgeError e) {
            error = e;
        }
                
        assertNotNull(error);
    }
    
    @Test
    public void test_build_record() {
        CherwellAdapter helper = new CherwellAdapter();
        ObjectMapper mapper = new ObjectMapper();

        List<String> list = new ArrayList();
        list.add("$.custom_fields[*].kind");
        list.add("FirstName");
        
        String jsonString = "{\"FirstName\": \"Foo\", \"custom_fields\": [{" +
            "\"kind\": \"Total activities\", \"value\": 0," +
            "\"fieldType\": \"integer\"}]}";
        JSONObject jsonobj = (JSONObject)JSONValue.parse(jsonString);
        
        String recordControl = "{\"FirstName\":\"Foo\",\"$.custom_fields[*].kind\":"
            + "[\"Total activities\"]}";
        
        Record record = helper.buildRecord(list, jsonobj);
        String recordString = ((JSONObject)record.getRecord()).toJSONString();
        
        IOException ioError = null;
        try {
            assertEquals(mapper.readTree(recordString), mapper.readTree(recordControl));
        } catch (IOException e) {
            ioError = e;
        }
        assertNull(ioError);
        
        // jsonPath is valid but property does not exist
        recordControl = "{\"FirstName\":\"Foo\",\"$.custom_fields[*].kind\":null}";
        
        jsonobj.remove("custom_fields");
        record = helper.buildRecord(list, jsonobj);
        recordString = ((JSONObject)record.getRecord()).toJSONString();
        
        try {
            assertEquals(mapper.readTree(recordString), mapper.readTree(recordControl));
        } catch (IOException e) {
            ioError = e;
        }
        assertNull(ioError);
        
        // jsonPath is invalid
        list.add("$[custom_fields]");
        
        JsonPathException error = null;
        try {
            helper.buildRecord(list, jsonobj);
        } catch (JsonPathException e) {
            error = e;
        }
        
        assertNotNull(error);
    }
    
    @Test
    public void test_default_path() throws Exception {        
        AdapterMapping mapper = new AdapterMapping("", "",
            CherwellAdapter::pathDefault);
        
        Map<String, String> parameters = new HashMap();
        
        // Testing Contact endpoint path
        String path = mapper.getPathbuilder().apply(Arrays.asList("Contact"), parameters);
        assertEquals("/contact", path);
        
        // Testing "id" parameter code
        parameters.put("id", "12345");
        path = mapper.getPathbuilder().apply(Arrays.asList("Contact"), parameters);
        parameters.remove("id");
        assertEquals("/contact/12345", path);
        
        // Testing User endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("User"), parameters);        
        assertEquals("/user", path);
        
        // Testing Incident endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Incident"), parameters);        
        assertEquals("/incident", path);
        
        // Testing Service Request endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Service Request"), parameters);        
        assertEquals("/servicerequest", path);
        
        // Testing Action endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Action"), parameters);        
        assertEquals("/action", path);
        
        // Testing Task endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Task"), parameters);        
        assertEquals("/task", path);
        
        // Testing Device endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Device"), parameters);        
        assertEquals("/device", path);
        
        // Testing Job endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Job"), parameters);        
        assertEquals("/job", path);
        
        // Testing Managed Service endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Managed Service"), parameters);        
        assertEquals("/managedservice", path);
        
        // Testing Job Report endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Job Report"), parameters);        
        assertEquals("/jobreport", path);
        
        // Testing Statistic endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Statistic"), parameters);        
        assertEquals("/statistic", path);
        
        // Testing Customer Balance endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Customer Balance"), parameters);        
        assertEquals("/customerbalance", path);
        
        // Testing Product endpoint path
        path = mapper.getPathbuilder().apply(Arrays.asList("Product"), parameters);        
        assertEquals("/product", path);
    }
}
