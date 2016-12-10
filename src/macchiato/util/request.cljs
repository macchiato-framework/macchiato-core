(ns macchiato.util.request
  "Functions for augmenting and pulling information from request maps.")

(def ^{:doc "HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068"}
re-token #"[!#$%&'*\-+.0-9A-Z\^_`a-z\|~]+")


(def ^{:doc "HTTP quoted-string: <\"> *<any TEXT except \"> <\">. See RFC2068."}
re-quoted #"\"(\\\"|[^\"])*\"")

(def ^{:doc "HTTP value: token | quoted-string. See RFC2109"}
re-value (str re-token "|" re-quoted))

(defn request-url
  "Return the full URL of the request."
  [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])
       (:uri request)
       (if-let [query (:query-string request)]
         (str "?" query))))

(defn content-type
  "Return the content-type of the request, or nil if no content-type is set."
  [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (second (re-find #"^(.*?)(?:;|$)" type))))

(defn content-length
  "Return the content-length of the request, or nil no content-length is set."
  [request]
  (if-let [length (get-in request [:headers "content-length"])]
    (js/parseFloat length)))

;;TODO
#_(def ^:private charset-pattern
  (re-pattern (str ";(?:.*\\s)?(?i:charset)=(" re-value ")\\s*(?:;|$)")))

(defn character-encoding
  "Return the character encoding for the request, or nil if it is not set."
  [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (second (.split type "charset="))
    #_(second (re-find charset-pattern type))))

(defn urlencoded-form?
  "True if a request contains a urlencoded form in the body."
  [request]
  (if-let [type (content-type request)]
    (.startsWith type "application/x-www-form-urlencoded")))

(defn render [v]
  (let [t (type v)]
    (cond
      (= t Keyword) :keyword
      (= t js/String) :stting
      (satisfies? ICollection v) :coll
      nil :nil)))

(defmulti body-string
          "Return the request body as a string." {:arglists '([request])}
          (fn [{:keys [body]}]
            (let [t (type body)]
              (cond
                (= t Keyword) :keyword
                (= t js/String) :string
                (satisfies? ICollection body) :coll
                nil? :nil))))

(defmethod body-string :nil [_] nil)

(defmethod body-string :string [request]
  (:body request))

(defmethod body-string :string [request]
  (apply str (:body request)))

(defn path-info
  "Returns the relative path of the request."
  [request]
  (or (:path-info request)
      (:uri request)))

(defn in-context?
  "Returns true if the URI of the request is a subpath of the supplied context."
  {:added "1.2"}
  [request context]
  (.startsWith (:uri request) context))

(defn set-context
  "Associate a context and path-info with the  request. The request URI must be
  a subpath of the supplied context."
  {:added "1.2"}
  [request context]
  {:pre [(in-context? request context)]}
  (assoc request
    :context context
    :path-info (subs (:uri request) (.-length context))))
