(ns macchiato.test.middleware.content-type
  (:require
    [macchiato.middleware.content-type :as ct]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(defn test-handler [& opts]
  (apply ct/wrap-content-type (fn [req res raise] (res {})) opts))

(defn test-handler-with-fn [f & opts]
  (apply ct/wrap-content-type f opts))

(deftest wrap-content-type-test
  (testing "response without content-type"
    (let [response {:headers {}}
          handler  (test-handler)]
      (is (= (handler {:uri "/foo/bar.png"} identity nil)
             {:headers {"Content-Type" "image/png"}}))
      (is (= (handler {:uri "/foo/bar.txt"} identity nil)
             {:headers {"Content-Type" "text/plain"}}))))

  (testing "response with content-type"
    (let [response {:headers {"Content-Type" "application/x-foo"}}
          handler  (test-handler-with-fn (fn [req res raise] (res response)))]
      (is (= (handler {:uri "/foo/bar.png"} identity nil)
             {:headers {"Content-Type" "application/x-foo"}}))))

  (testing "unknown file extension"
    (let [response {:headers {}}
          handler  (test-handler)]
      (is (= (handler {:uri "/foo/bar.xxxaaa"} identity nil)
             {:headers {"Content-Type" "text/plain"}}))
      (is (= (handler {:uri "/foo/bar"} identity nil)
             {:headers {"Content-Type" "text/plain"}}))))

  (testing "response with mime-types option"
    (let [response {:headers {}}
          handler  (test-handler {:mime-types {"edn" "application/edn"}})]
      (is (= (handler {:uri "/all.edn"} identity nil)
             {:headers {"Content-Type" "application/edn"}}))))

  (testing "nil response"
    (let [handler (ct/wrap-content-type (fn [_ _ _] nil))]
      (is (nil? (handler {:uri "/foo/bar.txt"} identity nil)))))

  (testing "response header case insensitivity"
    (let [response {:headers {"CoNteNt-typE" "application/x-overridden"}}
          handler  (test-handler-with-fn (fn [req res raise] (res response)))]
      (is (= (handler {:uri "/foo/bar.png"} identity nil)
             {:headers {"CoNteNt-typE" "application/x-overridden"}})))))

