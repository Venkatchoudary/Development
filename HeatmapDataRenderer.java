package com.spacetimeinsight.si.businessview.renderer.window.chart;

import java.io.IOException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;

import com.spacetimeinsight.si.businessview.IDataRenderable;
import com.spacetimeinsight.si.businessview.IMetadata;
import com.spacetimeinsight.si.businessview.renderer.window.BaseDataRenderableWindowDataRenderer;
import com.spacetimeinsight.si.businessview.renderer.window.BaseGroupDataRenderableWindowDataRenderer;
import com.spacetimeinsight.si.common.EAggregationType;
import com.spacetimeinsight.si.common.INameValue;
import com.spacetimeinsight.si.common.utils.StringUtils;
import com.spacetimeinsight.si.constants.CommonConstants;
import com.spacetimeinsight.si.dataobject.IDataObject;
import com.spacetimeinsight.si.datasource.IAggregateResult;
import com.spacetimeinsight.si.exception.ExceptionKey;
import com.spacetimeinsight.si.exception.SIException;
import com.spacetimeinsight.si.model.window.config.chart.Axis.EAxisType;
import com.spacetimeinsight.si.model.window.config.chart.ChartColumnSeries;
import com.spacetimeinsight.si.model.window.config.chart.ChartDetails;
import com.spacetimeinsight.si.model.window.config.chart.ChartSeries;
import com.spacetimeinsight.si.model.window.config.chart.XYChartWindow;
import com.spacetimeinsight.si.model.window.config.table.TableColumn;
import com.spacetimeinsight.si.model.window.data.chart.ChartRowData;
import com.spacetimeinsight.si.model.window.data.chart.ChartSeriesData;
import com.spacetimeinsight.si.model.window.data.chart.XYChartData;
import com.spacetimeinsight.si.window.constants.IChartPropertyConstants;

public class HeatmapDataRenderer extends BaseGroupDataRenderableWindowDataRenderer<XYChartWindow, XYChartData> implements IChartPropertyConstants {
	private static final String HEATMAP_PARAM_SWAP_AXIS_DATA = "swapAxisData";
	private static final String X_AXIS_CATEGORY = "X-Axis";
	private static final String Y_AXIS_CATEGORY = "Y-Axis";
	private List<String> xColCategories = new ArrayList<String>();
	private List<String> yColCategories = new ArrayList<String>();
	Map<String, TableColumn> additionalTableColumns = null;
	public HeatmapDataRenderer() {
		// TODO Auto-generated constructor stub
	}
	
	private String[] getSeriesNames() {
		XYChartWindow chartWindow = (XYChartWindow)window;
		ChartSeries chartseries = chartWindow.getChartSeries();
		Map<String, ChartColumnSeries> seriesFields = chartseries.getChartSeriesFields();
		ChartColumnSeries chartSeriesField = null;
		List<String> legendList = null;
		for (Map.Entry<String, ChartColumnSeries> entry : seriesFields.entrySet()) {
			chartSeriesField = entry.getValue();
			if (legendList == null) {
				legendList = new ArrayList<String>();
			}
			legendList.add(chartSeriesField.getSeriesName());
		}
		return legendList.toArray(new String[0]);
	}
	
	private String[] getSeriesNamesForRow(String seriesFieldType) {
		List<String> legendList = null;
		try{
		XYChartWindow chartWindow = (XYChartWindow)window;
		Map <String, Object> seriesNamesMap = new LinkedHashMap<>();
		Iterator <IDataObject> iterator =getDataSourceIterator();
		String seriesName="";
		IDataObject object=null;
		while(iterator.hasNext()){
			object=	iterator.next();
			seriesName=object.getFieldValue(seriesFieldType).toString();
			seriesNamesMap.put(seriesName,seriesName);
			
		
		}
		for (Map.Entry<String, Object> entry : seriesNamesMap.entrySet()) {
			if (legendList == null) {
				legendList = new ArrayList<String>();
			}
			
			legendList.add(entry.getKey());
		}
		
		}
		finally{
			closeDataSource();
		}
		return legendList.toArray(new String[0]);
	}
	
	
	
	private String getXField() {
		XYChartWindow chartWindow = (XYChartWindow)window;
		ChartSeries chartseries = chartWindow.getChartSeries();
		Map<String, ChartColumnSeries> seriesFields = chartseries.getChartSeriesFields();
		ChartColumnSeries chartSeriesField = null;
		String xField = null;
		for (Map.Entry<String, ChartColumnSeries> entry : seriesFields.entrySet()) {
			chartSeriesField = entry.getValue();
			xField =chartSeriesField.getXField();
			 break;
			
		}
		return xField;
	}


