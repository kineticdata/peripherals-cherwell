{
  'info' => {
    'api_location' => '',
    'client_id' => '',
    'client_secret' => '',
    'username' => '',
    'password' => '',
    'auth_mode' => 'Internal',
    'enable_debug_logging'=>'yes'
  },
  'parameters' => {
    'error_handling' => 'Error Message',
    'object_name' => 'Incident',
    'fields' => 'Status,Description,Short Description,CreatedDateTime',
    'filters' => '[{
      "fieldname":"CreatedDateTime",
      "operator":"gt",
      "value":"01/01/2023"
    },{
      "fieldname":"Description",
      "operator":"contains",
      "value":"Service Portal"
    },{
      "fieldname":"Description",
      "operator":"contains",
      "value":"integration"
    }]',
    'page_size' => '15'
  }
}
