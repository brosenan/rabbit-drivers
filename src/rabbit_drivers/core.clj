(ns rabbit-drivers.core
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.basic :as lb]
            [langohr.consumers :as lc])
  (:import (injectthedriver.interfaces QueueService
                                       QueueService$Queue
                                       Stoppable
                                       RecoverableError)))

(gen-class
 :name "rabbit_drivers.RMQueuingService"
 :implements [injectthedriver.interfaces.QueueService]
 :state state
 :init init
 :prefix "-qs-"
 :constructors {[java.util.Map] []})
(gen-class
 :name "rabbit_drivers.RMQPubSubService"
 :implements [injectthedriver.interfaces.PubSubService]
 :state "state"
 :init "init"
 :prefix "-ps-"
 :constructors {[java.util.Map] []})

(defn- convert-props [props]
  {:host (-> props (.get "hostname"))
   :port (-> props (.get "ports") (.get "amqp"))})

(defn qs-init [props]
  (let [props (convert-props props)
        conn (rmq/connect props)
        chan (lch/open conn)]
    [[] {:conn conn
         :chan chan}]))

(def -qs-init qs-init)

(defn qs-callback-wrapper [cb ack nack log]
  (fn [chan {:keys [delivery-tag]} task]
    (try
      (.handleTask cb task)
      (ack chan delivery-tag)
      (catch RecoverableError e
        (log e)
        (nack chan delivery-tag))
      (catch Exception e
        (log e)
        (ack chan delivery-tag)))
    nil))

(defn qs-defineQueue [this name]
  (let [chan (-> this .state :chan)]
    (lq/declare chan name {:exclusive false :auto-delete false})
    (reify QueueService$Queue
      (enqueue [this' task]
        (lb/publish chan "" name task {:content-type "application/octet-stream"}))
      (register [this' cb]
        (let [constag (lc/subscribe chan name (qs-callback-wrapper cb lb/ack lb/nack prn))]
          (reify Stoppable
            (stop [this']
              (lb/cancel chan constag))))))))


(def -qs-defineQueue qs-defineQueue)

(defn ps-init [props]
  (let [props (convert-props props)
        conn (rmq/connect props)
        chan (lch/open conn)]
    (le/declare chan "pubsub" "topic" {:durable true})
    [[] {:conn conn
         :chan chan}]))

(def -ps-init ps-init)

(defn ps-publish [this topic msg]
  (let [chan (-> this .state :chan)]
    (lb/publish chan "pubsub" topic msg)
    nil))

(def -ps-publish ps-publish)

(defn ps-subscribe [this topic cb]
  (let [chan (-> this .state :chan)
        q (lq/declare chan)
        handler (qs-callback-wrapper cb lb/ack lb/nack println)
        subs (lc/subscribe chan q handler)]
    (lq/bind chan q "pubsub" {:routing-key topic})
    (reify Stoppable
      (stop [this']
        (lb/cancel chan subs)))))

(def -ps-subscribe ps-subscribe)
