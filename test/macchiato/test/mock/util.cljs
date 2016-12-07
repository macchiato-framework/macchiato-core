(ns macchiato.test.mock.util
  (:require
    [macchiato.util.response :refer [ok]]))

(defn mock-handler [mw-fn handler & [opts]]
  #((if opts (mw-fn handler opts) (mw-fn handler)) % identity nil))

(defn ok-response [value]
  (fn [req res raise]
    (res (ok value))))

(defn raw-response [value]
  (fn [req res raise]
    (res value)))
