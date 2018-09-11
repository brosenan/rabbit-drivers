(ns rabbit-drivers.lk
  (:require [lambdakube.testing :as lkt]
            [lambdakube.core :as lk]
            [lambdakube.util :as lku]
            [rabbit-drivers.core :as rmqs])
  (:import (rabbit_drivers RMQueuingService
                           RMQPubSubService)))


(defn module [$]
  (let [jarurl "https://github.com/brosenan/rabbit-drivers/raw/master/rabbit-drivers-uber.jar"]
    (-> $
        (lk/rule :event-broker [:use-single-rabbitmq]
                 (fn [use-rabbitmq]
                   (-> (lk/pod :rabbitmq {:tier :event-bus
                                          :app :rabbitmq})
                       (lk/add-container :rabbit "rabbitmq:3.7-management")
                       (lk/deployment 1)
                       (lku/add-itd-annotations RMQueuingService jarurl)
                       (lku/add-itd-annotations RMQPubSubService jarurl)
                       (lk/expose-cluster-ip :rabbitmq
                                             (comp
                                              (lk/port :rabbit :amqp 5672 5672)
                                              (lk/port :rabbit :mgmt 15672 15672)))))))))
