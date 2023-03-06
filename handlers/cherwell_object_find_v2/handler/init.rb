# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class CherwellObjectFindV2
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
    json_results = []
    matched = 0

   begin
    # Retrieve an access token from Cherwell
    resp = RestClient.post(
      @info_values['api_location']+"/token?auth_mode=#{@info_values['auth_mode']}",
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
    if objects.empty?
      error_message = "'#{@parameters['object_name']}' does not appear to be a valid object name (no results found when searching for busobname)"
      raise "#{error_message}" if error_handling == "Raise Error"
    else
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
      parsed_filters = @parameters["filters"].to_s.empty? ? [] : JSON.parse(@parameters['filters'])
      parsed_fields = @parameters["fields"].to_s.strip.split(",").map!{|item| item.strip}
      filter_error = false
      supported_operations = ["eq","gt","lt","startswith","contains"]
      if !parsed_filters.empty? || !parsed_fields.empty?
        parsed_filters.each do |filter|
          if supported_operations.include?(filter['operator'])
            field_found = false
            template_fields.each do |field|
              if filter['fieldname'] == field["displayName"] || filter['fieldname'] == field["name"]
                filters.push({
                  "fieldId" => field['fieldId'],
                  "operator" => filter['operator'],
                  "value" => filter['value']
                })
                field_found = true
              end
            end
            # Invalid field
            if !field_found
              error_message = "This filter\n\n#{filter.to_json}\n\nHas an invalid field: #{filter['fieldname']}."
              filter_error = true
              break
            end
          else
            # Invalid operator
            error_message = "This filter\n\n#{filter.to_json}\n\nHas an invalid operator: #{filter['operator']}.  Supported operations are: #{supported_operations.join(",")}"
            filter_error = true
            break
          end
        end
      end
      raise "#{error_message}" if filter_error

      # Populate which fields to retrieve
      # Invalid fields don't break things, the API just doesn't return data for invalid fields.
      template_fields.each do |field|
        if parsed_fields.include?(field["displayName"]) || parsed_fields.include?(field["name"])
          fields.push(field["fieldId"])
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

      #RestClient.log = "stdout"
      resp = resource["/api/V1/getsearchresults"].post(data.to_json)

      body = JSON.parse(resp.body)
      matched = body["totalRows"]
      body["businessObjects"].each do |object|
        flat_obj = {}
        object.each do |k,v|
          if k == "fields"
            v.each do |field|
              #flat_obj[field["displayName"]] = field["value"]
              flat_obj[field["displayName"]] = field["value"].force_encoding('ISO-8859-1').force_encoding('UTF-8')
            end
          else
            flat_obj[k] = v
          end
        end
        json_results.push(flat_obj)
      end
    end
  rescue RestClient::ResourceNotFound => error
    error_message = error.inspect
    raise "404 Not Found: Make sure the 'server', 'api_route'. and 'api_parameters' are valid inputs: #{error.http_body}."
  rescue RestClient::ExceptionWithResponse => error
    begin
      error_message = error.response
    rescue
      error_message = error.inspect
    end
    raise error if error_handling == "Raise Error"
  rescue RestClient::Exception => error
    error_message = error.inspect
    raise error if error_handling == "Raise Error"
  rescue Exception => error
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
