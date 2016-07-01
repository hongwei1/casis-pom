package com.osthus.casis.index;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osthus.casis.index.ioc.ElasticSearchIocModule;

import de.osthus.ambeth.ioc.annotation.Autowired;
import de.osthus.ambeth.testutil.AbstractIocTest;
import de.osthus.ambeth.testutil.TestFrameworkModule;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

@TestFrameworkModule(value = ElasticSearchIocModule.class)
public class ElastichSearchImporterTest extends AbstractIocTest {

	private final String esIndex = "bb";
	private final String tableName = "CASIS_DOCUMENT_RUNS"; //CASIS_DOCUMENT
	private final int esPageSize = 2;
	private final int esPageCount = 1;
	private JestClient clinet;

	@Autowired
	private ElastichSearchImporter elastichSearchImporterService;

	@Autowired
	protected JdbcDao jdbcDaoService;
	private Connection conn;
	
	@Before
	public void before() {
		conn = DBManager.getConn();
		clinet = ElasticSearchUtil.getEsClinet();
	}

	@Test
	public void importFromOralcePartTest() throws Exception {
		
		
		
		elastichSearchImporterService.importFromOralce(esPageSize, esPageCount, esIndex, tableName);
		
		
		
		Thread.sleep(1000);// supend 1 s
		String query1 = "{\r\n" + "  \"size\":1,\r\n" + "  \"query\":\r\n" + "  {\r\n" + "    \"match_all\": {}\r\n"
				+ "  }\r\n" + "}";

		Search search1 = new Search.Builder(query1).addIndex(esIndex).addType("documents").build();

		SearchResult result = clinet.execute(search1);

		// System.out.println(result.getJsonString());
		String allResponse = result.getJsonString();
		// System.out.println(allResponse);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(allResponse);
		JsonNode path = rootNode.path("hits").path("hits");

		String objString = null;
		if (path.isArray()) {
			for (final JsonNode objNode : path) {
				objString = objNode.path("_source").toString();
			}
		}
		
		
		Assert.assertTrue(objString.contains("DOCNO"));
		Assert.assertTrue(objString.contains("DOCUMENT"));
		
		if(objString.contains("CHEM_STRUCTURE_DATA")){
			Assert.assertTrue(objString.contains("CHEM_STRUCTURE_DATA"));
		}
		if(objString.contains("CASIS_DEVSTATUS")){
			Assert.assertTrue(objString.contains("CASIS_DEVSTATUS"));
		}
		if(objString.contains("CASIS_COMPOUND")){
		 Assert.assertTrue(objString.contains("CASIS_COMPOUND"));
		}
		if(objString.contains("CASIS_USE")){
		 Assert.assertTrue(objString.contains("CASIS_USE"));
		}
		if(objString.contains("CASIS_COMPANY")){
		 Assert.assertTrue(objString.contains("CASIS_COMPANY"));
		}
	}

	@Test
	// index the whole database.
	public void importFromOralceTest() throws Exception {
		elastichSearchImporterService.importFromOralce(2, 2, esIndex, tableName);
	}

	@Test
	public void updateDataFromOracleTest() throws Exception {
		System.out.println("top");
		// Thread.sleep(10000000);
		// elastichSearchImporterService.updateDataFromOracle();
	}

	@Test
	public void importFromOralceArrayTest() throws Exception {
		JSONArray resultSetToJson = new JSONArray();
		resultSetToJson.put("DGL1483041");
		resultSetToJson.put("DGL1510302");

		Thread.sleep(1000);// supend 1 s
		String query1 = "{\r\n" + "  \"size\":1,\r\n" + "  \"query\":\r\n" + "  {\r\n" + "    \"match_all\": {}\r\n"
				+ "  }\r\n" + "}";

		Search search1 = new Search.Builder(query1).addIndex(esIndex).addType("documents").build();

		SearchResult result = clinet.execute(search1);

		// System.out.println(result.getJsonString());
		String allResponse = result.getJsonString();
		// System.out.println(allResponse);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(allResponse);
		JsonNode path = rootNode.path("hits").path("hits");

		String objString = null;
		if (path.isArray()) {
			for (final JsonNode objNode : path) {
				objString = objNode.path("_source").toString();
			}
		}

		Assert.assertTrue(objString.contains("CHEM_STRUCTURE_DATA"));
		Assert.assertTrue(objString.contains("DOCUMENT"));
		Assert.assertTrue(objString.contains("CASIS_DEVSTATUS"));
		Assert.assertTrue(objString.contains("DOCNO"));
		Assert.assertTrue(objString.contains("CASIS_COMPOUND"));
		Assert.assertTrue(objString.contains("CASIS_USE"));
		Assert.assertTrue(objString.contains("CASIS_COMPANY"));
	}

