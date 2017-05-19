package com.spacetimeinsight.si.businessview.renderer.window.chart;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import au.com.bytecode.opencsv.CSVWriter;

import com.spacetimeinsight.si.businessview.IDataRenderable;
import com.spacetimeinsight.si.businessview.IMetadata;
import com.spacetimeinsight.si.businessview.renderer.window.BaseGroupDataRenderableWindowDataRenderer;
import com.spacetimeinsight.si.common.ColorUtils;
import com.spacetimeinsight.si.common.EAggregationType;
import com.spacetimeinsight.si.common.EChartType;
import com.spacetimeinsight.si.common.utils.ArrayUtils;
import com.spacetimeinsight.si.common.utils.DateUtils;
import com.spacetimeinsight.si.common.utils.StringUtils;
import com.spacetimeinsight.si.constants.CommonConstants;
import com.spacetimeinsight.si.dataobject.IDataObject;
import com.spacetimeinsight.si.datasource.IAggregateResult;
import com.spacetimeinsight.si.datasource.IStructuredDataSource;
import com.spacetimeinsight.si.datasource.PIDataSource;
import com.spacetimeinsight.si.exception.ExceptionKey;
import com.spacetimeinsight.si.exception.SIException;
import com.spacetimeinsight.si.model.window.config.chart.Axis.EAxisType;
import com.spacetimeinsight.si.model.window.config.chart.ChartColumnSeries;
import com.spacetimeinsight.si.model.window.config.chart.ChartRowSeries;
import com.spacetimeinsight.si.model.window.config.chart.ChartSeries;
import com.spacetimeinsight.si.model.window.config.chart.ChartSeries.ESeriesIn;
import com.spacetimeinsight.si.model.window.config.chart.XYChartWindow;
import com.spacetimeinsight.si.model.window.config.table.TableColumn;
import com.spacetimeinsight.si.model.window.data.chart.ChartLegend;
import com.spacetimeinsight.si.model.window.data.chart.ChartRowData;
import com.spacetimeinsight.si.model.window.data.chart.ChartSeriesData;
import com.spacetimeinsight.si.model.window.data.chart.XYChartData;
import com.spacetimeinsight.si.window.constants.IChartPropertyConstants;
import com.spacetimeinsight.si.xml.studio.model.commonbvm.DataFilterFieldCondition;
import com.spacetimeinsight.si.xml.studio.model.util.BVMXmlUtils;

public class XYChartDataRenderer extends BaseGroupDataRenderableWindowDataRenderer<XYChartWindow, XYChartData> implements IChartPropertyConstants {

	private boolean sortSeriesData = false;
	private Map<String, Integer> xAxisCategoryIndexMap = null;
	Map<String, TableColumn> additionalTableColumns = null;;
	private Map<String, String> fieldDataSourceldsMap = new LinkedHashMap<String, String>();
	private Map<String, String> xfieldDataSourceldsMap = new LinkedHashMap<String, String>();
	private Map<String, Set<String>> dataSourceldFieldsMap = new LinkedHashMap<String, Set<String>>();
	private Map<String, IStructuredDataSource> datasourceMap = new LinkedHashMap<String, IStructuredDataSource>();
	private static final String IS_TABLE_TOOL_CLICKED = "isTableToolClicked";
	private static final String IS_REFRESH_CALL = "isRefreshCall";
	private static final String IS_INITIAL_CALL = "isInitialCall";
	private static final String PARAM_CHART_SERIES_TYPE_SERIES_NAMES = "chartSeriesTypeNames";
	private List<String> xColCategories = new ArrayList<String>();
	List<Map<String, Object>> dataListExport = new ArrayList<Map<String, Object>>();
	protected Map<String, Object> tableColumnsMap = null;

	private void initializeDatasourceMaps() {
		if (window != null && window.getChartSeries() != null) {
			Set<String> datasourceIdsSet = new HashSet<String>();

			//loop through all column based series
			Map<String, ChartColumnSeries> columnBasedSeriesFields = window.getChartSeries().getChartSeriesFields();
			if (columnBasedSeriesFields != null) {
				Iterator<Entry<String, ChartColumnSeries>> chartSeriesFieldsIterator = columnBasedSeriesFields.entrySet().iterator();
				Entry<String, ChartColumnSeries> chartSeriesFieldEntry = null;
				String fieldName = null;
				String datasourceId = null;
				Set<String> fieldsList = null;

				while (chartSeriesFieldsIterator.hasNext()) {
					chartSeriesFieldEntry = chartSeriesFieldsIterator.next();
					datasourceId = chartSeriesFieldEntry.getValue().getDataSourceId();
					if (org.apache.commons.lang3.StringUtils.isNotBlank(datasourceId)) {
						fieldName = chartSeriesFieldEntry.getKey();
						fieldDataSourceldsMap.put(fieldName, datasourceId);
						xfieldDataSourceldsMap.put(datasourceId, chartSeriesFieldEntry.getValue().getXField());
						fieldsList = dataSourceldFieldsMap.get(datasourceId);
						if (fieldsList == null) {
							fieldsList = new HashSet<String>();
							dataSourceldFieldsMap.put(datasourceId, fieldsList);
						}
						fieldsList.add(fieldName);

						datasourceIdsSet.add(datasourceId);
					} else {
						//FIXME:log error
					}
				}
			}
			ChartRowSeries rowBasedSeries = window.getChartSeries().getChartRowSeries();
			if (rowBasedSeries != null) {
				String datasourceId = rowBasedSeries.getDataSourceId();
				if (org.apache.commons.lang3.StringUtils.isNotBlank(datasourceId)) {
					datasourceIdsSet.add(datasourceId);
					fieldDataSourceldsMap.put(rowBasedSeries.getYField(), datasourceId);
					xfieldDataSourceldsMap.put(datasourceId, rowBasedSeries.getXField());

				}
			}

			//loop through all datasources and initialize
			IStructuredDataSource dataSource = null;
			for (String datasourceIdStr : datasourceIdsSet) {
				dataSource = getDataSource(datasourceIdStr);
				if (dataSource != null) {
					dataSource.addConstraint(getWindowConstraint());
				}

				dataFilterFieldConditions = BVMXmlUtils.getDataSourceInputFilterConditonsForWindow(this.bvmRoot, this.window.getId(),
						dataSource.getId());
				if (requestModel.getParentWindowId() != null) {
					List<DataFilterFieldCondition> parentDataFilterFieldConditions = BVMXmlUtils.getDataSourceInputFilterConditonsForWindow(
							this.bvmRoot, this.window.getId(), dataSource.getId(), requestModel.getParentWindowId());

					if (parentDataFilterFieldConditions != null && parentDataFilterFieldConditions.size() > 0) {
						if ((dataFilterFieldConditions == null || dataFilterFieldConditions.size() == 0)) {
							dataFilterFieldConditions = parentDataFilterFieldConditions;
						} else {
							dataFilterFieldConditions.addAll(parentDataFilterFieldConditions);
						}
					}
				}
				populateInputFilterCriteria();
				this.setInputFilterCriteria(dataSource);
				datasourceMap.put(datasourceIdStr, dataSource);
			}

		}
	}

	@Override
	protected XYChartData getWindowData() {
		initializeDatasourceMaps();
		boolean isTableToolClicked = false;
		boolean isRefreshCall = false;
		boolean isInitialCall = false;
		try {
			if (requestModel.getDataParameters() != null) {
				isTableToolClicked = (requestModel.getDataParameters().get(IS_TABLE_TOOL_CLICKED) != null) ? StringUtils.getBoolean(requestModel
						.getDataParameters().get(IS_TABLE_TOOL_CLICKED).toString()) : false;
				isRefreshCall = (requestModel.getDataParameters().get(IS_REFRESH_CALL) != null) ? StringUtils.getBoolean(requestModel
						.getDataParameters().get(IS_REFRESH_CALL).toString()) : false;
				isInitialCall = (requestModel.getDataParameters().get(IS_INITIAL_CALL) != null) ? StringUtils.getBoolean(requestModel
						.getDataParameters().get(IS_INITIAL_CALL).toString()) : true;

			}
			windowData = new XYChartData();

			// To check the call for data for both series and table or series or table.
			if ((isInitialCall && isTableToolClicked) || (isTableToolClicked && isRefreshCall)) {
				getChartTableData(windowData);
				getChartSeriesData(windowData, false);
			} else if (isTableToolClicked && !isRefreshCall) {
				getChartTableData(windowData);
			} else if ((!isTableToolClicked && isRefreshCall) || (isInitialCall && !isTableToolClicked)) {
				getChartSeriesData(windowData, true);
			} else {
				getChartSeriesData(windowData, true);
			}
			windowData.setChartLegendData(populateChartLegendData((XYChartWindow) window));
			windowData.setLastUpdateFormattedTime(DateUtils.format(DateUtils.DEFAULT_DATE_TIME_FORMAT));

			if (xColCategories != null && xColCategories.size() > 0) {
				windowData.setXAxisCategories(xColCategories.toArray(new String[0]));
			}
			if (sortSeriesData) {
				windowData.sort();
			}

		} catch (Throwable e) {
			if (e instanceof SIException) {
				throw (SIException) e;
			}
			throw new SIException(ExceptionKey.WindowDataException, e);
		} finally {
			closeDataSource();
		}

		return windowData;
	}

	// to get series data only
	private void getChartSeriesData(XYChartData windowData, boolean onlyChartData) {

		addColumnSeriesChartData(windowData);
		addRowSeriesChartData(windowData);
		addNavigatorSeriesData(windowData);
		//windowData.setNavigatorData(null);
		if (onlyChartData) {
			windowData.setChartTableData(null);
		}

	}

	// to get table data only
	private void getChartTableData(XYChartData windowData) {

		List<Map<String, Object>> tableData = null;
		String[] seriesNames = getSeriesNames(window);
		windowData.setChartSeriesList(null);
		if (window.getChartTable().isGroupedTable()) {
			tableData = getGroupedTableData(seriesNames);
		} else {
			if (datasourceMap.size() > 1) {
				tableData = getFlatTableDataForMultipleDS(seriesNames);
			} else {
				tableData = getFlatTableDataForSingleDS(seriesNames);
			}

		}
		windowData.setChartTableData(tableData);
		windowData.setChartSeriesList(null);

	}

