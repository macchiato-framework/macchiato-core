(defproject macchiato/core "0.0.3"
  :description "core Macchiato HTTP library"
  :url "https://github.com/yogthos/macchiato-framework/macchiato-core"
  :scm {:name "git"
        :url "https://github.com/macchiato-framework/macchiato-core.git"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :clojurescript? true
  :dependencies [[funcool/cuerdas "2.0.1"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]]
  :plugins [[codox "0.6.4"]
            [lein-npm "0.6.2"]]
  :npm {:dependencies [[multiparty "4.1.2"]
                       [cookies "0.6.2"]
                       [etag "1.7.0"]
                       [random-bytes "1.0.0"]
                       [simple-encryptor "1.1.0"]
                       [url "0.11.0"]
                       [x-www-form-urlencode "0.1.1"]]}
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "1.9.293"]]
              :plugins [[lein-cljsbuild "1.1.4"]]}})
