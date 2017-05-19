package com.spacetimeinsight.app.sample;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.spacetimeinsight.si.common.EGeoFieldType;
import com.spacetimeinsight.si.dataobject.IDataObject;
import com.spacetimeinsight.si.datasink.IDataSink;
import com.spacetimeinsight.si.datasource.IDataSource;
import com.spacetimeinsight.si.db.helper.DBGeometryHelper;
import com.spacetimeinsight.si.db.helper.oracle.OracleGeometryHelper;
import com.spacetimeinsight.si.geometry.utils.GeometryParser;
import com.spacetimeinsight.si.geometry.utils.GeometryUtils;
import com.spacetimeinsight.si.logging.ILogger;
import com.spacetimeinsight.si.mapfeature.IMapFeature;
import com.spacetimeinsight.si.transformer.IAmendFieldTransformer;
import com.spacetimeinsight.si.transformer.IJoinTransformer;
import com.spacetimeinsight.si.transformer.ITransformer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class SampleAppUtils {

	static DBGeometryHelper geometryHelper = new DBGeometryHelper();

	@Autowired
	private static ILogger logger;

	public static void afterAmendTransformation(
			IAmendFieldTransformer amendTransformer) {
		System.out.println("after amendTransformer..." + amendTransformer);
	}

	public static void afterAmendTransformationRowUpdate(IDataObject dataObject) {
		System.out.println("after amendTransformer..." + dataObject);
	}

	public static void afterDataRetrieval() {
		System.out.println("after retrieving data...");
	}

	public static void afterDataRetrieval(IDataSource datasource) {
		System.out.println("after retrieving data..." + datasource);
	}

	public static void afterDedupeTransformation(ITransformer transformer) {
		System.out.println("after dedube..." + transformer);
	}

	public static void afterFilterTransformation(ITransformer transformer) {
		System.out.println("after filter..." + transformer);
	}

	public static void afterFilterTransformationRowUpdate(IDataObject dataObject) {
		System.out.println("after amendTransformer..." + dataObject);
	}

	public static void afterJoinTransformation(IJoinTransformer joinTransformer) {
		System.out.println("after joinTransformer..." + joinTransformer);
	}

	public static void afterMapFeatureGeneration(IMapFeature<?> mapFeature) {
		System.out.println("after map feature generation..." + mapFeature);
	}

	public static void afterMapFeatureLayerGeneration(IMapFeature<?> mapFeature) {
		System.out.println("after map feature generation..." + mapFeature);
	}

	public static void afterRowUpdate(IDataObject dataObject) {
		System.out.println("after row update..." + dataObject);
	}

	public static void afterTableUpdate(IDataSink dataSink) {
		System.out.println("after table update...");
	}

	public static void afterUnionTransformation(ITransformer transformer) {
		System.out.println("after union..." + transformer);
	}

	public static void beforeAmendTransformation(
			IAmendFieldTransformer amendTransformer) {
		System.out.println("before amendTransformer..." + amendTransformer);
	}

	public static void beforeAmendTransformationRowUpdate(IDataObject dataObject) {
		System.out.println("before amendTransformer..." + dataObject);
	}

	public static void beforeDataRetrieval() {
		System.out.println("before retrieving data...");
	}

	public static void beforeDataRetrieval(IDataSource datasource) {
		System.out.println("before retrieving data..." + datasource);
	}

	public static void beforeDedupeTransformation(ITransformer transformer) {
		System.out.println("before dedupe..." + transformer);
	}

	public static void beforeFilterTransformation(ITransformer transformer) {
		System.out.println("before filer..." + transformer);
	}

	public static void beforeFilterTransformationRowUpdate(
			IDataObject dataObject) {
		System.out.println("before amendTransformer..." + dataObject);
	}

	public static void beforeJoinTransformation(IJoinTransformer joinTransformer) {
		System.out.println("before joinTransformer..." + joinTransformer);
	}

	public static void beforeMapFeatureGeneration() {
		System.out.println("before map feature generation...");
	}

	public static void beforeMapFeatureLayerGeneration() {
		System.out.println("before map feature generation...");
	}

	public static void beforeRowUpdate(IDataObject dataObject) {
		System.out.println("before row update..." + dataObject);
	}

	public static void beforeTableUpdate(IDataSink dataSink) {
		System.out.println("before table update...");
	}

	public static void beforeUnionTransformation(ITransformer transformer) {
		System.out.println("before union..." + transformer);
	}	
	

	public static Geometry getGeom(IDataObject IDataObj,
			boolean preserveToplogy, Double distanceTolerance) {
		Geometry geom = (Geometry) IDataObj.getFieldValue("geometry");

		System.out.println("before simplify: " + geom.toText().length());

		if (preserveToplogy) {
			geom = TopologyPreservingSimplifier.simplify(geom,
					distanceTolerance);
		} else {
			geom = DouglasPeuckerSimplifier.simplify(geom, distanceTolerance);
		}

		System.out.println("after simplify: " + geom.toText().length());
		return geom;
	}

	public static void getAssetID(IDataObject IDataObj) {
		System.out.println("getting AssetID  services" + IDataObj);

		System.out.println("getting AssetID  services"
				+ IDataObj.getFieldValue("asset_id"));
		System.out.println("getting Geom  services"
				+ IDataObj.getFieldValue("geometry"));
		System.out.println("getting Geom Field  services"
				+ IDataObj.getGeometryFieldName());
		System.out.println("getting Geom Object  services"
				+ IDataObj.getGeometry());

		// return obj;
	}

	public static String getStyleId(IDataObject dataObject) {
		return "High".equalsIgnoreCase((String) dataObject
				.getFieldValue("priority")) ? "HS" : "NS";
	}

	public static Date formatJSONDate(IDataObject iDataObj) {

		Object obj = iDataObj.getFieldValue("date");
		System.out.println(obj);
		return new Date();
	}

	public static Geometry getGeomText(IDataObject iDataObj) {
		Geometry geom = null;
		try {
			geom = geometryHelper.toGeometry(iDataObj.getFieldValue("coords"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return geom;
	}

}
