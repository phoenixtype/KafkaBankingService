curl --request POST \
  --url http://localhost:8082/topics/CustomerBalance \
  --header 'accept: application/vnd.kafka.v2+json' \
  --header 'content-type: application/vnd.kafka.avro.v2+json' \
  --data '{
    "key_schema": "{\"name\":\"key\",\"type\": \"string\"}",
    "value_schema_id": "3",
    "records": [
        {
            "key" : "1",
            "value": {
                "accountId": "b",
                "customerId": "a",
                "phoneNumber": "888-888-8888",
                "balance": 20.23
            }
        }
    ]
}'
