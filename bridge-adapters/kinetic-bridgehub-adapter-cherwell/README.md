# Harvest Bridge Adapter
An adapter for interacting with the Harvest v2 api

## Configuration Values
| Name                    | Description |
| :---------------------- | :------------------------- |
| Access Token            | The personal access token set up for the Harvest account |
| Account Id              | The account id associated to the personal access token |

## Example Configuration
| Name | Value |
| :---- | :--- |
| Access Token | 555555.rt.a.34t3g45yt5h45... |
| Account Id   | 123123 |

## Supported Structures
| Name                    | Description |
| :---------------------- | :------------------------- |
| Contacts                | Get client contacts   |
| Clients                 | Get clients   |
| Invoices                | Get invoices   |
| Invoice > Messages      | Get messages associated with a given invoice   |
| Invoice > Payments      | Get payments associate with a given invoice   |
| Invoice Item Categories | Get invoice item categories   |
| Estimate > Messages     | Get messages associated with a given estimate   |
| Estimates               | Get estimates   |
| Estimate Item Categories| Get estimate item categories |
| Tasks                   | Get tasks |
| Time Entries            | Get time entries |
| User Assignments        | Get projects user assignments, active and archived |
| Task Assignments        | Get task assignments |
| Projects                | Get projects |
| Reports > Expenses      | Requires a report_type parameter.  Valid values are clients, projects, categories, and team. |
| Adhoc                   | Requires an accessor parameter.  |

## Configuration example
| Structure               | Qualification Mapping      | Description |
| :---------------------- | :------------------------- | :------------------------- |
| Projects                |                   | Get a list of projects     |
| Projects                | client_id=2372100 | Only return projects belonging to the client with the given ID|
| Projects                | id=14308069          | Retrieve a single project  |
| Users                   | id={USER_ID} | Get a user |
| Adhoc                   | projects/{PROJECT_ID} | Retrieve a single project using Adhoc |
| Adhoc                   | projects?accessor=projects&client_id=2372100 | Retrieve projects using Adhoc |

## Notes
* [JsonPath](https://github.com/json-path/JsonPath#path-examples) can be used to access nested values. The root of the path is the accessor for the Structure.
* This adapter has been tested with the 1.0.3 bridgehub adapter.
* The adapter only supports personal access token authentication at this time.  
    - To configure [personal access token](https://id.getharvest.com/)  
    - Information on [personal access token](https://help.getharvest.com/api-v2/authentication-api/authentication/authentication/#personal-access-tokens)  
* Pagination and sort order are not supported by the adapter, but Harvest source api behavior is supported.  
* From more information about harvest api visit [Harvest API v2 Documentation](https://help.getharvest.com/api-v2/)
* This adapter requires an id parameter to be passed to do a retrieve an element.