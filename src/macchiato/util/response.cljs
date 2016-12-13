(ns macchiato.util.response
  (:require
    [cuerdas.core :as string]
    [macchiato.fs :as fs]))

(defn find-header
  "Looks up a header in a response (or request) case insensitively,
  returning the header map entry, or nil if not present."
  [resp header-name]
  (->> (:headers resp)
       (filter #(= (string/lower header-name)
                   (-> % key string/lower)))
       (first)))

(defn get-header
  "Looks up a header in a response (or request) case insensitively,
  returning the value of the header, or nil if not present."
  [resp header-name]
  (some-> resp (find-header header-name) val))

(defn update-header
  "Looks up a header in a response (or request) case insensitively,
  then updates the header with the supplied function and arguments in the
  manner of update-in."
  [resp header-name f & args]
  (let [header-key (or (some-> resp (find-header header-name) key) header-name)]
    (update-in resp [:headers header-key] #(apply f % args))))

(defn header
  "Returns an updated response with the specified header added."
  [resp name value]
  (assoc-in resp [:headers name] (str value)))

(defn content-type
  "Returns an updated response with the a Content-Type header corresponding
  to the given content-type."
  [resp content-type]
  (header resp "Content-Type" content-type))

(defn charset
  "Returns an updated response with the supplied charset added to the
  Content-Type header."
  [resp charset]
  (update-header resp "Content-Type"
                 (fn [content-type]
                   (-> (or content-type "text/plain")
                       (string/replace #";\s*charset=[^;]*" "")
                       (str "; charset=" charset)))))

(defn status
  "sets the status code of the response"
  [resp status]
  (assoc resp :status status))

(defn file
  "accepts a path to a file, and returns a response with the body set to the file stream."
  [path]
  {:status  200
   :headers {}
   :body    (fs/stream path)})

(defn response?
  "True if the supplied value is a valid response map."
  [resp]
  (and (map? resp)
       (integer? (:status resp))
       (map? (:headers resp))))


(defn continue
  "100 Continue (Informational)
  The server has received the request headers and the client should proceed to send the request body."
  ([]
   {:status  100
    :headers {}
    :body    ""}))

(defn switching-protocols
  "101 Switching Protocols (Informational)
  The server is switching protocols because the client requested the switch."
  ([]
   {:status  101
    :headers {}
    :body    ""}))

(defn processing
  "102 Processing (Informational)
  The server is processing the request but no response is available yet."
  ([]
   {:status  102
    :headers {}
    :body    ""}))

(defn ok
  "200 OK (Success)
  OK"
  ([] (ok nil))
  ([body]
   {:status  200
    :headers {}
    :body    body}))

(defn created
  "201 Created (Success)
  The request has been fulfilled and resulted in a new resource being created."
  ([] (created nil))
  ([url] (created url nil))
  ([url body]
   {:status  201
    :headers {"Location" url}
    :body    body}))

(defn accepted
  "202 Accepted (Success)
  The request has been accepted for processing but the processing has not been completed."
  ([] (accepted nil))
  ([body]
   {:status  202
    :headers {}
    :body    body}))

(defn non-authoritative-information
  "203 Non-Authoritative Information (Success)
  The server successfully processed the request but is returning information that may be from another source."
  ([] (non-authoritative-information nil))
  ([body]
   {:status  203
    :headers {}
    :body    body}))

(defn no-content
  "204 No Content (Success)
  The server successfully processed the request, but is not returning any content. Usually used as a response to a successful delete request."
  ([]
   {:status  204
    :headers {}
    :body    ""}))

(defn reset-content
  "205 Reset Content (Success)
  The server successfully processed the request but is not returning any content. Unlike a 204 response, this response requires that the requester reset the document view."
  ([]
   {:status  205
    :headers {}
    :body    ""}))

(defn partial-content
  "206 Partial Content (Success)
  The server is delivering only part of the resource due to a range header sent by the client."
  ([] (partial-content nil))
  ([body]
   {:status  206
    :headers {}
    :body    body}))

(defn multi-status
  "207 Multi-Status (Success)
  The message body that follows is an XML message and can contain a number of separate response codes depending on how many sub-requests were made."
  ([] (multi-status nil))
  ([body]
   {:status  207
    :headers {}
    :body    body}))

(defn already-reported
  "208 Already Reported (Success)
  The members of a DAV binding have already been enumerated in a previous reply to this request and are not being included again."
  ([] (already-reported nil))
  ([body]
   {:status  208
    :headers {}
    :body    body}))

(defn im-used
  "226 IM Used (Success)
  The server has fulfilled a GET request for the resource and the response is a representation of the result of one or more instance-manipulations applied to the current instance."
  ([] (im-used nil))
  ([body]
   {:status  226
    :headers {}
    :body    body}))

(defn multiple-choices
  "300 Multiple Choices (Redirection)
  There are multiple options for the resource that the client may follow."
  ([url]
   {:status  300
    :headers {"Location" url}
    :body    ""}))

(defn moved-permanently
  "301 Moved Permanently (Redirection)
  This and all future requests should be directed to the given URI."
  ([url]
   {:status  301
    :headers {"Location" url}
    :body    ""}))

(defn found
  "302 Found (Redirection)
  The resource was found but at a different URI."
  ([url]
   {:status  302
    :headers {"Location" url}
    :body    ""}))

(defn see-other
  "303 See Other (Redirection)
  The response to the request can be found under another URI using a GET method."
  ([url]
   {:status  303
    :headers {"Location" url}
    :body    ""}))

(defn not-modified
  "304 Not Modified (Redirection)
  The resource has not been modified since last requested."
  ([]
   {:status  304
    :headers {}
    :body    ""}))

(defn use-proxy
  "305 Use Proxy (Redirection)
  This single request is to be repeated via the proxy given by the Location field."
  ([url]
   {:status  305
    :headers {"Location" url}
    :body    ""}))

(defn temporary-redirect
  "307 Temporary Redirect (Redirection)
  The request should be repeated with another URI but future requests can still use the original URI."
  ([url]
   {:status  307
    :headers {"Location" url}
    :body    ""}))

(defn permanent-redirect
  "308 Permanent Redirect (Redirection)
  The request and all future requests should be repeated using another URI."
  ([url]
   {:status  308
    :headers {"Location" url}
    :body    ""}))

(defn bad-request
  "400 Bad Request (ClientError)
  The request contains bad syntax or cannot be fulfilled."
  ([] (bad-request nil))
  ([body]
   {:status  400
    :headers {}
    :body    body}))

(defn unauthorized
  "401 Unauthorized (ClientError)
  Authentication is possible but has failed or not yet been provided."
  ([] (unauthorized nil))
  ([body]
   {:status  401
    :headers {}
    :body    body}))

(defn payment-required
  "402 Payment Required (ClientError)
  Reserved for future use."
  ([] (payment-required nil))
  ([body]
   {:status  402
    :headers {}
    :body    body}))

(defn forbidden
  "403 Forbidden (ClientError)
  The request was a legal request but the server is refusing to respond to it."
  ([] (forbidden nil))
  ([body]
   {:status  403
    :headers {}
    :body    body}))

(defn not-found
  "404 Not Found (ClientError)
  The requested resource could not be found but may be available again in the future."
  ([] (not-found nil))
  ([body]
   {:status  404
    :headers {}
    :body    body}))

(defn method-not-allowed
  "405 Method Not Allowed (ClientError)
  A request was made of a resource using a request method not supported by that resource;"
  ([] (method-not-allowed nil))
  ([body]
   {:status  405
    :headers {}
    :body    body}))

(defn not-acceptable
  "406 Not Acceptable (ClientError)
  The requested resource is only capable of generating content not acceptable according to the Accept headers sent in the request."
  ([] (not-acceptable nil))
  ([body]
   {:status  406
    :headers {}
    :body    body}))

(defn proxy-authentication-required
  "407 Proxy Authentication Required (ClientError)
  Proxy authentication is required to access the requested resource."
  ([] (proxy-authentication-required nil))
  ([body]
   {:status  407
    :headers {}
    :body    body}))

(defn request-timeout
  "408 Request Timeout (ClientError)
  The server timed out waiting for the request."
  ([] (request-timeout nil))
  ([body]
   {:status  408
    :headers {}
    :body    body}))

(defn conflict
  "409 Conflict (ClientError)
  The request could not be processed because of conflict in the request such as an edit conflict."
  ([] (conflict nil))
  ([body]
   {:status  409
    :headers {}
    :body    body}))

(defn gone
  "410 Gone (ClientError)
  The resource requested is no longer available and will not be available again."
  ([] (gone nil))
  ([body]
   {:status  410
    :headers {}
    :body    body}))


(defn length-required
  "411 Length Required (ClientError)
  The request did not specify the length of its content which is required by the requested resource."
  ([] (length-required nil))
  ([body]
   {:status  411
    :headers {}
    :body    body}))

(defn precondition-failed
  "412 Precondition Failed (ClientError)
  The server does not meet one of the preconditions that the requester put on the request."
  ([] (precondition-failed nil))
  ([body]
   {:status  412
    :headers {}
    :body    body}))

(defn request-entity-too-large
  "413 Request Entity Too Large (ClientError)
  The request is larger than the server is willing or able to process."
  ([] (request-entity-too-large nil))
  ([body]
   {:status  413
    :headers {}
    :body    body}))

(defn request-uri-too-long
  "414 Request-URI Too Long (ClientError)
  The URI provided was too long for the server to process."
  ([] (request-uri-too-long nil))
  ([body]
   {:status  414
    :headers {}
    :body    body}))

(defn unsupported-media-type
  "415 Unsupported Media Type (ClientError)
  The request entity has a media type which the server or resource does not support."
  ([] (unsupported-media-type nil))
  ([body]
   {:status  415
    :headers {}
    :body    body}))

(defn requested-range-not-satisfiable
  "416 Requested Range Not Satisfiable (ClientError)
  The client has asked for a portion of the file but the server cannot supply that portion."
  ([] (requested-range-not-satisfiable nil))
  ([body]
   {:status  416
    :headers {}
    :body    body}))

(defn expectation-failed
  "417 Expectation Failed (ClientError)
  The server cannot meet the requirements of the Expect request-header field."
  ([] (expectation-failed nil))
  ([body]
   {:status  417
    :headers {}
    :body    body}))

(defn enhance-your-calm
  "420 Enhance Your Calm (ClientError)
  You are being rate-limited."
  ([] (enhance-your-calm nil))
  ([body]
   {:status  420
    :headers {}
    :body    body}))

(defn unprocessable-entity
  "422 Unprocessable Entity (ClientError)
  The request was well-formed but was unable to be followed due to semantic errors."
  ([] (unprocessable-entity nil))
  ([body]
   {:status  422
    :headers {}
    :body    body}))

(defn locked
  "423 Locked (ClientError)
  The resource that is being accessed is locked."
  ([] (locked nil))
  ([body]
   {:status  423
    :headers {}
    :body    body}))

(defn failed-dependency
  "424 Failed Dependency (ClientError)
  The request failed due to failure of a previous request."
  ([] (failed-dependency nil))
  ([body]
   {:status  424
    :headers {}
    :body    body}))

(defn unordered-collection
  "425 Unordered Collection (ClientError)
  The collection is unordered."
  ([] (unordered-collection nil))
  ([body]
   {:status  425
    :headers {}
    :body    body}))

(defn upgrade-required
  "426 Upgrade Required (ClientError)
  The client should switch to a different protocol."
  ([] (upgrade-required nil))
  ([body]
   {:status  426
    :headers {}
    :body    body}))

(defn precondition-required
  "428 Precondition Required (ClientError)
  The server requires the request to be conditional."
  ([] (precondition-required nil))
  ([body]
   {:status  428
    :headers {}
    :body    body}))

(defn too-many-requests
  "429 Too Many Requests (ClientError)
  The user has sent too many requests in a given amount of time."
  ([] (too-many-requests nil))
  ([body]
   {:status  429
    :headers {}
    :body    body}))

(defn request-header-fields-too-large
  "431 Request Header Fields Too Large (ClientError)
  The server is unwilling to process the request because either an individual header field or all the header fields collectively are too large."
  ([] (request-header-fields-too-large nil))
  ([body]
   {:status  431
    :headers {}
    :body    body}))

(defn retry-with
  "449 Retry With (ClientError)
  The request should be retried after doing the appropriate action."
  ([] (retry-with nil))
  ([body]
   {:status  449
    :headers {}
    :body    body}))

(defn blocked-by-windows-parental-controls
  "450 Blocked by Windows Parental Controls (ClientError)
  Windows Parental Controls are turned on and are blocking access to the given webpage."
  ([] (blocked-by-windows-parental-controls nil))
  ([body]
   {:status  450
    :headers {}
    :body    body}))

(defn unavailable-for-legal-reasons
  "451 Unavailable For Legal Reasons (ClientError)
  Resource access is denied for legal reasons."
  ([] (unavailable-for-legal-reasons nil))
  ([body]
   {:status  451
    :headers {}
    :body    body}))

(defn internal-server-error
  "500 Internal Server Error (ServerError)
  There was an internal server error."
  ([] (internal-server-error nil))
  ([body]
   {:status  500
    :headers {}
    :body    body}))

(defn not-implemented
  "501 Not Implemented (ServerError)
  The server either does not recognize the request method or it lacks the ability to fulfill the request."
  ([] (not-implemented nil))
  ([body]
   {:status  501
    :headers {}
    :body    body}))

(defn bad-gateway
  "502 Bad Gateway (ServerError)
  The server was acting as a gateway or proxy and received an invalid response from the upstream server."
  ([] (bad-gateway nil))
  ([body]
   {:status  502
    :headers {}
    :body    body}))

(defn service-unavailable
  "503 Service Unavailable (ServerError)
  The server is currently unavailable (because it is overloaded or down for maintenance)."
  ([] (service-unavailable nil))
  ([body]
   {:status  503
    :headers {}
    :body    body}))

(defn gateway-timeout
  "504 Gateway Timeout (ServerError)
  The server was acting as a gateway or proxy and did not receive a timely request from the upstream server."
  ([] (gateway-timeout nil))
  ([body]
   {:status  504
    :headers {}
    :body    body}))

(defn http-version-not-supported
  "505 HTTP Version Not Supported (ServerError)
  The server does not support the HTTP protocol version used in the request."
  ([] (http-version-not-supported nil))
  ([body]
   {:status  505
    :headers {}
    :body    body}))

(defn variant-also-negotiates
  "506 Variant Also Negotiates (ServerError)
  Transparent content negotiation for the request results in a circular reference."
  ([] (variant-also-negotiates nil))
  ([body]
   {:status  506
    :headers {}
    :body    body}))

(defn insufficient-storage
  "507 Insufficient Storage (ServerError)
  Insufficient storage to complete the request."
  ([] (insufficient-storage nil))
  ([body]
   {:status  507
    :headers {}
    :body    body}))

(defn loop-detected
  "508 Loop Detected (ServerError)
  The server detected an infinite loop while processing the request."
  ([] (loop-detected nil))
  ([body]
   {:status  508
    :headers {}
    :body    body}))

(defn bandwidth-limit-exceeded
  "509 Bandwidth Limit Exceeded (ServerError)
  Bandwidth limit has been exceeded."
  ([] (bandwidth-limit-exceeded nil))
  ([body]
   {:status  509
    :headers {}
    :body    body}))

(defn not-extended
  "510 Not Extended (ServerError)
  Further extensions to the request are required for the server to fulfill it."
  ([] (not-extended nil))
  ([body]
   {:status  510
    :headers {}
    :body    body}))

(defn network-authentication-required
  "511 Network Authentication Required (ServerError)
  The client needs to authenticate to gain network access."
  ([] (network-authentication-required nil))
  ([body]
   {:status  511
    :headers {}
    :body    body}))

(defn network-read-timeout
  "598 Network read timeout (ServerError)
  "
  ([] (network-read-timeout nil))
  ([body]
   {:status  598
    :headers {}
    :body    body}))

(defn network-connect-timeout
  "599 Network connect timeout (ServerError)
  "
  ([] (network-connect-timeout nil))
  ([body]
   {:status  599
    :headers {}
    :body    body}))

