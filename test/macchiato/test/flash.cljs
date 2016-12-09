(ns macchiato.test.flash
  (:require
    [macchiato.middleware.flash :refer [wrap-flash]]
    [macchiato.test.mock.request :refer [header request]]
    [macchiato.test.mock.util :refer [mock-handler raw-response ok-response]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest flash-is-added-to-session
  (let [message  {:error "Could not save"}
        handler  (mock-handler wrap-flash (raw-response {:flash message}))
        response (handler {:session {}})]
    (is (= (:session response) {:_flash message}))))

(deftest flash-is-retrieved-from-session
  (let [message  {:error "Could not save"}
        handler  (mock-handler
                   wrap-flash
                   (fn [req res raise]
                     (is (= (:flash req) message))
                     (res {})))]
    (handler {:session {:_flash message}})))

(deftest flash-is-removed-after-read
  (let [message  {:error "Could not save"}
        handler  (mock-handler wrap-flash (raw-response {:session {:foo "bar"}}))
        response (handler {:session {:_flash message}})]
    (is (nil? (:flash response)))
    (is (= (:session response) {:foo "bar"}))))

(deftest flash-is-removed-after-read-not-touching-session-explicitly
  (let [message  {:error "Could not save"}
        handler  (mock-handler wrap-flash (raw-response {:status 200}))
        response (handler {:session {:_flash message :foo "bar"}})]
    (is (nil? (:flash response)))
    (is (= (:session response) {:foo "bar"}))))


(deftest flash-doesnt-wipe-session
  (let [message  {:error "Could not save"}
        handler  (mock-handler wrap-flash (raw-response {:flash message}))
        response (handler {:session {:foo "bar"}})]
    (is (= (:session response) {:foo "bar", :_flash message}))))

(deftest flash-overwrites-nil-session
  (let [message  {:error "Could not save"}
        handler  (mock-handler wrap-flash (raw-response {:flash message, :session nil}))
        response (handler {:session {:foo "bar"}})]
    (is (= (:session response) {:_flash message}))))

(deftest flash-not-except-on-nil-response
  (let [handler (mock-handler wrap-flash (raw-response nil))]
    (is (nil? (handler {})))))
