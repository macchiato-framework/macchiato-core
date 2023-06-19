(ns macchiato.test.mock.request
  (:require
    [clojure.string :as s]
    ["url" :as url]
    ["querystring" :as querystring]))

(defn- encode-params
  "Turn a map of parameters into a urlencoded string."
  [params]
  (if params
    (.stringify querystring params)))

(defn header
  "Add a HTTP header to the request map."
  [request header value]
  (let [header (s/lower-case (name header))]
    (assoc-in request [:headers header] (str value))))

(defn content-type
  "Set the content type of the request map."
  [request mime-type]
  (-> request
      (assoc :content-type mime-type)
      (header :content-type mime-type)))

(defn content-length
  "Set the content length of the request map."
  [request length]
  (-> request
      (assoc :content-length length)
      (header :content-length length)))

(defn- combined-query
  "Create a query string from a URI and a map of parameters."
  [request params]
  (let [query (:query-string request)]
    (if (or query params)
      (s/join "&"
              (remove s/blank?
                      [query (encode-params params)])))))

(defn- merge-query
  "Merge the supplied parameters into the query string of the request."
  [request params]
  (assoc request :query-string (combined-query request params)))

(defn query-string
  "Set the query string of the request to a string or a map of parameters."
  [request params]
  (if (map? params)
    (assoc request :query-string (encode-params params))
    (assoc request :query-string params)))

(defmulti body
          "Set the body of the request. The supplied body value can be a string or
          a map of parameters to be url-encoded."
          {:arglists '([request body-value])}
          (fn [request x] (type x)))

(defmethod body js/String [request s]
  (assoc request :body s))

(defmethod body PersistentHashMap [request params]
  (-> request
      (content-type "application/x-www-form-urlencoded")
      (body (encode-params params))))

(defmethod body nil [request params]
  request)

(def default-port
  "A map of the default ports for a scheme."
  {:http  80
   :https 443})

(defn request
  "Create a minimal valid request map from a HTTP method keyword, a string
  containing a URI, and an optional map of parameters that will be added to
  the query string of the URI. The URI can be relative or absolute. Relative
  URIs are assumed to go to http://localhost."
  ([method uri]
   (request method uri nil))
  ([method uri-str params]
   (let [uri      (.parse url uri-str)
         protocol (.-protocol uri)
         scheme   (keyword (if protocol
                             (subs protocol 0 (dec (count protocol)))
                             "http"))
         host     (or (.-hostname uri) "localhost")
         port     (.-port uri)
         path     (.-pathname uri)
         query    (.-query uri)
         request  {:server-port    (or port (default-port scheme))
                   :server-name    host
                   :remote-addr    "localhost"
                   :uri            (if (s/blank? path) "/" path)
                   :query-string   query
                   :scheme         scheme
                   :request-method method
                   :headers        {"host" (if port (str host ":" port) host)}}]
     (if (#{:get :head :delete} method)
       (merge-query request params)
       (body request params)))))
