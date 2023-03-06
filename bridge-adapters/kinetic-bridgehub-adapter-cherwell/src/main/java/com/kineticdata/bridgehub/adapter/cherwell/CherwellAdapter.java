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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
        put("Teams", new AdapterMapping("Teams", "teams",
            CherwellAdapter::pathTeams));
        put("Saved Search", new AdapterMapping("Saved Search", "businessObjects",
                CherwellAdapter::pathSavedSearch));
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
    /** Load the properties, version from the version.properties file. */
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
    
    // Constants
    static String PATH = "/api";    

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
            parser.parse(request.getQuery(),request.getParameters()), structureList);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
       
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));
        
        // Get the number of elements in the returned array
        JSONArray teamsArray = (JSONArray) responseObject.get(mapping.getAccessor());
        Integer count = teamsArray.size();

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
            parser.parse(request.getQuery(),request.getParameters()), structureList);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
                
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);

        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));
        
        JSONArray responseArray;
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
            parser.parse(request.getQuery(),request.getParameters()), structureList);
        
        Map<String, String> metadata = request.getMetadata() != null ?
                request.getMetadata() : new HashMap<>();

        String accessor;
        JSONObject responseObject;

        // If the Structure is Adhoc and there is more than one element in the list take another code path to support filtered search.
        if (structureList.get(0).equals("Adhoc") && structureList.size() > 1) {
            /* !Important! pagination is managed by platform developer for adhoc structures */

            // confirm the request has required property
            checkRequiredParamForStruct("dataRequest", parameters, structureList);

            // Build path to fetch business object by name.
            String path = mapping.getPathbuilder().apply(structureList, parameters);

            responseObject = getSearchByFilter(path, parameters);
            accessor = "businessObjects";
        } else {
            // Pagination Logic
            if (metadata.get("count") != null && metadata.get("nextPageToken") != null) {
                parameters.put("pagesize", metadata.get("count"));
                parameters.put("pagenumber", metadata.get("nextPageToken"));
            }

            // Path builder functions may mutate the parameters Map.
            String path = mapping.getPathbuilder().apply(structureList, parameters);

            // Accessor values is either passed as a parameter in the qualification
            // mapping for Adhoc or on the mapping for all other structures.
            accessor = getAccessor(mapping, parameters);

            Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);

            // Retrieve the objects based on the structure from the source
            responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));

            // TODO: Pagination is odd with cherwell.  Should we update metadata?
        }

        JSONArray responseArray;
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        // Create a List of records that will be used to make a RecordList object.
        List<Record> recordList = new ArrayList<>();
        List<String> fields = request.getFields() == null ? new ArrayList() : 
            request.getFields();        
        if(responseArray != null && responseArray.isEmpty() != true){
            fields = getFields(fields, (JSONObject)responseArray.get(0));

            // Iterate through the response objects and make a new Record for each.
            for (Object o : responseArray) {
                JSONObject obj = (JSONObject)o;
                Record record = buildRecord(fields, obj);
                
                // Add the created record to the list of records
                recordList.add(record);
            }
        }

        // Return the RecordList object
        return new RecordList(fields, recordList, metadata);
    }

    /*--------------------------------------------------------------------------
     * HELPER METHODS
     *------------------------------------------------------------------------*/
    private JSONObject getSearchByFilter (String path, Map<String, String> parameters) throws BridgeError {
        JSONObject responseObject;

        // Move "dataRequest" into a variable and JSON parse and remove parameter.
        JSONParser jsonParser = new JSONParser();
        JSONObject dataRequest = null;
        try {
            dataRequest = (JSONObject) jsonParser.parse(parameters.get("dataRequest"));
            parameters.remove("dataRequest");
        } catch (ParseException e) {
            throw new BridgeError("There was an issue parsing the dataRequest parameter: ", e);
        }

        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);

        /************************* Retrieve Business Object *****************************/
        // Retrieve business object to get the business object id.
        responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));

        // Only expecting a single element to be returned
        JSONObject businessObject = (JSONObject) ((JSONArray)responseObject.get("value")).get(0);
        String busObId = (String) businessObject.get("busObId");

        // Build body to be used in next request
        JSONObject businessObjectBody = new JSONObject() {{
            put("busObId", busObId);
            put("includeAll", true);
        }};

        // Add the business object id to be used in final search
        dataRequest.put("busObId", busObId);
        /*********************************************************************************/

        /************************* Retrieve Business Template ****************************/
        // The data request filters have field names but need field ids.  Users of the adapter will not know ids so
        // the adapter needs to look them up.

        // The path is always static because the request is a POST
        path = PATH + "/v1/getbusinessobjecttemplate";

        // TODO: Will we need parameters?

        // Retrieve the businesses template
        responseObject = apiHelper.executeRequest(path, businessObjectBody.toJSONString());

        /************** Modify the filters to be used in next request **************/
        DocumentContext jsonContext = JsonPath.parse(responseObject);

        JSONArray filters = (JSONArray) dataRequest.get("filters");
        Stream<JSONObject> arrayStream = filters.stream().map((filterItem) -> {
            JSONObject filterObj = (JSONObject)filterItem;

            // Build JsonPath expression to get field ids
            String expression = String.format("$.fields[?(@.name == \"%s\" )].fieldId", filterObj.get("fieldName"));
            net.minidev.json.JSONArray tempArray =  jsonContext.read(expression);

            if (tempArray.size() <= 0) {
                throw new RuntimeException(new BridgeError("Field name \""+filterObj.get("fieldName")+"\" was not found."));
            }

            String fieldId = (String)(tempArray).get(0);

            // Add the field id to the filter object to be used in next query
            filterObj.put("fieldId", fieldId);
            // Remove the field name from the filter object because it is no longer needed
            filterObj.remove("fieldName");

            return filterObj;
        });
        JSONArray filterArray = arrayStream.collect(
                JSONArray::new,
                (JSONArray jsonArray1, JSONObject jsonObject) ->  jsonArray1.add(jsonObject),
                (JSONArray jsonArray1, JSONArray jsonArray2) -> jsonArray1.addAll(jsonArray2)
        );

        // add the updated filters to the data request
        dataRequest.put("filters", filterArray);
        /*********************************************************************************/

        /************************* Retrieve Business Template ****************************/
        path = PATH + "/V1/getsearchresults";

        return apiHelper.executeRequest(path, dataRequest.toJSONString());

        /*********************************************************************************/
    }

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
            + "fields \"{}\" defined", sortOrderString);
        return sortOrderString;
    }
    
    protected List<String> getFields(List<String> fields, JSONObject jsonobj) {
        // if no fields were provided then all fields will be returned. 
        if(fields.isEmpty()){
            fields.addAll(jsonobj.keySet());
        }
        
        return fields;
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
     * This helper is intended to abstract the parser get parameters from the core methods.
     * 
     * @param query
     * @param structureList
     * @return
     * @throws BridgeError
     */
    protected Map<String, String> getParameters(String query, List<String> structureList) throws BridgeError {

        Map<String, String> parameters = new HashMap<>();
        if (structureList.get(0).equals("Adhoc") && structureList.size() == 1) {
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
     * This method checks that the structure on the request matches on in the Mapping internal class.
     * Mappings map directly to the adapters supported Structures.
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
    
    protected String getUrl (String path, Map<String, NameValuePair> parameters) {
        return String.format("%s?%s", path, URLEncodedUtils.format(parameters.values(), StandardCharsets.UTF_8));
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

    /**
     * Encode values for use in URLs
     *
     * @param input
     * @return
     */
    public static String urlEncode(String input) throws BridgeError {
         String encodedValue = null;
         try {
             encodedValue = URLEncoder
                     .encode(input, StandardCharsets.UTF_8.toString())
                     .replace("+", "%20");
         } catch (UnsupportedEncodingException e) {
             throw new BridgeError("There was an error encoding the URL parameter '"+input+"': ", e);
         }

         return encodedValue;
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
    protected static String pathTeams(List<String> structureList,
        Map<String, String> parameters) throws BridgeError{
        
        String path = PATH;
        
        if (parameters.containsKey("id")) {
            path += "/V1/getteam/" + urlEncode(parameters.get("id"));
            parameters.remove("id");
        } else {
            path += "/V1/getteams";
        }
        
        return path;
    }

    /**
     * Build a path to get saved search results for v1 and v2 along with running saved search by name and internal id.
     *
     * @param structureList
     * @param parameters
     * @return
     * @throws BridgeError
     */
    protected static String pathSavedSearch(List<String> structureList, Map<String, String> parameters) throws BridgeError {
        String path = PATH;

        switch (structureList.get(1)) {
            case "Internal ID":
            case "Name":

                // Search by Internal Id or Name
                String searchType = "";
                if (structureList.get(1).equals("Internal ID")) {
                    searchType = "searchid";
                } else {
                    searchType = "searchName";
                }

                if (!(parameters.containsKey("association")
                        && parameters.containsKey("scope")
                        && parameters.containsKey("scopeowner")
                        && parameters.containsKey(searchType))) {
                    throw new BridgeError("The Saved Search > "+ structureList.get(1) +" structure requires \"association\", \"scope\"," +
                            " \"scopeowner\", and \""+ searchType +"\"");
                }

                path += String.format("/V1/getsearchresults/association/%s/scope/%s/scopeowner/%s/%s/%s",
                        urlEncode(parameters.get("association")), urlEncode(parameters.get("scope")), urlEncode(parameters.get("scopeowner")),
                        searchType, urlEncode(parameters.get(searchType)));
                parameters.remove("association");
                parameters.remove("scope");
                parameters.remove("scopeowner");
                parameters.remove(searchType);
                break;

            case "Results V1":
            case "Results V2":
                if (!(parameters.containsKey("scope")
                        && parameters.containsKey("associationName")
                        && parameters.containsKey("searchName"))) {
                    throw new BridgeError("The Saved Search > Results V1 structure requires \"scope\", \"associationName\"," +
                            " and \"searchName\"");
                }

                path += String.format("/%s/storedsearches/%s/%s/%s",
                        structureList.get(1).equals("Results V1") ? "V1" : "V2", urlEncode(parameters.get("scope")),
                        urlEncode(parameters.get("associationName")), urlEncode(parameters.get("searchName")));
                parameters.remove("associationName");
                parameters.remove("scope");
                parameters.remove("searchName");
                break;

            default:
                // To be configured with each new case
                throw new BridgeError(String.format("The %s structure requires a substructure: \"Name\", \"Internal ID\", \"Results V1 \", or \"Results V2\"", structureList.get(0)));
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
    protected static String pathAdhoc(List<String> structureList, Map<String, String> parameters) throws BridgeError {
        String path = null;

        if (structureList.size() > 1) {
            path = String.format("%s/V1/getbusinessobjectsummary/busobname/%s", PATH, urlEncode(structureList.get(1)));
        } else {
            path = parameters.get("adapterPath");
        }

        return path;
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
            throw new BridgeError(String.format("The %s structure requires %s parameter.", structure, param));
        }
    }
}
