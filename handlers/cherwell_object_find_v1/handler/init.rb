# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class CherwellObjectFindV1
  def initialize(input)
    # Set the input document attribute
    @input_document = REXML::Document.new(input)

    # Retrieve all of the handler info values and store them in a hash variable named @info_values.
    @info_values = {}
    REXML::XPath.each(@input_document, "/handler/infos/info") do |item|
      @info_values[item.attributes["name"]] = item.text.to_s.strip
    end

    # Retrieve all of the handler parameters and store them in a hash variable named @parameters.
    @parameters = {}
    REXML::XPath.each(@input_document, "/handler/parameters/parameter") do |item|
      @parameters[item.attributes["name"]] = item.text.to_s.strip
    end

    @enable_debug_logging = @info_values['enable_debug_logging'].downcase == 'yes' ||
                            @info_values['enable_debug_logging'].downcase == 'true'
    puts "Parameters: #{@parameters.inspect}" if @enable_debug_logging
  end

  def execute
    error_handling  = @parameters["error_handling"]
    error_message = ""
    id = nil

   begin
    # Retrieve an access token from Cherwell
    resp = RestClient.post(
      @info_values['api_location']+"/token",
      {
        "grant_type" => "password",
        "client_id" => @info_values['client_id'],
        "client_secret" => @info_values['client_secret'],
        "username" => @info_values['username'],
        "password" => @info_values['password']
      }
    )

    resource = RestClient::Resource.new(@info_values['api_location'], {:headers =>
      {
        :authorization => "Bearer #{JSON.parse(resp.body)["access_token"]}",
        :content_type => "application/json",
        :accept => "application/json"
      }
    })

    # Retrieve the business object id
    resp = resource["/api/V1/getbusinessobjectsummary/busobname/#{@parameters['object_name']}"].get
    objects = JSON.parse(resp.body)
    raise "'#{@parameters['object_name']}' does not appear to be a valid object name (no results found when searching for busobname)" if objects.empty?
    bus_obj_id = objects[0]["busObId"]

    # Retrieve the business object template to be able to map field labels to field ids
    resp = resource["/api/V1/getbusinessobjecttemplate"].post({
      "busObId" => bus_obj_id,
      "includeAll" => true
    }.to_json)
    template_fields = JSON.parse(resp.body)["fields"]

    filters = []
    fields = []
    page_size = @parameters['page_size'].to_i

    # Translate field labels into field ids
    parsed_filters = @parameters["filters"].to_s.empty? ? {} : JSON.parse(@parameters['filters'])
    parsed_fields = @parameters["fields"].to_s.strip.split(",")
    if !parsed_filters.empty? || !parsed_fields.empty?
      template_fields.each do |field|
        if parsed_filters.keys.include?(field["displayName"])
          filters.push({
            "fieldId" => field["fieldId"],
            "operator" => "eq",
            "value" => parsed_filters[field["displayName"]]
          })
        end

        fields.push(field["fieldId"]) if parsed_fields.include?(field["displayName"])
      end
    end

    # Initialize the search
    data = {}
    data["busObId"] = bus_obj_id
    data["filters"] = filters
    if fields.empty?
      data["includeAllFields"] = true
    else
      data["fields"] = fields
    end
    data["pageSize"] = page_size

    RestClient.log = "stdout"
    resp = resource["/api/V1/getsearchresults"].post(data.to_json)

    body = JSON.parse(resp.body)
    matched = body["totalRows"]
    json_results = []
    body["businessObjects"].each do |object|
      flat_obj = {}
      object.each do |k,v|
        if k == "fields"
          v.each do |field|
            flat_obj[field["displayName"]] = field["value"]
          end
        else
          flat_obj[k] = v
        end
      end
      json_results.push(flat_obj)
    end
  rescue RestClient::ResourceNotFound => error
    error_message = error.inspect
    raise "404 Not Found: Make sure the 'server', 'api_route'. and 'api_parameters' are valid inputs: #{error.http_body}."
  rescue RestClient::Exception => error
    error_message = error.inspect
    raise error if error_handling == "Raise Error"
  end
    return <<-RESULTS
    <results>
      <result name="Handler Error Message">#{escape(error_message)}</result>
      <result name="JSON Results">#{escape(json_results.to_json)}</result>
      <result name="Count">#{json_results.size}</result>
      <result name="Total Matched">#{matched}</result>
    </results>
    RESULTS
  end

  ##############################################################################
  # General handler utility functions
  ##############################################################################

  # This is a template method that is used to escape results values (returned in
  # execute) that would cause the XML to be invalid.  This method is not
  # necessary if values do not contain character that have special meaning in
  # XML (&, ", <, and >), however it is a good practice to use it for all return
  # variable results in case the value could include one of those characters in
  # the future.  This method can be copied and reused between handlers.
  def escape(string)
    # Globally replace characters based on the ESCAPE_CHARACTERS constant
    string.to_s.gsub(/[&"><]/) { |special| ESCAPE_CHARACTERS[special] } if string
  end
  # This is a ruby constant that is used by the escape method
  ESCAPE_CHARACTERS = {'&'=>'&amp;', '>'=>'&gt;', '<'=>'&lt;', '"' => '&quot;'}
end