	private XYChartData buildHeatmapData() {
		XYChartData chartData = new XYChartData();
	    try{
		String[] seriesNames= getSeriesNames();
		boolean swapAxisData = Boolean.valueOf((String)requestModel.getDataParameters().get((HEATMAP_PARAM_SWAP_AXIS_DATA)));
		XYChartWindow chartWindow = (XYChartWindow)window;
		ChartSeries chartseries = chartWindow.getChartSeries();
		String xField = getXField();
		EAxisType xaxisType= ((EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE));
		EAxisType yaxisType= ((EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE));
		String xAxisType = xaxisType.toString();
		String yAxisType = yaxisType.toString();
		List<String> xAxisCatogeriesFromData = new ArrayList<String>();
		additionalTableColumns = window.getChartTable().getColumns();
		List<String> yAxisCatogeriesSeriesNames = new ArrayList<String>();
		int noOfSeries = seriesNames == null ? 0 : seriesNames.length;
		List<Object[]> valuesList = new ArrayList<Object[]>();
		populateMinMax(seriesNames,chartWindow);
		Iterator <IDataObject> iterator =getDataSourceIterator();
		List<IDataObject> dataObjectList = new ArrayList<IDataObject>();
		for (int i = 0; i < noOfSeries; i++) {
			yAxisCatogeriesSeriesNames.add(seriesNames[i]);
		}
		if (noOfSeries > 0) {
			//Object xColumnValue = null;

			List<Object> rowDataList = new ArrayList<Object>();
			Map<String, Object> attributeMap = null;
			List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
			
			Object[] values = null;
			Object[][] toolTips2d = null;
			IDataObject dataObject = null;
			List<Object[][]> toolTipList = new ArrayList<Object[][]>();
			String[] toolTipAttributes = null;
			ChartDetails chartDetails = chartWindow.getChartDetails();
			if (chartDetails != null) {
				toolTipAttributes = chartDetails.getTooltipAttributes();
			}
			int noOfTooltipAttributes = toolTipAttributes == null ? 0 : toolTipAttributes.length;
			boolean isUseRawData = (noOfTooltipAttributes <= 0);
		    
				while (iterator.hasNext()) {
					dataObject = iterator.next();
					if ((IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(xAxisType)||IChartPropertyConstants.AXIS_TYPE_DATE.equals(xAxisType))
							&& dataObject.getFieldValue(xField)!=null) {
						Object obj=dataObject.getFieldValue(xField);
						if(obj instanceof Date){
							String dateFormat=(String)window.getXAxis().get(IChartPropertyConstants.AXIS_TYPE_DATE_FORMAT);
							//if(dateFormat!=null){
							//	obj = new SimpleDateFormat(dateFormat).format(obj);
						//	}else{
								obj = new SimpleDateFormat("MMM,dd,yyyy").format(obj);
							//}
						}
						xAxisCatogeriesFromData.add(String.valueOf(obj));
					}
					values = new Object[noOfSeries];
					toolTips2d = new Object[noOfSeries][];
					
					for (int j = 0; j < noOfSeries; j++) {
						 
						values[j] = dataObject.getFieldValue(seriesNames[j]);
						
						toolTips2d[j] = new Object[noOfTooltipAttributes];
						for (int k = 0; k < noOfTooltipAttributes; k++) {
							toolTips2d[j][k] = dataObject.getFieldValue(toolTipAttributes[k]);
						}
					}
					toolTipList.add(toolTips2d);
					dataObjectList.add(dataObject);
					valuesList.add(values);
				}
				

			List<ChartRowData> chartRowDataList = null;

			int count = valuesList == null ? 0 : valuesList.size();
			if (count > 0) {
				String[] xAxisCategories = null;
				String[] yAxisCategories = null;
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(xAxisType)) {
					List <String> xAxisCategorieslist=	(ArrayList)chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_CATEGORIES);
					if(xAxisCategorieslist!=null){
						xAxisCategories = new String[xAxisCategorieslist.size()];
					  xAxisCategories = (String[]) (String[]) xAxisCategorieslist.toArray(xAxisCategories);
					}
				}
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(yAxisType)) {
				List <String> yaxiscategerylist=	(ArrayList)chartWindow.getYAxis().get(IChartPropertyConstants.PROPERTY_NAME_CATEGORIES);
				if(yaxiscategerylist!=null){
					yAxisCategories = new String[yaxiscategerylist.size()];
					yAxisCategories = (String[]) yaxiscategerylist.toArray(yAxisCategories);
				}
				}
				if (xAxisCategories!=null) {
					xColCategories = Arrays.asList(xAxisCategories);
				} else {
					xColCategories = xAxisCatogeriesFromData;
				}
				if (yAxisCategories!=null) {
					yColCategories = Arrays.asList(yAxisCategories);
				} else {
					yColCategories = yAxisCatogeriesSeriesNames;
				}
				if (isUseRawData) {
					if (!swapAxisData) {
						for (int i = 0; i < count; i++) {
							for (int j = 0; j < noOfSeries; j++) {
							//	 if(xColCategories!=null&&xColCategories.size()>0){
								//	 values = new Object[] { (String)xColCategories.get(i), j, valuesList.get(i)[j] };
								 //}else{
								Object obj = valuesList.get(i)[j];
								/*if(obj instanceof Date){
									obj = ((Date)obj).getTime();
								}*/
									 values = new Object[] {i, j, obj };
								// }
								rowDataList.add(values);
								attributeMap =buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
								if(additionalTableColumns != null){
									 addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(i));
								
								 }

								dataList.add(attributeMap);
							}
						}
					} else {
						for (int i = 0; i < noOfSeries; i++) {
							for (int j = 0; j < count; j++) {
								// if(xColCategories!=null&&xColCategories.size()>0){
									// values = new Object[] { (String)xColCategories.get(i), j, valuesList.get(j)[j] };
								// }else{
								Object obj =valuesList.get(j)[i];
								/*if(obj instanceof Date){
									obj = ((Date)obj).getTime();
								}*/
									 values = new Object[] {i, j,obj };
								// }
								rowDataList.add(values);
								attributeMap =buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
								if(additionalTableColumns != null){
									 addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(j));
								 }
								dataList.add(attributeMap);
							}
						}
					}
				} else {
					ChartRowData rowData = null;
					Object tooltipAttrValue = null;
					chartRowDataList = new ArrayList<ChartRowData>();
					int counter1 = !swapAxisData ? count : noOfSeries;
					int counter2 = !swapAxisData ? noOfSeries : count;
					Object[][] toolTipObject = null;
					for (int i = 0; i < counter1; i++) {
						toolTipObject = toolTipList.get(i);
						for (int j = 0; j < counter2; j++) {
							rowData = new ChartRowData();
							//if(xColCategories!=null&&xColCategories.size()>0){
								//rowData.setX((String)xColCategories.get(i));
							//}else{
								rowData.setX(i);
							//}
							rowData.setY(j);
							if (!swapAxisData) {
								rowData.setValue(valuesList.get(i)[j]);
								attributeMap = buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
							} else {
								rowData.setValue(valuesList.get(j)[i]);
								attributeMap = buildHeatmapTableData(xAxisCategories, yAxisCategories, j, i, seriesNames, valuesList,xColCategories,yColCategories);
							} 
							if(additionalTableColumns != null){
								if (!swapAxisData){
								addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(i));
								}else{
									addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(j));
								}
							 }

							for (int k = 0; k < noOfTooltipAttributes; k++) {
								if (toolTipObject[j] == null) {
									tooltipAttrValue = "";
								} else {
									tooltipAttrValue = toolTipObject[j][k];
								}
								String attributeName = getDisplayName(window,toolTipAttributes[k]);
								if(tooltipAttrValue!=null) {
									rowData.addAdditionalProperty(attributeName, tooltipAttrValue);
									attributeMap.put(attributeName, tooltipAttrValue);
								}
							}
							chartRowDataList.add(rowData);
							dataList.add(attributeMap);
						}
					}
				}

			List<ChartSeriesData> chartSeriesList = new ArrayList<ChartSeriesData>();
			ChartSeriesData seriesData=null;
             for (int i=0;i<seriesNames.length;i++){
				 seriesData = new ChartSeriesData();
				seriesData.setId(seriesNames[i]);
				seriesData.setName(seriesNames[i]);
				if (isUseRawData) {
					seriesData.setRawData(rowDataList);
				} else {
					seriesData.setData(chartRowDataList);
				}
				chartSeriesList.add(seriesData);
             }	
				if (xColCategories.size() > 0 && yColCategories.size() > 0) {
					chartData.setChartTableData(buildTableData(dataList, xColCategories, yColCategories));
				} else {
					chartData.setChartTableData(dataList);
				}
				if(xColCategories.size()>0){
					xAxisCategories = new String[xColCategories.size()];
					chartData.setXAxisCategories(xColCategories.toArray(xAxisCategories));
				}	
				if(yColCategories.size()>0){
					yAxisCategories = new String[yColCategories.size()];
					chartData.setYAxisCategories(yColCategories.toArray(yAxisCategories));
				}	
				
				chartData.setChartSeriesList(chartSeriesList);
				
				}
		}
	    }catch(Exception e){
	    	if (e instanceof SIException) {
				throw (SIException) e;
			}
			throw new SIException(ExceptionKey.WindowDataException, e);
	    
	    }finally{
	    	closeDataSource();
	    }
		return chartData;
	}
	
	
	private XYChartData buildHeatmapDataForRowAseries(String seriesFieldType, String colorField) {
		XYChartData chartData = new XYChartData();
	    try{
		String[] seriesNames= getSeriesNamesForRow(seriesFieldType);
		boolean swapAxisData = Boolean.valueOf((String)requestModel.getDataParameters().get((HEATMAP_PARAM_SWAP_AXIS_DATA)));
		XYChartWindow chartWindow = (XYChartWindow)window;
		ChartSeries chartseries = chartWindow.getChartSeries();
		String xField = getXField();
		EAxisType xaxisType= ((EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE));
		EAxisType yaxisType= ((EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE));
		String xAxisType = xaxisType.toString();
		String yAxisType = yaxisType.toString();
		List<String> xAxisCatogeriesFromData = new ArrayList<String>();
		additionalTableColumns = window.getChartTable().getColumns();
		List<String> yAxisCatogeriesSeriesNames = new ArrayList<String>();
		int noOfSeries = seriesNames == null ? 0 : seriesNames.length;
		List<Object[]> valuesList = new ArrayList<Object[]>();
		//populateMinMax(seriesNames,chartWindow);
		Iterator <IDataObject> iterator =getDataSourceIterator();
		List<IDataObject> dataObjectList = new ArrayList<IDataObject>();
		for (int i = 0; i < noOfSeries; i++) {
			yAxisCatogeriesSeriesNames.add(seriesNames[i]);
		}
		if (noOfSeries > 0) {
			//Object xColumnValue = null;

			List<Object> rowDataList = new ArrayList<Object>();
			Map<String, Object> attributeMap = null;
			List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
			
			Object[] values = null;
			Object[][] toolTips2d = null;
			IDataObject dataObject = null;
			List<Object[][]> toolTipList = new ArrayList<Object[][]>();
			String[] toolTipAttributes = null;
			ChartDetails chartDetails = chartWindow.getChartDetails();
			if (chartDetails != null) {
				toolTipAttributes = chartDetails.getTooltipAttributes();
			}
			int noOfTooltipAttributes = toolTipAttributes == null ? 0 : toolTipAttributes.length;
			boolean isUseRawData = (noOfTooltipAttributes <= 0);
		    
				while (iterator.hasNext()) {
					dataObject = iterator.next();
					if ((IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(xAxisType)||IChartPropertyConstants.AXIS_TYPE_DATE.equals(xAxisType))
							&& dataObject.getFieldValue(xField)!=null) {
						Object obj=dataObject.getFieldValue(xField);
						if(obj instanceof Date){
							String dateFormat=(String)window.getXAxis().get(IChartPropertyConstants.AXIS_TYPE_DATE_FORMAT);
							//if(dateFormat!=null){
							//	obj = new SimpleDateFormat(dateFormat).format(obj);
						//	}else{
								obj = new SimpleDateFormat("MMM,dd,yyyy").format(obj);
							//}
						}
						xAxisCatogeriesFromData.add(String.valueOf(obj));
					}
					values = new Object[noOfSeries];
					toolTips2d = new Object[noOfSeries][];
					
					for (int j = 0; j < noOfSeries; j++) {
						 
						values[j] = dataObject.getFieldValue(colorField);
						
						toolTips2d[j] = new Object[noOfTooltipAttributes];
						for (int k = 0; k < noOfTooltipAttributes; k++) {
							toolTips2d[j][k] = dataObject.getFieldValue(toolTipAttributes[k]);
						}
					}
					toolTipList.add(toolTips2d);
					dataObjectList.add(dataObject);
					valuesList.add(values);
				}
				

			List<ChartRowData> chartRowDataList = null;

			int count = valuesList == null ? 0 : valuesList.size();
			if (count > 0) {
				String[] xAxisCategories = null;
				String[] yAxisCategories = null;
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(xAxisType)) {
					List <String> xAxisCategorieslist=	(ArrayList)chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_CATEGORIES);
					if(xAxisCategorieslist!=null){
						xAxisCategories = new String[xAxisCategorieslist.size()];
					  xAxisCategories = (String[]) (String[]) xAxisCategorieslist.toArray(xAxisCategories);
					}
				}
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(yAxisType)) {
				List <String> yaxiscategerylist=	(ArrayList)chartWindow.getYAxis().get(IChartPropertyConstants.PROPERTY_NAME_CATEGORIES);
				if(yaxiscategerylist!=null){
					yAxisCategories = new String[yaxiscategerylist.size()];
					yAxisCategories = (String[]) yaxiscategerylist.toArray(yAxisCategories);
				}
				}
				if (xAxisCategories!=null) {
					xColCategories = Arrays.asList(xAxisCategories);
				} else {
					xColCategories = xAxisCatogeriesFromData;
				}
				if (yAxisCategories!=null) {
					yColCategories = Arrays.asList(yAxisCategories);
				} else {
					yColCategories = yAxisCatogeriesSeriesNames;
				}
				if (isUseRawData) {
					if (!swapAxisData) {
						for (int i = 0; i < count; i++) {
							for (int j = 0; j < noOfSeries; j++) {
							//	 if(xColCategories!=null&&xColCategories.size()>0){
								//	 values = new Object[] { (String)xColCategories.get(i), j, valuesList.get(i)[j] };
								 //}else{
								Object obj = valuesList.get(i)[j];
								/*if(obj instanceof Date){
									obj = ((Date)obj).getTime();
								}*/
									 values = new Object[] {i, j, obj };
								// }
								rowDataList.add(values);
								attributeMap =buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
								if(additionalTableColumns != null){
									 addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(i));
								
								 }

								dataList.add(attributeMap);
							}
						}
					} else {
						for (int i = 0; i < noOfSeries; i++) {
							for (int j = 0; j < count; j++) {
								// if(xColCategories!=null&&xColCategories.size()>0){
									// values = new Object[] { (String)xColCategories.get(i), j, valuesList.get(j)[j] };
								// }else{
								Object obj =valuesList.get(j)[i];
								/*if(obj instanceof Date){
									obj = ((Date)obj).getTime();
								}*/
									 values = new Object[] {i, j,obj };
								// }
								rowDataList.add(values);
								attributeMap =buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
								if(additionalTableColumns != null){
									 addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(j));
								 }
								dataList.add(attributeMap);
							}
						}
					}
				} else {
					ChartRowData rowData = null;
					Object tooltipAttrValue = null;
					chartRowDataList = new ArrayList<ChartRowData>();
					int counter1 = !swapAxisData ? count : noOfSeries;
					int counter2 = !swapAxisData ? noOfSeries : count;
					Object[][] toolTipObject = null;
					for (int i = 0; i < counter1; i++) {
						toolTipObject = toolTipList.get(i);
						for (int j = 0; j < counter2; j++) {
							rowData = new ChartRowData();
							//if(xColCategories!=null&&xColCategories.size()>0){
								//rowData.setX((String)xColCategories.get(i));
							//}else{
								rowData.setX(i);
							//}
							rowData.setY(j);
							if (!swapAxisData) {
								rowData.setValue(valuesList.get(i)[j]);
								attributeMap = buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
							} else {
								rowData.setValue(valuesList.get(j)[i]);
								attributeMap = buildHeatmapTableData(xAxisCategories, yAxisCategories, j, i, seriesNames, valuesList,xColCategories,yColCategories);
							} 
							if(additionalTableColumns != null){
								if (!swapAxisData){
								addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(i));
								}else{
									addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(j));
								}
							 }

							for (int k = 0; k < noOfTooltipAttributes; k++) {
								if (toolTipObject[j] == null) {
									tooltipAttrValue = "";
								} else {
									tooltipAttrValue = toolTipObject[j][k];
								}
								String attributeName = getDisplayName(window,toolTipAttributes[k]);
								if(tooltipAttrValue!=null) {
									rowData.addAdditionalProperty(attributeName, tooltipAttrValue);
									attributeMap.put(attributeName, tooltipAttrValue);
								}
							}
							chartRowDataList.add(rowData);
							dataList.add(attributeMap);
						}
					}
				}

			List<ChartSeriesData> chartSeriesList = new ArrayList<ChartSeriesData>();
			ChartSeriesData seriesData=null;
             for (int i=0;i<seriesNames.length;i++){
				 seriesData = new ChartSeriesData();
				seriesData.setId(seriesNames[i]);
				seriesData.setName(seriesNames[i]);
				if (isUseRawData) {
					seriesData.setRawData(rowDataList);
				} else {
					seriesData.setData(chartRowDataList);
				}
				chartSeriesList.add(seriesData);
             }	
				if (xColCategories.size() > 0 && yColCategories.size() > 0) {
					chartData.setChartTableData(buildTableData(dataList, xColCategories, yColCategories));
				} else {
					chartData.setChartTableData(dataList);
				}
				if(xColCategories.size()>0){
					xAxisCategories = new String[xColCategories.size()];
					chartData.setXAxisCategories(xColCategories.toArray(xAxisCategories));
				}	
				if(yColCategories.size()>0){
					yAxisCategories = new String[yColCategories.size()];
					chartData.setYAxisCategories(yColCategories.toArray(yAxisCategories));
				}	
				
				chartData.setChartSeriesList(chartSeriesList);
				
				}
		}
	    }catch(Exception e){
	    	if (e instanceof SIException) {
				throw (SIException) e;
			}
			throw new SIException(ExceptionKey.WindowDataException, e);
	    
	    }finally{
	    	closeDataSource();
	    }
		return chartData;
	}

	
