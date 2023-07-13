# 3Rivers Developer Interview "Assignment"

## Overview

At the 3rivers project, we are writing spring based kafka streams applications to construct data pipelines
that ingest and transform data from systems of record at the bank. The technologies used on this project are
relatively new, and we are looking to use this assignment to get candidates comfortable with the technology and
get a taste of the project.

We do not expect you to finish this before the interview. Let me repeat...we do *NOT* expect you to finish all of this before the interview.
We will be looking at the progress you have made
and discuss the problem together. Your approach is much more important than the final product.


## Problem Statement

Build Kafka Streams applications based on Spring (using **Gradle** preferably) that ingests data from the Customer Topic,
Balance Topic, and produces to a CustomerBalance topic.


### Schemas

Note that the complete avro schemas are in the avro folder:

Customer Topic
```
{
    "customerId",
    "name",
    "phoneNumber",
    "accountId"
}
```

Balance Topic:
```
{
    "balanceId",
    "accountId",
    "balance",

}
```

Example:

```json
Customer Topic:
customer { 
    "customerId": "a", 
    "name": "mehryar", 
    "phone": "888-888-8888", 
    "accountId": "b"                   
}

Balance Topic: 
balance  { 
    "balanceId": "j", 
    "accountId": "b",
    "balance": 20.23
}


Should yield: 
--------------

CustomerBalance topic: 
CustomerBalance { 
    "accountId": "b", 
    "customerId": "a", 
    "phone": "888-888-8888", 
    "balance": 20.23
}
```


## Steps

1. Install confluent local on your machine: https://docs.confluent.io/current/quickstart/ce-quickstart.html
2. Use this tutorial as a resources to get started with Kafka Streams on Spring https://www.baeldung.com/java-kafka-streams
3. Create topics from control center (@ localhost:9021):
   - Customer: 2 partitions
   - Balance: 2 partitions
   - CustomerBalance: 2 partitions
4. Navigate to each topic, and register the schema by navigating to the "schema" option and pasting
   the contents of each of the avro files in the respective schema. (e.g. set the schema for Balance topic with the
   contents in [balance avro](./avro/balance.avsc)
5. Go ahead and record the `"id"` for each of the schemas you registered by making requests to the schema registry API
    ```
    curl http://localhost:8081/subjects/Customer-value/versions/latest
    curl http://localhost:8081/subjects/Balance-value/versions/latest
    curl http://localhost:8081/subjects/CustomerBalance-value/versions/latest
    ```
   * Note: *you can also get the `id` through the control center at localhost:9021 which is under the `Schema` tab of a topic*
6. Change the sample requests by adjusting the `"id"` and run the shell script to generate data
7. Confirm that the data has produced by checking out control center.
8. Congrats and now you are at a stage to build your streaming application. The application should:
   - consume from the Balance, and Customer topics.
   - Join the events from Balance and Customer by accountId.
      - The key for balances is accountId and for customers it is customerId. So hmmmm...what to do, what to do ;)
   - Produce the joined message to the CustomerBalance topic. Simply map the input fields to the corresponding fields in CustomerBalance

Have fun developing :)


Tips:

- Start off by simply creating a working Streams application that can consume and deserialize data from both topics. I find
  that the most time-consuming part is just the set-up. Now that you have data in the system, you can debug any deserialization
  exceptions. After that, it'll just be coding.




Useful resources:
- https://www.confluent.io/blog/apache-kafka-spring-boot-application/
- https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Join+Semantics
- https://docs.confluent.io/current/streams/developer-guide/write-streams.html
- https://www.baeldung.com/java-kafka-streams
- there's a lot more stuff so feel free to google :) 