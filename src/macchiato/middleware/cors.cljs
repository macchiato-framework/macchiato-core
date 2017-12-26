(ns
  ^{:doc    "Ring middleware for Cross-Origin Resource Sharing."
    :author "Mihael Konjević"}
  macchiato.middleware.cors
  (:require [clojure.set :as set]
            [cuerdas.core :as str]
            [macchiato.util.response :as r :refer [get-header]]))

(defn origin
  "Returns the Origin request header."
  [request] (get-header request "origin"))

(defn preflight?
  "Returns true if the request is a preflight request"
  [request]
  (= (request :request-method) :options))

(defn lower-case-set
  "Converts strings in a sequence to lower-case, and put them into a set"
  [s]
  (->> s
       (map str/trim)
       (map str/lower)
       (set)))

(defn parse-headers
  "Transforms a comma-separated string to a set"
  [s]
  (->> (str/split (str s) #",")
       (remove str/blank?)
       (lower-case-set)))

(defn allow-preflight-headers?
  "Returns true if the request is a preflight request and all the headers that
  it's going to use are allowed. Returns false otherwise."
  [request allowed-headers]
  (if (nil? allowed-headers)
    true
    (set/subset?
      (parse-headers (get-header request "access-control-request-headers"))
      (lower-case-set (map name allowed-headers)))))

(defn allow-method?
  "In the case of regular requests it checks if the request-method is allowed.
  In the case of preflight requests it checks if the
  access-control-request-method is allowed."
  [request allowed-methods]
  (let [preflight-name [:headers "access-control-request-method"]
        request-method (if (preflight? request)
                         (keyword (str/lower
                                    (get-in request preflight-name "")))
                         (:request-method request))]
    (contains? allowed-methods request-method)))

(defn allow-request?
  "Returns true if the request's origin matches the access control
  origin, otherwise false."
  [request access-control]
  (let [origin          (origin request)
        allowed-origins (:access-control-allow-origin access-control)
        allowed-headers (:access-control-allow-headers access-control)
        allowed-methods (:access-control-allow-methods access-control)]
    (if (and origin
             (seq allowed-origins)
             (seq allowed-methods)
             (some #(re-matches % origin) allowed-origins)
             (if (preflight? request)
               (allow-preflight-headers? request allowed-headers)
               true)
             (allow-method? request allowed-methods))
      true false)))

(defn header-name
  "Returns the capitalized header name as a string."
  [header]
  (if header
    (->> (str/split (name header) #"-")
         (map str/capitalize)
         (str/join "-"))))

(defn normalize-headers
  "Normalize the headers by converting them to capitalized strings."
  [headers]
  (let [upcase          #(str/join ", " (sort (map (comp str/upper name) %)))
        to-header-names #(str/join ", " (sort (map (comp header-name name) %)))]
    (reduce
      (fn [acc [k v]]
        (assoc acc (header-name k)
                   (case k
                     :access-control-allow-methods (upcase v)
                     :access-control-allow-headers (to-header-names v)
                     v)))
      {} headers)))

(defn add-headers
  "Add the access control headers using the request's origin to the response."
  [request access-control response]
  (if-let [origin (origin request)]
    (update-in response [:headers] merge
               (assoc access-control :access-control-allow-origin origin))
    response))

(defn add-allowed-headers
  "Adds the allowed headers to the request"
  [request allowed-headers response]
  (if (preflight? request)
    (let [request-headers (get-header request "access-control-request-headers")
          allowed-headers (if (nil? allowed-headers)
                            (parse-headers request-headers)
                            allowed-headers)]
      (if allowed-headers
        (update-in response [:headers] merge
                   {:access-control-allow-headers allowed-headers})
        response))
    response))


(defn add-access-control
  "Add the access-control headers to the response based on the rules
  and what came on the header."
  [request access-control response]
  (let [allowed-headers   (:access-control-allow-headers access-control)
        rest-of-headers   (dissoc access-control
                                  :access-control-allow-headers)
        unnormalized-resp (->> response
                               (add-headers request rest-of-headers)
                               (add-allowed-headers request allowed-headers))]
    (update-in unnormalized-resp [:headers] normalize-headers)))

(defn normalize-config
  [access-control]
  (-> access-control
      (update-in [:access-control-allow-methods] set)
      (update-in [:access-control-allow-headers] #(if (coll? %) (set %) %))
      (update-in [:access-control-allow-origin] #(if (sequential? %) % [%]))))

(defn wrap-cors
  "Middleware that adds Cross-Origin Resource Sharing headers.
  (def handler
    (-> routes
        (wrap-cors
         {:access-control-allow-origin #\"http://example.com\"
          :access-control-allow-methods [:get :put :post :delete]))})
  "
  ([handler]
    (wrap-cors handler {}))
  ([handler {:keys [message] :as opts}]
   (let [access-control (normalize-config opts)]
     (fn [request respond raise]
       (if (and (preflight? request) (allow-request? request access-control))
         (let [blank-response {:status  200
                               :headers {}
                               :body    (or message "preflight complete")}]

           (respond (add-access-control request access-control blank-response)))
         (if (origin request)
           (if (allow-request? request access-control)
             (handler request #(respond (add-access-control request access-control %)) raise)
             (handler request respond raise))
           (handler request respond raise)))))))