private List<Map<String, Object>> getExportHeatMapData() {
		
	    
		String[] seriesNames= getSeriesNames();
		boolean swapAxisData = Boolean.valueOf((String)requestModel.getDataParameters().get((HEATMAP_PARAM_SWAP_AXIS_DATA)));
		XYChartWindow chartWindow = (XYChartWindow)window;
		String xField = getXField();
		EAxisType xaxisType= ((EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE));
		String xAxisType = xaxisType.toString();
		List<String> xAxisCatogeriesFromData = new ArrayList<String>();
		initailizeDataSource(window);
		additionalTableColumns = window.getChartTable().getColumns();
		List<String> yAxisCatogeriesSeriesNames = new ArrayList<String>();
		int noOfSeries = seriesNames == null ? 0 : seriesNames.length;
		List<Object[]> valuesList = new ArrayList<Object[]>();
	
		Iterator <IDataObject> iterator =getDataSourceIterator();
		List<IDataObject> dataObjectList = new ArrayList<IDataObject>();
		for (int i = 0; i < noOfSeries; i++) {
			yAxisCatogeriesSeriesNames.add(seriesNames[i]);
		}
		if (noOfSeries > 0) {
			Map<String, Object> attributeMap = null;
			List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
			
			Object[] values = null;
			Object[][] toolTips2d = null;
			IDataObject dataObject = null;
			List<Object[][]> toolTipList = new ArrayList<Object[][]>();
			String[] toolTipAttributes = null;
			ChartDetails chartDetails = chartWindow.getChartDetails();
			if (chartDetails != null) {
				toolTipAttributes = chartDetails.getTooltipAttributes();
			}
			int noOfTooltipAttributes = toolTipAttributes == null ? 0 : toolTipAttributes.length;
			boolean isUseRawData = (noOfTooltipAttributes <= 0);
		    
				while (iterator.hasNext()) {
					dataObject = iterator.next();
					if ((IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(xAxisType)||IChartPropertyConstants.AXIS_TYPE_DATE.equals(xAxisType))
							&& dataObject.getFieldValue(xField)!=null) {
						Object obj=dataObject.getFieldValue(xField);
						if(obj instanceof Date){
							String dateFormat=(String)window.getXAxis().get(IChartPropertyConstants.AXIS_TYPE_DATE_FORMAT);
							//if(dateFormat!=null){
							//	obj = new SimpleDateFormat(dateFormat).format(obj);
						//	}else{
								obj = new SimpleDateFormat("MMM,dd,yyyy").format(obj);
							//}
						}
						xAxisCatogeriesFromData.add(String.valueOf(obj));
					}
					values = new Object[noOfSeries];
					toolTips2d = new Object[noOfSeries][];
					
					for (int j = 0; j < noOfSeries; j++) {
						values[j] = dataObject.getFieldValue(seriesNames[j]);
						
						toolTips2d[j] = new Object[noOfTooltipAttributes];
						for (int k = 0; k < noOfTooltipAttributes; k++) {
							toolTips2d[j][k] = dataObject.getFieldValue(toolTipAttributes[k]);
						}
					}
					toolTipList.add(toolTips2d);
					dataObjectList.add(dataObject);
					valuesList.add(values);
				}
				


			int count = valuesList == null ? 0 : valuesList.size();
			if (count > 0) {
				String[] xAxisCategories = null;
				String[] yAxisCategories = null;
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(xAxisType)) {
					List <String> xAxisCategorieslist=	(ArrayList)chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_CATEGORIES);
					if(xAxisCategorieslist!=null){
						xAxisCategories = new String[xAxisCategorieslist.size()];
					  xAxisCategories = (String[]) (String[]) xAxisCategorieslist.toArray(xAxisCategories);
					}
				}
			
				if (isUseRawData) {
					if (!swapAxisData) {
						for (int i = 0; i < count; i++) {
							for (int j = 0; j < noOfSeries; j++) {
								attributeMap =buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
								populateMetaData((IDataObject)dataObjectList.get(i), attributeMap, window.getMetadata());
								if(additionalTableColumns != null){
									 addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(i));
								
								 }
								dataList.add(attributeMap);
							}
						}
					} else {
						for (int i = 0; i < noOfSeries; i++) {
							for (int j = 0; j < count; j++) {
								attributeMap =buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
								populateMetaData((IDataObject)dataObjectList.get(j), attributeMap, window.getMetadata());
								if(additionalTableColumns != null){
									 addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(j));
								 }
								dataList.add(attributeMap);
							}
						}
					}
				} else {
					ChartRowData rowData = null;
					Object tooltipAttrValue = null;
					int counter1 = !swapAxisData ? count : noOfSeries;
					int counter2 = !swapAxisData ? noOfSeries : count;
					Object[][] toolTipObject = null;
					for (int i = 0; i < counter1; i++) {
						toolTipObject = toolTipList.get(i);
						for (int j = 0; j < counter2; j++) {
							
							if (!swapAxisData) {
								attributeMap = buildHeatmapTableData(xAxisCategories, yAxisCategories, i, j, seriesNames, valuesList,xColCategories,yColCategories);
							} else {
								attributeMap = buildHeatmapTableData(xAxisCategories, yAxisCategories, j, i, seriesNames, valuesList,xColCategories,yColCategories);
							} 
							populateMetaData((IDataObject)dataObjectList.get(j), attributeMap, window.getMetadata());
							if(additionalTableColumns != null){
								if (!swapAxisData){
								addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(i));
								}else{
									addAdditionalTableColumns(attributeMap,(IDataObject)dataObjectList.get(j));
								}
							 }

							for (int k = 0; k < noOfTooltipAttributes; k++) {
								if (toolTipObject[j] == null) {
									tooltipAttrValue = "";
								} else {
									tooltipAttrValue = toolTipObject[j][k];
								}
								String attributeName = getDisplayName(window,toolTipAttributes[k]);
								if(tooltipAttrValue!=null) {
									attributeMap.put(attributeName, tooltipAttrValue);
								}
							}
							dataList.add(attributeMap);
						}
					}
				}

			
				
				
				}
			return dataList;
		}
		return null;
	}	
	
