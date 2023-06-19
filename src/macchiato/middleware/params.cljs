(ns macchiato.middleware.params
  (:require
    [macchiato.util.request :as req]
    ["qs" :as qs]
    ["concat-stream" :as concat-stream]))

(defn decode [s]
  (js->clj (.parse qs s)))

(defn- parse-params [params]
  (let [params (decode params)]
    (if (map? params) params {})))

(defn assoc-query-params
  "Parse and assoc parameters from the query string with the request."
  [request]
  (merge-with merge request
              (if-let [query-string (:query-string request)]
                (let [params (parse-params query-string)]
                  {:query-params params, :params params})
                {:query-params {}, :params {}})))

(defn assoc-form-params
  "Parse and assoc parameters from the request body with the request."
  [request]
  (merge-with merge request
              (if-let [body (and (req/urlencoded-form? request) (:body request))]
                (let [params (parse-params body)]
                  {:form-params params, :params params})
                {:form-params {}, :params {}})))

(defn params-request
  "Adds parameters from the query string and the request body to the request
  map. See: wrap-params."
  ([request]
   (params-request request {}))
  ([request options]
   (let [request (if (:form-params request)
                   request
                   (assoc-form-params request))]
     (if (:query-params request)
       request
       (assoc-query-params request)))))

(defn
  ^{:macchiato/middleware
    {:id :wrap-params}}
  wrap-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is a url-encoded form). Adds the following keys to
  the request map:
  :query-params - a map of parameters from the query string
  :form-params  - a map of parameters from the body
  :params       - a merged map of all types of parameter
  Accepts the following options:
  :encoding - encoding to use for url-decoding. If not specified, uses
              the request character encoding, or \"UTF-8\" if no request
              character encoding is set."
  ([handler]
   (wrap-params handler {}))
  ([handler options]
   (fn [{:keys [body] :as request} respond raise]
     (if (req/urlencoded-form? request)
       (if (string? body)
         (handler (params-request request options) respond raise)
         (.pipe body
                (let [encoding (or (:encoding options) (req/character-encoding request) "utf8")]
                  (concat-stream.
                    (fn [body]
                      (handler
                        (params-request (assoc request :body (.toString body encoding)) options)
                        respond
                        raise))))))
       (handler (params-request request options) respond raise)))))

