# Require the dependencies file to load the vendor libraries
require File.expand_path(File.join(File.dirname(__FILE__), "dependencies"))

class CherwellObjectAttachmentUploadV1
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
    attachment_ids = []

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
          :accept => "application/json"
        }
      })

      ### Retrieve the Attachment(s) from the submission and upload them to the Cherwell Business Object
      # Submission API Route including Values
      submission_api_route = "#{@info_values['request_ce_api_location']}"+
        "/submissions/#{URI.escape(@parameters['attachment_submission_id'])}?include=values"

      # Retrieve the Submission Values
      submission_result = RestClient::Resource.new(
        submission_api_route,
        user: @info_values["request_ce_username"],
        password: @info_values["request_ce_password"]
      ).get

      # If the submission exists
      unless submission_result.nil?
        submission = JSON.parse(submission_result)["submission"]
        field_value = submission["values"][@parameters["attachment_field_name"]]
        # If the attachment field value exists
        unless field_value.nil?
          files = []
          # Attachment field values are stored as arrays, one map for each file attachment
          field_value.each_index do |index|
            file_info = field_value[index]
            attachment_download_api_route = @info_values["request_ce_api_location"] +
              '/submissions/' + URI.escape(@parameters['attachment_submission_id']) +
              '/files/' + URI.escape(@parameters['attachment_field_name']) +
              '/' + index.to_s +
              '/' + URI.escape(file_info['name']) +
              '/url'
            puts "Attachment Download API Route: #{attachment_download_api_route}" if @enable_debug_logging

            # Retrieve the URL to download the attachment from Kinetic Request CE.
            # This URL will only be valid for a short amount of time before it expires
            # (usually about 5 seconds).
            attachment_download_result = RestClient::Resource.new(
              attachment_download_api_route,
              user: @info_values["request_ce_username"],
              password: @info_values["request_ce_password"]
            ).get

            unless attachment_download_result.nil?
              url = JSON.parse(attachment_download_result)['url']
              puts "File download URL: #{url}" if @enable_debug_logging
              file_info["url"] = url

              # Download File from Filehub
              attach_response = RestClient::Resource.new(
                file_info["url"],
                user: @info_values["request_ce_username"],
                password: @info_values["request_ce_password"]
              ).get
              attach = attach_response.body

              # Upload the file to
              resp = resource["/api/V1/uploadbusinessobjectattachment/filename/#{URI.encode(file_info["name"])}"+
                "/busobname/#{@parameters['object_name']}/busobrecid/#{@parameters['object_id']}"+
                "/offset/0/totalsize/#{attach.size}"].post(attach)
              attachment_ids.push(resp.body)
            end
          end
        end
      end
    rescue RestClient::ResourceNotFound => error
      error_message = error.inspect
      raise "404 Not Found: Make sure all configuration parameters are correct: #{error.http_body}."
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
      <result name="Attachment Ids">#{escape(attachment_ids.join(","))}</result>
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