	@Test
	public void importFromOralceUpdateEsTest() throws Exception {
		java.util.Date date = new java.util.Date();
		long t = date.getTime();
		// case
		// 1:if(lastHourUpdate.getLastCasisIndexTimestamp().getTime()<=lastHourUpdate.getLastIngestedRecordTs().getTime())
		Timestamp endTs = new Timestamp(t);
		Timestamp lastCasisIndexTimestamp = new Timestamp(t);
		String sqlInsert = "INSERT INTO CASIS2_BG_INGEST_RUNS2 (START_TS,END_TS,LAST_INGESTED_RECORD_TS,LOADING_PROCESS_ACTIVE) VALUES (?,?,?,?) ";
		String loadingProcessActive = null;
		Timestamp lastIngestedRecordTs = null;
		Timestamp startTs = null;

		String loadingProcessActiveTest = "N";
		Timestamp lastIngestedRecordTsTest = new Timestamp(t);
		Timestamp startTsTest = new Timestamp(t);

		LastHourState lastHourUpdate = new LastHourState();

		lastHourUpdate.setEndTs(endTs);
		lastHourUpdate.setLastIngestedRecordTs(lastIngestedRecordTsTest);
		lastHourUpdate.setLoadingProcessActive(loadingProcessActiveTest);

		lastHourUpdate.setLastCasisIndexTimestamp(lastCasisIndexTimestamp);
		elastichSearchImporterService.importFromOralceUpdateEs(conn, lastHourUpdate, startTsTest);

		// check results
		String sqlCheckLastRunTable = "select * from CASIS2_BG_INGEST_RUNS2 where START_TS = ?";
		try (PreparedStatement ps = conn.prepareStatement(sqlCheckLastRunTable);) {
			ps.setTimestamp(1, startTsTest);
			try (ResultSet resultSet = ps.executeQuery();) {
				while (resultSet.next()) {
					startTs = resultSet.getTimestamp("START_TS");
					lastIngestedRecordTs = resultSet.getTimestamp("LAST_INGESTED_RECORD_TS");
					loadingProcessActive = resultSet.getString("LOADING_PROCESS_ACTIVE");
				}
			}
		}

		Assert.assertEquals(startTsTest, startTs);
		Assert.assertEquals(lastIngestedRecordTsTest, lastIngestedRecordTs);
		Assert.assertEquals("N", loadingProcessActive);

		// delete the database
		String sqlDeleteTestRow = "DELETE CASIS2_BG_INGEST_RUNS2 where START_TS=?";
		try (PreparedStatement ps = conn.prepareStatement(sqlDeleteTestRow);) {
			ps.setTimestamp(1, startTs);
			try (ResultSet resultSet = ps.executeQuery();) {

			}
		}

		// case2 lastHourUpdate.getLastCasisIndexTimestamp().getTime()>
		// lastHourUpdate.getLastIngestedRecordTs().getTime()
		// && (lastHourUpdate.getLoadingProcessActive().equals("Y"))

		endTs = new Timestamp(t);
		sqlInsert = "INSERT INTO CASIS2_BG_INGEST_RUNS2 (START_TS,END_TS,LAST_INGESTED_RECORD_TS,LOADING_PROCESS_ACTIVE) VALUES (?,?,?,?) ";
		loadingProcessActive = null;
		lastIngestedRecordTs = null;
		startTs = null;

		loadingProcessActiveTest = "Y";
		lastIngestedRecordTsTest = new Timestamp(t);
		startTsTest = new Timestamp(t);

		lastHourUpdate = new LastHourState();

		lastHourUpdate.setEndTs(endTs);
		lastHourUpdate.setLastIngestedRecordTs(lastIngestedRecordTsTest);
		lastHourUpdate.setLoadingProcessActive(loadingProcessActiveTest);
		t = date.getTime();
		lastCasisIndexTimestamp = new Timestamp(t + 22);

		lastHourUpdate.setLastCasisIndexTimestamp(lastCasisIndexTimestamp);
		elastichSearchImporterService.importFromOralceUpdateEs(conn, lastHourUpdate, startTsTest);

		// check results
		sqlCheckLastRunTable = "select * from CASIS2_BG_INGEST_RUNS2 where START_TS = ?";
		try (PreparedStatement ps = conn.prepareStatement(sqlCheckLastRunTable);) {
			ps.setTimestamp(1, startTsTest);
			try (ResultSet resultSet = ps.executeQuery();) {
				while (resultSet.next()) {
					startTs = resultSet.getTimestamp("START_TS");
					lastIngestedRecordTs = resultSet.getTimestamp("LAST_INGESTED_RECORD_TS");
					loadingProcessActive = resultSet.getString("LOADING_PROCESS_ACTIVE");
				}
			}
		}

		Assert.assertEquals(startTsTest, startTs);
		Assert.assertEquals(lastIngestedRecordTsTest, lastIngestedRecordTs);
		Assert.assertEquals("Y", loadingProcessActive);

		// delete the database
		sqlDeleteTestRow = "DELETE CASIS2_BG_INGEST_RUNS2 where START_TS=?";
		try (PreparedStatement ps = conn.prepareStatement(sqlDeleteTestRow);) {
			ps.setTimestamp(1, startTs);
			try (ResultSet resultSet = ps.executeQuery();) {

			}
		}

		// case3 lastHourUpdate.getLastCasisIndexTimestamp().getTime()>
		// lastHourUpdate.getLastIngestedRecordTs().getTime()
		// && (lastHourUpdate.getLoadingProcessActive().equals("Y"))
		// && lastHourUpdate.getLastCasisIndexTimestamp().getTime() >=
		// lastHourUpdate.getEndTs().getTime()

		endTs = new Timestamp(t);
		sqlInsert = "INSERT INTO CASIS2_BG_INGEST_RUNS2 (START_TS,END_TS,LAST_INGESTED_RECORD_TS,LOADING_PROCESS_ACTIVE) VALUES (?,?,?,?) ";
		loadingProcessActive = null;
		lastIngestedRecordTs = null;
		startTs = null;

		loadingProcessActiveTest = "N";
		lastIngestedRecordTsTest = new Timestamp(t);
		startTsTest = new Timestamp(t);

		lastHourUpdate = new LastHourState();

		lastHourUpdate.setEndTs(endTs);
		lastHourUpdate.setLastIngestedRecordTs(lastIngestedRecordTsTest);
		lastHourUpdate.setLoadingProcessActive(loadingProcessActiveTest);
		t = date.getTime();
		lastCasisIndexTimestamp = new Timestamp(t + 22);

		lastHourUpdate.setLastCasisIndexTimestamp(lastCasisIndexTimestamp);
		elastichSearchImporterService.importFromOralceUpdateEs(conn, lastHourUpdate, startTsTest);

		// check results
		sqlCheckLastRunTable = "select * from CASIS2_BG_INGEST_RUNS2 where START_TS = ?";
		try (PreparedStatement ps = conn.prepareStatement(sqlCheckLastRunTable);) {
			ps.setTimestamp(1, startTsTest);
			try (ResultSet resultSet = ps.executeQuery();) {
				while (resultSet.next()) {
					startTs = resultSet.getTimestamp("START_TS");
					lastIngestedRecordTs = resultSet.getTimestamp("LAST_INGESTED_RECORD_TS");
					loadingProcessActive = resultSet.getString("LOADING_PROCESS_ACTIVE");
				}
			}
		}

		Assert.assertEquals(startTsTest, startTs);
		Assert.assertEquals(lastIngestedRecordTsTest, lastIngestedRecordTs);
		Assert.assertEquals("Y", loadingProcessActive);

		// delete the database
		sqlDeleteTestRow = "DELETE CASIS2_BG_INGEST_RUNS2 where START_TS=?";
		try (PreparedStatement ps = conn.prepareStatement(sqlDeleteTestRow);) {
			ps.setTimestamp(1, startTs);
			try (ResultSet resultSet = ps.executeQuery();) {

			}
		}

		// case4 Else of following 
		// lastHourUpdate.getLastCasisIndexTimestamp().getTime()>lastHourUpdate.getLastIngestedRecordTs().getTime()
		// && (lastHourUpdate.getLoadingProcessActive().equals("Y"))
		// && lastHourUpdate.getLastCasisIndexTimestamp().getTime() >= lastHourUpdate.getEndTs().getTime()
		
		//1  prepare data and conditions
		lastIngestedRecordTsTest = new Timestamp(t);
		endTs = new Timestamp(t+3);
		lastCasisIndexTimestamp = new Timestamp(t + 2);
		loadingProcessActiveTest = "N";
		startTsTest = new Timestamp(t);
		lastHourUpdate = new LastHourState();
		lastHourUpdate.setEndTs(endTs);
		lastHourUpdate.setLastIngestedRecordTs(lastIngestedRecordTsTest);
		lastHourUpdate.setLoadingProcessActive(loadingProcessActiveTest);
		lastHourUpdate.setLastCasisIndexTimestamp(lastCasisIndexTimestamp);
		
		//2 run the functions
		elastichSearchImporterService.importFromOralceUpdateEs(conn, lastHourUpdate, startTsTest);

		//3 check results
		sqlCheckLastRunTable = "select * from CASIS2_BG_INGEST_RUNS2 where START_TS = ?";
		try (PreparedStatement ps = conn.prepareStatement(sqlCheckLastRunTable);) {
			ps.setTimestamp(1, startTsTest);
			try (ResultSet resultSet = ps.executeQuery();) {
				while (resultSet.next()) {
					startTs = resultSet.getTimestamp("START_TS");
					lastIngestedRecordTs = resultSet.getTimestamp("LAST_INGESTED_RECORD_TS");
					loadingProcessActive = resultSet.getString("LOADING_PROCESS_ACTIVE");
				}
			}
		}

		Assert.assertEquals(startTsTest, startTs);
		Assert.assertEquals(lastCasisIndexTimestamp, lastIngestedRecordTs);
		Assert.assertEquals("N", loadingProcessActive);

		//4  delete the database
		sqlDeleteTestRow = "DELETE CASIS2_BG_INGEST_RUNS2 where START_TS=?";
		try (PreparedStatement ps = conn.prepareStatement(sqlDeleteTestRow);) {
			ps.setTimestamp(1, startTs);
			try (ResultSet resultSet = ps.executeQuery();) {

			}
		}
	}

	@Test
	public void getXXXXTest() throws Exception {

	}

	@After
	public void after() {
		DBManager.closeConn(conn);
		// client.shutdownClient();
	}
}
