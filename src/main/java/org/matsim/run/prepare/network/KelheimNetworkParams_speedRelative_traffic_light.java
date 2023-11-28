package org.matsim.run.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class KelheimNetworkParams_speedRelative_traffic_light implements FeatureRegressor {
    
    public static KelheimNetworkParams_speedRelative_traffic_light INSTANCE = new KelheimNetworkParams_speedRelative_traffic_light();
    public static final double[] DEFAULT_PARAMS = {0.3809090909090909, 0.2523020833333333, 0.3263333333333333, 0.43412121212121213, 0.5506666666666666};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 90.194) / 65.6979546409171;
		data[1] = (ft.getDouble("speed") - 13.334) / 1.6680000000000001;
		data[2] = (ft.getDouble("num_lanes") - 1.5) / 0.6708203932499369;
		data[3] = ft.getDouble("change_speed");
		data[4] = ft.getDouble("change_num_lanes");
		data[5] = ft.getDouble("num_to_links");
		data[6] = ft.getDouble("junction_inc_lanes");
		data[7] = ft.getDouble("priority_lower");
		data[8] = ft.getDouble("priority_equal");
		data[9] = ft.getDouble("priority_higher");
		data[10] = ft.getDouble("is_secondary_or_higher");
		data[11] = ft.getDouble("is_primary_or_higher");
		data[12] = ft.getDouble("is_motorway");
		data[13] = ft.getDouble("is_link");

        return data;
    }
    
    @Override
    public double predict(Object2DoubleMap<String> ft, double[] params) {

        double[] data = getData(ft);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);
    
        return score(data, params);
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[0] <= -0.07578622736036777) {
            if (input[4] <= -1.5) {
                var0 = params[0];
            } else {
                if (input[8] <= 0.5) {
                    var0 = params[1];
                } else {
                    var0 = params[2];
                }
            }
        } else {
            if (input[0] <= 0.9341386556625366) {
                var0 = params[3];
            } else {
                var0 = params[4];
            }
        }
        return var0;
    }
}
