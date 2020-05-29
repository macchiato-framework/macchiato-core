(ns macchiato.test.middleware.resource
  (:require
    [macchiato.fs :as fs]
    [macchiato.fs.path :as path]
    [macchiato.middleware.resource :refer [uri->path]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(def target (path/resolve "test"))

(def target-file (str target path/separator "runner.cljs"))

(deftest test-resource-path
  (testing "absolute path"
    (is (= target-file (uri->path target "runner.cljs"))))
  (testing "path"
    (is (= target-file (uri->path "test" "runner.cljs"))))
  (testing "path with slash"
    (is (= target-file (uri->path "test" "/runner.cljs"))))
  (testing "resource path with '..'"
    (is (= target-file (uri->path "test" "../test/runner.cljs"))))
  (testing "resource path with '..' outside root"
    (is (nil? (uri->path "test" "../project.clj")))))
