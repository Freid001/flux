(ns app.util.metric
  (:require [app.util.producer :as producer]
            [clojure.string :as string]
            [clj-time.core :as time]
            [amazonica.aws.cloudwatch :as cloudwatch]
            [amazonica.aws.ecs :as ecs]
            [amazonica.aws.ec2 :as ec2]
            [amazonica.aws.s3 :as s3]
            [amazonica.aws.sqs :as sqs]))

(defn- retrieve [id namespace metric statistic dimensions start-time end-time period]
  (let [statistics (cloudwatch/get-metric-statistics {:namespace   namespace
                                                      :metric-name metric
                                                      :statistics  [statistic]
                                                      :dimensions  dimensions
                                                      :start-time  start-time
                                                      :end-time    end-time
                                                      :period      period})]

    (map (fn [metric] {:id        id
                       :label     statistic
                       :timestamp (str (get metric :timestamp))
                       :value     (get metric (keyword (clojure.string/lower-case statistic)))
                       :unit      (get metric :unit)}) (get statistics :datapoints))))

(defprotocol Metric
  (namespace [this] "Metric namespace.")
  (fetch [this metric statistic start-time end-time period] "Fetch metrics."))

(defrecord Ecs [topic schema]
  Metric

  (namespace [this] "AWS/ECS")

  (fetch [this metric statistic start-time end-time period]
    (doseq [cluster (get (ecs/list-clusters) :cluster-arns)]
      (doseq [record (retrieve (last (string/split cluster #"/"))
                               (namespace this)
                               metric
                               statistic
                               [{:name "ClusterName" :value (last (string/split cluster #"/"))}]
                               start-time
                               end-time
                               period)]
        (producer/write topic schema record)))))

(defrecord Ec2 [topic schema]
  Metric

  (namespace [this] "AWS/EC2")

  (fetch [this metric statistic start-time end-time period]
    (doseq [group (get (ec2/describe-instances) :reservations)]
      (doseq [instance (get group :instances)]
        (doseq [record (retrieve (get instance :instance-id)
                                 (namespace this)
                                 metric
                                 statistic
                                 [{:name "InstanceId" :value (get instance :instance-id)}]
                                 start-time
                                 end-time
                                 period)]
          (producer/write topic schema record))))))

(defrecord S3 [topic schema]
  Metric

  (namespace [this] "AWS/S3")

  (fetch [this metric statistic start-time end-time period]
    (doseq [bucket (s3/list-buckets)]
      (producer/write topic schema (retrieve (get bucket :name)
                     (namespace this)
                     metric
                     statistic
                     [{:name "BucketName" :value (get bucket :name)} {:name "StorageType" :value "StandardStorage"}]
                     start-time
                     end-time
                     period)))))

(defrecord Sqs [topic schema]
  Metric

  (namespace [this] "AWS/SQS")

  (fetch [this metric statistic start-time end-time period]
    (doseq [queue (get (sqs/list-queues) :queue-urls)]
      (doseq [record (retrieve (last (string/split queue #"/"))
                               (namespace this)
                               metric
                               statistic
                               [{:name "QueueName" :value (last (string/split queue #"/"))}]
                               start-time
                               end-time
                               period)]
        (producer/write topic schema record)))))