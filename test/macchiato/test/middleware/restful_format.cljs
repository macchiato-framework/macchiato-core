(ns macchiato.test.middleware.restful-format
  (:require
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]
    [cognitect.transit :as t]
    [cuerdas.core :as string]
    [macchiato.middleware.restful-format :as rf]
    [macchiato.util.response :as r]
    [macchiato.test.mock.request :refer [request]]
    [macchiato.test.mock.util :refer [mock-handler raw-response ok-response]]
    [macchiato.test.mock.transit :refer [MockPoint mock-write-handlers mock-read-handlers]]
    [cljs.nodejs :as node]))

(defn- mock-node-request [body content-type accept-type]
  (-> (request :post "/")
      (assoc :body body)
      (assoc-in [:headers "content-type"] content-type)
      (assoc-in [:headers "accept"] accept-type)))

(defn json [body]
  (js/JSON.stringify (clj->js body)))

(defn transit [body & [{:keys [type opts]}]]
  (t/write (t/writer (or type :json) opts) body))

(deftest serialize
  (is
    (= (rf/deserialize-request
         {:type        "application/json"
          :charset     "utf8"
          :body        (json {:foo "bar"})
          :keywordize? true})
       {:foo "bar"}))
  (is
    (= (rf/deserialize-request
         {:type    "application/transit+json"
          :charset "utf8"
          :body    (transit {:foo "bar"})})
       {:foo "bar"}))
  (is
    (= (rf/deserialize-request
         {:type         "application/transit+json"
          :charset      "utf8"
          :body         (transit {:point (MockPoint. 0 0)}
                                 {:opts mock-write-handlers})
          :transit-opts {:opts mock-read-handlers}})
       {:point (MockPoint. 0 0)})))

(deftest deserialize
  (is
    (=
      (json {:foo "bar"})
      (rf/serialize-response
        {:type    "application/json"
         :charset "utf8"
         :body    {:foo "bar"}})))
  (is
    (=
      (transit {:foo "bar"})
      (rf/serialize-response
        {:type    "application/transit+json"
         :charset "utf8"
         :body    {:foo "bar"}})))
  (is
    (=
      (transit {:point (MockPoint. 0 0)}
               {:opts mock-write-handlers})
      (rf/serialize-response
        {:type         "application/transit+json"
         :charset      "utf8"
         :body         {:point (MockPoint. 0 0)}
         :transit-opts {:opts mock-write-handlers}}))))

(deftest infer-accept-type
  (is
    (=
      "application/json"
      (rf/infer-response-content-type
        (r/header {:body (json {:foo "bar"})} "Accept" "application/json")
        rf/default-accept-types)))
  (is
    (=
      "application/transit+json"
      (rf/infer-response-content-type
        (r/header {:body (transit {:foo "bar"})} "Accept" "application/transit+json")
        rf/default-accept-types))))

(deftest test-roundtrip
  (let [plain-request                   (mock-node-request "hello" nil nil)
        json-request                    (mock-node-request
                                          (json {:foo "bar"})
                                          "application/json"
                                          nil)
        invalid-json-request            (mock-node-request
                                         "{\"foo}"
                                         "application/json"
                                         nil)
        json-request-response           (mock-node-request
                                          (json {:foo "bar"})
                                          "application/json"
                                          "application/json")
        transit-request                 (mock-node-request
                                          (transit {:foo "bar"})
                                          "application/transit+json"
                                          nil)
        transit-request-response        (mock-node-request
                                          (transit {:foo "bar"})
                                          "application/transit+json"
                                          "application/transit+json")
        transit-custom-request-response (mock-node-request
                                          (transit {:point (MockPoint. 0 0)}
                                                   {:opts mock-write-handlers})
                                          "application/transit+json"
                                          "application/transit+json")

        handler                         (rf/wrap-restful-format
                                          (fn [req res raise]
                                            (res (r/ok (:body req)))))
        handler-keywordize              (rf/wrap-restful-format
                                          (fn [req res raise]
                                            (res (r/ok (:body req))))
                                          {:keywordize? true})
        handler-never                   (rf/wrap-restful-format
                                         (fn [req res raise]
                                           (throw "This handler is not expected to be called")))]
    (is (=
          (handler plain-request identity identity)
          {:status  200
           :headers {}
           :body    "hello"}))
    (is (=
          (handler json-request identity identity)
          {:status  200
           :headers {}
           :body    {"foo" "bar"}}))
    (is (=
          (handler-keywordize json-request identity identity)
          {:status  200
           :headers {}
           :body    {:foo "bar"}}))
    (is (=
          (handler json-request-response identity identity)
          {:status  200
           :headers {"Content-Type" "application/json"}
           :body    (json {:foo "bar"})}))
    (is (=
          (handler-never invalid-json-request (constantly :responded) (constantly :raised))
          :raised)
        "Request with invalid json")
    (is (=
          (handler transit-request identity identity)
          {:status  200
           :headers {}
           :body    {:foo "bar"}}))
    (is (=
          (handler transit-request-response identity identity)
          {:status  200
           :headers {"Content-Type" "application/transit+json"}
           :body    (transit {:foo "bar"})}))
    (is (=
          (handler transit-custom-request-response identity identity)
          {:status  200
           :headers {"Content-Type" "application/transit+json"}
           :body    (transit {:point (MockPoint. 0 0)}
                             {:opts mock-write-handlers})}))))
