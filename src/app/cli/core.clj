(ns app.cli.core
  (:require
    [environ.core :refer [env]]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.string :as string]
    [clj-time.core :as time]
    [clj-time.format :as format]
    [app.util.consumer :as consumer]
    [app.util.metric :as metric])
  (:gen-class))

(def ecs (metric/->Ecs "aws_metric" "aws_metric"))
(def ec2 (metric/->Ec2 "aws_metric" "aws_metric"))
(def s3 (metric/->S3 "aws_metric" "aws_metric"))
(def sqs (metric/->Sqs "aws_metric" "aws_metric"))

(def cli-options
  "Command line interface options."

  [[nil "--namespace NAMESPACE" "The namespace of the metric."
    :default nil
    :validate [#(contains? #{"AWS/ECS" "AWS/EC2" "AWS/S3" "AWS/SQS"} %)
               (string/join \newline
                            ["Must be:"
                             "\tAWS/ECS"
                             "\tAWS/EC2"
                             "\tAWS/S3"
                             "\tAWS/SQS"])]]

   [nil "--metric METRIC" "The name of the metric."
    :default nil
    :validate [#(contains? #{"CPUUtilization"
                             "MemoryUtilization"
                             "DiskReadBytes"
                             "DiskReadOps"
                             "DiskWriteBytes"
                             "DiskWriteOps"
                             "EBSReadBytes"
                             "EBSReadOps"
                             "EBSWriteBytes"
                             "EBSWriteOps"
                             "NetworkIn"
                             "NetworkOut"
                             "BucketSizeBytes"
                             "NumberOfObjects"
                             "SentMessageSize"
                             "NumberOfEmptyReceives"
                             "ApproximateNumberOfMessagesVisible"
                             "ApproximateNumberOfMessagesNotVisible"
                             "NumberOfMessagesDeleted"
                             "ApproximateNumberOfMessagesDelayed"
                             "NumberOfMessagesSent"
                             "ApproximateAgeOfOldestMessage"
                             "NumberOfMessagesReceived"} %)
               (string/join \newline
                            ["Must be:"
                             "\tCPUUtilization"
                             "\tMemoryUtilization"
                             "\tDiskReadBytes"
                             "\tDiskReadOps"
                             "\tDiskWriteBytes"
                             "\tDiskWriteOps"
                             "\tEBSReadBytes"
                             "\tEBSReadOps"
                             "\tEBSWriteBytes"
                             "\tEBSWriteOps"
                             "\tNetworkIn"
                             "\tNetworkOut"
                             "\tBucketSizeBytes"
                             "\tNumberOfObjects"
                             "\tSentMessageSize"
                             "\tNumberOfEmptyReceives"
                             "\tApproximateNumberOfMessagesVisible"
                             "\tApproximateNumberOfMessagesNotVisible"
                             "\tNumberOfMessagesDeleted"
                             "\tApproximateNumberOfMessagesDelayed"
                             "\tNumberOfMessagesSent"
                             "\tApproximateAgeOfOldestMessage"
                             "\tNumberOfMessagesReceived"])]]

   [nil "--statistic STATISTIC" "The name of the statistic."
    :default "Average"
    :validate [#(contains? #{"SampleCount" "Average" "Sum" "Minimum" "Maximum"} %)
               (string/join \newline
                            ["Must be:"
                             "\tSampleCount"
                             "\tAverage"
                             "\tSum"
                             "\tMinimum"
                             "\tMaximum"])]]

   [nil "--start-time TIME" "The time stamp that determines the first data point to return."
    :default (time/minus (time/now) (time/hours 1))
    :parse-fn #(try
                 (format/parse (format/formatter :date-time-no-ms) %)
                 (catch Exception e))
    :validate [#(instance? org.joda.time.DateTime %) "Must be a valid datetime format."]]

   [nil "--end-time TIME" "The time stamp that determines the last data point to return."
    :default (time/now)
    :parse-fn #(try
                 (format/parse (format/formatter :date-time-no-ms) %)
                 (catch Exception e))
    :validate [#(instance? org.joda.time.DateTime %) "Must be a valid datetime format."]]

   [nil "--period SECONDS" "The granularity, in seconds, of the returned data points."
    :default 3600
    :parse-fn #(try
                 (Integer/parseInt %)
                 (catch Exception e))
    :validate [#(pos? %) "Must be a positive number."]]

   ["-h" "--help" "List all commands and options."]])

(defn- usage [options-summary]
  (string/join \newline ["Usage: flux action [options]"
                         ""
                         "Options:" options-summary
                         ""
                         "Actions:"
                         "  fetch        Fetch aws cloud watch metrics and push them into kafka."
                         "  poll         Consume metrics from kafka."
                         ""]))

(defn- error-msg
  "Output error message."
  [errors]

  (str (string/join \newline errors) \newline))

(defn- exit
  "Output message with a system status."
  [status msg]

  (println msg)
  (System/exit status))

(defn- validate-args
  "Validate arguments."
  [args]

  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors {:exit-message (error-msg errors)}

      (and (= 1 (count arguments))
           (#{"fetch" "poll"} (first arguments))) {:action (first arguments) :options options}

      :else
      {:exit-message (error-msg ["Command not found, try running: --help"])})))

(defmulti metric-namespace str)
(defmethod metric-namespace (metric/namespace ecs) [this] ecs)
(defmethod metric-namespace (metric/namespace ec2) [this] ec2)
(defmethod metric-namespace (metric/namespace s3) [this] s3)
(defmethod metric-namespace (metric/namespace sqs) [this] sqs)
(defmethod metric-namespace :default [this] nil)

(defn -main
  "Run the command line interface."
  [& args]

  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "fetch" (if (not-empty (metric-namespace (get options :namespace)))
                  (if (not-empty (get options :metric))
                    (metric/fetch
                      (metric-namespace (get options :namespace))
                      (get options :metric)
                      (get options :statistic)
                      (get options :start-time)
                      (get options :end-time)
                      (get options :period))
                    (exit 1 (error-msg ["--metric is required."])))
                  (exit 1 (error-msg ["--namespace is required."])))

        "poll" (consumer/poll (fn [x] (prn x)))

        "default" nil))))