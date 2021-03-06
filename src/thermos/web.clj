(ns thermos.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]])
  (:use [thermos.memory :only [mset status delete]]
        [hiccup core form]
        [hiccup.page :only (html5)]
        [hiccup.middleware :only (wrap-base-url)]))

(defn- authenticated? [user pass]
  ;; TODO: heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]
  (= [user pass] [(env :repl-user false) (env :repl-password false)]))

(def ^:private drawbridge
  (-> (drawbridge/ring-handler)
      (session/wrap-session)
      (basic/wrap-basic-authentication authenticated?)))

(defroutes app
  (ANY "/repl" {:as req}
       (drawbridge req))

  (GET "/" [] 
       (html5 
        (form-to 
         [:post "/insert"]
         (submit-button "insert") "key:" (text-field :key) "value:" (text-field :value) )
        (form-to 
         [:post "/delete"]
         (submit-button "delete") "key:" (text-field :key))
        (form-to [:post "/status"] (submit-button "status"))

        [:pre (flatten (map #(vector (first %) " " (second %) "\n")
                            (take 100 (sort @thermos.memory/*drop*))))  ]))

  (ANY "/insert" [key value] 
        (do 
          (mset key value)
          (str "<a href=/>back</a>")))

 (ANY "/delete" [key value] 
      (do 
        (delete key)
        (str "<a href=/>back</a>")))

  (ANY "/status" []
       (html5 
        [:pre (flatten (map #(vector (first %) " " (second %) "\n")
                            (sort @thermos.memory/*drop*))) ]))
  #_(GET "/" []
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (pr-str ["Hello" :from 'Heroku])})
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))
        ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
        store (cookie/cookie-store {:key (env :session-secret)})]
    (jetty/run-jetty (-> #'app
                         ((if (env :production)
                            wrap-error-page
                            trace/wrap-stacktrace))
                         (site {:session {:store store}}))
                     {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
