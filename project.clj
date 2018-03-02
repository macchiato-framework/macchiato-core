(defproject macchiato/core "0.2.10"
  :description "core Macchiato HTTP library"
  :url "https://github.com/yogthos/macchiato-framework/macchiato-core"
  :scm {:name "git"
        :url  "https://github.com/macchiato-framework/macchiato-core.git"}
  :license {:name "MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :clojurescript? true
  :dependencies [[com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [funcool/cuerdas "2.0.5"]
                 [macchiato/fs "0.2.1"]
                 [org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]]
  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-codox "0.10.2"]
            [lein-doo "0.1.7"]
            [macchiato/lein-npm "0.6.3"]]
  :npm {:dependencies [[concat-stream "1.6.0"]
                       [content-type "1.0.4"]
                       [cookies "0.7.1"]
                       [etag "1.8.1"]
                       [lru "3.1.0"]
                       [multiparty "4.1.3"]
                       [random-bytes "1.0.0"]
                       [qs "6.5.1"]
                       [simple-encryptor "1.1.1"]
                       [url "0.11.0"]
                       [ws "3.3.3"]]}
  :filespecs [{:type  :bytes
               :path  "project.clj"
               :bytes ~(slurp "project.clj")}]
  :codox {:language :clojurescript}
  :profiles {:test
             {:cljsbuild
                   {:builds
                    {:test
                     {:source-paths ["src" "test"]
                      :compiler     {:main          macchiato.runner
                                     :output-to     "target/test/core.js"
                                     :target        :nodejs
                                     :optimizations :none
                                     :source-map    true
                                     :pretty-print  true}}}}
              :doo {:build "test"}}}
  :aliases
  {"test"
   ["do"
    ["npm" "install"]
    ["clean"]
    ["with-profile" "test" "doo" "node" "once"]]
   "test-watch"
   ["do"
    ["npm" "install"]
    ["clean"]
    ["with-profile" "test" "doo" "node"]]})
