(ns macchiato.util.time
  (:require
    [cuerdas.core :as s]
    [cljs-time.coerce :refer [from-date]]
    [cljs-time.format :as f]))

(def ^:no-doc http-date-formats
  {:rfc1123 (f/formatter "EEE, dd MMM yyyy HH:mm:ss zzz")
   :rfc1036 (f/formatter "EEEE, dd-MMM-yy HH:mm:ss zzz")
   :asctime (f/formatter "EEE MMM d HH:mm:ss yyyy")})

(defn attempt-parse [date fmt]
  (try
    (f/parse (f/formatter (http-date-formats fmt)) date)
    (catch js/Error e)))

(defn- trim-quotes [s]
  (s/replace s #"^'|'$" ""))

(defn parse-date
  "Attempt to parse a HTTP date. Returns nil if unsuccessful."
  {:added "1.2"}
  [http-date]
  (->> (keys http-date-formats)
       (map (partial attempt-parse (trim-quotes http-date)))
       (remove nil?)
       (first)))

(defn format-date
  "Format a date as RFC1123 format."
  [date]
  (.toUTCString date))

