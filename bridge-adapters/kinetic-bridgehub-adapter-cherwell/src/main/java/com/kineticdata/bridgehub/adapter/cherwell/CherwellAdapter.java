package com.kineticdata.bridgehub.adapter.cherwell;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.BridgeUtils;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;

public class CherwellAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    public CherwellAdapter () {
        // Parse the query and exchange out any parameters with their parameter 
        // values. ie. change the query username=<%=parameter["Username"]%> to
        // username=test.user where parameter["Username"]=test.user 
        parser = new CherwellQualificationParser();
        
    }
    
    /*----------------------------------------------------------------------------------------------
     * STRUCTURES
     *      AdapterMapping( Structure Name, accessor, Path Function)
     *--------------------------------------------------------------------------------------------*/
    public static Map<String,AdapterMapping> MAPPINGS 
        = new HashMap<String,AdapterMapping>() {{
        put("Contact", new AdapterMapping("Contact", "Data",
            CherwellAdapter::pathDefault));
        put("Contact Profile", new AdapterMapping("Contact Profile", "Data",
            CherwellAdapter::pathDefault));
        put("User", new AdapterMapping("User", "Data",
            CherwellAdapter::pathDefault));
        put("Incident", new AdapterMapping("Incident", "Data",
            CherwellAdapter::pathDefault));
        put("Service Request", new AdapterMapping("Service Request", "Data",
            CherwellAdapter::pathDefault));
        put("Action", new AdapterMapping("Action", "Data",
            CherwellAdapter::pathDefault));
        put("Task", new AdapterMapping("Task", "Data",
            CherwellAdapter::pathDefault));
        put("Device", new AdapterMapping("Device", "Data",
            CherwellAdapter::pathDefault));
        put("Job", new AdapterMapping("Job", "Data",
            CherwellAdapter::pathDefault));
        put("Managed Service", new AdapterMapping("Managed Service", "Data",
            CherwellAdapter::pathDefault));
        put("Job Report", new AdapterMapping("Job Report", "Data",
            CherwellAdapter::pathDefault));
        put("Statistic", new AdapterMapping("Statistic", "Data",
            CherwellAdapter::pathDefault));
        put("Customer Balance", new AdapterMapping("Customer Balance", "Data",
            CherwellAdapter::pathDefault));
        put("Product", new AdapterMapping("Product", "Data",
            CherwellAdapter::pathDefault));
        put("Adhoc", new AdapterMapping("Adhoc", "",
            CherwellAdapter::pathAdhoc));
    }};

    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/

    /** Defines the adapter display name */
    public static final String NAME = "Cherwell Bridge";

    /** Defines the LOGGER */
    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CherwellAdapter.class);

    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties
                    .load(CherwellAdapter.class.getResourceAsStream("/" + CherwellAdapter.class.getName() + ".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            LOGGER.warn("Unable to load " + CherwellAdapter.class.getName() + " version properties.", e);
            VERSION = "Unknown";
        }
    }

    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String PROPERTY_API_LOCATION = "API Location";
        public static final String PROPERTY_USERNAME = "Username";
        public static final String PROPERTY_PASSWORD = "Password";
        public static final String PROPERTY_CLIENT_ID = "Client ID";
        
    }
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
            new ConfigurableProperty(Properties.PROPERTY_USERNAME).setIsRequired(true),
            new ConfigurableProperty(Properties.PROPERTY_PASSWORD).setIsRequired(true).setIsSensitive(true),
            new ConfigurableProperty(Properties.PROPERTY_CLIENT_ID).setIsRequired(true),
            new ConfigurableProperty(Properties.PROPERTY_API_LOCATION).setIsRequired(true));

    // Local variables to store the property values in
    private final CherwellQualificationParser parser;
    private CherwellApiHelper apiHelper;
    

    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public void initialize() throws BridgeError {
        // Initializing the variables with the property values that were passed
        // when creating the bridge so that they are easier to use
        String baseUrl = properties.getValue(Properties.PROPERTY_API_LOCATION);
        String clientId = properties.getValue(Properties.PROPERTY_CLIENT_ID);
        String username = properties.getValue(Properties.PROPERTY_USERNAME);
        String password = properties.getValue(Properties.PROPERTY_PASSWORD);
        apiHelper = new CherwellApiHelper(baseUrl, clientId, username, password);
        // passing an empty string as xUserHeader to the validation request
        // since the validation endpoint does not require it
        apiHelper.getToken();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void setProperties(Map<String, String> parameters) {
        // This should always be the same unless there are special circumstances
        // for changing it
        properties.setValues(parameters);
    }

    @Override
    public ConfigurablePropertyMap getProperties() {
        // This should always be the same unless there are special circumstances
        // for changing it
        return properties;
    }
    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public Count count(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Counting records");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }

        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        parameters.put("GetTotal", "true");
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        // Access and remove X-User Parameter
        String xUserHeader = getXUser(parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
       
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeGetRequest(getUrl(path, parameterMap));
        
        // Get the number of elements in the returned array
        Long tempCount = (Long)responseObject.get("Total");
        Integer count = 0;
        count = (int) tempCount.intValue();

        // Create and return a count object that contains the count
        return new Count(count);
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Retrieving Kinetic Request CE Record");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            LOGGER.trace("  Fields: " + request.getFieldString());
        }
        
        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
                
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        // Access and remove X-User Parameter
        String xUserHeader = getXUser(parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);

        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeGetRequest(getUrl(path, parameterMap));
        
        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        Record record = new Record();
        if (responseArray.size() == 1) {
            // Reassign object to single result 
            JSONObject object = (JSONObject)responseArray.get(0);
                
            List<String> fields = getFields(request.getFields() == null ? 
                new ArrayList() : request.getFields(), object);
            record = buildRecord(fields, object);
        } else if (responseArray.isEmpty()) {
            LOGGER.debug("No results found for query: {}", request.getQuery());
        } else {
            throw new BridgeError ("Retrieve must return a single result."
                + " Multiple results found.");
        }

        // Return the created Record object
        return record;
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Searching Records");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            LOGGER.trace("  Fields: " + request.getFieldString());
        }
        
         // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        Map<String, String> metadata = request.getMetadata() != null ?
                request.getMetadata() : new HashMap<>();
        
        // If form defines sort order use it. This will overwrite $orderby in the
        // qualification mapping.
        if (metadata.get("order") != null) {
            parameters.put("OrderBy", addSort(metadata.get("order")));
        }
        
        // Pagination Logic
        // If PageToken is already a parameter, ignore metadata.
        String nextPage = metadata.get("next_page");
        if (!parameters.containsKey("PageToken") && nextPage != null && !nextPage.isBlank()) {
            parameters.put("PageToken", metadata.get("next_page"));
        }
                
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        // Access and remove X-User Parameter
        String xUserHeader = getXUser(parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeGetRequest(getUrl(path, 
            parameterMap));
        
        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        // Create a List of records that will be used to make a RecordList object.
        List<Record> recordList = new ArrayList<Record>();      
        List<String> fields = request.getFields() == null ? new ArrayList() : 
            request.getFields();        
        if(responseArray != null && responseArray.isEmpty() != true){
            fields = getFields(fields, (JSONObject)responseArray.get(0));

            // Iterate through the responce objects and make a new Record for each.
            for (Object o : responseArray) {
                JSONObject obj = (JSONObject)o;
                Record record = buildRecord(fields, obj);
                
                // Add the created record to the list of records
                recordList.add(record);
            }
        }

        metadata.put("nextPageToken", String.valueOf(responseObject.get("NextPage")));
        metadata.put("size", String.valueOf(responseObject.get("Total")));
        
        // Return the RecordList object
        return new RecordList(fields, recordList, metadata);
    }

    /*--------------------------------------------------------------------------
     * HELPER METHODS
     *------------------------------------------------------------------------*/
    
    /**
     * Take the sort order from metadata and add it to parameters for use with
     * request.
     * 
     * @param order
     * @return
     * @throws BridgeError 
     */
    protected String addSort(String order) throws BridgeError {
        
        LinkedHashMap<String,String> sortOrderItems = getSortOrderItems(
                BridgeUtils.parseOrder(order));
        String sortOrderString = sortOrderItems.entrySet().stream().map(entry -> {
            return entry.getKey() + " " + entry.getValue().toUpperCase();
        }).collect(Collectors.joining(","));
                    
        LOGGER.trace("Adding $orderby parameter because form has order "
            + "feilds \"{}\" defined", sortOrderString);
        return sortOrderString;
    }
    
    protected List<String> getFields(List<String> fields, JSONObject jsonobj) {
        // if no fields were provided then all fields will be returned. 
        if(fields.isEmpty()){
            fields.addAll(jsonobj.keySet());
        }
        
        return fields;
    }
    
    protected String getXUser(Map<String, String> parameters) throws BridgeError {
        String xUserHeader;
            if (parameters.containsKey("xUser")) {
                xUserHeader = parameters.get("xUser");
                parameters.remove("xUser");
            } else {
                throw new BridgeError("xUser parameter is required.");
            }
        
            return xUserHeader;
    }
    
    /**
     * Build a Record.  If no fields are provided all fields will be returned.
     * 
     * @param fields
     * @param jsonobj
     * @return Record
     */
    protected Record buildRecord (List<String> fields, JSONObject jsonobj) {
        JSONObject obj = new JSONObject();
        DocumentContext jsonContext = JsonPath.parse(jsonobj); 
        
        fields.stream().forEach(field -> {
            // either use JsonPath or just add the field value.  We're assuming
            // all JsonPath usages will begin with $[ or $.. 
            if (field.startsWith("$.") || field.startsWith("$[")) {
                try {
                    obj.put(field, jsonContext.read(field));
                } catch (JsonPathException e) {
                    // if field is a valid path but object is missing the property
                    // return null for field.  This is consistent with existing 
                    // adapter behavior.
                    if (e.getMessage().startsWith("Missing property")) {
                        obj.put(field, null);
                        LOGGER.debug(String.format("%s was not found, returning"
                            + " null value", field), e);
                    } else {   
                        throw new JsonPathException(String.format("There was an issue"
                            + " reading %s", field), e);
                    }
                }
            } else {
                obj.put(field, jsonobj.get(field));
            }
        });
        
        Record record = new Record(obj, fields);
        return record;
    }
    
        
    protected JSONArray getResponseData(Object responseData) {
        JSONArray responseArray = new JSONArray();
        
        if (responseData instanceof JSONArray) {
            responseArray = (JSONArray)responseData;
        }
        else if (responseData instanceof JSONObject) {
            // It's an object
            responseArray.add((JSONObject)responseData);
        }
        
        return responseArray;
    }
    
    /**
     * Get accessor value. If structure is Adhoc remove accessor from parameters.
     * 
     * @param mapping
     * @param parameters
     * @return 
     */
    private String getAccessor(AdapterMapping mapping, Map<String, String> parameters) {
        String accessor;
        
        if (mapping.getStructure().equals("Adhoc")) {
            accessor = parameters.get("accessor");
            parameters.remove("accessor");
        } else {
            accessor = mapping.getAccessor();
        }
        
        return accessor;
    }
    
    /**
     * This helper is intended to abstract the parser get parameters from the core
     * methods.
     * 
     * @param request
     * @param mapping
     * @return
     * @throws BridgeError
     */
    protected Map<String, String> getParameters(String query, AdapterMapping mapping) throws BridgeError {

        Map<String, String> parameters = new HashMap<>();
        if (mapping.getStructure() == "Adhoc") {
            // Adhoc qualifications are two segments. ie path?queryParameters
            String[] segments = query.split("[?]", 2);

            // getParameters only needs the queryParameters segment
            if (segments.length > 1) {
                parameters = parser.getParameters(segments[1]);
            }
            // Pass the path along to the functional operator
            parameters.put("adapterPath", segments[0]);
        } else {
            parameters = parser.getParameters(query);
        }

        return parameters;
    }

    /**
     * This method checks that the structure on the request matches on in the
     * Mapping internal class. Mappings map directly to the adapters supported
     * Structures.
     * 
     * @param structure
     * @return Mapping
     * @throws BridgeError
     */
    protected AdapterMapping getMapping(String structure) throws BridgeError {
        AdapterMapping mapping = MAPPINGS.get(structure);
        if (mapping == null) {
            throw new BridgeError("Invalid Structure: '" + structure + "' is not a valid structure");
        }
        return mapping;
    }

    protected Map<String, NameValuePair> buildNameValuePairMap(Map<String, String> parameters) {
        Map<String, NameValuePair> parameterMap = new HashMap<>();

        parameters.forEach((key, value) -> {
            parameterMap.put(key, new BasicNameValuePair(key, value));
        });

        return parameterMap;
    }
    
    protected String getUrl (String path,
        Map<String, NameValuePair> parameters) {
        
        return String.format("%s?%s", path, 
            URLEncodedUtils.format(parameters.values(), Charset.forName("UTF-8")));
    }
    
    /**
     * Ensure that the sort order list is linked so that order can not be changed.
     * 
     * @param uncastSortOrderItems
     * @return
     * @throws IllegalArgumentException 
     */
    private LinkedHashMap<String, String> 
        getSortOrderItems (Map<String, String> uncastSortOrderItems)
        throws IllegalArgumentException{
        
        /* results of parseOrder does not allow for a structure that 
         * guarantees order.  Casting is required to preserver order.
         */
        if (!(uncastSortOrderItems instanceof LinkedHashMap)) {
            throw new IllegalArgumentException("Sort Order Items was invalid.");
        }
        
        return (LinkedHashMap)uncastSortOrderItems;
    }
    
    /**************************** Path Definitions ****************************/
    
    /**
     * 
     * Default requires single path and retrieve parameters must contain an "id"
     * @param structureList
     * @param parameters
     * @return
     * @throws BridgeError 
     * 
     */
    protected static String pathDefault(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {
        
        String path = "";
        
        switch (structureList.get(0)) {
            case "Contact":
                path = "/contact";
                break;
            case "Contact Profile":
                path = "/contactprofile";
                break;
            case "User":
                path = "/user";
                break;
            case "Incident":
                path = "/incident";
                break;
            case "Service Request":
                path = "/servicerequest";
                break;
            case "Action":
                path = "/action";
                break;
            case "Task":
                path = "/task";
                break;
            case "Device":
                path = "/device";
                break;
            case "Job":
                path = "/job";
                break;
            case "Managed Service":
                path = "/managedservice";
                break;
            case "Job Report":
                path = "/jobreport";
                break;
            case "Statistic":
                path = "/statistic";
                break;
            case "Customer Balance":
                path = "/customerbalance";
                break;
            case "Product":
                path = "/product";
                break;
            default:
                throw new BridgeError("Structure must exist.");
        }
        
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }
    
    /**
     * Build path for Adhoc structure.
     * 
     * @param structureList
     * @param parameters
     * @return
     * @throws BridgeError 
     */
    protected static String pathAdhoc(List<String> structureList, 
        Map<String, String> parameters) throws BridgeError {
        
        return parameters.get("adapterPath");
    }

    /**
     * Checks if a parameter exists in the parameters Map.
     * 
     * @param param
     * @param parameters
     * @param structureList
     * @throws BridgeError 
     */
    protected static void checkRequiredParamForStruct(String param,
        Map<String, String> parameters, List<String> structureList)
        throws BridgeError{
        
        if (!parameters.containsKey(param)) {
            String structure = String.join(" > ", structureList);
            throw new BridgeError(String.format("The %s structure requires %s"
                + "parameter.", structure, param));
        }
    }
}
