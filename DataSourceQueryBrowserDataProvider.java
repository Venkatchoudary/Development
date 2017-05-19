package com.spacetimeinsight.si.uiscreen.dataprovider.admin;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.spacetimeinsight.si.cache.ICacheController;
import com.spacetimeinsight.si.common.EDatabaseVendor;
import com.spacetimeinsight.si.common.services.JsonMarshaller;
import com.spacetimeinsight.si.common.utils.ReflectionUtils;
import com.spacetimeinsight.si.common.utils.StringUtils;
import com.spacetimeinsight.si.constants.CommonConstants;
import com.spacetimeinsight.si.constants.JobConstants;
import com.spacetimeinsight.si.db.DBAccess;
import com.spacetimeinsight.si.db.DBException;
import com.spacetimeinsight.si.db.IDBDataSourceHelper;
import com.spacetimeinsight.si.db.ISQLParser;
import com.spacetimeinsight.si.db.helper.SQLParser;
import com.spacetimeinsight.si.db.helper.oracle.OracleGeometryHelper;
import com.spacetimeinsight.si.db.model.J2eeDataSource;
import com.spacetimeinsight.si.exception.ExceptionKey;
import com.spacetimeinsight.si.exception.SIException;
import com.spacetimeinsight.si.geometry.utils.GeometryParser;
import com.spacetimeinsight.si.model.window.data.uiscreen.DropDownUIComponentData;
import com.spacetimeinsight.si.server.repository.DatabaseTypeRepository;
import com.spacetimeinsight.si.server.repository.J2eeDataSourceRepository;
import com.spacetimeinsight.si.server.repository.SchedulerLogFileRepository;
import com.spacetimeinsight.si.services.ILocaleService;
import com.spacetimeinsight.si.xml.uiscreen.model.DropDownList;
import com.vividsolutions.jts.geom.Geometry;

@Repository
public class DataSourceQueryBrowserDataProvider extends ModelDataProvider {

	@Autowired
	private SchedulerLogFileRepository schedulerLogFileRepository;

	@Autowired
	ICacheController cacheController;

	@Autowired
	private J2eeDataSourceRepository j2eeDataSourceRepository;

	@Autowired
	private DatabaseTypeRepository databaseTypeRepository;

	private DBAccess dbaccess;
	private J2eeDataSource j2eeDataSource;
	private IDBDataSourceHelper dbDataSourceHelper;

	@Autowired
	ILocaleService localeService;
	
	private static final int MAX_DATA_ROW=1000;//SI-21947
	
