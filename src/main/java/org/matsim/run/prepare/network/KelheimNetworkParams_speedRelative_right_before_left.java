package org.matsim.run.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class KelheimNetworkParams_speedRelative_right_before_left implements FeatureRegressor {
    
    public static KelheimNetworkParams_speedRelative_right_before_left INSTANCE = new KelheimNetworkParams_speedRelative_right_before_left();
    public static final double[] DEFAULT_PARAMS = {0.9127822238271982, 0.8937512077294687, 0.9214581913246914, 0.8695833333333334, 0.9320463959103764, 0.9430751163088117, 0.86};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 90.22617266187049) / 60.92023402293393;
		data[1] = (ft.getDouble("speed") - 8.338) / 0.21075103795711186;
		data[2] = (ft.getDouble("num_lanes") - 1.0) / 1.0;
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
        if (input[0] <= 0.3752255290746689) {
            if (input[0] <= -0.6277253329753876) {
                if (input[6] <= 3.5) {
                    var0 = params[0];
                } else {
                    var0 = params[1];
                }
            } else {
                if (input[5] <= 3.5) {
                    var0 = params[2];
                } else {
                    var0 = params[3];
                }
            }
        } else {
            if (input[1] <= 13.152960108593106) {
                if (input[0] <= 1.0658007264137268) {
                    var0 = params[4];
                } else {
                    var0 = params[5];
                }
            } else {
                var0 = params[6];
            }
        }
        return var0;
    }
}
