(defproject screams "0.1.0-SNAPSHOT"
  :description "Scoped streams for clojure. Useful for processing data which doesn't fit in memory. Avoids resource scope issues and intermediate data structures associated with lazy sequences."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"])
