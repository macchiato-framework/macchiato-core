(ns macchiato.middleware.restful-format
  (:require
    [cuerdas.core :as s]
    [cljs.nodejs :as node]
    [cognitect.transit :as t]
    [cljs.reader :as edn]
    [macchiato.util.response :as r]))

(def concat-stream (node/require "concat-stream"))

(def ct (node/require "content-type"))

(def lru (let [LRUCache (node/require "lru")]
           (LRUCache. #js {:maxElementsToStore 500})))

(def ^:private accept-fragment-re
  #"^\s*(\*|[^()<>@,;:\"/\[\]?={}         ]+)/(\*|[^()<>@,;:\"/\[\]?={}         ]+)$")

(def ^:private accept-fragment-param-re
  #"([^()<>@,;:\"/\[\]?={} 	]+)=([^()<>@,;:\"/\[\]?={} 	]+|\"[^\"]*\")$")

(defn- parse-q [s]
  (try
    (->> (js/parseFloat s)
         (min 1)
         (max 0))
    (catch js/Error _
      nil)))

(defn ^:no-doc sort-by-check
  [by check headers]
  (sort-by by (fn [a b]
                (cond (= (= a check) (= b check)) 0
                      (= a check) 1
                      :else -1))
           headers))

(defn parse-accept-header*
  "Parse Accept headers into a sorted sequence of strings.
  \"application/json;level=1;q=0.4\"
  => (\"application/json\")"
  [accept-header]
  (if accept-header
    (->> (map (fn [fragment]
                (let [[media-range & params-list] (s/split fragment #"\s*;\s*")
                      [type sub-type] (rest (re-matches accept-fragment-re media-range))]
                  (-> (reduce (fn [m s]
                                (if-let [[k v] (seq (rest (re-matches accept-fragment-param-re s)))]
                                  (if (= "q" k)
                                    (update-in m [:q] #(or % (parse-q v)))
                                    (assoc m (keyword k) v))
                                  m))
                              {:type      type
                               :sub-type  sub-type
                               :full-type (str type "/" sub-type)}
                              params-list)
                      (update-in [:q] #(or % 1.0)))))
              (s/split accept-header #"[\s\n\r]*,[\s\n\r]*"))
         (sort-by-check :type "*")
         (sort-by-check :sub-type "*")
         (sort-by :q >)
         (map :full-type))))

(defn parse-accept-header [accept-header]
  (or
    (.get lru accept-header)
    (let [parsed-header (parse-accept-header* accept-header)]
      (.set lru accept-header parsed-header)
      parsed-header)))

(def default-content-types
  #{"application/json"
    "application/transit+json"})

(def default-accept-types
  #{"application/json"
    "application/transit+json"})

;; request content type multimethods for decoding the request body
(defmulti deserialize-request :type)

(defmethod deserialize-request "application/json"
  [{:keys [body keywordize?]}]
  (js->clj (js/JSON.parse body) :keywordize-keys keywordize?))

(defmethod deserialize-request "application/transit+json"
  [{:keys [body]}]
  (t/read (t/reader :json) body))

;; response accept multimethods for serializing the response
(defmulti serialize-response :type)

(defmethod serialize-response "application/json"
  [{:keys [body]}]
  (js/JSON.stringify (clj->js body)))

(defmethod serialize-response "application/transit+json"
  [{:keys [body]}]
  (t/write (t/writer :json) body))

(defn infer-request-content-type [headers content-types]
  (when-let [content-type (some->> (get headers "content-type") (.parse ct))]
    (when-let [type (get content-types (.-type content-type))]
      {:type type})))

(defn infer-response-content-type [{:keys [body] :as request} accept-types]
  (when body
    (if-let [parsed-accept-types (parse-accept-header
                                   (get-in request [:headers "accept"]
                                           (get-in request [:headers "Accept"])))]
      (first (drop-while #(not-any? #{%} (set accept-types)) parsed-accept-types)))))

(defn format-response-body [request response accept-types raise]
  (try
    (if-let [accept-type (infer-response-content-type request accept-types)]
      (-> response
          (update :body #(serialize-response {:type accept-type :body %}))
          (r/content-type accept-type))
      response)
    (catch js/Error e
      (raise e))))

(defn- parse-request-body [request content body keywordize? raise]
  (try
    (assoc request
      :body (deserialize-request
              (assoc content :body body :keywordize? keywordize?)))
    (catch js/Error e
      (raise e))))

(defn
  ^{:macchiato/middleware
    {:id :wrap-restful-format}}
  wrap-restful-format
  "attempts to infer the request content type using the content-type header
   serializes the response based on the first known accept header

   the handler can be followed by a map of options

   option keys:
   :content-types a set of strings matching the content types
   :accept-types a set of strings mactching accept types, ressolves in client preferred order
   :keywordize? a boolean specifying whether to keywordize the parsed JSON request body"
  [handler & [{:keys [content-types accept-types keywordize?]}]]
  (let [content-types (or content-types default-content-types)
        accept-types  (clj->js (or accept-types default-accept-types))]
    (fn [{:keys [headers body] :as request} respond raise]
      (let [respond (fn [response]
                      (respond (format-response-body request response accept-types raise)))
            content (infer-request-content-type headers content-types)]
        (if (and body content)
          (if (string? body)
            (handler
              (parse-request-body request content body keywordize? raise)
              respond
              raise)
            (.pipe body
                   (concat-stream.
                     (fn [body]
                       (handler
                         (parse-request-body request content body keywordize? raise)
                         respond
                         raise)))))
          (handler request respond raise))))))