	@Override
	protected Map<String, Object> getData(Long keyId, Set<String> formColumnNames) {

		Map<String, Object> resultValue = new HashMap<String, Object>();
		if (keyId != null) {

			String query = CommonConstants.EMPTY_STRING;
			String dataSourceName = CommonConstants.EMPTY_STRING;
			String excMsg = CommonConstants.EMPTY_STRING;
			List<Object> rowObjectList = null;
			Map<String, Object> queryRstMap = null;
			ISQLParser sqlParser = new SQLParser(EDatabaseVendor.Generic);
			
			try {
				if (cacheController.retrieve(JobConstants.QUERY_BROWSER_CACHE, JobConstants.QUERY_BROWSER_QUERY) instanceof String) {
					query = (String) cacheController.retrieve(JobConstants.QUERY_BROWSER_CACHE, JobConstants.QUERY_BROWSER_QUERY);
				}
				if (!query.equals(CommonConstants.EMPTY_STRING)) {
					
					if(!sqlParser.isSelectQuery(query)){
						throw new SIException(ExceptionKey.SelectQuerySupported, j2eeDataSource, query);
					}
					
					saveQueryInCacheForQuerybrowser(JobConstants.QUERY_BROWSER_CACHE, JobConstants.QUERY_BROWSER_QUERY, "");

					dataSourceName = (String) cacheController.retrieve(JobConstants.QUERY_BROWSER_CACHE, JobConstants.QUERY_BROWSER_DATASOURCENAME);
					this.j2eeDataSource = j2eeDataSourceRepository.findByDatasourceName(dataSourceName);

					this.dbDataSourceHelper = (IDBDataSourceHelper) ReflectionUtils.getClass(this.j2eeDataSource.getQueryHelperClass()).newInstance();
					this.dbaccess = (DBAccess) this.dbDataSourceHelper.createDBAccess();
					this.dbaccess.setJ2eeDataSource(this.j2eeDataSource);
					ResultSet rs = dbaccess.executeQuery(query,MAX_DATA_ROW);
					ResultSetMetaData rsMetaData = rs.getMetaData();
					int columnCount = rsMetaData.getColumnCount();

					String[] columnNames = new String[columnCount];
					for (int i = 0, j = 1; i < columnCount; i++, j++) {
						columnNames[i] = rsMetaData.getColumnName(j);
					}
					List<Object> row = new ArrayList<Object>();
					Map<String, Object> queryResultMap = null;
					OracleGeometryHelper oracleHelper = new OracleGeometryHelper();

					while (rs.next()) {
						queryResultMap = new HashMap<String, Object>();
						for (int i = 0; i < columnCount; i++) {
							if (columnNames[i] != null) {
								if (rs.getObject(columnNames[i]) instanceof oracle.sql.STRUCT) {
									Geometry ge = oracleHelper.toGeometry(rs.getObject(columnNames[i]));
									queryResultMap.put(columnNames[i],
											GeometryParser.getCoordinatesAsString(GeometryParser.toMultiGeometryCoordinates(ge), false, null));
								} else {
									queryResultMap.put(StringUtils.replaceSpecialCharacter(columnNames[i]),
											rs.getString(columnNames[i]));//SI-23194 Crate: Error on executing count query
								}
							}
						}
						row.add(queryResultMap);
					}
					if(CollectionUtils.isEmpty(row)){//SI-22986
						queryResultMap = new HashMap<String, Object>();
						for (int i = 0; i < columnCount; i++) {							
							if (columnNames[i] != null) {
								queryResultMap.put(columnNames[i],CommonConstants.EMPTY_STRING);
							}
						}
						row.add(queryResultMap);							
						resultValue.put("Count",CommonConstants.ZERO_STR);
						resultValue.put("datasourceResultLabel",JsonMarshaller.getInstance().toJSONString(row));
						return resultValue;
					}					
					resultValue.put("datasourceResultLabel", JsonMarshaller.getInstance().toJSONString(row));
				} else {
					Map<String, Object> queryResultMap = new HashMap<String, Object>();
					List<Object> row = new ArrayList<Object>();
					row.add(queryResultMap);
					resultValue.put("datasourceResultLabel", JsonMarshaller.getInstance().toJSONString(row));
				}
			} catch (SIException se) {
				logger.error(DataSourceQueryBrowserDataProvider.class, "error while getting query result- se", se);				
				excMsg = localeService.getMessage("query.executing.exception", ILocaleService.ADMIN_CATEGORY);
				rowObjectList = new ArrayList<Object>();
				queryRstMap = new HashMap<String, Object>();
				if(se instanceof DBException){
					excMsg=se.getRootCauseException().getMessage();
				}
				if(se.getKey().equals(ExceptionKey.SelectQuerySupported)){
					excMsg = localeService.getMessage(String.valueOf(ExceptionKey.SelectQuerySupported.getException()), ILocaleService.ERROR_CATEGORY);
				}
				queryRstMap.put("Exception",excMsg);//For error message uiScreenWindow.js is dependent "Exception" 
				rowObjectList.add(queryRstMap);
				resultValue.put("datasourceResultLabel",JsonMarshaller.getInstance().toJSONString(rowObjectList) );				
			} catch (Exception e) {				
				logger.error(DataSourceQueryBrowserDataProvider.class, "error while getting query result -e", e);				
				excMsg = localeService.getMessage("query.executing.exception", ILocaleService.ADMIN_CATEGORY);
				rowObjectList = new ArrayList<Object>();
				queryRstMap = new HashMap<String, Object>();
				queryRstMap.put("Exception",excMsg);//For error message uiScreenWindow.js is dependent "Exception" 
				rowObjectList.add(queryRstMap);
				resultValue.put("datasourceResultLabel",JsonMarshaller.getInstance().toJSONString(rowObjectList) );
			} finally {
				if (dbaccess != null) {
					dbaccess.close();
				}
			}
		}
		return resultValue;
	}

	@Override
	protected DropDownUIComponentData getData(DropDownList dropDownList) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		List<J2eeDataSource> j2eeDatasourceList = j2eeDataSourceRepository.findAll();
		for (J2eeDataSource j2eeDataSource : j2eeDatasourceList) {
			HashMap<String, Object> values = new HashMap<String, Object>();
			values = new HashMap<String, Object>();
			values.put("name", j2eeDataSource.getDatasourceName());
			values.put("value", j2eeDataSource.getDatasourceName());
			resultList.add(values);
		}
		return new DropDownUIComponentData(getNameValueData(resultList));
	}

	private void saveQueryInCacheForQuerybrowser(String cacheName, String cacheKey, String query) {
		if (query != null) {
			cacheController.store(cacheName, cacheKey, query);
		}
	}

}