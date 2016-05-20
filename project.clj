(defproject pyqt4-testbed "0.1.0-SNAPSHOT"
  :description "ClojureScript in PyQt4 Testbed"
  :url "http://example.com/FIXME"
  :license {:name "BSD simplified 3-clause License"
            :url "https://opensource.org/licenses/BSD-3-Clause"}

  :min-lein-version "2.6.1"
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]]
  
  :plugins [[lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["testbed/lib"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                :compiler {:main pyqt4-testbed.core
                           :asset-path "testbed/lib"
                           :output-to "testbed/main.js"
                           :output-dir "testbed/lib"
                           :source-map-timestamp true}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "testbed/main.js"
                           :main pyqt4-testbed.core
                           :optimizations :advanced
                           :pretty-print false}}]})