(ns macchiato.middleware.ssl
  "Middleware for managing handlers operating over HTTPS."
  (:require [macchiato.util.response :as resp]
            [macchiato.util.request :as req]
            [clojure.string :as str]))

(def url (js/require "url"))

(def default-scheme-header
  "The default header used in wrap-forwarded-scheme (x-forwarded-proto)."
  "x-forwarded-proto")

(defn forwarded-scheme-request
  "Change the :scheme of the request to the value present in a request header.
  See: wrap-forwarded-scheme."
  ([request]
   (forwarded-scheme-request request default-scheme-header))
  ([request header]
   (let [header  (str/lower-case header)
         default (name (:scheme request))
         scheme  (str/lower-case (get-in request [:headers header] default))]
     (if (#{"http" "https"} scheme)
       (assoc request :scheme (keyword scheme))
       request))))

(defn wrap-forwarded-scheme
  "Middleware that changes the :scheme of the request map to the value present
  in a request header. This is useful if your application sits behind a
  reverse proxy or load balancer that handles the SSL transport.

  The header defaults to x-forwarded-proto."
  ([handler]
   (wrap-forwarded-scheme handler default-scheme-header))
  ([handler header]
   (fn [request respond raise]
     (handler (forwarded-scheme-request request header) respond raise))))

(defn- get-request? [{method :request-method}]
  (or (= method :head)
      (= method :get)))

(defn- valid-url? [url-string]
  (re-matches #"^http[s]?\://.*" url-string))

(defn- https-url [url-string port]
  (when-not (valid-url? url-string)
    (throw (js/Error. (str "invalid URL: " url-string))))
  (let [url (.parse url url-string)]
    (str
      "https://"
      (when-let [auth (.-auth url)]
        (str auth "@"))
      (.-hostname url)
      (when port (str ":" port))
      (.-path url)
      (.-hash url))))

(defn ssl-redirect-response
  "Given a HTTP request, return a redirect response to the equivalent HTTPS URL.
  See: wrap-ssl-redirect."
  [request options]
  (-> (resp/found (https-url (req/request-url request) (:ssl-port options)))
      (resp/status (if (get-request? request) 301 307))))

(defn wrap-ssl-redirect
  "Middleware that redirects any HTTP request to the equivalent HTTPS URL.

  Accepts the following options:

  :ssl-port - the SSL port to use for redirects, defaults to 443."
  ([handler]
   (wrap-ssl-redirect handler {}))
  ([handler options]
   (fn [request respond raise]
     (if (= (:scheme request) :https)
       (handler request respond raise)
       (try
         (respond (ssl-redirect-response request options))
         (catch js/Error e
           (raise e)))))))

(defn- build-hsts-header
  [{:keys [max-age include-subdomains?]
    :or   {max-age 31536000, include-subdomains? true}}]
  (str "max-age=" max-age
       (if include-subdomains? "; includeSubDomains")))

(defn hsts-response
  "Add the Strict-Transport-Security header to the response. See: wrap-hsts."
  ([response]
   (hsts-response response {}))
  ([response options]
   (some-> response (resp/header "Strict-Transport-Security" (build-hsts-header options)))))

(defn wrap-hsts
  "Middleware that adds the Strict-Transport-Security header to the response
  from the handler. This ensures the browser will only use HTTPS for future
  requests to the domain.

  Accepts the following options:

  :max-age             - the max time in seconds the HSTS policy applies
                         (defaults to 31536000 seconds, or 1 year)

  :include-subdomains? - true if subdomains should be included in the HSTS
                         policy (defaults to true)

  See RFC 6797 for more information (https://tools.ietf.org/html/rfc6797)."
  ([handler]
   (wrap-hsts handler {}))
  ([handler options]
   (fn [request respond raise]
     (handler request #(respond (hsts-response % options)) raise))))
