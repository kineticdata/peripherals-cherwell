# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class CherwellObjectCreateV1
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
    id = nil,
    recid = nil,

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
      raise "'#{@parameters['object_name']}' does not appear to be a valid object name (no results found when searching for busobname)" if objects.empty?
      bus_obj_id = objects[0]["busObId"]

      # Retrieve the business object template for the business object id
      resp = resource["/api/V1/getbusinessobjecttemplate"].post({
        "busObId" => bus_obj_id,
        "includeAll" => true
      }.to_json)
      template_fields = JSON.parse(resp.body)["fields"]

      begin
        new_fields = JSON.parse(@parameters['object_json'])

        # Assign the inputted field values into template_fields which will be the fields that will be
        # passed to the create call
        new_field_display_names = new_fields.keys
        body_fields = []
        template_fields.each do |field|
          if new_field_display_names.include?(field["displayName"])
            body_fields.push({
              "dirty" => true,
              "displayName" => field["displayName"],
              "fieldId" => field["fieldId"],
              "html" => field["html"],
              "name" => field["name"],
              "value" => new_fields[field["displayName"]]
            })
            field["value"] = new_fields[field["displayName"]]
            field["dirty"] = true
          end
        end
      rescue
        error_message = "Error encountered while attempting to parse the Object JSON parameter values."
        raise error if error_handling == "Raise Error"
      end

      # Create the new object using the business object id and the template field
      resp = resource["/api/V1/savebusinessobject"].post({
        "busObId" => bus_obj_id,
        "fields" => body_fields
      }.to_json)
      id = JSON.parse(resp.body)["busObPublicId"]
      recid = JSON.parse(resp.body)["busObRecId"]
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
      <result name="Object Id">#{escape(id)}</result>
      <result name="Rec Id">#{escape(recid)}</result>
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