@Override
protected void toCSV(CSVWriter csvWriter) throws IOException {
	try {
		String selectedColumns = (String) this.requestModel.getDataParameters().get("selectedColumns");
		String sortedColumns = (String) this.requestModel.getDataParameters().get("sortedColumns");
		String sortedIndex = (String) this.requestModel.getDataParameters().get("sortedIndex");
		String isAscending = (String) this.requestModel.getDataParameters().get("isAscending");
		String[] selectedColumnArray = selectedColumns != null ? selectedColumns.split(CommonConstants.COMMA_STRING) : null;
		String[] sortedColumnsArray = sortedColumns != null&&(!CommonConstants.EMPTY_STRING.endsWith(sortedColumns))? sortedColumns.split(CommonConstants.COMMA_STRING) : null;
		String[] sortedIndexArray = sortedIndex != null&&(!CommonConstants.EMPTY_STRING.endsWith(sortedIndex))? sortedIndex.split(CommonConstants.COMMA_STRING) : null;
		String[] isAscendingArray = isAscending != null&& (!CommonConstants.EMPTY_STRING.endsWith(isAscending))? isAscending.split(CommonConstants.COMMA_STRING) : null;
		
		if (!(window instanceof IDataRenderable)) {
			return;
		}
		String[] fieldsToExport = getFieldsToExport(selectedColumnArray);
		String[] rowDataExport = new String[fieldsToExport.length+1];
		
        String[] seriesNames = getSeriesNames(); 
		IDataRenderable dataRenderable = (IDataRenderable) window;
		
		String[] displayNames = getDisplayNames(dataRenderable, fieldsToExport);
		String [] fieldsToDisplay =  new String[displayNames.length+1];
		fieldsToDisplay[0]=Y_AXIS_CATEGORY;
		for(int i=0;i<displayNames.length;i++){
			fieldsToDisplay[i+1]=displayNames[i];
		}
		csvWriter.writeNext(fieldsToDisplay);
		if (fieldsToExport == null || fieldsToExport.length == 0) {
			// FIXME: log info
			return;
		}
		List<Map<String,Object>> dataListExport = getExportHeatMapData();
		if(sortedColumnsArray!=null){
			dataListExport=sortExportList(dataListExport,sortedColumnsArray,sortedIndexArray,isAscendingArray);
		}
		int i = 0;
		for (Map<String, Object> dataMap : dataListExport) {
			i = 0;
			if(dataMap.containsKey(Y_AXIS_CATEGORY)){
				 int index = Integer.parseInt(dataMap.get(Y_AXIS_CATEGORY).toString());
				rowDataExport[i++] = seriesNames[index];
			}
			for (String fieldName : fieldsToExport) {
				if (dataMap.containsKey(fieldName)) {
					rowDataExport[i++] = dataMap.get(fieldName) == null ? CommonConstants.EMPTY_STRING : dataMap.get(fieldName).toString();

				} else {
					rowDataExport[i++] = CommonConstants.EMPTY_STRING;
				}

			}
			csvWriter.writeNext(rowDataExport);
		}

	} catch (Exception e) {
       logger.error(HeatmapDataRenderer.class, e.getMessage());
	} finally {
		csvWriter.flush();
	}

}



	
	
	private void populateMinMax(String[] seriesFields, XYChartWindow chartWindow) {
			 List<IAggregateResult> valueList=null;
		 Map<String, IMetadata> metaDataMap= chartWindow.getMetadata();
		 double value=0;
		 for(int i=0;i<seriesFields.length;i++){
			 IMetadata metaData=	 metaDataMap.get(seriesFields[i]);
			 if(metaData!=null&&(metaData.getType().equals("java.lang.Double")||(metaData.getType().equals("java.math.BigDecimal")))){
			 Map<String,Object> colorAxisPropertiesMap =chartWindow.getColorAxis();
			 if(!colorAxisPropertiesMap.containsKey(PROPERTY_NAME_MIN)||((double)colorAxisPropertiesMap.get(PROPERTY_NAME_MIN)==0)){
				 valueList=getGroupData(toList(seriesFields[i]), EAggregationType.MINIMUM, null);
				 value = ((IAggregateResult)valueList.get(0)).getResult();
				 colorAxisPropertiesMap.put(PROPERTY_NAME_MIN, value);
			   }
			 if(!colorAxisPropertiesMap.containsKey(PROPERTY_NAME_MAX)||((double)colorAxisPropertiesMap.get(PROPERTY_NAME_MAX)==0)){
				 valueList=getGroupData(toList(seriesFields[i]), EAggregationType.MAXIMUM, null);
				 value = ((IAggregateResult)valueList.get(0)).getResult();
				 colorAxisPropertiesMap.put(PROPERTY_NAME_MAX, value);
			 	}
			 }
		 }
	}
	private void populateMetaData(IDataObject dataObject, Map<String, Object> attributeMap, Map<String, IMetadata> metaDataMap) {

		for (String key : metaDataMap.keySet()) {
			attributeMap.put(key, dataObject.getFieldValue(key));
		}

	}

	private Map<String, Object> buildHeatmapTableData(String[] xAxisCategories, String[] yAxisCategories, int i, int j, String[] seriesNames,
			List<Object[]> valuesList, List<String> xColCategories, List<String> yColCategories) {
		Map<String, Object> attributeMap = new LinkedHashMap<String, Object>();
		if (xAxisCategories != null && xAxisCategories.length > i) {
			attributeMap.put(X_AXIS_CATEGORY, xAxisCategories[i]);
		} else {
			if(xColCategories!=null&&xColCategories.size()>0 && i<xColCategories.size()){
				attributeMap.put(X_AXIS_CATEGORY, "" + xColCategories.get(i));
			
			}else{
				attributeMap.put(X_AXIS_CATEGORY, "" + i);
				
			}
		}
		if (yAxisCategories != null && yAxisCategories.length > j) {
			attributeMap.put(Y_AXIS_CATEGORY, yAxisCategories[j]);
		} else {
			if(yColCategories!=null&&yColCategories.size()>0&&j<yColCategories.size()){
				attributeMap.put(Y_AXIS_CATEGORY, "" +yColCategories.get(j));
			}else{
				attributeMap.put(Y_AXIS_CATEGORY, "" + j);
			}
		}
		int k = 0;
		for (String name : seriesNames) {
			if (k == j) {
				attributeMap.put(name, valuesList.get(i)[j]);
			} else {
				attributeMap.put(name, null);
			}
			k++;
		}
		return attributeMap;
	}

	private List<Map<String, Object>> buildTableData(List<Map<String, Object>> seriesData, List<String> xAxisCategories, List<String> yAxisCategories) {
		for (int i = 0; i < seriesData.size(); i++) {
			Map<String, Object> row = seriesData.get(i);
			if(xAxisCategories!=null&&xAxisCategories.size() >i){
				if(xAxisCategories.contains(row.get(X_AXIS_CATEGORY))){
				int index =getCategoryIndex(xAxisCategories,(String)row.get(X_AXIS_CATEGORY));
		    	row.put(X_AXIS_CATEGORY,xAxisCategories.get(index));
				}
			}
			if(yAxisCategories!=null&&yAxisCategories.size()>i){
				if(yAxisCategories.contains(row.get(Y_AXIS_CATEGORY))){
					int index =getCategoryIndex(yAxisCategories,(String)row.get(Y_AXIS_CATEGORY));
					row.put(Y_AXIS_CATEGORY, yAxisCategories.get(index));
				}
				
			}
		}

		return seriesData;
	}
	private void addAdditionalTableColumns(Map<String, Object> attributeMap, IDataObject dataObject) {
		if (additionalTableColumns != null) {
			String columnName = null;
			for (Map.Entry<String, TableColumn> tableColumn : additionalTableColumns.entrySet()) {
				columnName = tableColumn.getKey();
				attributeMap.put(columnName, dataObject.getFieldValue(columnName) == null ? CommonConstants.EMPTY_STRING : dataObject.getFieldValue(columnName));
			}
		}
	}
	
	
	private int getCategoryIndex( List<String>  categories, String name) {

	for (int i = 0; i < categories.size(); i++) {
		if (name.equals(categories.get(i))) {
			return i;
		}
	}
	return 0;
}
	@Override
	protected XYChartData getWindowData() {
		initailizeDataSource(window);
		String seriesFieldType="";
		String colorField="";
		XYChartWindow chartWindow = (XYChartWindow)window;
		List<INameValue> valueList=chartWindow.getRenderer().getProperties();
		if(StringUtils.isNotBlank(valueList)){
		for(INameValue nameValue:valueList){
			if("seriesTypeField".equals(nameValue.getName())){
				seriesFieldType=nameValue.getValue();
			}
			if("colorField".equals(nameValue.getName())){
				colorField=nameValue.getValue();
			}
		  
		} 
		 if(!"".equals(seriesFieldType)&&seriesFieldType.length()>0){
			   return buildHeatmapDataForRowAseries(seriesFieldType,colorField);
		   }
		}
		return buildHeatmapData();
	}


}
