(ns clojurescript-ethereum-example.core
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [com.jakemccrary.middleware.reload :as reload]
            [environ.core :refer [env]]
            [cheshire.core :as json]
            [org.httpkit.server :refer [run-server]]
            [me.raynes.fs :as fs])
  (:import org.web3j.protocol.Web3j
           org.web3j.protocol.infura.InfuraHttpService
           org.web3j.crypto.Credentials
           org.web3j.utils.Numeric
           org.web3j.abi.Transfer
           java.math.BigDecimal)
  (:gen-class))

(def ^:dynamic *server*)

(def users (atom {"customer@a.a" {:email    "customer@a.a"
                                  :password "password"
                                  :keystore "{\"encSeed\":{\"encStr\":\"KBGoHjdX6slMs5FYq1PPEiAZHolnjWy786eSKqL1f7bmo5imdZ9am5FPep+xzoOAgsQfGM9eVSWeHUiq5UyTtiM4czy0jqeB1doLBYPUa6T4URHNMGrqxcWcj/jmige2Ll92vlGsszxnf+2o2h7HXwB8xku4jD2kxIhVoASrfw8WpA15Tj5fpg==\",\"nonce\":\"6pebJdf9NDt+mCbryON+P+C2F2tALRhJ\"},\"ksData\":{\"m/0'/0'/0'\":{\"info\":{\"curve\":\"secp256k1\",\"purpose\":\"sign\"},\"encHdPathPriv\":{\"encStr\":\"FgyxQ4yW6tPxPkLg6B9lWM3C4u8R9T6PY0KQ2QOXH0wf5WAtBFyHxiVZ8T97EcTRcsX5XOqaW1WDzr6+tEoxEHuvtPHKUU/oAo2BLz0ClMw9p+ZhFOGOi0Ge1YRAXYQ/fGHkUBo0xmHLFEMFhqBXI8CLQNqsZRqBO6i5J/fgTQ==\",\"nonce\":\"Yg9HGaIM8NUkkHsK+Yl+Pb4n2TOaeXoM\"},\"hdIndex\":3,\"encPrivKeys\":{\"01a2ef00acc85e376a3d02a566878a47f5876570\":{\"key\":\"Oy1ziIdwOx6MZ191ibEeQ3jjL67mxoaPBxwrosVMvtJLka6/sbZBMZDHtLKezKVo\",\"nonce\":\"CJMOxZOWsM8ZzOHDNsZ4HXtdCpjhuNet\"},\"67c143715b5002bda3568880f879b5074a02fc25\":{\"key\":\"wB13Xy/5AQvCKl6lBDuwCWP3xuATDZEcnneCqU2qv2jC3n7Lyx3QK/5VPgvTPlzq\",\"nonce\":\"jJSW0Mltu2fscPSCoydXfbGtfw7GNK01\"},\"18da23b10f6bbeb06efef774b2ddce5040bb8a12\":{\"key\":\"LvvdJaAcxrVL/GWTTAAK7PKImxdlCRNOg4VQPNLb6kouv1hmI8MRhM7HQ/WwPP2d\",\"nonce\":\"dRqPFsUT34fCXlFLksPhxm9GdLjAH3x+\"}},\"addresses\":[\"01a2ef00acc85e376a3d02a566878a47f5876570\",\"67c143715b5002bda3568880f879b5074a02fc25\",\"18da23b10f6bbeb06efef774b2ddce5040bb8a12\"]},\"m/0'/0'/1'\":{\"info\":{\"curve\":\"curve25519\",\"purpose\":\"asymEncrypt\"},\"encHdPathPriv\":{\"encStr\":\"W6u5lKMAaxXwBtK0d01wtjFA1XxG8kMfWtUH7ybIQylaYGvvZItpqiT6nlbakbNLb2iJnlN1DfEYO3VY4ya8CRaEMU1Px+jh90wlNxGCNk103jLSZqf8+ETYd7ZW2ofrfDCn4wxrdpNYgrxoc0jWMZMiWpmDs743Zh22tcg3Xw==\",\"nonce\":\"uFZlJIHXkTTUyIW7eIHDAZ7Ve7UB3nAI\"},\"hdIndex\":1,\"encPrivKeys\":{\"76f561554ec4e417dce24118333b228b1b07348340b7510747a922dc458e762f\":{\"key\":\"Z8O4yIUNeQvftOEutXRKuZjikV/BBQiJfCaxd8domwxw9cuF9VZBK4uC44G7lknN\",\"nonce\":\"O9LMjH6ANx4mAJwK+PIcpssmJCDSHvC9\"}},\"pubKeys\":[\"76f561554ec4e417dce24118333b228b1b07348340b7510747a922dc458e762f\"]}},\"encHdRootPriv\":{\"encStr\":\"tqEVjY5Jd1sizL2z2qRsaYLwVfwbtz7/0kWUYRoCJi5JMO8sXS5dBYJGSfOFKtmSvJcw0qiqb0Kr4Y3YFFnAINmH0iSNYfGCsDqOWgaW5LiRziPBsFv0ERsahDM/2u8/ES22f7McGuEGnopul8OxiTtnLx5LIprgZDc7R4xgWA==\",\"nonce\":\"sV6bqqmG4Zqh9aWcA/RH0QBrWwqbnIJg\"},\"salt\":\"BUYmUloC6FIwPkBvnznOcn+7TdeGTj9jYq0+kZWEEf0=\",\"version\":2}"
                                  :pubkey   "76f561554ec4e417dce24118333b228b1b07348340b7510747a922dc458e762f"
                                  :type     "customer"
                                  :name     "customer"
                                  :address  "0x01a2ef00acc85e376a3d02a566878a47f5876570"}
                  "dealer1@a.a"  {:email    "dealer1@a.a",
                                  :password "password",
                                  :keystore "{\"encSeed\":{\"encStr\":\"j+sLDog4ViJoDsxtXeOoVbfNV3eOQbXlxJj9DLzqt3Y7pSq9o1u8GANBDs0ZI3zdFaOTNQgVorNTuqI3X/Ypv59RSGYy7767clhUedcBOg1QHWGrwCqnQ7h3Lks94U8elsFPYKB0kpOGNX+GuyjnnJ3dxstkIE8Z2SvClZc1vtMJsLwgg5QQiA==\",\"nonce\":\"20eNTGxnsSRew6gmseaXqMRJDTB+cnqZ\"},\"ksData\":{\"m/0'/0'/0'\":{\"info\":{\"curve\":\"secp256k1\",\"purpose\":\"sign\"},\"encHdPathPriv\":{\"encStr\":\"6R0RKlzrzGTyqW0Het4dMGRCnnNu6N45jOHe4pPSYDZbh6TpUW6XADBeg2xlJZEvZ+ofjkR0f8TevKggsaOCelRB6Zo5PZUdtJiG+RDPxWZT2PJ0HNAMu5H38BgPZU8Vz5sdUkDDgu6wQQZYfL763DRRFsVMGATE+gNVsBg2hQ==\",\"nonce\":\"/Ggf96kuOuJqdfZEulkbz8nbcI0p9BOS\"},\"hdIndex\":3,\"encPrivKeys\":{\"fff3f282061c2add91d8850e4eb824a02f063f16\":{\"key\":\"nwUvbevEdBXd1yhGxXfEhw5u33RdScgntC4khy+u5q+FPgjIQ/+D8s2PsBdfRHHy\",\"nonce\":\"7us14bOjHQ7a1x3f520K+Z7OxsLQ9vdJ\"},\"b356d76573fe4af4a8440d6e535f8b486941a005\":{\"key\":\"5XdEBNpkMwDJI+POKBb1F2yflVfKg/vFbIkwDHUUMss5Avi1J1R87kDZvw14OzIU\",\"nonce\":\"l3U/e13Hk74PnqNQ1FytIYvr1sQ0Fs8v\"},\"c8fbe89e5b035183824c52007887a67ff0ec08dd\":{\"key\":\"G7nVTDoAt+qbluLr+IpfU/Ea3nCcJ+JRbeJLtb3t8ZiYKYZvPybUvU/1tOGsdntw\",\"nonce\":\"x6WhsZQ+mZPvRD+F+40y+pcFNyCM6G4S\"}},\"addresses\":[\"fff3f282061c2add91d8850e4eb824a02f063f16\",\"b356d76573fe4af4a8440d6e535f8b486941a005\",\"c8fbe89e5b035183824c52007887a67ff0ec08dd\"]},\"m/0'/0'/1'\":{\"info\":{\"curve\":\"curve25519\",\"purpose\":\"asymEncrypt\"},\"encHdPathPriv\":{\"encStr\":\"GXpeKXbKlmGvG2ADdr/rpP/z0CnNaLpKBlcffXHVHyscgbViKnIuI/athNo9oyqHr2vZA8h/ONYAMDzM3/Zz430YJz2DIoCFSqUEFwOAZQYLwvOZBt/hWCy4UYjzd5W4qsx57d5tb63f6biGwzg+ow9LJxHT604FfLR9P9lCUg==\",\"nonce\":\"R8cMyvGSVRrmFd1/INKSK231Ok6neSv7\"},\"hdIndex\":1,\"encPrivKeys\":{\"06b3f43652c7c282eda7653c22b5bcb35547f81ba80d072f2a7af0697128f551\":{\"key\":\"ahwzQPOsetc8+xSdNn2on0VcYPm54axnnyOvLSqzfKh6S+r+gFwZUF45f1trnUiR\",\"nonce\":\"RHikOmbkCP267hSIYKcgljmxHYHliqmp\"}},\"pubKeys\":[\"06b3f43652c7c282eda7653c22b5bcb35547f81ba80d072f2a7af0697128f551\"]}},\"encHdRootPriv\":{\"encStr\":\"WHsg71wEQhsoWMbkZliTS7shOKvQIMuYRlf1eUBZdHiKo1MmsWbCAwgoDoaCEOXrzotEfQkyZQSFfntdhxbwns6F9J9PqWhUKmvpR4XMwOdLqZbGfzc8ML9Y7N+4uSHjefISJkFxcvyjrowMSE9LfalFcaCmzgbPsVYJWJv7BA==\",\"nonce\":\"AfgWgU+tA26YIXyyhUCh8bQNDpASeto3\"},\"salt\":\"xyV60uwIQ4TuOCSSykT7s878S2ai3GctctGn8y0QC9Q=\",\"version\":2}",
                                  :pubkey   "06b3f43652c7c282eda7653c22b5bcb35547f81ba80d072f2a7af0697128f551",
                                  :type     "dealer"
                                  :name     "dealer1"
                                  :address  "0xfff3f282061c2add91d8850e4eb824a02f063f16"}
                  "admin@a.a"    {:email    "admin@a.a",
                                  :password "password",
                                  :keystore "{\"encSeed\":{\"encStr\":\"58uC7gCtU3ZMo6Uedd/b80lds/ZohqizCtNt9s/eiQGttqGmqGymMxkgRGusvMZA+QdiSR23OEEysS4nzDR1SGy6AbS5cMijx8ACNOAqzGj/tDik9dZ8hT3cs68OeLM3EQ32u+PT8Eaj4mQBWPcuTSemz6rKC9i5yN/HmTSAWkgz4fRZJ1S58g==\",\"nonce\":\"1WU9YLKDWTf0FqENiikUhMWHtHocm2l+\"},\"ksData\":{\"m/0'/0'/0'\":{\"info\":{\"curve\":\"secp256k1\",\"purpose\":\"sign\"},\"encHdPathPriv\":{\"encStr\":\"MU1DyCAU3kn4/CJ0utKkd9Now7mPGMPZnUaENGeGq9K6r6a6UT188x3gD55AoUr91NVCsMQ/T3SwQupf7reYBFvn8XJx6HN5tbJdn3onQ5J+y05XXvAZz3tQTrA60F4rTGCQQWBcK1TCoSc5KLnomBnx9YezspACTOzmPCJKIA==\",\"nonce\":\"/45xOxUEFGGMab8O4y1lyIO8VCACr9rr\"},\"hdIndex\":3,\"encPrivKeys\":{\"62ded09e27f2876ce3e4fee8e3ae9f9448508415\":{\"key\":\"e79vKnhtZLUswkbmJMpTBWDqp2J9F9kjTzvxPrNzM1Jz8EWe+NhWlmwYAbT5oWpZ\",\"nonce\":\"PWMuytRWl+vu/l6pbVUoO7+ih0eDzPf5\"},\"2d4fedd66ea7508160c417c52c1a902cffa07187\":{\"key\":\"N3GuaYQCVDI24/46PDmfenEvh4Wm/58fHMdVXYQ3pB1KtxDuspfW5dQqWyGVRvlK\",\"nonce\":\"HwKiIDrZpj/JHJ5oaHL734kFHt6hUEwe\"},\"70d0840afe39601efa1cd5b73fd201e27c327ca6\":{\"key\":\"D9OOMKHJxyV0oBsl680Mw8+3IH8EIjMIcroTBFUY5NIz57cXQ3UJyX5OrL6guMIM\",\"nonce\":\"Q6R4zt47pwKI/o75MNDRBV5FDbyMmKH5\"}},\"addresses\":[\"62ded09e27f2876ce3e4fee8e3ae9f9448508415\",\"2d4fedd66ea7508160c417c52c1a902cffa07187\",\"70d0840afe39601efa1cd5b73fd201e27c327ca6\"]},\"m/0'/0'/1'\":{\"info\":{\"curve\":\"curve25519\",\"purpose\":\"asymEncrypt\"},\"encHdPathPriv\":{\"encStr\":\"Ufv4STodLhNnkM/bjKwWoq8aYw96aEAgnvPqpKO+rIauV0zehsLshO9BYjFPcATW9dqAtCMtWRuVycPP1kx54wjmc6Qjt9HA6/N602ryHSYVvn1gl2WbnMECAi+UBjTrHqCVsCNt6k0zlPEcPsgl+qrQ5y0Dg5kzo+Mz1cj95A==\",\"nonce\":\"Bgys420AqbniNSDTCQfiloVr2xhj8Z83\"},\"hdIndex\":1,\"encPrivKeys\":{\"55c059dcceeba7ad1e70d72c65b7ea48c0741957c916165c8e1a6cd2ae40d115\":{\"key\":\"+CFSF1RcN+oeHclU8RpcwfvyLxvhMcTFTR5Jhb53+Nmrzkf9LexsphcgygpY31P3\",\"nonce\":\"uwek0NVb9RB0782MOKLbRLWbBhkfmcwR\"}},\"pubKeys\":[\"55c059dcceeba7ad1e70d72c65b7ea48c0741957c916165c8e1a6cd2ae40d115\"]}},\"encHdRootPriv\":{\"encStr\":\"kmbrgWqJvn4nCs61q4QWHoMctHkjSDZQ2cdbJgjQx4xzM5jMWU4Nhmll7SLjiUMRcvxeQim91HJH1/hrYJ5AYPxROPxa1A4wE8iRarA29RV00QmzrDbwetwfkumPHoS9BWMYLUSi1HWWgLQW9GITs9kC2MJrL9fmV2RahTYUcg==\",\"nonce\":\"okzgohTBApQNxAQLas6JkvoWHj0ReTnx\"},\"salt\":\"5rkcxgQ+bWy6CMX/PALqXpSCmKH54pez0YQidrL26go=\",\"version\":2}",
                                  :pubkey   "55c059dcceeba7ad1e70d72c65b7ea48c0741957c916165c8e1a6cd2ae40d115",
                                  :type     "admin"
                                  :name     "admin"
                                  :address  "0x62ded09e27f2876ce3e4fee8e3ae9f9448508415"}
                  "dealer2@a.a"  {:email    "dealer2@a.a",
                                  :password "password",
                                  :keystore "{\"encSeed\":{\"encStr\":\"Dhnb892Zm4BaGYXQHHAe/kw1k54gUr452jUUqLnMX13Lg2cW4/kkeAf3kqGoxXH32uhq9tOB6/axFeoNjxRrc1cwSS0llV8ZA/ivCDWJU3rDM8Ip86h8J4/2cjnkhQWE3/BqcckJ5ffi5PVdcjMo0LOwSs1vUZ283+gmuF1PVXFq93wa6OsCsw==\",\"nonce\":\"0ftpI8UtlXX+E9yAZdDDsyEgszwSeu0S\"},\"ksData\":{\"m/0'/0'/0'\":{\"info\":{\"curve\":\"secp256k1\",\"purpose\":\"sign\"},\"encHdPathPriv\":{\"encStr\":\"E6POVWK9rYD8DgGN5WO4GdDvbRohvXrF3hSv7hUDgm3WvfZShiNW62DGiggmhdZ6M1zb4jfvvrPSFjKfbqlR7Dq/91PPi6oJ9rPVBaYY0ZnRrwiht2Ufq7ri7klmPOVvx/ddatJGb/aLRKVUXJoZyJRFMJfFDzkzpMRY4veYXQ==\",\"nonce\":\"/7jr3keMYkYCerwDKKbY9pGeEeOi6ujR\"},\"hdIndex\":3,\"encPrivKeys\":{\"4829028a81a3379074cd72cb2bb598339a5dc71c\":{\"key\":\"X9Xwy7tP5xzr1p8sMrB2efgVbuDHkPUJpm7Zzrm3B2ZlX0t7PikMvDVXLJ/NMQ8H\",\"nonce\":\"tUUDtU9TVayT3/rhHMB/9x9T6aAqNlS+\"},\"c26149faf281dfa9358795fa19916029fecfd954\":{\"key\":\"rh/hGL+IljYBbh/yCUSZZIY0eP0UXOZFdpZtyjB4DQ8QnU9CbaNKxzqLUlGKAXMf\",\"nonce\":\"wx9NWJcz6TTG7nZkLqZJatVhWVWO5mEi\"},\"66ebf25fc4547a9a8b3812f42643950b2146c776\":{\"key\":\"PAMqCmArUBXCGvS3iQQHW3bOWoHlLE/lPedo/APPhF8aVbBJA+1sBglhzxo/pxk2\",\"nonce\":\"bk7NS1o/2zet98dh/nj3ur2b0EXPKYjI\"}},\"addresses\":[\"4829028a81a3379074cd72cb2bb598339a5dc71c\",\"c26149faf281dfa9358795fa19916029fecfd954\",\"66ebf25fc4547a9a8b3812f42643950b2146c776\"]},\"m/0'/0'/1'\":{\"info\":{\"curve\":\"curve25519\",\"purpose\":\"asymEncrypt\"},\"encHdPathPriv\":{\"encStr\":\"Gn6B6tCvPFiIqdXss73Y4xhAznAVU32slcIjL2VZLMu77Toc3Ma8RUf9qY8m24trA34Co18Mlqmc0TnXANeyL3+afkloObLt6TaFLunedVMPT3J/iZGuQUOjETxnvDj0qzVXd0j2khr3CqpwCE/wDy52KShycPsTXnGrNeb1CA==\",\"nonce\":\"fg2jwP1HUd7CAQO6zzeIYr7lCj5O8/Ng\"},\"hdIndex\":1,\"encPrivKeys\":{\"1bfa7b367c30f2482ec7e03b246ac9b2e3197cc24612cc8cad3ea5e6f9381c4a\":{\"key\":\"UlxVIfWO2LLeQFJWXwRj/Eqrn63Gl3gqA+3mO8phLjq1JAMYbFgkGh9KMJmAX20x\",\"nonce\":\"MygXRWE3e5p6s9Sw38xK08m4A+fpi1R/\"}},\"pubKeys\":[\"1bfa7b367c30f2482ec7e03b246ac9b2e3197cc24612cc8cad3ea5e6f9381c4a\"]}},\"encHdRootPriv\":{\"encStr\":\"aFmFAHkKeP3TiDA0DQvGektGAqEkz0vnu9Np4B9erv4EQeMBUWtBHFzmRrcq1t12XseIBDFsF0oHGNPsVYOQTtQu8iGJdhvvosE6IwJLVJVB6qu9HOrvLwbsrjl9PYMPWldOWGw9gj+gxmzt1T+BeciQKqyWwnoxD7bb4sTsAQ==\",\"nonce\":\"RQ0rtIo9AjgcO6YOUVSYglRLpcjq3K/b\"},\"salt\":\"Ki45CSt4bGif5KEt8ySjeCOX2sEKB0Kf5N65Rj62cgc=\",\"version\":2}",
                                  :pubkey   "1bfa7b367c30f2482ec7e03b246ac9b2e3197cc24612cc8cad3ea5e6f9381c4a",
                                  :type     "dealer",
                                  :name     "dealer2",
                                  :address  "0x4829028a81a3379074cd72cb2bb598339a5dc71c"}}))

