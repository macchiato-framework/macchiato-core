(ns macchiato.test.middleware.middleware-meta
  (:require
    [macchiato.middleware :as m]
    [macchiato.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [macchiato.middleware.flash :refer [wrap-flash]]
    [macchiato.middleware.params :refer [wrap-params]]
    [macchiato.middleware.keyword-params :refer [wrap-keyword-params]]
    [macchiato.middleware.session :as session]
    [macchiato.middleware.nested-params :refer [wrap-nested-params]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(defn
  ^{:macchiato/middleware
    {:id       :foo
     :required [:bar :baz]}}
  foo [handler] handler)

(defn
  ^{:macchiato/middleware
    {:id :bar}}
  bar
  "middleware wrapper"
  ([handler] (bar handler {}))
  ([handler opts] handler))

(defn
  ^{:macchiato/middleware
    {:id       :baz
     :required [:bar]}}
  baz [handler] handler)

(defn handler [_ _ _])

(deftest meta-test
  (testing "missing middleware"
    (try
      (m/wrap-middleware
        handler
        #'wrap-anti-forgery
        #'wrap-nested-params
        #'wrap-keyword-params
        #'wrap-params)
      (is (not= :true :false))
      (catch js/Error e
        (is (= (.-message e) ":wrap-anti-forgery is missing required middleware: [:wrap-session]")))))
  (testing "expected middleware"
    (is
      (=
        (meta
          (m/wrap-middleware
            handler
            #'wrap-anti-forgery
            #'session/wrap-session
            #'wrap-nested-params
            #'wrap-keyword-params
            #'wrap-params))
        {:macchiato/middleware
         [{:id       :wrap-anti-forgery
           :required [:wrap-session]}
          {:id :wrap-session}
          {:id       :wrap-nested-params
           :required [:wrap-params]}
          {:id       :wrap-keyword-params
           :required [:wrap-params]}
          {:id :wrap-params}]}))

    (is (=
          (meta
            (m/wrap-middleware
              identity
              #'foo
              #'baz
              [#'bar {}]
              #'bar))
          {:macchiato/middleware
           [{:id       :foo
             :required [:bar
                        :baz]}
            {:id       :baz
             :required [:bar]}
            {:id :bar}]})))
  (testing "wrap"
    (m/validate-handler
      (-> identity
          (m/wrap #'foo)
          (m/wrap #'baz)
          (m/wrap #'bar)))
    (try
      (m/validate-handler
        (-> identity
            (m/wrap #'baz)
            (m/wrap #'bar)
            (m/wrap #'foo)))
      (catch js/Error e
        (= (.-message e) ":foo is missing required middleware: [:bar :baz]]"))))
  (testing "wrap conflicting"
    (try
      (-> identity
          (m/wrap-middleware
            #'baz
            #'bar)
          (m/wrap-middleware
            #'foo
            #'baz
            #'bar)
          (meta))
      (catch js/Error e
        (is (= (.-message e) "following middleware has already been wrapped:(:baz :bar)")))))
  (testing "nested middleware"
    (is
      (=
        (-> identity
            (m/wrap-middleware
              #'wrap-params)
            (m/wrap-middleware
              #'foo
              #'baz
              [#'bar {}]
              #'bar)
            (meta))
        {:macchiato/middleware
         [{:id :wrap-params}
          {:id :foo, :required [:bar :baz]}
          {:id :baz, :required [:bar]}
          {:id :bar}]}))))
