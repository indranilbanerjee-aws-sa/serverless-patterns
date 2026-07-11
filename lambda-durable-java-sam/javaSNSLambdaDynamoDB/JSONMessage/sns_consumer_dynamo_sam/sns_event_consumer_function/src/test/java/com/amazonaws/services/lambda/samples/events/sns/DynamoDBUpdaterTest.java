package com.amazonaws.services.lambda.samples.events.sns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;


import org.mockito.ArgumentMatchers;

class DynamoDBUpdaterTest {
	
	private static final String snsEventJson = "{\n"
			+ "    \"records\": [\n"
			+ "        {\n"
			+ "            \"sns\": {\n"
			+ "                \"messageAttributes\": {\n"
			+ "                    \"MessageKey\": {\n"
			+ "                        \"type\": \"String\",\n"
			+ "                        \"value\": \"TestKey03-05-29-2023-06-05-49\"\n"
			+ "                    },\n"
			+ "                    \"MessageNumber\": {\n"
			+ "                        \"type\": \"String\",\n"
			+ "                        \"value\": \"89\"\n"
			+ "                    }\n"
			+ "                },\n"
			+ "                \"signingCertUrl\": \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-01d088a6f77103d0fe307c0069e40ed6.pem\",\n"
			+ "                \"messageId\": \"e31e34d1-7349-539d-9b0e-683f306298ac\",\n"
			+ "                \"message\": \"{\\\"firstname\\\":\\\"Tyra\\\",\\\"lastname\\\":\\\"Shields\\\",\\\"company\\\":\\\"\\\\\\\"Assink, Anne H Esq\\\\\\\"\\\",\\\"street\\\":\\\"3 Fort Worth Ave\\\",\\\"city\\\":\\\"Philadelphia\\\",\\\"county\\\":\\\"Philadelphia\\\",\\\"state\\\":\\\"PA\\\",\\\"zip\\\":\\\"19106\\\",\\\"homePhone\\\":\\\"215-255-1641\\\",\\\"cellPhone\\\":\\\"215-228-8264\\\",\\\"email\\\":\\\"tshields@gmail.com\\\",\\\"website\\\":\\\"http://www.assinkannehesq.com\\\"}\",\n"
			+ "                \"subject\": \"Sending Message with Key = TestKey03-05-29-2023-06-05-49 and message number = 89\",\n"
			+ "                \"unsubscribeUrl\": \"https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:664251831272:LambdaSNSTopicDynamoJavaSAM:740325aa-8302-42e6-ab5c-70c2c38633d3\",\n"
			+ "                \"type\": \"Notification\",\n"
			+ "                \"signatureVersion\": \"1\",\n"
			+ "                \"signature\": \"VzYoZ8lESJ+s9sMsy1Xlb8CfV8Y7M8Qk/w18onb2jEd0Q71GI8q1688PGQU3EFHkRtKLB/DYFPCZzZDezhmpQo6iHqSW2yVGgb5nt9x6NcccFOhK7jRl7QEmEkTmVn1AbtmJOL7ya5U+CZDbMKebCJnRdWgRlrtbE06TaklPnv0REHavXJ4c3fPdkDWc8mBiOhRX6+aAVII9D9Zt7zQLl6ROI8xH6RzIe0ZOy7gv9pF3YycAnrjX3ibD+5+7hvFKZPvgnhDansm++MjHa/mON5L08VxTY9lEFiTlQv2L7bS6XihwhWpmk6uQHQWpdURMdZsL2l2WRUNR+IROhvEQqg==\",\n"
			+ "                \"timestamp\": 1685340395498,\n"
			+ "                \"topicArn\": \"arn:aws:sns:us-west-2:664251831272:LambdaSNSTopicDynamoJavaSAM\"\n"
			+ "            },\n"
			+ "            \"eventVersion\": \"1.0\",\n"
			+ "            \"eventSource\": \"aws:sns\",\n"
			+ "            \"eventSubscriptionArn\": \"arn:aws:sns:us-west-2:664251831272:LambdaSNSTopicDynamoJavaSAM:740325aa-8302-42e6-ab5c-70c2c38633d3\"\n"
			+ "        }\n"
			+ "    ]\n"
			+ "}";

	@Test
	void testDynamoDBUpdater() {
		DynamoDBUpdater ddbUpdater = new DynamoDBUpdater("DBTable");
		assertNotNull(ddbUpdater);
		assertEquals(ddbUpdater.dynamoDBTableName, "DBTable");
		assertNotNull(ddbUpdater.client);
		assertNotNull(ddbUpdater.dynamoDB);
		assertNotNull(ddbUpdater.dynamoTable);
	}

	@Test
	void testInsertIntoDynamoDB() {
		ObjectMapper om = new ObjectMapper().registerModule(new JodaModule());
		SNSEvent event = null;
		try {
			event = om.readValue(snsEventJson, SNSEvent.class);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		List<SNSRecord> records = event.getRecords();
		for(SNSRecord msg : records){
			Table dynamoDbTable = mock(Table.class);
		    AmazonDynamoDB client = mock(AmazonDynamoDB.class);
			DynamoDB dynamoDB = mock(DynamoDB.class);
		    PutItemOutcome putoutcome = mock(PutItemOutcome.class);
		    LambdaLogger logger = mock(LambdaLogger.class);
		    DynamoDBUpdater ddbUpdater = new DynamoDBUpdater("DBTable");
		    ddbUpdater.client = client;
		    ddbUpdater.dynamoDB = dynamoDB;
		    ddbUpdater.dynamoTable = dynamoDbTable;
		    Person person = new Person();
			try {
				person = om.readValue(msg.getSNS().getMessage(), Person.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		    when(ddbUpdater.dynamoTable.putItem(ArgumentMatchers.any(Item.class))).thenReturn(putoutcome);
			PutItemOutcome putOutcome = ddbUpdater.insertIntoDynamoDB(msg, person, logger);
			assertNotNull(putOutcome);
		}
	    
	}
}
