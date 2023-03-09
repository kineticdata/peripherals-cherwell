# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class CherwellObjectRetrieveV1
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

        # Retrieve the object based on the business object id and the inputted object id
        resp = resource["/api/V1/getbusinessobject/busobid/#{bus_obj_id}/busobrecid/#{@parameters['object_id']}"].get

        object = JSON.parse(resp.body)
        # Move the fields from a list to a map of {displayName: value}
        fields = {}
        object["fields"].each { |f| fields[f["displayName"]] = f["value"]}
        object["fields"] = fields
      end
    rescue RestClient::ResourceNotFound => error
      error_message = error.inspect
      raise "404 Not Found: Make sure the 'server', 'api_route'. and 'api_parameters' are valid inputs: #{error.http_body}." if error_handling == "Raise Error"
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
      <result name="Object JSON">#{escape(begin object.to_json rescue nil end)}</result>
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
