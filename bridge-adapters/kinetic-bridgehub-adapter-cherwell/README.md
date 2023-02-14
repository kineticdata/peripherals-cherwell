# Cherwell Bridge Adapter
An adapter for interacting with some Cherwell rest api endpoints.  The adapter authenticates using [Internal Mode](https://help.cherwell.com/bundle/cherwell_rest_api_960_help_only/page/content/system_administration/rest_api/csm_rest_oauth2_internal_authentication.html) which requires a username, password, and client id

## Configuration Values
| Name         | Description                                         | Example                               |
|:-------------|:----------------------------------------------------|:--------------------------------------|
| API Location | The domain and partial path                         | https://cherwell.acme.com/CherwellAPI |
| Username     | The username of an integration account              | Admin                                 |
| Password     | The password assocated with the integration account | *****                                 |
| Client Id    | An Id created in the [CSM Administrator](https://help.cherwell.com/bundle/cherwell_rest_api_960_help_only/page/content/system_administration/rest_api/csm_rest_obtaining_client_ids.html)          | b3xxxx8e-3xx4-41d0-96f4-xxxx33e5xxxx  |


## Supported Structures
| Name                         | Description                                                  |
|:-----------------------------|:-------------------------------------------------------------|
| Teams                        | Get a list of teams.                                         |
| Adhoc > BUSINESS_OBJECT_NAME | Adhoc filter query. See additional details in notes section. |
| Saved Search > Internal Id   | Run a saved search by id number.                             |
| Saved Search > Name          | Run a saved search by name                                   |
| Saved Search > Results V1    | Get the results of a saved search using v1 api.              |
| Saved Search > Results V2    | Get the results of a saved search using v2 api.              |

## Configuration example
See unit tests written for the adapter.

## Notes
* [JsonPath](https://github.com/json-path/JsonPath#path-examples) can be used to access nested values. The root of the path is the accessor for the Structure.
* This adapter has been tested with the 1.0.3 bridgehub adapter. 
* Pagination and sort order are not supported by the adapter, but some Cherwell source api behavior is supported.  
* From more information about harvest api visit [Cherwell Documentation](https://help.cherwell.com/bundle/cherwell_rest_api_10_2_help_only/page/content/system_administration/rest_api/csm_rest_api_landing_page.html)

* **Adhoc Filter search details.**  Using the Adhoc > BUSINESS_OBJECT_NAME structure will make several rest requests sequentially.  There are several requests made so that human-readable names can be used in the bridge model definition that will be replaced with ids. 
  * Adhoc filter Structure example:
  `Adhoc > Incident`
  * Adhoc filter qualification example:
  ```
    dataRequest={
     "filters":[{
       "fieldName":"Status",
       "operator":"eq",
       "value":"Open"
     }],
    "includeAllFields":true,
    "pageSize":3
  }
  ```
  * `dataRequest` is a key used to pass the Adhoc Filter data request JSON object to Cherwell.
  * The adapter will look up the business object id using the business object name with a GET request to the [Business Object summary](https://cherwell.kineticdata.com/CherwellAPI/swagger/ui/index#!/BusinessObject/BusinessObject_GetBusinessObjectSummaryByNameV1)
    * The business object name is passed to the bridge adapter using the Structure. **Incident** in the above example configuration.
  * Using the business object id the next requests gets the field ids for the business object using a POST request to the [Business Object template](https://cherwell.kineticdata.com/CherwellAPI/swagger/ui/index#!/BusinessObject/BusinessObject_GetBusinessObjectTemplateV1)
    * The field names are passed to the adapter using the `dataRequest` parameter in the bridge model Query.  In the above example **Status** is the field that will be used in the Cherwell filter search.
  * To run an ad-hoc Business Object search the `dataRequest` parameter must be provided in the bridge model qualification. An example of the dataRequest is above.  The `fieldName` property will be replaced with a `fieldId` property and the field id found from previous request.  The busObId will also be added to the dataRequest object.