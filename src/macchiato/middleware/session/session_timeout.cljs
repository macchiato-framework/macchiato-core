(ns macchiato.middleware.session.session-timeout)

(defn- current-time []
  (quot (.getTime (js/Date.)) 1000))

(defn wrap-idle-session-timeout
  "Middleware that times out idle sessions after a specified number of seconds.
  If a session is timed out, the timeout-response option is returned. This is
  usually a redirect to the login page. Alternatively, the timeout-handler
  option may be specified. This should contain a Ring handler function that
  takes the current request and returns a timeout response.
  The following options are accepted:
  :timeout          - the idle timeout in seconds (default 600 seconds)
  :timeout-response - the response to send if an idle timeout occurs
  :timeout-handler  - the handler to run if an idle timeout occurs"
  {:arglists '([handler options])}
  [handler {:keys [timeout timeout-response timeout-handler] :or {timeout 600}}]
  {:pre [(integer? timeout)
         (if (map? timeout-response)
           (nil? timeout-handler)
           (ifn? timeout-handler))]}
  (fn [request]
    (let [session  (:session request {})
          end-time (::idle-timeout session)]
      (if (and end-time (< end-time (current-time)))
        (assoc (or timeout-response (timeout-handler request)) :session nil)
        (when-let [response (handler request)]
          (let [session (:session response session)]
            (if (nil? session)
              response
              (let [end-time (+ (current-time) timeout)]
                (assoc response :session (assoc session ::idle-timeout end-time))))))))))

(defn wrap-absolute-session-timeout
  "Middleware that times out sessions after a specified number of seconds,
  regardless of whether the session is being used or idle. This places an upper
  limit on how long a compromised session can be exploited.
  If a session is timed out, the timeout-response option is returned. This is
  usually a redirect to the login page. Alternatively, the timeout-handler
  option may be specified. This should contain a Ring handler function that
  takes the current request and returns a timeout response.
  The following options are accepted:
  :timeout          - the absolute timeout in seconds
  :timeout-response - the response to send if an absolute timeout occurs
  :timeout-handler  - the handler to run if an absolute timeout occurs"
  {:arglists '([handler options])}
  [handler {:keys [timeout timeout-response timeout-handler]}]
  {:pre [(integer? timeout)
         (if (map? timeout-response)
           (nil? timeout-handler)
           (ifn? timeout-handler))]}
  (fn [request respond raise]
    (let [session  (:session request {})
          end-time (::absolute-timeout session)]
      (if (and end-time (< end-time (current-time)))
        (respond (assoc (or timeout-response (timeout-handler request)) :session nil))
        (handler
          request
          (fn [response]
            (when response
              (respond
                (let [session (:session response session)]
                  (if (or (nil? session)
                          (and end-time
                               (not (contains? response :session))))
                    response
                    (let [end-time (or end-time (+ (current-time) timeout))
                          session  (assoc session ::absolute-timeout end-time)]
                      (assoc response :session session)))))))
          raise)))))
