(ns macchiato.test.middleware.session
  (:require
    [cuerdas.core :as string]
    [macchiato.middleware.session.store :as store]
    [macchiato.middleware.session.memory :as memory]
    [macchiato.middleware.session :as session]
    [macchiato.test.mock.util :refer [mock-handler ok-response raw-response]]
    [macchiato.util.response :refer [ok get-header]]
    [macchiato.test.mock.request :refer [header request]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(defn trace-fn [f]
  (let [trace (atom [])]
    (with-meta
      (fn [& args]
        (swap! trace conj args)
        (apply f args))
      {:trace trace})))

(defn trace [f]
  (-> f meta :trace deref))

(defn- make-store [reader writer deleter]
  (reify store/SessionStore
    (read-session [_ k] (reader k))
    (write-session [_ k s] (writer k s))
    (delete-session [_ k] (deleter k))))

(defn get-session-cookie [response]
  (get-in response [:cookies "macchiato-session"]))

(deftest session-is-read
  (let [session-atom (atom {})
        reader       (trace-fn (constantly {:bar "foo"}))
        writer       (trace-fn (constantly nil))
        deleter      (trace-fn (constantly nil))
        store        (make-store reader writer deleter)
        handler      (trace-fn (raw-response {}))
        handler*     (mock-handler session/wrap-session handler {:store store})]
    (handler* {:cookies {"macchiato-session" {:value "test"}}})
    (is (= (trace reader) [["test"]]))
    (is (= (trace writer) []))
    (is (= (trace deleter) []))
    (is (= (-> handler trace first first :session)
           {:bar "foo"}))))

(deftest session-is-written
  (let [reader  (trace-fn (constantly {}))
        writer  (trace-fn (constantly nil))
        deleter (trace-fn (constantly nil))
        store   (make-store reader writer deleter)
        handler (raw-response {:session {:foo "bar"}})
        handler (mock-handler session/wrap-session handler {:store store})]
    (handler {:cookies {}})
    (is (= (trace reader) [[nil]]))
    (is (= (trace writer) [[nil {:foo "bar"}]]))
    (is (= (trace deleter) []))))

(deftest session-is-deleted
  (let [reader  (trace-fn (constantly {}))
        writer  (trace-fn (constantly nil))
        deleter (trace-fn (constantly nil))
        store   (make-store reader writer deleter)
        handler (raw-response {:session nil})
        handler (mock-handler session/wrap-session handler {:store store})]
    (handler {:cookies {"macchiato-session" {:value "test"}}})
    (is (= (trace reader) [["test"]]))
    (is (= (trace writer) []))
    (is (= (trace deleter) [["test"]]))))


(deftest session-write-outputs-cookie
  (let [store    (make-store (constantly {})
                             (constantly "foo:bar")
                             (constantly nil))
        handler  (raw-response {:session {:foo "bar"}})
        handler  (mock-handler session/wrap-session handler {:store store})
        response (handler {:cookies {}})]
    (is (get-session-cookie response))))

(deftest session-delete-outputs-cookie
  (let [store    (make-store (constantly {:foo "bar"})
                             (constantly nil)
                             (constantly "deleted"))
        handler  (raw-response {:session nil})
        handler  (mock-handler session/wrap-session handler {:store store})
        response (handler {:cookies {"macchiato-session" {:value "foo:bar"}}})]
    (is (= (:value (get-session-cookie response)) "deleted"))))

(deftest session-cookie-has-attributes
  (let [store          (make-store (constantly {})
                                   (constantly "foo:bar")
                                   (constantly nil))
        handler        (raw-response {:session {:foo "bar"}})
        handler        (mock-handler session/wrap-session handler {:store        store
                                                                   :cookie-attrs {:max-age 5 :path "/foo"}})
        response       (handler {:cookies {}})
        session-cookie (get-session-cookie response)]
    (is (= session-cookie {:path "/foo", :http-only true, :max-age 5, :value "foo:bar"}))))

(deftest session-does-not-clobber-response-cookies
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
        handler (raw-response {:session {:foo "bar"}
                             :cookies {"cookie2" "value2"}})
        handler (mock-handler session/wrap-session handler {:store store :cookie-attrs {:max-age 5}})
        response (handler {:cookies {}})]
    (is (= response {:cookies {"cookie2" "value2", "macchiato-session" {:path "/", :http-only true, :max-age 5, :value "foo:bar"}}}))))


(deftest session-root-can-be-set
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
        handler (raw-response {:session {:foo "bar"}})
        handler (mock-handler session/wrap-session handler {:store store, :root "/foo"})
        response (handler {:cookies {}})]
    (is (= response {:cookies {"macchiato-session" {:path "/foo", :http-only true, :value "foo:bar"}}}))))

(deftest session-attrs-can-be-set-per-request
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
        handler (raw-response {:session {:foo "bar"}
                             :session-cookie-attrs {:max-age 5}})
        handler (mock-handler session/wrap-session handler {:store store})
        response (handler {:cookies {}})]
    (is (= response {:cookies {"macchiato-session" {:path "/", :http-only true, :max-age 5, :value "foo:bar"}}}))))

(deftest cookie-attrs-override-is-respected
  (let [store (make-store (constantly {})
                          (constantly {})
                          (constantly nil))
        handler (raw-response {:session {}})
        handler (mock-handler session/wrap-session handler {:store store :cookie-attrs {:http-only false}})
        response (handler {:cookies {}})]
    (is (= response {:cookies {"macchiato-session" {:path "/", :http-only false, :value {}}}}))
    #_(is (not (.contains (get-session-cookie response)
                        "HttpOnly")))))

(deftest session-response-is-nil
  (let [handler (mock-handler session/wrap-session (constantly nil))]
    (is (nil? (handler {})))))

(deftest session-made-up-key
  (let [store-ref (atom {})
        store     (make-store
                    #(@store-ref %)
                    #(do (swap! store-ref assoc %1 %2) %1)
                    #(do (swap! store-ref dissoc %) nil))
        handler   (mock-handler session/wrap-session
                    (raw-response {:session {:foo "bar"}})
                    {:store store})]
    (handler {:cookies {"macchiato-session" {:value "faked-key"}}})
    (is (not (contains? @store-ref "faked-key")))))

(deftest session-request-test
  (is (fn? session/session-request)))

(deftest session-response-test
  (is (fn? session/session-response)))

(deftest session-cookie-attrs-change
  (let [a-resp   (atom {:session {:foo "bar"}})
        handler  (mock-handler session/wrap-session (fn [_ res _] (res @a-resp)))
        response (handler {})
        sess-key (->> (get-in response [:cookies "macchiato-session" :path])
                      #_(re-find #"(?<==)[^;]+"))]
    (is (= sess-key "/"))
    (reset! a-resp {:session-cookie-attrs {:max-age 3600}})

    (testing "Session cookie attrs with no active session"
      (is (= (handler {}) {})))))

(deftest session-is-recreated-when-recreate-key-present-in-metadata
  (let [reader  (trace-fn (constantly {}))
        writer  (trace-fn (constantly nil))
        deleter (trace-fn (constantly nil))
        store   (make-store reader writer deleter)
        handler (raw-response {:session ^:recreate {:foo "bar"}})
        handler (mock-handler session/wrap-session handler {:store store})]
    (handler {:cookies {"macchiato-session" {:value "test"}}})
    (is (= (trace reader) [["test"]]))
    (is (= (trace writer) [[nil {:foo "bar"}]]))
    (is (= (trace deleter) [["test"]]))
    (testing "session was not written with :recreate metadata intact"
      (let [[[_ written]] (trace writer)]
        (is (not (:recreate (meta written))))))))
