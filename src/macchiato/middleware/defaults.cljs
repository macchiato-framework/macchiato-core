(ns macchiato.middleware.defaults
  (:require
    [macchiato.middleware :as middleware]
    [macchiato.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [macchiato.middleware.content-type :refer [wrap-content-type]]
    [macchiato.middleware.default-charset :refer [wrap-default-charset]]
    [macchiato.middleware.file :refer [wrap-file]]
    [macchiato.middleware.flash :refer [wrap-flash]]
    [macchiato.middleware.keyword-params :refer [wrap-keyword-params]]
    [macchiato.middleware.multipart-params :refer [wrap-multipart]]
    [macchiato.middleware.nested-params :refer [wrap-nested-params]]
    [macchiato.middleware.not-modified :refer [wrap-not-modified]]
    [macchiato.middleware.params :refer [wrap-params]]
    [macchiato.middleware.proxy-headers :refer [wrap-forwarded-remote-addr]]
    [macchiato.middleware.resource :refer [wrap-resource]]
    [macchiato.middleware.session :refer [wrap-session]]
    [macchiato.middleware.ssl :refer [wrap-ssl-redirect wrap-hsts wrap-forwarded-scheme]]
    [macchiato.middleware.x-headers :as x]))

(def api-defaults
  "A default configuration for a HTTP API."
  {:params    {:urlencoded true
               :keywordize true}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true
               :default-charset        "utf-8"}})

(def secure-api-defaults
  "A default configuration for a HTTP API that's accessed securely over HTTPS."
  (-> api-defaults
      (assoc-in [:security :ssl-redirect] true)
      (assoc-in [:security :hsts] true)))

(def site-defaults
  "A default configuration for a browser-accessible website, based on current
  best practice."
  {:params    {:urlencoded true
               :multipart  true
               :nested     true
               :keywordize true}
   :session   {:flash        true
               :cookie-attrs {:http-only true}}
   :security  {:anti-forgery         true
               :xss-protection       {:enable? true, :mode :block}
               :frame-options        :sameorigin
               :content-type-options :nosniff}
   :static    {:resources "public"}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true
               :default-charset        "utf-8"}})

(def secure-site-defaults
  "A default configuration for a browser-accessible website that's accessed
  securely over HTTPS."
  (-> site-defaults
      (assoc-in [:session :cookie-attrs :secure] true)
      (assoc-in [:session :cookie-name] "secure-ring-session")
      (assoc-in [:security :ssl-redirect] true)
      (assoc-in [:security :hsts] true)))

(defn- wrap [handler middleware-fn options]
  (cond
    (true? options)
    (middleware/wrap handler middleware-fn)

    options
    (middleware/wrap handler middleware-fn options)

    :else
    handler))

(defn wrap-defaults
  "Wraps a handler in default Ring middleware, as specified by the supplied
  configuration map.
  See: api-defaults
       site-defaults
       secure-api-defaults
       secure-site-defaults"
  [handler config]
  (-> handler
      (wrap #'wrap-anti-forgery           (get-in config [:security :anti-forgery] false))
      (wrap #'wrap-flash                  (get-in config [:session :flash] false))
      (wrap #'wrap-session                (:session config false))
      (wrap #'wrap-keyword-params         (get-in config [:params :keywordize] false))
      (wrap #'wrap-nested-params          (get-in config [:params :nested] false))
      (wrap #'wrap-multipart              (get-in config [:params :multipart] false))
      (wrap #'wrap-params                 (get-in config [:params :urlencoded] false))
      (wrap #'wrap-resource               (get-in config [:static :resources] false))
      (wrap #'wrap-file                   (get-in config [:static :files] false))
      (wrap #'wrap-content-type           (get-in config [:responses :content-types] false))
      (wrap #'wrap-default-charset        (get-in config [:responses :default-charset] false))
      (wrap #'wrap-not-modified           (get-in config [:responses :not-modified-responses] false))
      (wrap #'x/wrap-xss-protection       (get-in config [:security :xss-protection] false))
      (wrap #'x/wrap-frame-options        (get-in config [:security :frame-options] false))
      (wrap #'x/wrap-content-type-options (get-in config [:security :content-type-options] false))
      (wrap #'wrap-hsts                   (get-in config [:security :hsts] false))
      (wrap #'wrap-ssl-redirect           (get-in config [:security :ssl-redirect] false))
      (wrap #'wrap-forwarded-scheme       (boolean (:proxy config)))
      (wrap #'wrap-forwarded-remote-addr  (boolean (:proxy config)))
      middleware/validate-handler))