;;
(defn- write-users-to-file [checkfn folder email params]
  (if (checkfn)
    (do
      (println "write: " email)
      (with-open [wrtr (clojure.java.io/writer
                        (str folder (:address params) ".dat"))]
        (.write wrtr (pr-str {email params})) ))))

(defn- read-users-from-file [folder]
  (let [files (fs/list-dir folder)]
    (doall (map #(let [tmpMap (load-string (slurp %))]
                   (println "read:" (.getName %) (keys tmpMap))
                   (if (.startsWith (.getName %) "0x")
                     (reset! users (merge @users tmpMap))) ) files))))


(defn login-ok?
  [email password]
  (if (and (not (nil? (@users email)))
           (= password (:password (@users email))))
    true
    false))

(defn login [session {email :email password :password  :as params}]
  (if (login-ok? email password)
    {:success true :user (@users email)}
    {:success false}))

(defn reg-keystore
  [session {email :email keystore :keystore}]
  (swap! users assoc-in [email :keystore] keystore))

(defn register
  [session {email :email password :password keystore :keystore :as params}]
  (swap! users assoc email params) 
  ;;
  (write-users-to-file #(contains? @users email) "users/" email params)
  ;;
  {:success true :user params})

(defn json-response
  [body & more]
  (let [response {:status  200
                  :headers {"Content-Type" "text/html; charset=utf-8"}
                  :body    (json/generate-string body)}
        session  (first more)]
    (if-not (nil? session)
      (assoc response :session session)
      response)))

(defroutes routes

  (resources "/browser-solidity/" {:root "browser-solidity"})

  (resources "/images/" {:root "images"})

  (POST "/login" {session :session params :params} (json-response (login session params)))
  (POST "/register" {session :session params :params} (json-response (register session params)))

  ;; DEALER KEY
  (GET "/key/:address" [address];; "/dealers/" isnt dealt with.
       (println "id:" address)
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (json/generate-string
                  (if (= 0 (count (filter #(= address (:address %)) (vals @users))))
                    {}
                    (dissoc (first (filter #(= address (:address %)) (vals @users))) :keystore)))})

  ;; DEALER INFO
  (GET "/dealers/:address" [address];; "/dealers/" isnt dealt with.
       (println "dealers: " address)
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (json/generate-string
                  (if (= 0 (count (filter #(= address (:address %)) (vals @users))))
                    {}
                    (dissoc (first (filter #(= address (:address %)) (vals @users))) :keystore)))})

  (GET "/users/" []
       (println "users: all")
       (read-users-from-file "users/")
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (json/generate-string (map #(dissoc (get @users %) :keystore) (keys @users)))})
  
  (GET "/js/*" _
       {:status 404})

  (GET "/" _
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (io/input-stream (io/resource "public/index.html"))}) )

(def http-handler
  (-> routes
      ;; (wrap-defaults site-defaults)
      reload/wrap-reload
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-with-logger
      wrap-json-params
      (wrap-transit-params {:opts{}})
      wrap-gzip))

(defn- sendFund [toAddr etherVal]
  (let [conn (InfuraHttpService. (str "https://ropsten.infura.io/" (env :infuraiokey)))
        web3j (Web3j/build conn)
        cred (Credentials/create (env :senderprivkey))]
    (println "clientVer: " (.getWeb3ClientVersion (.send (.web3ClientVersion web3j))))
    (println "senderAdr: " (.getAddress cred))
    (let [x  (Transfer/sendFundsAsync web3j cred toAddr
                                      (BigDecimal/valueOf etherVal)
                                      (org.web3j.utils.Convert$Unit/ETHER) )]
      (println "Transfer: " x)
      )
    )
  )

(defn -main [& [port]]
  ;;
  (sendFund "0x39c4B70174041AB054f7CDb188d270Cc56D90da8" 0.000402)
  ;;
  (let [port (Integer. (or port (env :port) 6655))]
    (alter-var-root (var *server*)
                    (constantly (run-server http-handler {:port port :join? false})))))

(defn stop-server []
  (*server*)
  (alter-var-root (var *server*) (constantly nil)))

(defn restart-server []
  (stop-server)
  (-main))

(comment
  (restart-server)
  (-main)
  (stop-server))
