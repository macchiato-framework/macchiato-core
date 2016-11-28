(defproject macchiato/http "0.0.4"
  :description "Macchiato Node.js HTTP wrapper"
  :url "https://github.com/yogthos/macchiato/macchiato-http"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :clojurescript? true
  :dependencies []
  :plugins [[codox "0.6.4"]
            [lein-npm "0.6.2"]]
  :npm {:dependencies [[stream "0.0.2"]
                       [url "0.11.0"]]}
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "1.9.293"]]
              :plugins [[lein-cljsbuild "1.1.4"]]}})
