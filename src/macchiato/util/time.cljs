(ns macchiato.util.time
  (:require
    [cuerdas.core :as s]
    [cljs-time.coerce :refer [from-date]]
    [cljs-time.format :as f]))

(defn- trim-quotes [s]
  (s/replace s #"^'|'$" ""))

(defn parse-date
  "Attempt to parse a HTTP date. Returns nil if unsuccessful."
  [http-date]
  (-> (trim-quotes http-date) (js/Date.) (from-date)))

(defn format-date
  "Format a date as RFC1123 format."
  [date]
  (.toUTCString date))

