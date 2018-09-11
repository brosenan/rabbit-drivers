# rabbit-drivers
InjectTheDriver driver implementations for RabbitMQ.
[![Clojars Project](https://img.shields.io/clojars/v/brosenan/rabbit-drivers.svg)](https://clojars.org/brosenan/rabbit-drivers)

## Usage

This library defines
a [lambda-kube](https://github.com/brosenan/lambda-kube) module:
`rabbit-drivers.lk/module`. Add this module to your project:

```clojure
(ns my-app
  (:require [rabbit-drivers.lk :as rmqd]
            ;; ...
            ))
;; ...

(def config {:use-single-rabbitmq true
             ;; ...
		     })

(defn -main []
  (-> (lk/injector)
      ;; ...
	  (rmqd/module)
	  ;; ...
	  (lk/get-deployable config)
	  ;; ...
	  ))
```

The key `:use-single-rabbitmq` in the configuration will create a
single-node RabbitMQ cluster. The value (`true` in the example above)
is ignored.

It contributes a resource named `:event-broker`.

Within modules that define (micro-)services that depend on RabbitMQ,
add `:event-broker` as a dependency. To inject drivers into a
container,
use
[lku/inject-driver](https://github.com/brosenan/lambda-kube/blob/master/util.md#client-side):

```clojure
(defn my-module [$]
  (-> $
      ;; ...
      (lk/rule :my-microservice
               [:event-broker :other :deps]
               (fn [event-broker other deps]
                 (-> (lk/pod :my-microservice {:my :lables})
                     (lk/add-container :my-cont "my-image")
                     ;; Force this pod to wait for the amqp port to be
                     ;; available befoe launching
                     (lku/wait-for-service-port event-broker :amqp)
                     ;; Inject the QueueService driver into the my-cont container
                     (lk/update-container :my-cont lku/inject-driver QueueService event-broker)
                     ;; ...
                     )))))
```

## Development

This library consists of two parts:
1. `core`, which defines the drivers, and
2. `lk`, which defines the lambda-kube module.

While both parts are related, they live on two different levels. The
`core` namespace lives within client (micro-)services, while the `lk`
namespace helps build Kubernetes deployment manifests. The connection
between them is the following:

1. The driver is packaged in an uber-jar, by running `lein uberjar` in the project directory.
2. The newly-created uber-jar is copied (manually) to the file `rabbit-drivers-uber.jar`, located in the root of this repository.
3. The file `rabbit-drivers-uber.jar` is tracked in this repository, so when committing it and pushing it upstream it is uploaded to GitHub.
4. `lk/module` specifies the URL to this uberjar, so that when it is used in an application, the latest driver version is used.

__Note:__ The URL being used does not refer to a specific version of
the uber-jar, but rather to the latest. One needs to be careful when
introducing breaking changes.

## License

Copyright © 2018 Boaz Rosenan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
