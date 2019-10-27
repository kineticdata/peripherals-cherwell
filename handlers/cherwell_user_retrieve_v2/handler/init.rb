# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class CherwellUserRetrieveV2
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
    # Validate Login ID grant_type
    if !(['Internal','Windows'].include?(@parameters['login_id_type'].capitalize))
      raise "Login ID Type '#{@parameters['login_id_type']}' does not match a valid option of 'Internal' or 'Windows'."
    end

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
    begin
      resp2 = resource["/api/V2/getuserbyloginid?loginid=#{CGI::escape(@parameters['login_id'])}&loginidtype=#{@parameters['login_id_type']}"].get
      object = JSON.parse(resp2.body)
    rescue RestClient::ExceptionWithResponse => e
      if e.message == "500 Internal Server Error" && JSON.parse(e.response)['Message'].start_with?("RECORDNOTFOUND")
        message = "User #{@parameters['login_id']} not found"
      else
        raise
      end
    end
puts @parameters['login_id']
    # Move the fields from a list to a map of {displayName: value}
    if object && !object.empty?
      fields = {}
      object["fields"].each { |f|
        if !fields[f["displayName"]].nil?
          fields[f["displayName"]] = f["value"]
        else
          fields[f["name"]] = f["value"]
        end
      }
      object["fields"] = fields
    else
      object={}
    end

    return <<-RESULTS
    <results>
      <result name="Message">#{escape(message || "")}</result>
      <result name="User JSON">#{escape(object.to_json)}</result>
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