	protected XYChartData addColumnSeriesChartData(XYChartData chartData) {
		Map<String, ChartSeriesData> chartSeriesDataMap = new HashMap<String, ChartSeriesData>();
		String[] seriesNames = getSeriesNames(window);
		Map<String, IMetadata> chartMetadata = window.getMetadata();
		ChartSeries chartSeries = window.getChartSeries();
		if (chartSeries != null) {
			Map<String, ChartColumnSeries> chartColumnSeriesMap = chartSeries.getChartSeriesFields();
			if (chartColumnSeriesMap != null) {

				List<ChartRowData> navigatorSeriesData = new ArrayList<ChartRowData>();
				String navigatorSeriesName = null;
				String navDataSourceId = null;
				if (window.isTimeSeriesChart() && !StringUtils.isNull(window.getTimeseriesChartDetails())) {
					navigatorSeriesName = window.getTimeseriesChartDetails().getTimeLineNavigatorSeriesField();
					navDataSourceId = window.getTimeseriesChartDetails().getDataSourceId();
				}
				String[] toolTipAttributes = null;
				if (seriesNames != null) {
					for (int i = 0; i < seriesNames.length; i++) {
						ChartColumnSeries chartColumnSeries = chartColumnSeriesMap.get(seriesNames[i]);
						if (chartColumnSeries != null) {
							toolTipAttributes = chartColumnSeries.getTooltipAttributes();
							if (toolTipAttributes != null && toolTipAttributes.length > 0) {
								break;
							}
						}
					}
				}

				int noOfTooltipAttributes = toolTipAttributes == null ? 0 : toolTipAttributes.length;
				boolean isUseRawData = (noOfTooltipAttributes <= 0);
				if (isUseRawData) {
					return buildRawChartData(chartData, seriesNames, chartMetadata, navigatorSeriesName, navDataSourceId);

				}

				try {
					EAxisType xAxisType = (EAxisType) window.getXAxis().get(PROPERTY_NAME_TYPE);
					if (EAxisType.category.equals(xAxisType)) {
						xAxisCategoryIndexMap = Collections.synchronizedMap(new LinkedHashMap<String, Integer>());
						@SuppressWarnings("unchecked")
						List<String> categoryList = (List<String>) window.getXAxis().get(PROPERTY_NAME_CATEGORIES);
						int noOfCategories = categoryList == null ? 0 : categoryList.size();
						// sort when there is more than one datasource as we do
						// not know the order in which the series may be added
						sortSeriesData = dataSourceldFieldsMap.size() > 1;
						if (noOfCategories > 0) {
							sortSeriesData = true;
							for (int i = 0; i < noOfCategories; i++) {
								xAxisCategoryIndexMap.put(categoryList.get(i), i);
								xColCategories.add(categoryList.get(i));
							}
						}
					}

					int xIndex = -1;

					if (datasourceMap.size() > 1) {
						extractDataForMultipleDataSource(chartData, chartSeriesDataMap, seriesNames, chartSeries, xAxisType, xIndex,
								navigatorSeriesData, navigatorSeriesName, navDataSourceId);
					} else {
						String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[0]);
						IStructuredDataSource datasource = datasourceMap.get(datasourceId);
						if (datasource instanceof PIDataSource) {
							Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
							return buildChartDataForPIDataSource(dataSourceIterator, chartData, chartSeries);
						} else {

							extractDataForSingleDataSource(chartData, chartSeriesDataMap, seriesNames, chartSeries, xAxisType, xIndex,
									navigatorSeriesData, navigatorSeriesName, navDataSourceId);
						}
					}
					if (window.isTimeSeriesChart() && navigatorSeriesData.size() > 0) {
						Collections.sort(navigatorSeriesData, new SeriesDataComparator());
						chartData.setNavigatorData(navigatorSeriesData);
					}
				} finally {
					chartSeriesDataMap.clear();
				}
			}
		}
		return chartData;
	}

	private int extractDataForSingleDataSource(XYChartData chartData, Map<String, ChartSeriesData> chartSeriesDataMap, String[] seriesNames,
			ChartSeries chartSeries, EAxisType xAxisType, int xIndex, List<ChartRowData> navigatorSeriesData, String navigatorSeriesName,
			String navDataSourceId) {
		IDataObject dataObject;
		String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[0]);
		IStructuredDataSource datasource = datasourceMap.get(datasourceId);
		Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
		boolean populateNavigatorData = false;
		populateNavigatorData = checkToPopulatNavigatorSeriesData(seriesNames, navigatorSeriesName, navDataSourceId, datasourceId);

		if (dataSourceIterator != null) {
			while (dataSourceIterator.hasNext()) {

				dataObject = dataSourceIterator.next();
				for (int i = 0; i < seriesNames.length; i++) {
					xIndex = populateSeriesData(chartData, chartSeriesDataMap, chartSeries, xAxisType, xIndex, dataObject, seriesNames[i],
							navigatorSeriesData, populateNavigatorData, navigatorSeriesName);

				}
				sortChartData(chartSeriesDataMap, xAxisType);

			}
		}
		return xIndex;
	}

	private void addNavigatorSeriesData(XYChartData chartData) {
		try {
			ChartRowData navigatorRowData = null;
			Object navigatorObj = null;
			IDataObject dataObject;
			if (window.isTimeSeriesChart() && window.getTimeseriesChartDetails() != null) {
				String dataSourceId = window.getTimeseriesChartDetails().getDataSourceId();
				if (!datasourceMap.containsKey(dataSourceId)) {
					String navigatorSeriesName = window.getTimeseriesChartDetails().getTimeLineNavigatorSeriesField();
					String navigatorTimeField = window.getTimeseriesChartDetails().getTimeLineNavigatorTimeField();
					if ((navigatorSeriesName != null)) {
						List<ChartRowData> navigatorSeriesData = new ArrayList<ChartRowData>();
						IStructuredDataSource dataSource = getDataSource(dataSourceId);
						if (dataSource != null) {
							Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(dataSource);

							if (dataSourceIterator != null) {
								while (dataSourceIterator.hasNext()) {
									dataObject = dataSourceIterator.next();
									navigatorRowData = new ChartRowData();
									navigatorObj = dataObject.getFieldValue(navigatorTimeField);
									if (navigatorObj != null && (navigatorObj instanceof Date)) {
										navigatorRowData.setX(((Date) navigatorObj).getTime());
									} else {
										navigatorRowData.setX(navigatorObj);
									}

									navigatorObj = dataObject.getFieldValue(navigatorSeriesName);
									if (navigatorObj != null && (navigatorObj instanceof Date)) {
										navigatorRowData.setY(((Date) navigatorObj).getTime());
									} else {
										navigatorRowData.setY(navigatorObj);
									}

									navigatorSeriesData.add(navigatorRowData);

								}
								if (window.isTimeSeriesChart() && !StringUtils.isNull(navigatorSeriesName)) {
									Collections.sort(navigatorSeriesData, new SeriesDataComparator());
									chartData.setNavigatorData(navigatorSeriesData);
								}

							}

						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(XYChartDataRenderer.class, e.getMessage());

		} finally {
			closeDataSource();
		}

	}

	private int populateSeriesData(XYChartData chartData, Map<String, ChartSeriesData> chartSeriesDataMap, ChartSeries chartSeries,
			EAxisType xAxisType, int xIndex, IDataObject dataObject, String seriesField, List<ChartRowData> navigatorSeriesData,
			boolean populateNavigatorData, String navigatorSeriesName) {
		Object tooltipAttrValue;
		ChartSeriesData chartSeriesData;
		ChartRowData rowData;
		ChartColumnSeries columnSeries;
		String category;
		Integer categoryIndex;
		String xFieldName;
		Object xValue;
		Object value;
		String minField;
		String maxField;
		String highField;
		String lowField;
		String medianField;
		String radiusField;
		EChartType seriesChartType;
		EAxisType yAxisType;
		String tooltipAttributes[] = null;
		int noOfTooltipAttributes = 0;
		ChartRowData navigatorRowData = null;

		columnSeries = window.getChartSeries().getChartColumnSeries(seriesField);
		if (window.getYAxis() != null) {
			yAxisType = (EAxisType) window.getYAxis().get(PROPERTY_NAME_TYPE);
		} else {
			yAxisType = (EAxisType) window.getYAxisList().get(columnSeries.getVerticalAxis()).get(PROPERTY_NAME_TYPE);

		}
		boolean isINcludeNulls = false;
		if (columnSeries.getSeriesProperties() != null && columnSeries.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL) != null) {
			isINcludeNulls = (boolean) columnSeries.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL);
		}

		seriesChartType = columnSeries.getSeriesChartType();
		if (StringUtils.isNull(seriesChartType)) {
			throw new SIException(ExceptionKey.SeriesChartTypeRequired);
		}
		xFieldName = columnSeries.getXField();
		if (StringUtils.isNull(xFieldName)) {
			throw new SIException(ExceptionKey.XFieldRequired);
		}

		chartSeriesData = chartSeriesDataMap.get(seriesField);
		if (chartSeriesData == null) {
			chartSeriesData = new ChartSeriesData();
			chartSeriesDataMap.put(seriesField, chartSeriesData);
			chartData.addChartSeriesData(chartSeriesData);

			chartSeriesData.setId(seriesField);
			chartSeriesData.setName(getDisplayName(columnSeries, seriesField));

		}
		//FIXME:add xaxisfield to columnseries to handle multi datasource
		//xFieldName = columnSeries.getx
		rowData = new ChartRowData();

		xValue = dataObject.getFieldValue(xFieldName);
		if (xValue == null) {
			return xIndex;
		}
		if (EAxisType.category.equals(xAxisType)) {
			category = xValue.toString().trim();
			rowData.setName(category);
			xColCategories.add(category);
			categoryIndex = xAxisCategoryIndexMap.get(category);
			if (categoryIndex == null) {
				categoryIndex = ++xIndex;
				xAxisCategoryIndexMap.put(category, categoryIndex);
			}
			//rowData.setX(categoryIndex);
		} else if (EAxisType.datetime.equals(xAxisType)) {
			if (xValue instanceof Date) {
				xValue = ((Date) xValue).getTime();
			}
			rowData.setX(xValue);
		} else {
			rowData.setX(xValue);
		}

		if (populateNavigatorData) {
			navigatorRowData = new ChartRowData();
			Object obj = dataObject.getFieldValue(navigatorSeriesName);
			if (obj != null && obj instanceof Date) {
				navigatorRowData.setY(((Date) obj).getTime());
			} else {
				navigatorRowData.setY(dataObject.getFieldValue(navigatorSeriesName));
			}
			navigatorRowData.setX(xValue);
			navigatorSeriesData.add(navigatorRowData);
		}

		switch (seriesChartType) {
		case AreaRangeChart:
		case AreaSplineRangeChart:
		case ColumnRangeChart:
		case ErrorbarChart:
			minField = columnSeries.getMinField();
			if (!StringUtils.isNull(minField)) {
				value = dataObject.getFieldValue(minField);
				if (isINcludeNulls) {
					rowData.setLow(value);
				} else {
					rowData.setLow(value == null ? CommonConstants.EMPTY_STRING : value);
				}
			}

			rowData.setHigh(dataObject.getFieldValue(seriesField));

			break;
		case BubbleChart:
			radiusField = columnSeries.getRadiusField();
			if (!StringUtils.isNull(radiusField)) {
				value = dataObject.getFieldValue(radiusField);
				rowData.setZ(value == null ? CommonConstants.EMPTY_STRING : value);
			}
			if (EAxisType.datetime.equals(yAxisType)) {

				rowData.setY(((Date) dataObject.getFieldValue(seriesField)).getTime());
			} else {
				rowData.setY(dataObject.getFieldValue(seriesField));
			}

			break;
		case OHLCChart:
		case CandlestickChart:
			maxField = columnSeries.getMaxField();
			highField = columnSeries.getHighField();
			lowField = columnSeries.getLowField();
			minField = columnSeries.getMinField();

			value = dataObject.getFieldValue(lowField);
			rowData.setLow(value == null ? CommonConstants.EMPTY_STRING : value);
			value = dataObject.getFieldValue(minField);
			rowData.setOpen(value == null ? CommonConstants.EMPTY_STRING : value);

			/*value = dataObject.getFieldValue(seriesField);
			rowData.setMedian(value == null ? CommonConstants.EMPTY_STRING : value);*/

			value = dataObject.getFieldValue(maxField);
			rowData.setClose(value == null ? CommonConstants.EMPTY_STRING : value);

			value = dataObject.getFieldValue(highField);
			rowData.setHigh(value == null ? CommonConstants.EMPTY_STRING : value);

			break;
		case BoxplotChart:
			maxField = columnSeries.getMaxField();
			highField = columnSeries.getHighField();
			lowField = columnSeries.getLowField();
			minField = columnSeries.getMinField();
			medianField = columnSeries.getMedianField();
			value = dataObject.getFieldValue(minField);
			rowData.setLow(value == null ? CommonConstants.EMPTY_STRING : value);

			value = dataObject.getFieldValue(lowField);
			rowData.setQ1(value == null ? CommonConstants.EMPTY_STRING : value);

			value = dataObject.getFieldValue(medianField);
			rowData.setMedian(value == null ? CommonConstants.EMPTY_STRING : value);

			value = dataObject.getFieldValue(highField);
			rowData.setQ3(value == null ? CommonConstants.EMPTY_STRING : value);

			value = dataObject.getFieldValue(maxField);
			rowData.setHigh(value == null ? CommonConstants.EMPTY_STRING : value);
			break;
		default:
			if (EAxisType.datetime.equals(yAxisType)) {
				rowData.setY(((Date) dataObject.getFieldValue(seriesField)).getTime());
			} else {
				rowData.setY(dataObject.getFieldValue(seriesField));
			}

			break;
		}

		if (chartSeries != null) {
			Map<String, ChartColumnSeries> chartColumnSeriesMap = chartSeries.getChartSeriesFields();
			if (chartColumnSeriesMap != null) {
				ChartColumnSeries chartColumnSeries = chartColumnSeriesMap.get(seriesField);
				if (chartColumnSeries != null) {
					tooltipAttributes = chartColumnSeries.getTooltipAttributes();
				}
			}
		}
		noOfTooltipAttributes = tooltipAttributes == null ? 0 : tooltipAttributes.length;

		for (int j = 0; j < noOfTooltipAttributes; j++) {
			tooltipAttrValue = dataObject.getFieldValue(tooltipAttributes[j]);
			if (!StringUtils.isNull(tooltipAttrValue)) {
				rowData.addAdditionalProperty(getDisplayName(window, tooltipAttributes[j]), tooltipAttrValue);
			}
		}
		chartSeriesData.addData(rowData);
		return xIndex;
	}

	private int extractDataForMultipleDataSource(XYChartData chartData, Map<String, ChartSeriesData> chartSeriesDataMap, String[] seriesNames,
			ChartSeries chartSeries, EAxisType xAxisType, int xIndex, List<ChartRowData> navigatorSeriesData, String navigatorSeriesName,
			String navDataSourceId) {
		IDataObject dataObject;
		boolean populateNavigatorData = false;
		for (int i = 0; i < seriesNames.length; i++) {
			String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[i]);
			populateNavigatorData = checkToPopulatNavigatorSeriesData(seriesNames, navigatorSeriesName, navDataSourceId, datasourceId);
			IStructuredDataSource datasource = datasourceMap.get(datasourceId);
			Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
			while (dataSourceIterator.hasNext()) {
				dataObject = dataSourceIterator.next();
				xIndex = populateSeriesData(chartData, chartSeriesDataMap, chartSeries, xAxisType, xIndex, dataObject, seriesNames[i],
						navigatorSeriesData, populateNavigatorData, navigatorSeriesName);
			}
			sortChartData(chartSeriesDataMap, xAxisType);

		}

		return xIndex;
	}

	private void sortChartData(Map<String, ChartSeriesData> chartSeriesDataMap, EAxisType xAxisType) {
		if (xAxisType != null) {
			if (!EAxisType.category.equals(xAxisType)) {
				for (Map.Entry<String, ChartSeriesData> map : chartSeriesDataMap.entrySet()) {
					List<ChartRowData> sortedData = map.getValue().getData();
					Collections.sort(sortedData, new SeriesDataComparator());
				}
			}/*else{// not required for SI-18328, already handled this logic
				for (Map.Entry<String, ChartSeriesData> map : chartSeriesDataMap.entrySet()) {
					List<ChartRowData> sortedData = map.getValue().getData();
					if (xColCategories != null&&xColCategories.size()>0) {
						for (ChartRowData rowData1 : sortedData) {
							rowData1.setX(xColCategories.indexOf(rowData1.getName()));
						}
					}
				}
				
				
				}*/
		}
	}

	private List<Map<String, Object>> getGroupedTableData(String[] seriesNames) {
		List<Map<String, Object>> dataList = new ArrayList<>();
		ChartColumnSeries columnSeries = null;
		Map<String, Object> attributeMap = new LinkedHashMap<>();
		String xField = null;
		IStructuredDataSource datasource = null;
		IDataObject dataObject = null;
		Map<String, Map<String, TableColumn>> groupedColumns = window.getChartTable().getGroupedcolumns();
		for (String seriesField : seriesNames) {
			additionalTableColumns = groupedColumns.get(seriesField);
			String datasourceId = (String) fieldDataSourceldsMap.get(seriesField);
			columnSeries = window.getChartSeries().getChartColumnSeries(seriesField);
			xField = columnSeries.getXField();
			datasource = datasourceMap.get(datasourceId);
			Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
			while (dataSourceIterator.hasNext()) {
				dataObject = dataSourceIterator.next();
				attributeMap = new LinkedHashMap<>();
				attributeMap.put("Series", seriesField);
				attributeMap.put(xField, dataObject.getFieldValue(xField));
				attributeMap.put("SeriesValue", dataObject.getFieldValue(seriesField));
				addAdditionalTableColumns(attributeMap, dataObject);
				dataList.add(attributeMap);
			}
		}

		return dataList;
	}

	private List<Map<String, Object>> getFlatTableDataForMultipleDS(String[] seriesNames) {
		List<Map<String, Object>> dataList = new ArrayList<>();
		ChartColumnSeries columnSeries = null;
		Map<String, Object> attributeMap = new LinkedHashMap<>();
		String xField = null;
		IStructuredDataSource datasource = null;
		IDataObject dataObject = null;
		additionalTableColumns = window.getChartTable().getColumns();
		for (String seriesField : seriesNames) {
			String datasourceId = (String) fieldDataSourceldsMap.get(seriesField);
			ESeriesIn seriesIn = window.getChartSeries().getSeriesIn();
			if (seriesIn != null) {
				datasourceId = window.getChartSeries().getChartRowSeries().getDataSourceId();
				xField = window.getChartSeries().getChartRowSeries().getXField();
			} else {
				columnSeries = window.getChartSeries().getChartColumnSeries(seriesField);
				xField = columnSeries.getXField();
			}
			datasource = datasourceMap.get(datasourceId);

			Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);

			while (dataSourceIterator.hasNext()) {
				dataObject = dataSourceIterator.next();
				attributeMap = new LinkedHashMap<>();
				attributeMap.put(xField, dataObject.getFieldValue(xField));
				if (seriesField != null && dataObject.getFieldValue(seriesField) != null) {
					attributeMap.put(seriesField, dataObject.getFieldValue(seriesField));
				}
				addAdditionalTableColumns(attributeMap, dataObject);
				dataList.add(attributeMap);
			}
		}

		return dataList;
	}

	private List<Map<String, Object>> getFlatTableDataForSingleDS(String[] seriesNames) {
		// TODO Auto-generated method stub
		List<Map<String, Object>> dataList = new ArrayList<>();
		try {
			ChartColumnSeries columnSeries = null;
			Map<String, Object> attributeMap = new LinkedHashMap<>();
			String xField = null;
			IStructuredDataSource datasource = null;
			IDataObject dataObject = null;
			additionalTableColumns = window.getChartTable().getColumns();
			String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[0]);
			ESeriesIn seriesIn = window.getChartSeries().getSeriesIn();
			if (seriesIn != null) {
				datasourceId = window.getChartSeries().getChartRowSeries().getDataSourceId();
			}

			datasource = datasourceMap.get(datasourceId);
			Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
			while (dataSourceIterator.hasNext()) {
				dataObject = dataSourceIterator.next();
				attributeMap = new LinkedHashMap<>();
				for (String seriesField : seriesNames) {

					if (seriesIn != null) {
						xField = window.getChartSeries().getChartRowSeries().getXField();
					} else {
						columnSeries = window.getChartSeries().getChartColumnSeries(seriesField);
						xField = columnSeries.getXField();
					}

					attributeMap.put(xField, dataObject.getFieldValue(xField));
					if (seriesField != null && dataObject.getFieldValue(seriesField) != null) {
						attributeMap.put(seriesField, dataObject.getFieldValue(seriesField));
					}

				}
				addAdditionalTableColumns(attributeMap, dataObject);
				dataList.add(attributeMap);
			}

		} catch (Exception e) {

		} finally {
			closeDataSource();
		}

		return dataList;
	}

	private List<Map<String, Object>> getExportDataForMultipleDS(String[] seriesNames) {
		List<Map<String, Object>> dataList = new ArrayList<>();
		ChartColumnSeries columnSeries = null;
		Map<String, Object> attributeMap = new LinkedHashMap<>();
		IStructuredDataSource datasource = null;
		IDataObject dataObject = null;
		Map<String, IMetadata> metaDataMap;
		for (String seriesField : seriesNames) {
			String datasourceId = (String) fieldDataSourceldsMap.get(seriesField);
			ESeriesIn seriesIn = window.getChartSeries().getSeriesIn();
			if (seriesIn != null) {
				datasourceId = window.getChartSeries().getChartRowSeries().getDataSourceId();
				metaDataMap = window.getChartSeries().getChartRowSeries().getMetadata();
			} else {
				columnSeries = window.getChartSeries().getChartColumnSeries(seriesField);
				metaDataMap = columnSeries.getMetadata();
			}
			datasource = datasourceMap.get(datasourceId);

			Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);

			while (dataSourceIterator.hasNext()) {
				dataObject = dataSourceIterator.next();
				attributeMap = new LinkedHashMap<>();
				populateMetaData(dataObject, attributeMap, metaDataMap);
				dataList.add(attributeMap);
			}
		}

		return dataList;
	}

	private List<Map<String, Object>> getExportDataForSingleDS(String[] seriesNames) {
		List<Map<String, Object>> dataList = new ArrayList<>();
		ChartColumnSeries columnSeries = null;
		Map<String, Object> attributeMap = new LinkedHashMap<>();
		IStructuredDataSource datasource = null;
		IDataObject dataObject = null;
		Map<String, IMetadata> metaDataMap;
		String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[0]);
		ESeriesIn seriesIn = window.getChartSeries().getSeriesIn();
		if (seriesIn != null) {
			datasourceId = window.getChartSeries().getChartRowSeries().getDataSourceId();
		}
		datasource = datasourceMap.get(datasourceId);
		Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
		while (dataSourceIterator.hasNext()) {
			dataObject = dataSourceIterator.next();
			attributeMap = new LinkedHashMap<>();

			for (String seriesField : seriesNames) {

				if (seriesIn != null) {
					metaDataMap = window.getChartSeries().getChartRowSeries().getMetadata();
				} else {
					columnSeries = window.getChartSeries().getChartColumnSeries(seriesField);
					metaDataMap = columnSeries.getMetadata();
				}
				populateMetaData(dataObject, attributeMap, metaDataMap);
			}
			dataList.add(attributeMap);
		}

		return dataList;
	}

	//to sort series data based on xField value in case of date
	class SeriesDataComparator implements Comparator<ChartRowData> {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public int compare(ChartRowData rowData1, ChartRowData rowData2) {
			if (rowData1 != null && rowData2 != null) {
				if (rowData1.getX() instanceof Comparable && rowData2.getX() instanceof Comparable) {
					return ((Comparable) rowData1.getX()).compareTo((Comparable) rowData2.getX());
				}
			}
			return 0;
		}
	}

	private void populateMetaData(IDataObject dataObject, Map<String, Object> attributeMap, Map<String, IMetadata> metaDataMap) {

		for (String key : metaDataMap.keySet()) {
			attributeMap.put(key, dataObject.getFieldValue(key));
		}

	}

	@SuppressWarnings("unchecked")
	protected void addRowSeriesChartData(XYChartData chartData) {
		ChartRowSeries rowSeries = window.getChartSeries().getChartRowSeries();
		if (rowSeries == null) {
			return;
		}

		Map<String, ChartSeriesData> chartSeriesDataMap = new HashMap<String, ChartSeriesData>();

		try {
			String datasourceId = null;
			IStructuredDataSource datasource = null;
			IDataObject dataObject = null;
			ChartSeriesData chartSeriesData = null;
			ChartRowData rowData = null;
			ChartRowData navigatorRowData = null;
			String seriesTypeField = null;
			String seriesField = null;
			String category = null;
			Integer categoryIndex = null;
			String xFieldName = null;
			String yFieldName = null;

			Object xValue = null;
			Object value = null;

			String minField = null;
			String maxField = null;
			String highField = null;
			String lowField = null;

			String radiusField = null;

			EChartType seriesChartType = null;
			String navigatorSeriesName = null;
			String navDataSourceId = null;
			List<ChartRowData> navigatorSeriesData = new ArrayList<ChartRowData>();
			boolean isINcludeNulls = true;
			if (rowSeries.getSeriesProperties() != null
					&& rowSeries.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL) != null) {
				isINcludeNulls = (boolean) rowSeries.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL);
			}
			if (window.isTimeSeriesChart() && !StringUtils.isNull(window.getTimeseriesChartDetails())) {
				navigatorSeriesName = window.getTimeseriesChartDetails().getTimeLineNavigatorSeriesField();
				navDataSourceId = window.getTimeseriesChartDetails().getDataSourceId();
			}

			EAxisType xAxisType = (EAxisType) window.getXAxis().get(PROPERTY_NAME_TYPE);
			//	EAxisType yAxisType = (EAxisType) window.getYAxis().get(PROPERTY_NAME_TYPE);
			if (EAxisType.category.equals(xAxisType)) {
				xAxisCategoryIndexMap = Collections.synchronizedMap(new LinkedHashMap<String, Integer>());
				List<String> categoryList = (List<String>) window.getXAxis().get(PROPERTY_NAME_CATEGORIES);
				int noOfCategories = categoryList == null ? 0 : categoryList.size();
				//sort when there is more than one datasource as we do not know the order in which the series may be added
				//sortSeriesData = dataSourceldFieldsMap.size() > 1;
				if (noOfCategories > 0) {
					sortSeriesData = true;
					for (int i = 0; i < noOfCategories; i++) {
						xAxisCategoryIndexMap.put(categoryList.get(i), i);
					}
				}
			}

			int xIndex = -1;
			datasourceId = rowSeries.getDataSourceId();

			datasource = datasourceMap.get(datasourceId);
			if (datasource == null) {
				//FIXME: log error
				return;
			} else {

				Iterator<IDataObject> dataSourceIterator = getDataSourceIterator(datasource);
				if (dataSourceIterator != null) {
					seriesTypeField = rowSeries.getSeriesTypeField();
					xFieldName = rowSeries.getXField();
					if (StringUtils.isNull(xFieldName)) {
						throw new SIException(ExceptionKey.XFieldRequired);
					}

					yFieldName = rowSeries.getYField();
					if (StringUtils.isNull(yFieldName)) {
						throw new SIException(ExceptionKey.YFieldRequired);
					}

					seriesChartType = rowSeries.getSeriesChartType();
					if (StringUtils.isNull(seriesChartType)) {
						throw new SIException(ExceptionKey.SeriesChartTypeRequired);
					}

					ChartRowSeries advancedChartSeries = null;
					while (dataSourceIterator.hasNext()) {
						dataObject = dataSourceIterator.next();
						value = dataObject.getFieldValue(seriesTypeField);
						seriesField = value == null ? CommonConstants.EMPTY_STRING : value.toString();
						rowData = new ChartRowData();
						//for (String seriesField : seriesFieldsSet) {
						if (!seriesField.equals(CommonConstants.EMPTY_STRING)) {
							chartSeriesData = chartSeriesDataMap.get(seriesField);
							if (chartSeriesData == null) {
								chartSeriesData = new ChartSeriesData();

								chartSeriesDataMap.put(seriesField, chartSeriesData);
								chartData.addChartSeriesData(chartSeriesData);

								chartSeriesData.setId(seriesField);
								chartSeriesData.setName(seriesField);

							}
							populateAdvancedProperties(chartSeriesData, seriesField, rowSeries, rowData, dataObject);
							if (chartSeriesData.getSeriesProperties() != null) {
								advancedChartSeries = (ChartRowSeries) chartSeriesData.getSeriesProperties().get(
										PROPERTY_NAME_SERIES_CHART_PROPERTIES);
							}
							if (chartSeriesData.getColor() == null) {
								chartSeriesData.setColor("#" + ColorUtils.toHex(new Random().nextInt(256))
										+ ColorUtils.toHex(new Random().nextInt(256)) + ColorUtils.toHex(new Random().nextInt(256)));
							}
							//FIXME:add xaxisfield to columnseries to handle multi datasource
							//xFieldName = columnSeries.getx

							xValue = dataObject.getFieldValue(xFieldName);
							if (xValue == null) {
								continue;
							}
							if (EAxisType.category.equals(xAxisType)) {
								category = xValue.toString().trim();
								rowData.setName(category);
								categoryIndex = xAxisCategoryIndexMap.get(category);
								if (categoryIndex == null) {
									categoryIndex = ++xIndex;
									xAxisCategoryIndexMap.put(category, categoryIndex);
								}
								rowData.setX(categoryIndex);
							} else if (EAxisType.datetime.equals(xAxisType)) {
								xValue = ((Date) xValue).getTime();
								rowData.setX(xValue);
							} else {
								rowData.setX(xValue);
							}

							if (chartSeriesData.getSeriesChartType() != null) {
								seriesChartType = EChartType.valueOf(chartSeriesData.getSeriesChartType());
							} else {
								seriesChartType = rowSeries.getSeriesChartType();
							}
							if (window.isTimeSeriesChart() && navDataSourceId != null && (navDataSourceId.equals(datasourceId))) {
								navigatorRowData = new ChartRowData();
								Object obj = dataObject.getFieldValue(navigatorSeriesName);
								if (obj != null && obj instanceof Date) {
									navigatorRowData.setY(((Date) obj).getTime());
								} else {
									navigatorRowData.setY(dataObject.getFieldValue(navigatorSeriesName));
								}
								navigatorRowData.setX(xValue);
								navigatorSeriesData.add(navigatorRowData);
							}
							switch (seriesChartType) {
							case AreaRangeChart:
							case AreaSplineRangeChart:
							case ColumnRangeChart:
							case ErrorbarChart:
								minField = rowSeries.getMinField();
								if (minField == null) {
									minField = advancedChartSeries.getMinField();
								}
								if (!StringUtils.isNull(minField)) {
									value = dataObject.getFieldValue(minField);
									rowData.setLow(value == null ? CommonConstants.EMPTY_STRING : value);
								}

								rowData.setHigh(dataObject.getFieldValue(yFieldName));
								break;
							case BubbleChart:
								radiusField = rowSeries.getRadiusField();
								if (radiusField == null) {
									radiusField = advancedChartSeries.getRadiusField();
								}
								if (!StringUtils.isNull(radiusField)) {
									value = dataObject.getFieldValue(radiusField);
									rowData.setZ(value == null ? CommonConstants.EMPTY_STRING : value);
								}
								Object obj = dataObject.getFieldValue(yFieldName);
								if (obj instanceof Date) {
									rowData.setY(((Date) dataObject.getFieldValue(yFieldName)).getTime());
								} else {
									rowData.setY(dataObject.getFieldValue(yFieldName));
								}

								break;
							case OHLCChart:
							case CandlestickChart:
								maxField = rowSeries.getMaxField();
								if (maxField == null) {
									maxField = advancedChartSeries.getMaxField();
								}
								highField = rowSeries.getHighField();
								if (highField == null) {
									highField = advancedChartSeries.getHighField();
								}
								lowField = rowSeries.getLowField();
								if (lowField == null) {
									lowField = advancedChartSeries.getLowField();
								}
								minField = rowSeries.getMinField();
								if (minField == null) {
									lowField = advancedChartSeries.getMinField();
								}

								value = dataObject.getFieldValue(lowField);
								rowData.setLow(value == null ? CommonConstants.EMPTY_STRING : value);

								value = dataObject.getFieldValue(minField);
								rowData.setOpen(value == null ? CommonConstants.EMPTY_STRING : value);

								/*value = dataObject.getFieldValue(seriesTypeField);
								rowData.setMedian(value == null ? CommonConstants.EMPTY_STRING : value);*/

								value = dataObject.getFieldValue(maxField);
								rowData.setClose(value == null ? CommonConstants.EMPTY_STRING : value);

								value = dataObject.getFieldValue(highField);
								rowData.setHigh(value == null ? CommonConstants.EMPTY_STRING : value);
								break;

							case BoxplotChart:
								maxField = rowSeries.getMaxField();
								if (maxField == null) {
									maxField = advancedChartSeries.getMaxField();
								}
								highField = rowSeries.getHighField();
								if (highField == null) {
									highField = advancedChartSeries.getHighField();
								}
								lowField = rowSeries.getLowField();
								if (lowField == null) {
									lowField = advancedChartSeries.getLowField();
								}
								minField = rowSeries.getMinField();
								if (minField == null) {
									lowField = advancedChartSeries.getMinField();
								}

								value = dataObject.getFieldValue(minField);
								rowData.setLow(value == null ? CommonConstants.EMPTY_STRING : value);

								value = dataObject.getFieldValue(lowField);
								rowData.setQ1(value == null ? CommonConstants.EMPTY_STRING : value);

								value = dataObject.getFieldValue(seriesTypeField);
								rowData.setMedian(value == null ? CommonConstants.EMPTY_STRING : value);

								value = dataObject.getFieldValue(highField);
								rowData.setQ3(value == null ? CommonConstants.EMPTY_STRING : value);

								value = dataObject.getFieldValue(maxField);
								rowData.setHigh(value == null ? CommonConstants.EMPTY_STRING : value);
								break;
							default:
								rowData.setY(dataObject.getFieldValue(yFieldName));
								break;
							}
							if (seriesField != null) {
								if(isINcludeNulls){
									chartSeriesData.addData(rowData);
								}else{
									if(rowData.getY()!=null){
										chartSeriesData.addData(rowData);
									}
									
								}
							}
						}

					}
				}
			}
			if (EAxisType.datetime.equals(xAxisType)) {
				if (chartSeriesData != null) {
					List<ChartRowData> sortedData = chartSeriesData.getData();
					Collections.sort(sortedData, new SeriesDataComparator());
				}
			}
			if (window.isTimeSeriesChart() && navigatorSeriesData.size() > 0) {
				Collections.sort(navigatorSeriesData, new SeriesDataComparator());
				chartData.setNavigatorData(navigatorSeriesData);
			}

			//}

		} finally {
			chartSeriesDataMap.clear();
		}
	}

	private Map<String, Object> populateAdvancedProperties(ChartSeriesData chartSeriesData, String seriesField, ChartRowSeries rowSeries,
			ChartRowData rowData, IDataObject dataObject) {
		Map<String, Object> chartElementMap = null;
		List<Map<String, Object>> booleanValueMapList = rowSeries.getAdvancedBooleanValueConditionProperties();

		List<Map<String, Object>> chartElementMapList = rowSeries.getAdvancedChartSeriesProperties();
		int index = 0;
		String value = null;
		List<String> valueList;
		boolean findValue = false;
		String[] toolTipAttributes = null;
		Map<String, Object> seriesProperties = new HashMap<String, Object>();
		for (Map<String, Object> booleanValueMap : booleanValueMapList) {
			String operator = (String) booleanValueMap.get(PROPERTY_NAME_OPERATOR);
			chartElementMap = chartElementMapList.get(index);
			@SuppressWarnings("unchecked")
			List<String> hoverFields = (List<String>) chartElementMap.get(PROPERTY_NAME_TOOLTIPATTRIBUTES);
			if (hoverFields != null) {
				toolTipAttributes = new String[hoverFields.size()];
				toolTipAttributes = hoverFields.toArray(toolTipAttributes);
			}
			index++;
			findValue = false;
			switch (operator) {
			case PROPERTY_NAME_OPERATOR_EQUALS:
				value = (String) booleanValueMap.get(PROPERTY_NAME_VALUE);
				if (seriesField.equals(value)) {
					populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);
				}
				break;
			case PROPERTY_NAME_OPERATOR_NOTEQUALS:
				value = (String) booleanValueMap.get(PROPERTY_NAME_VALUE);
				if (!seriesField.equals(value)) {
					populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);

				}
				break;

			case PROPERTY_NAME_OPERATOR_LIKE:
				value = (String) booleanValueMap.get(PROPERTY_NAME_VALUE);
				if (seriesField.contains(value)) {
					populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);
				}
				break;

			case PROPERTY_NAME_OPERATOR_NOTLIKE:
				value = (String) booleanValueMap.get(PROPERTY_NAME_VALUE);
				if (!seriesField.contains(value)) {
					populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);
				}
				break;
			case PROPERTY_NAME_OPERATOR_IN:
				valueList = (ArrayList<String>) booleanValueMap.get(PROPERTY_NAME_VALUE);
				for (String valueStr : valueList) {
					if (seriesField.equals(valueStr)) {
						findValue = true;
						break;
					}
				}
				if (findValue) {
					populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);
				}
				break;
			case PROPERTY_NAME_OPERATOR_NOTIN:
				valueList = (ArrayList<String>) booleanValueMap.get(PROPERTY_NAME_VALUE);
				for (String valueStr : valueList) {
					if (seriesField.equals(valueStr)) {
						findValue = true;
						break;
					}
				}
				if (!findValue) {
					populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);

				}
			case PROPERTY_NAME_OPERATOR_NOTNULL:
				populateChartSeriesData(chartSeriesData, rowData, dataObject, toolTipAttributes, seriesProperties, chartElementMap);
				break;
			default:
				break;

			}

		}
		return chartElementMap;
	}

	@SuppressWarnings("unchecked")
	private void populateChartSeriesData(ChartSeriesData chartSeriesData, ChartRowData rowData, IDataObject dataObject, String[] toolTipAttributes,
			Map<String, Object> seriesProperties, Map<String, Object> chartElementMap) {
		String seriesType;
		HashMap<String, Object> chartSeriesEvents;
		chartSeriesData.setColor((String) chartElementMap.get(PROPERTY_NAME_SERIES_COLOR));
		chartSeriesEvents = (HashMap<String, Object>) chartElementMap.get(PROPERTY_NAME_SERIES_EVENTS);
		if (chartSeriesEvents != null) {
			seriesProperties.put(PROPERTY_NAME_EVENTS, chartSeriesEvents);

		}
		ChartRowSeries chartRowSeries = (ChartRowSeries) chartElementMap.get(PROPERTY_NAME_SERIES_CHART_PROPERTIES);
		if (chartRowSeries != null && chartRowSeries.getSeriesProperties() != null) {
			seriesProperties.putAll(chartRowSeries.getSeriesProperties());
		}
		seriesProperties.putAll(chartElementMap);
		chartSeriesData.setSeriesProperties(seriesProperties);
		seriesType = (String) chartElementMap.get(PROPERTY_NAME_SERIES_CHART_TYPE);
		chartSeriesData.setSeriesChartType(seriesType);
		populateToolTipAttributes(toolTipAttributes, dataObject, rowData);

	}

	@Override
	protected void closeDataSource() {
		for (Entry<String, IStructuredDataSource> datasourceEntry : datasourceMap.entrySet()) {
			closeDataSource(datasourceEntry.getValue());
		}
	}

	private void getUniqueCategories() {

		List<String> uniqueList = new ArrayList<String>();
		for (String s : xColCategories) {
			if (!uniqueList.contains(s)) {
				uniqueList.add(s);
			}
		}
		xColCategories = uniqueList;
		Collections.sort(xColCategories);

	}

	private XYChartData buildRawChartData(XYChartData chartData, String[] seriesNames, Map<String, IMetadata> metadata, String navigatorSeriesName,
			String navDataSourceId) {
		XYChartWindow chartWindow = (XYChartWindow) window;
		EChartType baseChartType = chartWindow.getChartDetails().getChartType();
		int noOfSeries = seriesNames == null ? 0 : seriesNames.length;
		//String xFieldType = null;
		String xField = null;
		EAxisType xAxisType = (EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE);
		String axisType = (xAxisType != null) ? xAxisType.toString() : null;
		ChartSeries chartSeries = chartWindow.getChartSeries();
		Map<String, ChartColumnSeries> seriesFieldsMap = chartSeries.getChartSeriesFields();
		String[][] dataFields = new String[noOfSeries][0];

		String radiusField = null;
		List<Object> naviagtorSerieData = new ArrayList<Object>();

		boolean isSeriesInRow = populateCharts(seriesNames, metadata, baseChartType, noOfSeries, xField, chartSeries, seriesFieldsMap, dataFields,
				radiusField);
		List<ChartSeriesData> chartSeriesList = null;
		if (datasourceMap.size() > 1) {
			chartSeriesList = extractRawDataForMultipleDS(seriesNames, metadata, chartWindow, noOfSeries, axisType, chartSeries, dataFields,
					isSeriesInRow, naviagtorSerieData, navigatorSeriesName, navDataSourceId);
		} else {
			String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[0]);
			IStructuredDataSource dataSource = datasourceMap.get(datasourceId);
			if (dataSource instanceof PIDataSource) {
				Iterator<IDataObject> dataEnumerator = getDataSourceIterator(dataSource);
				return buildChartDataForPIDataSource(dataEnumerator, chartData, window);
			} else {
				chartSeriesList = extractRawDataForSingleDS(seriesNames, metadata, chartWindow, noOfSeries, axisType, chartSeries, dataFields,
						isSeriesInRow, naviagtorSerieData, navigatorSeriesName, navDataSourceId);
			}
		}

		chartData.setChartSeriesList(chartSeriesList);
		if (chartWindow.isTimeSeriesChart() && naviagtorSerieData.size() > 0) {
			sortRawSeriesDataForTimeSeries(naviagtorSerieData);
			chartData.setNavigatorRawData(naviagtorSerieData);
		}

		return chartData;

	}

	private List<ChartSeriesData> extractRawDataForSingleDS(String[] seriesNames, Map<String, IMetadata> metadata, XYChartWindow chartWindow,
			int noOfSeries, String axisType, ChartSeries chartSeries, String[][] dataFields, boolean isSeriesInRow, List<Object> naviagtorSerieData,
			String navigatorSeriesName, String navDataSourceId) {
		List<ChartSeriesData> chartSeriesList = null;
		try {
			String xField;
			ChartColumnSeries chartSeriesField;
			Map<String, List<Object>> chartSeriesMap = new LinkedHashMap<String, List<Object>>();
			int noOfDataFields = 0;
			String[] chartSeriesTypeSeriesNames = null;
			String tmpStr = null;
			boolean isSingleDs=true;
			boolean populateNavigatorData = false;
			String seriesTypeField = null;
			if (isSeriesInRow) {
				seriesTypeField = chartSeries.getSeriesTypeField();
				tmpStr = (String) requestModel.getDataParameters().get(PARAM_CHART_SERIES_TYPE_SERIES_NAMES);
				if (!StringUtils.isNull(tmpStr)) {
					chartSeriesTypeSeriesNames = StringUtils.split(tmpStr);
				}
			}

			Map<Object, Map<String, Object>> dataListMap = new LinkedHashMap<Object, Map<String, Object>>();
			Set<Object> uniqueXFields = null;
			Map<String, Set<String>> seriesIdMap = new LinkedHashMap<String, Set<String>>();
			int navigatorSeriesCount = 0;
			//boolean isConvertToMilliseconds = chartWindow.isTimeSeriesChart();
			Map<String, Map<String, List<Object>>> chartSeriesDataMap = new LinkedHashMap<String, Map<String, List<Object>>>();

			if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equalsIgnoreCase(axisType)) {
				uniqueXFields = new LinkedHashSet<Object>();
			} else {
				uniqueXFields = new TreeSet<Object>();
			}

			String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[0]);
			populateNavigatorData = checkToPopulatNavigatorSeriesData(seriesNames, navigatorSeriesName, navDataSourceId, datasourceId);
			IStructuredDataSource dataSource = datasourceMap.get(datasourceId);

			if (chartSeries.getxFieldAggregationType() != null) {
				for (int i = 0; i < noOfSeries; i++) {
					boolean isINcludeNulls = false;
					window.setDataSourceId(datasourceId);
					initailizeDataSource(window);
					chartSeriesField = window.getChartSeries().getChartSeriesFields().get(seriesNames[i]);
					if (chartSeriesField.getSeriesProperties() != null
							&& chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL) != null) {
						isINcludeNulls = (boolean) chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL);
					}
					xField = (window.getChartSeries().getChartSeriesFields().get(seriesNames[i])).getXField();
					Map<Object, Map<Object, List<Object>>> seriesDataMap = new LinkedHashMap<Object, Map<Object, List<Object>>>();
					Map<Object, List<Object>> seriesData = null;
					EAggregationType aggregationType = chartSeriesField.getSeriesFieldAggregationType() != null ? chartSeriesField
							.getSeriesFieldAggregationType() : chartSeries.getxFieldAggregationType();
					noOfDataFields = populateRawDataForAgggreagateSeries(seriesNames, metadata, chartWindow, axisType, dataFields, isSeriesInRow,
							xField, chartSeriesMap, chartSeriesTypeSeriesNames, seriesTypeField, dataListMap, uniqueXFields, seriesIdMap,
							navigatorSeriesCount, chartSeriesDataMap, i, isINcludeNulls, aggregationType, seriesDataMap, seriesData,
							naviagtorSerieData, populateNavigatorData, navigatorSeriesName);

				}
			} else {
				Iterator<IDataObject> dataEnumerator = getDataSourceIterator(dataSource);
				while (dataEnumerator.hasNext()) {
					IDataObject dataObject = (IDataObject) dataEnumerator.next();
					Map<Object, Map<Object, List<Object>>> seriesDataMap = new LinkedHashMap<Object, Map<Object, List<Object>>>();
					Map<Object, List<Object>> seriesData = null;

					for (int i = 0; i < noOfSeries; i++) {
						boolean isINcludeNulls = false;
						chartSeriesField = window.getChartSeries().getChartSeriesFields().get(seriesNames[i]);
						if (chartSeriesField.getSeriesProperties() != null
								&& chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL) != null) {
							isINcludeNulls = (boolean) chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL);
						}
						xField = (window.getChartSeries().getChartSeriesFields().get(seriesNames[i])).getXField();
						noOfDataFields = populateRawDataForSeries(seriesNames, metadata, chartWindow, axisType, dataFields, isSeriesInRow, xField,
								chartSeriesMap, chartSeriesTypeSeriesNames, seriesTypeField, dataListMap, uniqueXFields, seriesIdMap,
								chartSeriesDataMap, i, dataObject, isINcludeNulls, seriesDataMap, seriesData, naviagtorSerieData,
								populateNavigatorData, navigatorSeriesName,isSingleDs);

					}
					if (noOfDataFields == 0) {
						continue;
					}

				}
				if (isSeriesInRow) {
					getUniqueCategories();
				}
			}
			//boolean isSortByTime = chartWindow.isTimeSeriesChart() && chartWindow.getChartSeries().isEnableDatagrouping();
			for (String seriesKey : chartSeriesDataMap.keySet()) {
				chartSeriesList = sortRawSeriesData(axisType, chartSeriesDataMap, chartSeriesList, seriesKey);
			}
		} catch (Exception e) {
			logger.error(XYChartDataRenderer.class, e.getMessage());
		} finally {
			closeDataSource();
		}

		return chartSeriesList;
	}

	private boolean checkToPopulatNavigatorSeriesData(String[] seriesNames, String navigatorSeriesName, String navDataSourceId, String datasourceId) {
		boolean populateNavigatorData = false;
		if (navDataSourceId != null && (navDataSourceId.equals(datasourceId))) {
			if (!fieldDataSourceldsMap.containsKey(navigatorSeriesName)) {
				populateNavigatorData = true;
			} else if (seriesNames != null) {
				for (int i = 0; i < seriesNames.length; i++) {
					if (navigatorSeriesName != null && seriesNames[i].equals(navigatorSeriesName)) {
						windowData.setBaseSeiresIndex(i);
					}
				}

			}

		}
		return populateNavigatorData;
	}

	private List<ChartSeriesData> sortRawSeriesData(String axisType, Map<String, Map<String, List<Object>>> chartSeriesDataMap,
			List<ChartSeriesData> chartSeriesList, String seriesKey) {
		ChartSeriesData seriesData;
		List<Object> valuesList;
		Map<String, List<Object>> seriesValue = chartSeriesDataMap.get(seriesKey);
		for (String seriesContent : seriesValue.keySet()) {
			seriesData = new ChartSeriesData();
			seriesData.setId(seriesContent);
			seriesData.setName(getDisplayName(window, seriesKey));
			valuesList = seriesValue.get(seriesContent);
			//if (CommonConstants.JAVA_UTIL_DATE_STR.equals(xFieldType) && valuesList != null) {
			//always sort by x-value
			if (valuesList != null) {
				Map<Object, List<Object>> valuesMap = new LinkedHashMap<Object, List<Object>>();
				List<Object> listOfValues = new ArrayList<Object>();
				for (Object obj : valuesList) {
					if (obj instanceof Object[]) {
						Object[] currentObject = (Object[]) obj;
						Object xFieldValue = currentObject[0];
						if (valuesMap.containsKey(xFieldValue)) {
							listOfValues = valuesMap.get(xFieldValue);
						} else {
							listOfValues = new ArrayList<Object>();
						}
						listOfValues.add(currentObject[1]);
						valuesMap.put(xFieldValue, listOfValues);
					}
				}
				if (!IChartPropertyConstants.AXIS_TYPE_CATEGORY.equalsIgnoreCase(axisType)) {
					Collections.sort(valuesList, new Comparator<Object>() {
						@Override
						public int compare(Object o1, Object o2) {
							if (o1 != null && o2 != null) {
								if (o1 instanceof Object[] && o2 instanceof Object[] && ((Object[]) o1)[0] instanceof Comparable
										&& ((Object[]) o2)[0] instanceof Comparable) {
									return ((Comparable) ((Object[]) o1)[0]).compareTo(((Comparable) ((Object[]) o2)[0]));
								}
							}
							return 0;
						}
					});
				}
			}
			//}
			seriesData.setRawData(valuesList);
			if (chartSeriesList == null) {
				chartSeriesList = new ArrayList<ChartSeriesData>();
			}
			chartSeriesList.add(seriesData);
		}
		return chartSeriesList;
	}

	private List<ChartSeriesData> extractRawDataForMultipleDS(String[] seriesNames, Map<String, IMetadata> metadata, XYChartWindow chartWindow,
			int noOfSeries, String axisType, ChartSeries chartSeries, String[][] dataFields, boolean isSeriesInRow, List<Object> naviagtorSerieData,
			String navigatorSeriesName, String navDataSourceId) {
		String xField;
		ChartColumnSeries chartSeriesField;
		Map<String, List<Object>> chartSeriesMap = new LinkedHashMap<String, List<Object>>();
        boolean isSingleDS=false;
		String[] chartSeriesTypeSeriesNames = null;
		String tmpStr = null;
		boolean populateNavigatorData = false;
		String seriesTypeField = null;
		if (isSeriesInRow) {
			seriesTypeField = chartSeries.getSeriesTypeField();
			tmpStr = (String) requestModel.getDataParameters().get(PARAM_CHART_SERIES_TYPE_SERIES_NAMES);
			if (!StringUtils.isNull(tmpStr)) {
				chartSeriesTypeSeriesNames = StringUtils.split(tmpStr);
			}
		}

		Map<Object, Map<String, Object>> dataListMap = new LinkedHashMap<Object, Map<String, Object>>();
		Set<Object> uniqueXFields = null;
		Map<String, Map<Object, Map<Object, List<Object>>>> seriesDataMapMap = new LinkedHashMap<String, Map<Object, Map<Object, List<Object>>>>();
		Map<String, Set<String>> seriesIdMap = new LinkedHashMap<String, Set<String>>();
		int navigatorSeriesCount = 0;
		//boolean isConvertToMilliseconds = chartWindow.isTimeSeriesChart();
		Map<String, Map<String, List<Object>>> chartSeriesDataMap = new LinkedHashMap<String, Map<String, List<Object>>>();

		if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equalsIgnoreCase(axisType)) {
			uniqueXFields = new LinkedHashSet<Object>();
		} else {
			uniqueXFields = new TreeSet<Object>();
		}
		for (int i = 0; i < noOfSeries; i++) {

			String datasourceId = (String) fieldDataSourceldsMap.get(seriesNames[i]);
			populateNavigatorData = checkToPopulatNavigatorSeriesData(seriesNames, navigatorSeriesName, navDataSourceId, datasourceId);

			IStructuredDataSource dataSource = datasourceMap.get(datasourceId);

			chartSeriesField = window.getChartSeries().getChartSeriesFields().get(seriesNames[i]);
			boolean isINcludeNulls = false;
			if (chartSeriesField.getSeriesProperties() != null && chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL) != null) {
				isINcludeNulls = (boolean) chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL);
			}
			xField = (window.getChartSeries().getChartSeriesFields().get(seriesNames[i])).getXField();
			Map<Object, Map<Object, List<Object>>> seriesDataMap = new LinkedHashMap<Object, Map<Object, List<Object>>>();
			Map<Object, List<Object>> seriesData = null;
			if (chartSeries.getxFieldAggregationType() != null) {

				window.setDataSourceId(datasourceId);
				initailizeDataSource(window);
				chartSeriesField = window.getChartSeries().getChartSeriesFields().get(seriesNames[i]);
				if (chartSeriesField.getSeriesProperties() != null && chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL) != null) {
					isINcludeNulls = (boolean) chartSeriesField.getSeriesProperties().get(PROPERTY_NAME_ISINCLUDENULL);
				}
				xField = (window.getChartSeries().getChartSeriesFields().get(seriesNames[i])).getXField();

				EAggregationType aggregationType = chartSeriesField.getSeriesFieldAggregationType() != null ? chartSeriesField
						.getSeriesFieldAggregationType() : chartSeries.getxFieldAggregationType();
				populateRawDataForAgggreagateSeries(seriesNames, metadata, chartWindow, axisType, dataFields, isSeriesInRow, xField, chartSeriesMap,
						chartSeriesTypeSeriesNames, seriesTypeField, dataListMap, uniqueXFields, seriesIdMap, navigatorSeriesCount,
						chartSeriesDataMap, i, isINcludeNulls, aggregationType, seriesDataMap, seriesData, naviagtorSerieData, populateNavigatorData,
						navigatorSeriesName);

			} else {
				Iterator<IDataObject> dataEnumerator = getDataSourceIterator(dataSource);
				while (dataEnumerator.hasNext()) {
					IDataObject dataObject = (IDataObject) dataEnumerator.next();
					int noOfDataFields = populateRawDataForSeries(seriesNames, metadata, chartWindow, axisType, dataFields, isSeriesInRow, xField,
							chartSeriesMap, chartSeriesTypeSeriesNames, seriesTypeField, dataListMap, uniqueXFields, seriesIdMap, chartSeriesDataMap,
							i, dataObject, isINcludeNulls, seriesDataMap, seriesData, naviagtorSerieData, populateNavigatorData, navigatorSeriesName,isSingleDS);
					if (noOfDataFields == 0) {
						continue;
					}
				}

			}
			seriesDataMapMap.put(seriesNames[i], seriesDataMap);
		}
		if (isSeriesInRow) {
			getUniqueCategories();
		}
		List<ChartSeriesData> chartSeriesList = new ArrayList<ChartSeriesData>();
		;

		//boolean isSortByTime = chartWindow.isTimeSeriesChart() && chartWindow.getChartSeries().isEnableDatagrouping();
		for (String seriesKey : chartSeriesDataMap.keySet()) {
			sortRawSeriesData(axisType, chartSeriesDataMap, chartSeriesList, seriesKey);
		}
		return chartSeriesList;
	}

	private void sortRawSeriesDataForTimeSeries(List<Object> navigatorSeriesData) {
		Collections.sort(navigatorSeriesData, new Comparator<Object>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public int compare(Object o1, Object o2) {
				if (o1 != null && o2 != null) {
					if (o1 instanceof Object[] && o2 instanceof Object[] && ((Object[]) o1)[0] instanceof Comparable
							&& ((Object[]) o2)[0] instanceof Comparable) {
						return ((Comparable) ((Object[]) o1)[0]).compareTo(((Comparable) ((Object[]) o2)[0]));
					}
				}
				return 0;
			}
		});
	}

	private int populateRawDataForSeries(String[] seriesNames, Map<String, IMetadata> metadata, XYChartWindow chartWindow, String axisType,
			String[][] dataFields, boolean isSeriesInRow, String xField, Map<String, List<Object>> chartSeriesMap,
			String[] chartSeriesTypeSeriesNames, String seriesTypeField, Map<Object, Map<String, Object>> dataListMap, Set<Object> uniqueXFields,
			Map<String, Set<String>> seriesIdMap, Map<String, Map<String, List<Object>>> chartSeriesDataMap, int i, IDataObject dataObject,
			boolean isINcludeNulls, Map<Object, Map<Object, List<Object>>> seriesDataMap, Map<Object, List<Object>> seriesData,
			List<Object> naviagtorSerieData, boolean populateNavigatorData, String navigatorSeriesName, boolean isSingleDs) {
		Object[] values;
		Object xColumnValue;
		Object seriesTypeSeriesObj;
		Map<String, Object> attributeMap;
		boolean isValid;
		attributeMap = new LinkedHashMap<String, Object>();
		xColumnValue = dataObject.getFieldValue(xField);
		String seriesName = null;
		Object tempXValue = null;
		Map<String, List<Object>> tempChartSeriesMap = null;
		List<Object> list1 = null;
		List<Object> rowDataList = null;
		Object[] navigatorSeriesValues = null;

		if (xColumnValue == null) {
			return 0;
		}
		if (xColumnValue instanceof Date) {
			xColumnValue = ((Date) xColumnValue).getTime();
		}
		seriesData = seriesDataMap.get(xColumnValue);
		if (seriesData == null) {
			seriesData = new LinkedHashMap<Object, List<Object>>();
		}
		if (isSeriesInRow) {
			if (chartSeriesTypeSeriesNames != null && !ArrayUtils.contains(chartSeriesTypeSeriesNames, seriesNames[i])) {
				seriesName = seriesNames[i];
			} else {
				seriesTypeSeriesObj = dataObject.getFieldValue(seriesTypeField);
				if (seriesTypeSeriesObj != null) {
					seriesName = seriesTypeSeriesObj.toString().trim();
				}
			}
		} else {
			seriesName = seriesNames[i];
		}
		if (seriesName == null) {
			return 0;
		}

		if (!chartSeriesDataMap.containsKey(seriesName)) {
			chartSeriesDataMap.put(seriesName, new LinkedHashMap<String, List<Object>>());
		}
		tempChartSeriesMap = chartSeriesDataMap.get(seriesName);
		if (!tempChartSeriesMap.containsKey(seriesNames[i])) {
			tempChartSeriesMap.put(seriesNames[i], new ArrayList<Object>());
		}
		list1 = tempChartSeriesMap.get(seriesNames[i]);
		if (!seriesIdMap.containsKey(seriesName)) {
			seriesIdMap.put(seriesName, new LinkedHashSet<String>());
		}
		seriesIdMap.get(seriesName).add(seriesNames[i]);

		rowDataList = chartSeriesMap.get(seriesName);
		if (rowDataList == null) {
			rowDataList = new ArrayList<Object>();
			chartSeriesMap.put(seriesName, rowDataList);
		}
		int noOfDataFields = 0;
		noOfDataFields = dataFields[i].length;
		if (populateNavigatorData) {
			navigatorSeriesValues = new Object[2];
			Object obj = dataObject.getFieldValue(navigatorSeriesName);
			if (obj != null && obj instanceof Date) {
				obj = ((Date) obj).getTime();
			}
			navigatorSeriesValues[0] = xColumnValue;
			navigatorSeriesValues[1] = obj;
			naviagtorSerieData.add(navigatorSeriesValues);

		}

		if (noOfDataFields == 1) {
			rowDataList.add(dataObject.getFieldValue(dataFields[i][0]));
		} else {
			if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
				values = new Object[noOfDataFields + 1];
			} else {
				values = new Object[noOfDataFields];
			}
			isValid = true;
			inner: for (int j = 0; j < noOfDataFields; j++) {
				if (isSeriesInRow) {
					if (chartSeriesTypeSeriesNames != null && !metadata.containsKey(dataFields[i][j])) {
						isValid = false;
						break inner;
					}
				}
				values[j] = dataObject.getFieldValue(dataFields[i][j]);
				if (j == 0) {
					uniqueXFields.add(values[j]);
				}
				if (dataFields[i][j].equals(xField) && IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
					//if(!xColCategories.contains(values[j].toString())){
					if(i==0&&isSingleDs){
						xColCategories.add(values[j].toString());
					}else if(!isSingleDs){
						xColCategories.add(values[j].toString());
					}
					//}
					tempXValue = values[j].toString();
					values[j] = tempXValue;// xColCategories.indexOf(values[j].toString());//getCategoryIndex(xColCategories.toArray(new String[xColCategories.size()]), values[j].toString());
				}
				if (values[j] instanceof Date) {
					values[j] = ((Date) values[j]).getTime();
				}

				if (dataFields[i][j] != null) {
					if (dataFields[i][j].equals(xField) && IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
						attributeMap.put(dataFields[i][j], values[j] == null ? CommonConstants.EMPTY_STRING : tempXValue);
					} else {
						attributeMap.put(dataFields[i][j], values[j] == null ? CommonConstants.EMPTY_STRING : values[j]);
					}
				}
				List<Object> currentData = seriesData.get(dataFields[i][j]);
				if (currentData == null) {
					currentData = new ArrayList<Object>();
				}
				if (isINcludeNulls) {
					currentData.add(values[j]);

				} else {
					if (values[j] != null) {
						if (values[j] instanceof Double) {
							double val = (double) values[j];
							//if (val > 0) {
							currentData.add(val);
							//}
						} else {
							currentData.add(values[j]);
						}
					}

				}

				seriesData.put(dataFields[i][j], currentData);

			}

			if (additionalTableColumns != null) {
				Map<String, Object> extraAttributeMap = new HashMap<String, Object>();
				addAdditionalTableColumns(extraAttributeMap, dataObject);
				attributeMap.putAll(extraAttributeMap);
				for (Map.Entry<String, Object> e : extraAttributeMap.entrySet()) {
					List<Object> extraValues = new ArrayList<Object>();
					if (seriesData.containsKey(e.getKey())) {
						extraValues = seriesData.get(e.getKey());
					}

					extraValues.add(e.getValue());
					seriesData.put(e.getKey(), extraValues);
				}
			}
			if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
				values[noOfDataFields] = tempXValue;
			}
			if (isINcludeNulls) {
				if (isValid && (values[0] != null)) {
					rowDataList.add(values);
					list1.add(values);
				}
			} else {
				if (isValid && (values[0] != null && values[1] != null)) {
					rowDataList.add(values);
					list1.add(values);
				}
			}
		}
		if (xColumnValue instanceof BigDecimal) {
			xColumnValue = ((BigDecimal) xColumnValue).doubleValue();
		}
		if (dataListMap.containsKey(xColumnValue)) {
			Map<String, Object> tempDataMap = dataListMap.get(xColumnValue);
			for (Map.Entry<String, Object> e : attributeMap.entrySet()) {
				tempDataMap.put(e.getKey(), e.getValue());
			}
			dataListMap.remove(xColumnValue);
			dataListMap.put(xColumnValue, tempDataMap);
		} else {
			dataListMap.put(xColumnValue, attributeMap);

		}
		seriesDataMap.put(xColumnValue, seriesData);
		return noOfDataFields;
	}

	private int populateRawDataForAgggreagateSeries(String[] seriesNames, Map<String, IMetadata> metadata, XYChartWindow chartWindow,
			String axisType, String[][] dataFields, boolean isSeriesInRow, String xField, Map<String, List<Object>> chartSeriesMap,
			String[] chartSeriesTypeSeriesNames, String seriesTypeField, Map<Object, Map<String, Object>> dataListMap, Set<Object> uniqueXFields,
			Map<String, Set<String>> seriesIdMap, int navigatorSeriesCount, Map<String, Map<String, List<Object>>> chartSeriesDataMap, int i,
			boolean isINcludeNulls, EAggregationType aggregationType, Map<Object, Map<Object, List<Object>>> seriesDataMap,
			Map<Object, List<Object>> seriesData, List<Object> naviagtorSerieData, boolean populateNavigatorData, String navigatorSeriesName) {
		try {
			List<String> groupByFieldList = new ArrayList<String>();
			groupByFieldList.add(xField);

			//ChartColumnSeries chartColumnSeries  = chartWindow.getChartSeries().getChartSeriesFields().get(seriesNames[i]);
			int noOfDataFields = 0;
			noOfDataFields = dataFields[i].length;
			Object[] values;
			String[] group = null;
			boolean isValid = false;
			String seriesName = null;
			Object tempXValue = null;
			Map<String, List<Object>> tempChartSeriesMap = null;
			List<Object> list1 = null;
			List<Object> rowDataList = null;
			Object[] navigatorSeriesValues = null;

			seriesName = seriesNames[i];
			if (seriesName == null) {
				return 0;
			}

			if (!chartSeriesDataMap.containsKey(seriesName)) {
				chartSeriesDataMap.put(seriesName, new LinkedHashMap<String, List<Object>>());
			}
			tempChartSeriesMap = chartSeriesDataMap.get(seriesName);
			if (!tempChartSeriesMap.containsKey(seriesNames[i])) {
				tempChartSeriesMap.put(seriesNames[i], new ArrayList<Object>());
			}
			list1 = tempChartSeriesMap.get(seriesNames[i]);
			if (!seriesIdMap.containsKey(seriesName)) {
				seriesIdMap.put(seriesName, new LinkedHashSet<String>());
			}
			seriesIdMap.get(seriesName).add(seriesNames[i]);

			rowDataList = chartSeriesMap.get(seriesName);
			if (rowDataList == null) {
				rowDataList = new ArrayList<Object>();
				chartSeriesMap.put(seriesName, rowDataList);
			}

			if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
				values = new Object[noOfDataFields + 1];
			} else {
				values = new Object[noOfDataFields];
			}
			for (int k = 1; k < noOfDataFields; k++) {
				List<String> valueFieldsList = new ArrayList<String>();
				valueFieldsList.add(dataFields[i][k]);
				List<IAggregateResult> aggreagateResults = getGroupData(valueFieldsList, aggregationType, groupByFieldList);
				for (IAggregateResult aggregateResult : aggreagateResults) {
					double result = aggregateResult.getResult();
					group = aggregateResult.getGroup();
					//Object[] navigatorSeriesValues;
					Object xColumnValue;
					//Object seriesTypeSeriesObj;
					Map<String, Object> attributeMap;

					attributeMap = new LinkedHashMap<String, Object>();

					xColumnValue = group[0]; //dataObject.getFieldValue(xField);

					if (xColumnValue == null) {
						return 0;
					}
					if (xColumnValue instanceof Date) {
						xColumnValue = ((Date) xColumnValue).getTime();
					}
					seriesData = seriesDataMap.get(xColumnValue);
					if (seriesData == null) {
						seriesData = new LinkedHashMap<Object, List<Object>>();
					}
					if (chartWindow.isTimeSeriesChart() && populateNavigatorData) {
						navigatorSeriesValues = new Object[2];
						if (navigatorSeriesName.equals(aggregateResult.getFieldName())) {
							navigatorSeriesValues[0] = xColumnValue;
							navigatorSeriesValues[1] = aggregateResult.getResult();
							naviagtorSerieData.add(navigatorSeriesValues);
						}

					}

					if (noOfDataFields == 1) {
						IAggregateResult agrrAggregateResult = (IAggregateResult) aggreagateResults.get(0);
						rowDataList.add(agrrAggregateResult.getResult());
					} else {

						isValid = true;
						for (int j = 0; j < noOfDataFields; j++) {
							if (!(dataFields[i][j].equals(xField) && IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType))) {
								if (dataFields[i][j].equals(aggregateResult.getFieldName())) {
									values[j] = result;
								}
							}
							if (j == 0) {
								uniqueXFields.add(group[0]);
							}
							if (dataFields[i][j].equals(xField) && IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
								xColCategories.add(group[0]);
								tempXValue = group[0];
								values[j] = tempXValue;
							}
							if (values[j] instanceof Date) {
								values[j] = ((Date) values[j]).getTime();
							}

							if (dataFields[i][j] != null) {
								if (dataFields[i][j].equals(xField) && IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
									attributeMap.put(dataFields[i][j], values[j] == null ? CommonConstants.EMPTY_STRING : tempXValue);
								} else {
									attributeMap.put(dataFields[i][j], values[j] == null ? CommonConstants.EMPTY_STRING : values[j]);
								}
							}
							if (dataFields[i][j].equals(aggregateResult.getFieldName())) {
								List<Object> currentData = seriesData.get(dataFields[i][j]);
								if (currentData == null) {
									currentData = new ArrayList<Object>();
								}
								if (isINcludeNulls) {
									currentData.add(values[j]);

								} else {
									if (values[j] != null) {
										if (values[j] instanceof Double) {
											double val = (double) values[j];
											//if (val > 0) {
											currentData.add(val);
											//}
										} else {
											currentData.add(values[j]);
										}
									}

								}

								seriesData.put(dataFields[i][j], currentData);
							}

						}

						/*if (additionalTableColumns != null) {
							Map<String, Object> extraAttributeMap = new HashMap<String, Object>();
							//addAdditionalTableColumns(extraAttributeMap, dataObject);
							attributeMap.putAll(extraAttributeMap);
							for (Map.Entry<String, Object> e : extraAttributeMap.entrySet()) {
								List<Object> extraValues = new ArrayList<Object>();
								if (seriesData.containsKey(e.getKey())) {
									extraValues = seriesData.get(e.getKey());
								}

								extraValues.add(e.getValue());
								seriesData.put(e.getKey(), extraValues);
							}
						}*/

					}
					if (xColumnValue instanceof BigDecimal) {
						xColumnValue = ((BigDecimal) xColumnValue).doubleValue();
					}
					if (dataListMap.containsKey(xColumnValue)) {
						Map<String, Object> tempDataMap = dataListMap.get(xColumnValue);
						for (Map.Entry<String, Object> e : attributeMap.entrySet()) {
							tempDataMap.put(e.getKey(), e.getValue());
						}
						dataListMap.remove(xColumnValue);
						dataListMap.put(xColumnValue, tempDataMap);
					} else {
						dataListMap.put(xColumnValue, attributeMap);

					}
					seriesDataMap.put(xColumnValue, seriesData);
				}
			}

			for (Map.Entry<Object, Map<Object, List<Object>>> entry : seriesDataMap.entrySet()) {

				Map<Object, List<Object>> map = entry.getValue();
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
					values = new Object[noOfDataFields + 1];
				} else {
					values = new Object[noOfDataFields];
				}
				values[0] = entry.getKey();
				for (int j = 1; j < noOfDataFields; j++) {
					List<Object> list = map.get(dataFields[i][j]);
					if (list.size() > 0) {
						values[j] = list.get(0);
					} else {
						values[j] = null;
					}

				}

				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
					values[noOfDataFields] = entry.getKey();
				}
				if (isINcludeNulls) {
					if (isValid && (values[0] != null)) {
						rowDataList.add(values);
						list1.add(values);

					}
				} else {
					if (isValid && (values[0] != null && values[1] != null)) {
						rowDataList.add(values);
						list1.add(values);

					}
				}

			}
			return noOfDataFields;
		} catch (Exception e) {
			logger.error(XYChartDataRenderer.class, e.getMessage());
			//e.printStackTrace();
		}
		return 0;
	}

	private boolean populateCharts(String[] seriesNames, Map<String, IMetadata> metadata, EChartType baseChartType, int noOfSeries, String xField,
			ChartSeries chartSeries, Map<String, ChartColumnSeries> seriesFieldsMap, String[][] dataFields, String radiusField) {
		EChartType seriesChartType;
		boolean isSeriesType;
		String maxField = null;
		String highField = null;
		String lowField = null;
		String minField = null;
		String medianField = null;
		ChartColumnSeries chartSeriesField = null;

		boolean isSeriesInRow = ESeriesIn.Row.equals(chartSeries.getSeriesIn());
		if (metadata != null && !StringUtils.isNull(metadata.get(xField))) {
			//xFieldType = metadata.get(xField).getType();
		}
		for (int i = 0; i < noOfSeries; i++) {
			seriesChartType = null;

			if (seriesFieldsMap != null) {
				chartSeriesField = seriesFieldsMap.get(seriesNames[i]);
				if (seriesFieldsMap.containsKey(seriesNames[i])) {
					seriesChartType = seriesFieldsMap.get(seriesNames[i]).getSeriesChartType();

				}

				xField = chartSeriesField.getXField();
			}
			isSeriesType = false;
			//if (!EAxisType.category.equals(xAxisType)) {
			dataFields[i] = ArrayUtils.add(dataFields[i], xField);
			//}
			if (seriesChartType == null) {
				seriesChartType = baseChartType;
			} else {
				isSeriesType = true;
			}

			switch (seriesChartType) {
			case AreaRangeChart:
			case AreaSplineRangeChart:
			case ColumnRangeChart:
			case ErrorbarChart:
				if (isSeriesType) {
					minField = chartSeriesField.getMinField();
				} else {
					//	minField = chartSeries.getMinField();
				}
				dataFields[i] = ArrayUtils.add(dataFields[i], new String[] { minField, seriesNames[i] });
				break;
			case BubbleChart:
				if (isSeriesType) {
					radiusField = chartSeriesField.getRadiusField();
				} else {
					//	radiusField = chartSeries.getRadiusField();
				}
				dataFields[i] = ArrayUtils.add(dataFields[i], new String[] { seriesNames[i], radiusField });
				break;
			case OHLCChart:
			case CandlestickChart:
				if (isSeriesType) {
					maxField = chartSeriesField.getMaxField();
					highField = chartSeriesField.getHighField();
					lowField = chartSeriesField.getLowField();
					minField = chartSeriesField.getMinField();
				} else {
					//maxField = chartSeries.getMaxField();
					///highField = chartSeries.getHighField();
					//lowField = chartSeries.getLowField();
					//minField = chartSeries.getMinField();
				}
				dataFields[i] = ArrayUtils.add(dataFields[i], new String[] { minField, highField, lowField, maxField });
				break;
			case BoxplotChart:
				if (isSeriesType) {
					maxField = chartSeriesField.getMaxField();
					highField = chartSeriesField.getHighField();
					lowField = chartSeriesField.getLowField();
					minField = chartSeriesField.getMinField();
					medianField = chartSeriesField.getMedianField();
				} else {
					//maxField = chartSeries.getMaxField();
					///highField = chartSeries.getHighField();
					//lowField = chartSeries.getLowField();
					//minField = chartSeries.getMinField();
				}
				dataFields[i] = ArrayUtils.add(dataFields[i], new String[] { minField, lowField, medianField, highField, maxField });
				break;
			default:
				dataFields[i] = ArrayUtils.add(dataFields[i], seriesNames[i]);
				break;
			}
		}
		return isSeriesInRow;
	}

	private String[] getSeriesNames(XYChartWindow windowModel) {
		ChartSeries chartseries = windowModel.getChartSeries();
		Map<String, ChartColumnSeries> seriesFields = chartseries.getChartSeriesFields();
		ChartRowSeries rowSeries = chartseries.getChartRowSeries();
		ChartColumnSeries chartSeriesField = null;
		List<String> legendList = new ArrayList<>();
		if (seriesFields != null) {
			for (Map.Entry<String, ChartColumnSeries> entry : seriesFields.entrySet()) {
				chartSeriesField = entry.getValue();

				legendList.add(chartSeriesField.getSeriesName());
			}
		}
		if (rowSeries != null) {
			legendList.add(rowSeries.getYField());
		}
		return legendList.size() > 0 ? legendList.toArray(new String[0]) : null;
	}

	@SuppressWarnings("rawtypes")
	class HashMapComparator implements Comparator {
		private String xField;

		public HashMapComparator(String xField) {
			this.xField = xField;
		}

		@SuppressWarnings({ "unchecked" })
		@Override
		public int compare(Object arg0, Object arg1) {

			if (arg0 instanceof Map && arg1 instanceof Map) {
				if (((HashMap<String, Object>) arg0).get(xField) != null && ((HashMap<String, Object>) arg1).get(xField) != null) {
					Object arg0Value = ((HashMap<String, Object>) arg0).get(xField);
					if (arg0Value instanceof Date) {
						arg0Value = ((Date) arg0Value).getTime();
					}
					Object arg1Value = ((HashMap<String, Object>) arg1).get(xField);
					if (arg1Value instanceof Date) {
						arg1Value = ((Date) arg1Value).getTime();
					}
					return ((Comparable) arg0Value).compareTo((Comparable) arg1Value);
				}
			}
			return 0;
		}
	}

	private List<ChartLegend> populateChartLegendData(XYChartWindow windowModel) {
		ChartSeries chartseries = windowModel.getChartSeries();
		Map<String, ChartColumnSeries> seriesFields = chartseries.getChartSeriesFields();
		ChartLegend chartLegend = null;
		ChartColumnSeries chartSeriesField = null;
		String displayName;

		List<ChartLegend> legendList = null;
		if (seriesFields != null) {
			if (windowData.getChartSeriesList() != null) {
				for (ChartSeriesData chartSeriesData : windowData.getChartSeriesList()) {
					//for (Map.Entry<String, ChartColumnSeries> entry : seriesFields.entrySet()) {
					chartLegend = new ChartLegend();
					chartSeriesField = seriesFields.get(chartSeriesData.getId());
					chartLegend.setSeriesName(chartSeriesField.getSeriesName());
					displayName = getDisplayName(windowModel, chartSeriesField.getSeriesName());
					if (displayName == null) {
						displayName = chartSeriesField.getSeriesName();
					}
					chartLegend.setDisplayName(displayName);

					chartLegend.setColor(chartSeriesField.getSeriesColor());
					if (legendList == null) {
						legendList = new LinkedList<ChartLegend>();
					}
					legendList.add(chartLegend);
					//}
				}
			}
		}
		return legendList;
	}

	public void populateToolTipAttributes(String[] toolTipAttributes, IDataObject dataObject, ChartRowData rowData) {

		Object tooltipAttrValue = null;
		int noOfTooltipAttributes = toolTipAttributes == null ? 0 : toolTipAttributes.length;

		for (int j = 0; j < noOfTooltipAttributes; j++) {
			tooltipAttrValue = dataObject.getFieldValue(toolTipAttributes[j]);
			if (!StringUtils.isNull(tooltipAttrValue)) {
				rowData.addAdditionalProperty(getDisplayName(window, toolTipAttributes[j]), tooltipAttrValue);

			}

		}
	}

	@Override
	protected void toCSV(CSVWriter csvWriter) throws IOException {
		try {
			initializeDatasourceMaps();
			String selectedColumns = (String) this.requestModel.getDataParameters().get("selectedColumns");
			String sortedColumns = (String) this.requestModel.getDataParameters().get("sortedColumns");
			String sortedIndex = (String) this.requestModel.getDataParameters().get("sortedIndex");
			String isAscending = (String) this.requestModel.getDataParameters().get("isAscending");
			String[] selectedColumnArray = selectedColumns != null ? selectedColumns.split(CommonConstants.COMMA_STRING) : null;
			String[] sortedColumnsArray = sortedColumns != null && (!CommonConstants.EMPTY_STRING.endsWith(sortedColumns)) ? sortedColumns
					.split(CommonConstants.COMMA_STRING) : null;
			String[] sortedIndexArray = sortedIndex != null && (!CommonConstants.EMPTY_STRING.endsWith(sortedIndex)) ? sortedIndex
					.split(CommonConstants.COMMA_STRING) : null;
			String[] isAscendingArray = isAscending != null && (!CommonConstants.EMPTY_STRING.endsWith(isAscending)) ? isAscending
					.split(CommonConstants.COMMA_STRING) : null;
			if (!(window instanceof IDataRenderable)) {
				return;
			}
			String[] fieldsToExport = getFieldsToExport(selectedColumnArray);
			String[] rowDataExport = new String[fieldsToExport.length];

			IDataRenderable dataRenderable = (IDataRenderable) window;
			csvWriter.writeNext(getDisplayNames(dataRenderable, fieldsToExport));
			if (fieldsToExport == null || fieldsToExport.length == 0) {
				// FIXME: log info
				return;
			}
			if (datasourceMap.size() > 1) {
				dataListExport = getExportDataForMultipleDS(getSeriesNames(window));
			} else {
				dataListExport = getExportDataForSingleDS(getSeriesNames(window));
			}
			if (sortedColumnsArray != null) {
				dataListExport = sortExportList(dataListExport, sortedColumnsArray, sortedIndexArray, isAscendingArray);
			}
			int i = 0;
			for (Map<String, Object> dataMap : dataListExport) {
				i = 0;
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
			logger.error(XYChartDataRenderer.class, e.getMessage());
		} finally {
			csvWriter.flush();
		}

	}

	private XYChartData buildChartDataForPIDataSource(Iterator<IDataObject> dataEnumerator, XYChartData chartData, IDataRenderable window) {

		try {

			XYChartWindow chartWindow = (XYChartWindow) window;
			ChartSeries chartSeries = chartWindow.getChartSeries();
			String xField = chartSeries.getxField();

			/*if (StringUtils.isNull(xField)) {
				throw new EHRuntimeException(LocaleUtils.getResourceString(languageCode, IServerConstants.PARAM_ADMIN_RESOURCES_NAME,
						IHTMLViewerMessagesConstants.ERROR_XY_XFIELD_REQUIRED,DataModelsCacheHelper.getLanguageCharset(languageCode)));
			}*/

			Object xColumnValue = null;
			ChartSeriesData seriesData = null;
			String axisType = ((EAxisType) chartWindow.getXAxis().get(IChartPropertyConstants.PROPERTY_NAME_TYPE)).toString();
			Map<String, List<ChartRowData>> chartSeriesMap = new LinkedHashMap<String, List<ChartRowData>>();

			List<ChartRowData> tmpSeries = null;
			ChartRowData rowData = null;
			IDataObject dataObject = null;

			Map<String, Object> attributeMap = null;
			List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();

			Map<String, Set<String>> seriesIdMap = new HashMap<String, Set<String>>();
			String seriesName = null;
			Map<String, Map<String, List<ChartRowData>>> chartSeriesDataMap = new LinkedHashMap<String, Map<String, List<ChartRowData>>>();
			Map<String, List<ChartRowData>> tempChartSeriesMap = null;

			while (dataEnumerator.hasNext()) {
				dataObject = dataEnumerator.next();
				xColumnValue = dataObject.getFieldValue(xField);
				attributeMap = new LinkedHashMap<String, Object>();

				if (xColumnValue instanceof Date) {
					xColumnValue = ((Date) xColumnValue).getTime();
				}

				seriesName = dataObject.getFieldValue("tag").toString();

				if (!seriesIdMap.containsKey(seriesName)) {
					seriesIdMap.put(seriesName, new LinkedHashSet<String>());
				}
				seriesIdMap.get(seriesName).add(seriesName);

				tmpSeries = chartSeriesMap.get(seriesName);
				if (tmpSeries == null) {
					tmpSeries = new ArrayList<ChartRowData>();
					chartSeriesMap.put(seriesName, tmpSeries);
				}

				if (!chartSeriesDataMap.containsKey(seriesName)) {
					chartSeriesDataMap.put(seriesName, new LinkedHashMap<String, List<ChartRowData>>());
				}
				tempChartSeriesMap = chartSeriesDataMap.get(seriesName);
				if (!tempChartSeriesMap.containsKey(seriesName)) {
					tempChartSeriesMap.put(seriesName, new ArrayList<ChartRowData>());
				}
				List<ChartRowData> list1 = tempChartSeriesMap.get(seriesName);
				if (!seriesIdMap.containsKey(seriesName)) {
					seriesIdMap.put(seriesName, new LinkedHashSet<String>());
				}
				seriesIdMap.get(seriesName).add(seriesName);

				rowData = new ChartRowData();
				if (IChartPropertyConstants.AXIS_TYPE_CATEGORY.equals(axisType)) {
					rowData.setName(xColumnValue.toString());
				} else {
					rowData.setX(xColumnValue);
				}

				attributeMap.put(xField, xColumnValue);
				rowData.setY(dataObject.getFieldValue("value"));

				attributeMap.put(seriesName, rowData.getY());
				if (tableColumnsMap != null) {
					//	addAdditionalTableColumns(attributeMap, dataObject, dataEnumerator);
				}
				tmpSeries.add(rowData);
				list1.add(rowData);
				dataList.add(attributeMap);
			}

			List<ChartSeriesData> chartSeriesList = null;
			for (String seriesKey : chartSeriesDataMap.keySet()) {
				Map<String, List<ChartRowData>> seriesValue = chartSeriesDataMap.get(seriesKey);
				for (String seriesContent : seriesValue.keySet()) {
					seriesData = new ChartSeriesData();
					seriesData.setId(seriesContent);
					//seriesData.setName(getDisplayName(seriesKey));
					seriesData.setName(seriesKey);
					seriesData.setData(seriesValue.get(seriesContent));
					if (chartSeriesList == null) {
						chartSeriesList = new ArrayList<ChartSeriesData>();
					}
					chartSeriesList.add(seriesData);
				}
			}

			chartData.setChartSeriesList(chartSeriesList);

			if (chartSeriesList != null) {
				setChartDetails(chartSeriesList, chartWindow);
			}
			chartData.setChartLegendData(populateChartLegendData((XYChartWindow) window));
			chartData.setChartSeriesFields(((XYChartWindow) window).getChartSeries().getChartSeriesFields());
		} catch (Exception e) {
			logger.error(XYChartDataRenderer.class, e.getMessage());
		}

		return chartData;
	}

	private void setChartDetails(List<ChartSeriesData> chartSeriesList, XYChartWindow chartWindow) {
		ChartSeries series = chartWindow.getChartSeries();
		Map<String, ChartColumnSeries> chartSeriesFields = series.getChartSeriesFields();
		if (chartSeriesFields == null) {
			chartSeriesFields = new HashMap<String, ChartColumnSeries>();
		}
		for (ChartSeriesData chartSeriesData : chartSeriesList) {
			String seriesName = chartSeriesData.getName();
			if (chartSeriesFields.containsKey(seriesName)) {
				continue;
			}
			ChartColumnSeries chartSeriesField = new ChartColumnSeries();
			chartSeriesField.setSeriesName(seriesName);
			chartSeriesField.setSeriesChartType(chartWindow.getChartDetails().getChartType());
			chartSeriesField.setSeriesEnabled(true);
			chartSeriesField.setVerticalAxis(0);
			//Color color = new Color(ColorUtils.getRandomColor());
			chartSeriesField.setSeriesColor("#" + ColorUtils.toHex(new Random().nextInt(256)) + ColorUtils.toHex(new Random().nextInt(256))
					+ ColorUtils.toHex(new Random().nextInt(256)));
			chartSeriesFields.put(seriesName, chartSeriesField);
		}
		series.setChartSeriesFields(chartSeriesFields);
	}

	/**
	 * 
	 * @param attributeMap map to store table data
	 * @param dataObject dataObject to get data
	 */
	private void addAdditionalTableColumns(Map<String, Object> attributeMap, IDataObject dataObject) {
		if (additionalTableColumns != null) {
			String columnName = null;
			for (Map.Entry<String, TableColumn> tableColumn : additionalTableColumns.entrySet()) {
				columnName = tableColumn.getKey();
				attributeMap.put(columnName,
						dataObject.getFieldValue(columnName) == null ? CommonConstants.EMPTY_STRING : dataObject.getFieldValue(columnName));
			}
		}
	}

}
