(ns macchiato.test.anti-forgery
  (:require
    [cuerdas.core :as string]
    [macchiato.middleware.anti-forgery :as af]
    [macchiato.test.mock.request :refer [header request]]
    [macchiato.test.mock.util :refer [mock-handler raw-response ok-response]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest forgery-protection-test
  (let [response {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery (ok-response response))]
    (are [status req] (= (:status (handler req)) status)
                      403 (-> (request :post "/")
                              (assoc :form-params {"__anti-forgery-token" "foo"}))
                      403 (-> (request :post "/")
                              (assoc :session {::af/anti-forgery-token "foo"})
                              (assoc :form-params {"__anti-forgery-token" "bar"}))
                      200 (-> (request :post "/")
                              (assoc :session {::af/anti-forgery-token "foo"})
                              (assoc :form-params {"__anti-forgery-token" "foo"})))))

(deftest request-method-test
  (let [response {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery (ok-response response))]
    (are [status req] (= (:status (handler req)) status)
                      200 (request :head "/")
                      200 (request :get "/")
                      200 (request :options "/")
                      403 (request :post "/")
                      403 (request :put "/")
                      403 (request :patch "/")
                      403 (request :delete "/"))))

(deftest csrf-header-test
  (let [response {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery (ok-response response))
        sess-req (-> (request :post "/")
                     (assoc :session {::af/anti-forgery-token "foo"}))]
    (are [status req] (= (:status (handler req)) status)
                      200 (assoc sess-req :headers {"x-csrf-token" "foo"})
                      200 (assoc sess-req :headers {"x-xsrf-token" "foo"}))))

(deftest multipart-form-test
  (let [response {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery (ok-response response))]
    (is (= (-> (request :post "/")
               (assoc :session {::af/anti-forgery-token "foo"})
               (assoc :multipart-params {"__anti-forgery-token" "foo"})
               handler
               :status)
           200))))

(deftest token-in-session-test
  (let [response {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery (ok-response response))]
    (is (contains? (:session (handler (request :get "/")))
                   ::af/anti-forgery-token))
    (is (not= (get-in (handler (request :get "/"))
                      [:session ::af/anti-forgery-token])
              (get-in (handler (request :get "/"))
                      [:session ::af/anti-forgery-token])))))

(deftest token-binding-test
  (letfn [(handler [request respond raise]
            (respond
              {:status  200
               :headers {}
               :body    af/*anti-forgery-token*}))]
    (let [response ((mock-handler af/wrap-anti-forgery handler) (request :get "/"))]
      (is (= (get-in response [:session ::af/anti-forgery-token])
             (:body response))))))

(deftest nil-response-test
  (letfn [(handler [request respond raise] (respond nil))]
    (let [response ((mock-handler af/wrap-anti-forgery handler) (request :get "/"))]
      (is (nil? response)))))

(deftest no-lf-in-token-test
    (letfn [(handler [request respond raise]
              (respond
                {:status  200
                 :headers {}
                 :body    af/*anti-forgery-token*}))]
      (let [response ((mock-handler af/wrap-anti-forgery handler) (request :get "/"))
            token    (get-in response [:session ::af/anti-forgery-token])]
        (is (not (string/includes? token "\n"))))))

(deftest single-token-per-session-test
  (let [expected {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery (raw-response expected))
        actual   (handler
                   (-> (request :get "/")
                       (assoc-in [:session ::af/anti-forgery-token] "foo")))]
    (is (= actual expected))))

(deftest not-overwrite-session-test
  (let [response {:status 200 :headers {} :body nil}
        handler  (mock-handler af/wrap-anti-forgery (raw-response response))
        session  (:session (handler (-> (request :get "/")
                                        (assoc-in [:session "foo"] "bar"))))]
    (is (contains? session ::af/anti-forgery-token))
    (is (= (session "foo") "bar"))))


(deftest session-response-test
  (let [response {:status 200 :headers {} :session {"foo" "bar"} :body nil}
        handler  (mock-handler af/wrap-anti-forgery (raw-response response))
        session  (:session (handler (request :get "/")))]
    (is (contains? session ::af/anti-forgery-token))
    (is (= (session "foo") "bar"))))


(deftest custom-error-response-test
  (let [response   {:status 200, :headers {}, :body "Foo"}
        error-resp {:status 500, :headers {}, :body "Bar"}
        handler    (mock-handler af/wrap-anti-forgery (raw-response response)
                                      {:error-response error-resp})]
    (is (= (dissoc (handler (request :get "/")) :session)
           response))
    (is (= (dissoc (handler (request :post "/")) :session)
           error-resp))))

(deftest custom-error-handler-test
  (let [response   {:status 200, :headers {}, :body "Foo"}
        error-resp {:status 500, :headers {}, :body "Bar"}
        handler    (mock-handler af/wrap-anti-forgery (raw-response response)
                                      {:error-handler (fn [request] error-resp)})]
    (is (= (dissoc (handler (request :get "/")) :session)
           response))
    (is (= (dissoc (handler (request :post "/")) :session)
           error-resp))))

(deftest disallow-both-error-response-and-error-handler
  (try
    (af/wrap-anti-forgery (raw-response {:status 200})
                       {:error-handler (raw-response {:status 500 :body "Handler"})
                        :error-response {:status 500 :body "Response"}})
    (is (= "shouldn't be here" :ok))
    (catch js/Error e
      (= (.-message e)
         "Assert failed: (not (and (:error-response options) (:error-handler options)))"))))

(deftest custom-read-token-test
  (let [response {:status 200, :headers {}, :body "Foo"}
        handler  (mock-handler af/wrap-anti-forgery
                   (raw-response response)
                   {:read-token #(get-in % [:headers "x-forgery-token"])})
        req      (-> (request :post "/")
                     (assoc :session {::af/anti-forgery-token "foo"})
                     (assoc :headers {"x-forgery-token" "foo"}))]
    (is (= (:status (handler req))
           200))
    (is (= (:status (handler (assoc req :headers {"x-csrf-token" "foo"})))
           403))))

(deftest random-tokens-test
  (let [handler      (raw-response {})
        get-response (fn [] ((mock-handler af/wrap-anti-forgery handler) (request :get "/")))
        tokens       (map #(-> % :session ::af/anti-forgery-token) (repeatedly 1000 get-response))]
    (is (every? #(re-matches #"[A-Za-z0-9+/]{80}" %) tokens))
    (is (= (count tokens) (count (set tokens))))))
